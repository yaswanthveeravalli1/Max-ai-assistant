# 🤖 Jarvis — Autonomous Android Assistant with Long-Term Memory

**Jarvis** is an intelligent, autonomous personal AI assistant built for Android using Kotlin, Jetpack Compose, and the **Google Gemini API**. 

Unlike standard conversational assistants that only respond with text, Jarvis features a highly capable **Accessibility-driven Automation Engine** that can interact with the Android operating system to perform tasks directly inside other apps (such as WhatsApp, Telegram, and Instagram) on your behalf.

---

## 💡 Core Project Idea

The core concept behind **Jarvis** is bridging the gap between conversational AI and device control. By leveraging Android's **Accessibility Service framework** and the advanced reasoning capabilities of **Gemini**, Jarvis acts as a digital agent. It listens to natural language prompts, determines if they require local device automation, generates a precise JSON-based execution plan, and executes those steps (clicks, text input, navigation) directly on the screen.

---

## 🚀 Key Features

### 🧠 1. Intelligent AI Brain (Dual-Mode Engine & App Resolver)
* **App Brain Architecture**: Core engine that immediately parses commands and resolves them via natural language to direct app intents (e.g., WhatsApp, Instagram, Telegram).
* **Local AI Integration**: Run local requests directly with the **Google Gemini API**. Override and secure your Google AI Studio API key directly from the Settings interface.
* **Groq Turbo Mode**: Paste a Groq API key (`gsk_...`) in the settings to automatically route prompts to Groq's insanely fast cloud for near-zero latency voice interactions (using `llama-3.3-70b-versatile`).
* **Server AI Fallback**: Support pre-configured cloud endpoints for zero-friction boarding.
* **Conversational Fluency**: A witty, highly helpful, and charming personal assistant persona.

### 🎤 2. Voice Control & Continuous Wake Detection
* **Continuous Listening**: Uses a robust continuous `SpeechRecognizer` loop to maintain an active listening state, automatically resuming after minor errors.
* **Wake Word Trigger**: Say a command containing "Jarvis" (e.g., "Hey Jarvis, send WhatsApp to Bannu"), and Jarvis instantly parses your intent and executes the command.
* **Auto-Execution**: Speeds up the interaction flow by avoiding manual button clicks, routing your speech directly into the App Brain.

### ⚡ 3. Autonomous Device Automation (Dynamic Actions)
Using a custom-built, event-driven Accessibility service, Jarvis can execute complex multi-step UI flows on your Android device. It parses incoming conversational instructions and breaks them down into dynamic action plans:
* **Dynamic App Resolver**: Jarvis scans your installed applications dynamically and launches apps strictly by name using intelligent matching (`QUERY_ALL_PACKAGES`).
* **Direct Phone Calls**: Say "Call [Name]" or "Call [Number]". Jarvis securely queries your contacts and immediately initiates a phone call.
* **`CLICK_ID` / `CLICK_TEXT`**: Scans the active viewport and performs click gestures on elements matching target IDs or text strings.
* **`TYPE_TEXT`**: Inputs text into specified text fields or active input focuses.
* **`WAIT`**: Suspends execution for exact millisecond intervals to accommodate network and transition delays.
* **Smart UI Settling**: Features post-step delay stabilization (e.g., waiting for screen renders or keyboard transitions) to ensure highly reliable click and input accuracy.
* **Package Fallbacks**: Intelligently handles UI changes or variation between app versions (e.g., toggling between WhatsApp's `search_input` and `search_src_text`).

### 🤖 4. WhatsApp Auto-Reply Engine (SKEDit Hybrid System)
* **Rule-Based Engine**: Define high-precision keyword-trigger rules (e.g. if an incoming message contains "pricing", reply with price sheets).
* **Supercharged AI Fallback**: Leverages low-latency Gemini/Groq APIs to automatically draft and send friendly, personalized responses if no keyword rules match.
* **Fast-Path RemoteInput**: Sends replies back natively via Android's notification quick-reply system in milliseconds.
* **Accessibility UI Fallback**: If the notification is dismissed, it triggers the Accessibility engine to launch the chat via deep link, type, and tap send.

### 🔍 5. Live Screen Debug & Node Dumper
* **Live Screen Hierarchy Dump**: Perfect for analyzing and testing. Tap **Dump Screen (JSON)** inside Settings to analyze the raw active node structure of whatever app is in the foreground.
* **Target Elements Discovery**: Safely locate element IDs, class names, or text labels to write or prompt precise automation steps.

### 📅 6. Natural Language Reminders
* **Dynamic Scheduler**: Instruct Jarvis to "remind me to play chess in 10 seconds" or schedule calendar alerts.
* **Local Reminder Manager**: Jarvis parses time offsets directly from natural language and fires local system alerts.

### 💾 7. Long-Term Memory System
* **Context Preservation**: Jarvis remembers key facts, user preferences, and custom instructions over long sessions.
* **Memory Management**: Easily check what Jarvis remembers, or tell him to "forget everything" using memory controls in the dashboard.

---

## 🛠️ Architecture & Under the Hood

### 1. The Accessibility Engine (`JarvisAccessibilityService.kt`)
The heart of Jarvis's automation is a background Accessibility service declared with `android.permission.BIND_ACCESSIBILITY_SERVICE`.
* **Configured for Deep Scanning**: Using `flagRetrieveInteractiveWindows` and `flagReportViewIds`, Jarvis is granted visibility to see layout nodes in active packages.
* **Event-Driven Execution**: Wakes up only on crucial triggers (`TYPE_WINDOW_STATE_CHANGED`, `TYPE_WINDOWS_CHANGED`, `TYPE_WINDOW_CONTENT_CHANGED`) to preserve battery life and memory overhead.
* **Failsafe Timeout**: Automatically aborts automation tasks if they exceed 20 seconds to prevent infinite loops or UI lockups.

### 2. Conversational Intent Processing (`JarvisRepository.kt`)
Whenever you send a chat message, Jarvis determines if the task requires phone automation:
1. Jarvis requests a structured response from **Gemini**.
2. Gemini responds with a strict, valid JSON format containing:
   * `is_automation`: Boolean flag.
   * `speech_reply`: Conversational feedback spoken to the user.
   * `steps`: Ordered list of UI steps (`OPEN_APP`, `CLICK_ID`, `TYPE_TEXT`, etc.).
3. The applet reads the schema, registers a `pendingTask`, launches the target app, and passes execution to the background Accessibility service.

---

## 🚀 Getting Started & Setup

To experience Jarvis's full capabilities on your device, follow these configuration steps:

### Step 1: Enable Accessibility Service
1. On your Android device, go to **Settings** ➔ **Accessibility**.
2. Locate **Jarvis Automation** in the installed services list.
3. Toggle it **ON** and accept the permissions dialog.

### Step 2: Set Up your Gemini API Key
1. Obtain an API key from [Google AI Studio](https://aistudio.google.com/).
2. Open the **Settings** tab inside the Jarvis app.
3. Toggle on **Local AI Mode**.
4. Enter your API Key in the **API Key Override** field and tap **Save API Key**.

### Step 3: Trigger an Automation
In the **Chat** tab, try sending an automation command like:
> *"Send a WhatsApp message to Bannu saying Hii"*

### Step 4: Use the Screen Dumper for Debugging
1. Open the app you want to automate (e.g., WhatsApp).
2. Switch back to **Jarvis** Settings.
3. Scroll down to **Automation Debug Screen**.
4. Click **Dump Screen (JSON)** to print the active screen's layout nodes, helping you identify exact view IDs!
