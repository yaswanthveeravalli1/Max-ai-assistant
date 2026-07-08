import os
import asyncio
import httpx
import json
import uuid
import time
from typing import Dict, Any
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Query
from memory_engine import MemoryEngine

app = FastAPI()
memory_engine = MemoryEngine()

TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")
TELEGRAM_CHAT_ID = os.getenv("TELEGRAM_CHAT_ID", "")
APP_SECRET = os.getenv("APP_SECRET", "default_secret_key")

# Connection registry
# Format: user_id -> {"android": WebSocket, "telegram": chat_id, "capabilities": [...]}
active_connections: Dict[str, Dict[str, Any]] = {}

async def send_telegram_message(chat_id: str, text: str):
    url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/sendMessage"
    payload = {"chat_id": chat_id, "text": text}
    async with httpx.AsyncClient() as client:
        try:
            await client.post(url, json=payload)
        except Exception as e:
            print(f"Error sending message to Telegram: {e}")

async def process_message_from_client(user_id: str, client_type: str, text: str, request_id: str = None):
    """Core function that takes input from ANY client and routes it through the AI brain."""
    if not request_id:
        request_id = f"req_{uuid.uuid4().hex[:8]}"
        
    print(f"[{client_type.upper()}] user:{user_id} req:{request_id} -> {text}")
    
    # Send to AI brain (MemoryEngine)
    reply_text = await asyncio.to_thread(memory_engine.answer_question, text)
    
    conn = active_connections.get(user_id, {})
    android_ws = conn.get("android")
    telegram_chat = conn.get("telegram")
    
    # Determine where to send the final response
    if client_type == "telegram":
        # Reply to Telegram
        await send_telegram_message(telegram_chat or TELEGRAM_CHAT_ID, f"[Cloud Brain] {reply_text}")
    elif client_type == "android":
        # Reply to Android
        if android_ws:
            final_response = {
                "type": "final_response",
                "request_id": request_id,
                "user_id": user_id,
                "payload": {
                    "text": reply_text,
                    "speak": True
                }
            }
            try:
                # If we received JSON, we reply with JSON.
                # However, if it's the old app, it will fail to parse the JSON.
                # Since we are implementing Phase 1, we will send JSON. We will fix the Android app in Phase 2.
                await android_ws.send_text(json.dumps(final_response))
            except Exception as e:
                print(f"Failed to send to Android: {e}")
                # Fallback to Telegram if Android died mid-request
                if telegram_chat or TELEGRAM_CHAT_ID:
                    await send_telegram_message(telegram_chat or TELEGRAM_CHAT_ID, f"[Android Offline] {reply_text}")

async def poll_telegram():
    if not TELEGRAM_BOT_TOKEN:
        print("No TELEGRAM_BOT_TOKEN provided. Polling disabled.")
        return
        
    last_update_id = 0
    url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/getUpdates"
    
    # Default user for Telegram until we support multi-user registration
    default_user_id = "default_user"
    if default_user_id not in active_connections:
        active_connections[default_user_id] = {}
    active_connections[default_user_id]["telegram"] = TELEGRAM_CHAT_ID
    
    async with httpx.AsyncClient(timeout=60.0) as client:
        while True:
            try:
                response = await client.get(url, params={"offset": last_update_id, "timeout": 50})
                if response.status_code == 200:
                    data = response.json()
                    if data.get("ok"):
                        results = data.get("result", [])
                        for update in results:
                            update_id = update.get("update_id")
                            last_update_id = update_id + 1
                            
                            message = update.get("message")
                            if message:
                                chat_id = str(message.get("chat", {}).get("id", ""))
                                text = message.get("text", "")
                                
                                if text:
                                    if chat_id == TELEGRAM_CHAT_ID:
                                        # Process through central brain
                                        asyncio.create_task(process_message_from_client(
                                            user_id=default_user_id,
                                            client_type="telegram",
                                            text=text
                                        ))
                                    else:
                                        print(f"Unauthorized access attempt from Chat ID: {chat_id}")
                                        await send_telegram_message(chat_id, f"Unauthorized! Your Chat ID is: {chat_id}")
                    else:
                        print(f"Telegram API Error: {data}")
                        await asyncio.sleep(5)
                else:
                    print(f"HTTP Error: {response.status_code}")
                    await asyncio.sleep(5)
            except Exception as e:
                print(f"Polling Exception: {e}")
                await asyncio.sleep(5)
            
            await asyncio.sleep(1)

@app.on_event("startup")
async def startup_event():
    asyncio.create_task(asyncio.to_thread(memory_engine.build_index))
    asyncio.create_task(poll_telegram())

@app.get("/")
async def root():
    return {"status": "MAX Telegram Cloud Bot is running.", "connections": len(active_connections)}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket, secret: str = Query(default=None)):
    print(f"WebSocket connection attempt. Secret provided: {secret is not None}")
    
    if secret != APP_SECRET:
        print(f"Invalid secret! Expected '{APP_SECRET}', got '{secret}'")
        await websocket.close(code=1008, reason="Invalid Secret Key")
        return
        
    await websocket.accept()
    
    # We don't know who this is until the handshake
    current_user_id = None
    
    try:
        while True:
            data_str = await websocket.receive_text()
            
            try:
                payload = json.loads(data_str)
                msg_type = payload.get("type")
                
                if msg_type == "handshake":
                    current_user_id = payload.get("user_id", "default_user")
                    capabilities = payload.get("capabilities", [])
                    
                    if current_user_id not in active_connections:
                        active_connections[current_user_id] = {}
                        
                    # Handle existing connection
                    existing_ws = active_connections[current_user_id].get("android")
                    if existing_ws and existing_ws != websocket:
                        try:
                            await existing_ws.close(code=1000, reason="New connection replaced this one")
                        except:
                            pass
                            
                    active_connections[current_user_id]["android"] = websocket
                    active_connections[current_user_id]["capabilities"] = capabilities
                    print(f"Handshake complete for user: {current_user_id}. Capabilities: {capabilities}")
                    
                elif msg_type == "user_message":
                    text = payload.get("payload", {}).get("text", "")
                    req_id = payload.get("request_id")
                    if text and current_user_id:
                        asyncio.create_task(process_message_from_client(
                            user_id=current_user_id,
                            client_type="android",
                            text=text,
                            request_id=req_id
                        ))
                        
                elif msg_type == "action_result":
                    print(f"Action result from {current_user_id}: {payload}")
                    # TODO: Phase 2 action correlation
                    
                elif msg_type == "pong":
                    pass # Heartbeat response
                    
            except json.JSONDecodeError:
                # Legacy fallback for old Android app until Phase 2 is deployed
                print(f"Received raw text from App (Legacy): {data_str}")
                current_user_id = "default_user"
                if current_user_id not in active_connections:
                    active_connections[current_user_id] = {}
                active_connections[current_user_id]["android"] = websocket
                
                # Treat raw text as a message from Android
                asyncio.create_task(process_message_from_client(
                    user_id=current_user_id,
                    client_type="android",
                    text=data_str
                ))

    except WebSocketDisconnect:
        print(f"App Disconnected (User: {current_user_id})")
        if current_user_id and current_user_id in active_connections:
            if active_connections[current_user_id].get("android") == websocket:
                active_connections[current_user_id]["android"] = None
    except Exception as e:
        print(f"WebSocket error: {e}")
        if current_user_id and current_user_id in active_connections:
            if active_connections[current_user_id].get("android") == websocket:
                active_connections[current_user_id]["android"] = None
