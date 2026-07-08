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
# Global state
active_connections: Dict[str, Dict[str, Any]] = {}
pending_actions: Dict[str, asyncio.Future] = {}

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
    
    conn = active_connections.get(user_id, {})
    android_ws = conn.get("android")
    telegram_chat = conn.get("telegram")
    capabilities = conn.get("capabilities", [])
    
    # Send to AI brain (Planner)
    reply_dict = await asyncio.to_thread(memory_engine.process_request, user_id, text, capabilities)
    
    reply_type = reply_dict.get("type", "chat")
    reply_text = reply_dict.get("text", "")
    
    # If the response dictates an action, we MUST route it to the Android client
    if reply_type == "action" and android_ws:
        action_name = reply_dict.get("action_id", "NONE")
        params = reply_dict.get("params", {})
        
        action_req = {
            "type": "action_request",
            "request_id": request_id,
            "user_id": user_id,
            "payload": {
                "actions": [
                    {
                        "action_id": f"act_{uuid.uuid4().hex[:8]}",
                        "action": action_name,
                        "params": params
                    }
                ]
            }
        }
        try:
            # Create a future to wait for the action_result
            future = asyncio.get_running_loop().create_future()
            pending_actions[request_id] = future
            
            await android_ws.send_text(json.dumps(action_req))
            print(f"Sent action_request to Android for {user_id}. Waiting for result...")
            
            # Wait for action_result with a 15-second timeout
            result_payload = await asyncio.wait_for(future, timeout=15.0)
            status = result_payload.get("results", [{}])[0].get("status", "unknown")
            if status == "success":
                pass # reply_text is already correct
            else:
                error_msg = result_payload.get("results", [{}])[0].get("error", "Unknown error")
                reply_text += f"\n[Action failed: {error_msg}]"
                
        except asyncio.TimeoutError:
            print(f"Action {request_id} timed out")
            reply_text = "I sent the command, but the phone timed out before confirming."
        except Exception as e:
            print(f"Failed to send action to Android: {e}")
            reply_text = "I tried to perform the action, but your phone seems disconnected."
        finally:
            pending_actions.pop(request_id, None)
            
    elif reply_type == "action" and not android_ws:
        # Planner output action but phone is not connected (capability mismatch catch-all)
        reply_text = "I cannot perform that action right now because your phone is offline."

    # Determine where to send the final conversational response
    if client_type == "telegram":
        await send_telegram_message(telegram_chat or TELEGRAM_CHAT_ID, f"[Cloud Brain] {reply_text}")
    elif client_type == "android":
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
                await android_ws.send_text(json.dumps(final_response))
            except Exception as e:
                print(f"Failed to send final_response to Android: {e}")
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
                    auth_token = payload.get("auth_token")
                    
                    if auth_token != APP_SECRET:
                        print(f"Invalid auth_token in handshake for user {current_user_id}")
                        await websocket.close(code=1008, reason="Invalid Auth Token")
                        return
                    
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
                    req_id = payload.get("request_id")
                    if req_id and req_id in pending_actions:
                        future = pending_actions[req_id]
                        if not future.done():
                            future.set_result(payload.get("payload", {}))
                    
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
