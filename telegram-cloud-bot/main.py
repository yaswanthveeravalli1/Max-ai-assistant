import os
import asyncio
import httpx
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Query
from memory_engine import MemoryEngine

app = FastAPI()
memory_engine = MemoryEngine()

TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")
TELEGRAM_CHAT_ID = os.getenv("TELEGRAM_CHAT_ID", "")
APP_SECRET = os.getenv("APP_SECRET", "default_secret_key")

active_connection: WebSocket = None

async def send_telegram_message(chat_id: str, text: str):
    url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/sendMessage"
    payload = {"chat_id": chat_id, "text": text}
    async with httpx.AsyncClient() as client:
        try:
            await client.post(url, json=payload)
        except Exception as e:
            print(f"Error sending message to Telegram: {e}")

async def poll_telegram():
    if not TELEGRAM_BOT_TOKEN:
        print("No TELEGRAM_BOT_TOKEN provided. Polling disabled.")
        return
        
    last_update_id = 0
    url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/getUpdates"
    
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
                                        print(f"Received from Telegram: {text}")
                                        if active_connection:
                                            try:
                                                # Forward to Android app via WebSocket
                                                await active_connection.send_text(text)
                                            except Exception as ws_err:
                                                # Phone disconnected but we didn't detect it yet
                                                print(f"Phone connection stale, clearing: {ws_err}")
                                                active_connection = None
                                                # Fall back to Cloud Mode
                                                print("Falling back to Cloud Mode...")
                                                offline_reply = await asyncio.to_thread(memory_engine.answer_question, text)
                                                print(f"Cloud Mode reply: {offline_reply[:100]}...")
                                                await send_telegram_message(chat_id, offline_reply)
                                        else:
                                            # App is offline, use Cloud Mode (Memory + Gemini)
                                            print("App offline. Generating Cloud Mode response...")
                                            offline_reply = await asyncio.to_thread(memory_engine.answer_question, text)
                                            print(f"Cloud Mode reply: {offline_reply[:100]}...")
                                            await send_telegram_message(chat_id, offline_reply)
                                    else:
                                        print(f"Unauthorized access attempt from Chat ID: {chat_id}")
                                        await send_telegram_message(chat_id, f"Unauthorized! Your Chat ID is: {chat_id}\n\nPlease enter this exact Chat ID in the MAX app settings to gain access.")
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
    # Build memory index in the background so it doesn't block startup
    asyncio.create_task(asyncio.to_thread(memory_engine.build_index))
    asyncio.create_task(poll_telegram())

@app.get("/")
async def root():
    return {"status": "MAX Telegram Cloud Bot is running."}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket, secret: str = Query(default=None)):
    global active_connection
    
    print(f"WebSocket connection attempt. Secret provided: {secret is not None}")
    
    if secret != APP_SECRET:
        print(f"Invalid secret! Expected '{APP_SECRET}', got '{secret}'")
        await websocket.close(code=1008, reason="Invalid Secret Key")
        return
        
    await websocket.accept()
    if active_connection:
        try:
            await active_connection.close()
        except:
            pass
            
    active_connection = websocket
    print("MAX App Connected successfully!")
    
    try:
        while True:
            # Receive response from Android app
            data = await websocket.receive_text()
            print(f"Received from App: {data}")
            # Forward back to Telegram
            if TELEGRAM_CHAT_ID:
                await send_telegram_message(TELEGRAM_CHAT_ID, data)
    except WebSocketDisconnect:
        print("MAX App Disconnected.")
        if active_connection == websocket:
            active_connection = None
    except Exception as e:
        print(f"WebSocket error: {e}")
        if active_connection == websocket:
            active_connection = None
