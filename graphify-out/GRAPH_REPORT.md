# Graph Report - .  (2026-07-07)

## Corpus Check
- Corpus is ~38,070 words - fits in a single context window. You may not need a graph.

## Summary
- 757 nodes · 980 edges · 114 communities (97 shown, 17 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 44 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 9
- Community 10
- Community 11
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16
- Community 17
- Community 18
- Community 19
- Community 20
- Community 21
- Community 22
- Community 23
- Community 24
- Community 25
- Community 26
- Community 27
- Community 28
- Community 29
- Community 30
- Community 31
- Community 32
- Community 33
- Community 34
- Community 35
- Community 36
- Community 37
- Community 38
- Community 39
- Community 40
- Community 41
- Community 42
- Community 43
- Community 44
- Community 45
- Community 46
- Community 47
- Community 48
- Community 49
- Community 50
- Community 51
- Community 52
- Community 53
- Community 54
- Community 55
- Community 56
- Community 57
- Community 58
- Community 59
- Community 60
- Community 61

## God Nodes (most connected - your core abstractions)
1. `JarvisViewModel` - 47 edges
2. `JarvisAccessibilityService` - 26 edges
3. `JarvisRepository` - 25 edges
4. `VoiceSessionManager` - 23 edges
5. `BaseAction` - 14 edges
6. `AutomationTask` - 14 edges
7. `CustomLifecycleOwner` - 13 edges
8. `MaxLockAutomationEngine` - 12 edges
9. `Reminder` - 11 edges
10. `DynamicIslandManager` - 11 edges

## Surprising Connections (you probably didn't know these)
- `ServiceLocator` --references--> `JarvisRepository`  [EXTRACTED]
  app/src/main/java/com/example/automation/AutoReplyController.kt → app/src/main/java/com/example/data/repository/JarvisRepository.kt
- `MainScreen()` --calls--> `ChatTab()`  [INFERRED]
  app/src/main/java/com/example/ui/screens/MainScreen.kt → app/src/main/java/com/example/ui/screens/ChatTab.kt
- `AccessibilityStepsAction` --inherits--> `BaseAction`  [EXTRACTED]
  app/src/main/java/com/example/automation/actions/AccessibilityStepsAction.kt → app/src/main/java/com/example/automation/actions/BaseAction.kt
- `BaseAction` --implements--> `JarvisAction`  [EXTRACTED]
  app/src/main/java/com/example/automation/actions/BaseAction.kt → app/src/main/java/com/example/automation/actions/JarvisAction.kt
- `DirectAutomationAction` --inherits--> `BaseAction`  [EXTRACTED]
  app/src/main/java/com/example/automation/actions/DirectAutomationAction.kt → app/src/main/java/com/example/automation/actions/BaseAction.kt

## Import Cycles
- None detected.

## Communities (114 total, 17 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.05
Nodes (24): android, AndroidViewModel, Modifier, MainScreen(), OnboardingScreen(), AddReminderDialog(), Modifier, String (+16 more)

### Community 1 - "Community 1"
Cohesion: 0.05
Nodes (37): AIBrainRouter, Context, JSONObject, String, AIProvider, Context, JSONObject, String (+29 more)

### Community 2 - "Community 2"
Cohesion: 0.09
Nodes (19): AccessibilityEvent, AccessibilityService, AccessibilityStepsAction, Context, AutomationStep, AutomationTask, JarvisAccessibilityService, AccessibilityNodeInfo (+11 more)

### Community 3 - "Community 3"
Cohesion: 0.08
Nodes (16): AutoReplyRule, Reminder, Flow, Int, List, Long, String, ReminderDao (+8 more)

### Community 4 - "Community 4"
Cohesion: 0.09
Nodes (21): CompactState(), DotsAnimation(), DynamicIslandManager, DynamicIslandUI(), ExpandedState(), String, ListeningState(), ProcessingState() (+13 more)

### Community 5 - "Community 5"
Cohesion: 0.07
Nodes (15): AppDatabase, getDatabase(), Context, AutoReplyRuleDao, Flow, Int, List, Long (+7 more)

### Community 6 - "Community 6"
Cohesion: 0.15
Nodes (12): Boolean, Context, SpeechRecognizer, StateFlow, String, State, VoiceSessionManager, File (+4 more)

### Community 7 - "Community 7"
Cohesion: 0.10
Nodes (10): Bundle, MainActivity, Boolean, MyApplicationTheme(), JarvisVoiceTriggerActivity, Bundle, SpeechRecognizer, VoiceListener (+2 more)

### Community 8 - "Community 8"
Cohesion: 0.10
Nodes (12): AppAutomation, Context, String, InstagramController, Context, String, Context, String (+4 more)

### Community 9 - "Community 9"
Cohesion: 0.12
Nodes (12): AutoReplyActionReceiver, Context, Intent, BootReceiver, Context, Intent, Context, Int (+4 more)

### Community 10 - "Community 10"
Cohesion: 0.26
Nodes (7): AccessibilityNodeInfo, Boolean, Float, String, Unit, MaxLockAutomationEngine, ScreenType

### Community 11 - "Community 11"
Cohesion: 0.17
Nodes (11): Any, JarvisApiService, String, RetrofitBuilder, ChatRequest, ChatResponse, CommonResponse, ForgetRequest (+3 more)

### Community 12 - "Community 12"
Cohesion: 0.17
Nodes (10): ConversationEngine, Boolean, Context, JSONObject, String, InputSource, JarvisCore, JarvisResponse (+2 more)

### Community 13 - "Community 13"
Cohesion: 0.19
Nodes (8): JarvisForegroundService, com, IBinder, Int, Intent, String, Notification, PowerManager

### Community 14 - "Community 14"
Cohesion: 0.22
Nodes (8): AutoReplyController, Boolean, Context, StatusBarNotification, String, MatchEngine, normalize(), ServiceLocator

### Community 15 - "Community 15"
Cohesion: 0.14
Nodes (7): CustomLifecycleOwner, Lifecycle, LifecycleOwner, SavedStateRegistry, SavedStateRegistryOwner, ViewModelStore, ViewModelStoreOwner

### Community 16 - "Community 16"
Cohesion: 0.18
Nodes (6): Boolean, Context, Int, String, VoiceOutputManager, TextToSpeech

### Community 17 - "Community 17"
Cohesion: 0.17
Nodes (8): JarvisVoiceSession, Bundle, Int, JarvisVoiceSessionService, Bundle, VoiceInteractionSession, VoiceInteractionSession, VoiceInteractionSessionService

### Community 18 - "Community 18"
Cohesion: 0.44
Nodes (4): Context, Int, String, VoiceDiagnosticLogger

### Community 19 - "Community 19"
Cohesion: 0.22
Nodes (6): Context, JSONObject, ScheduleMessageAction, ActionDispatcher, Context, JSONObject

### Community 20 - "Community 20"
Cohesion: 0.31
Nodes (3): String, TelegramManager, Job

### Community 21 - "Community 21"
Cohesion: 0.29
Nodes (5): Boolean, IShizukuShell, String, ShizukuShellPlugin, CountDownLatch

### Community 22 - "Community 22"
Cohesion: 0.39
Nodes (4): Boolean, Context, String, QuickActions

### Community 23 - "Community 23"
Cohesion: 0.32
Nodes (4): JarvisRecognitionService, Intent, Callback, RecognitionService

### Community 24 - "Community 24"
Cohesion: 0.43
Nodes (3): Activity, Boolean, ShizukuManager

### Community 25 - "Community 25"
Cohesion: 0.33
Nodes (4): BaseAction, String, T, Exception

### Community 26 - "Community 26"
Cohesion: 0.29
Nodes (5): Boolean, Context, StatusBarNotification, String, RemoteInputSender

### Community 27 - "Community 27"
Cohesion: 0.33
Nodes (5): Boolean, Long, SharedPreferences, String, SecuritySettings

### Community 29 - "Community 29"
Cohesion: 0.43
Nodes (4): IntentParser, String, ParsedRequest, Pair

### Community 30 - "Community 30"
Cohesion: 0.48
Nodes (3): Context, String, NotificationHelper

### Community 31 - "Community 31"
Cohesion: 0.40
Nodes (3): Boolean, String, ShizukuExecutor

### Community 32 - "Community 32"
Cohesion: 0.33
Nodes (3): String, ShizukuShellService, IShizukuShell

### Community 33 - "Community 33"
Cohesion: 0.33
Nodes (4): CustomAppIconManager, Context, String, Uri

### Community 34 - "Community 34"
Cohesion: 0.47
Nodes (3): JarvisServiceStateManager, Boolean, StateFlow

### Community 35 - "Community 35"
Cohesion: 0.33
Nodes (3): StatusBarNotification, WhatsAppNotificationService, NotificationListenerService

### Community 36 - "Community 36"
Cohesion: 0.50
Nodes (3): DirectAutomationAction, Context, JSONObject

### Community 37 - "Community 37"
Cohesion: 0.50
Nodes (3): FlashlightAction, Context, JSONObject

### Community 38 - "Community 38"
Cohesion: 0.40
Nodes (3): JarvisAction, Context, T

### Community 39 - "Community 39"
Cohesion: 0.50
Nodes (3): Context, JSONObject, OpenAppAction

### Community 40 - "Community 40"
Cohesion: 0.50
Nodes (3): Context, JSONObject, SendMessageAction

### Community 41 - "Community 41"
Cohesion: 0.50
Nodes (3): Context, JSONObject, ShizukuAction

### Community 42 - "Community 42"
Cohesion: 0.50
Nodes (3): Context, JSONObject, SystemAction

### Community 43 - "Community 43"
Cohesion: 0.50
Nodes (3): Context, JSONObject, TimeAction

### Community 44 - "Community 44"
Cohesion: 0.40
Nodes (3): AutomationEngine, Context, JSONObject

### Community 47 - "Community 47"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **9 isolated node(s):** `GeminiCandidate`, `GroqChoice`, `fix_compilation.sh script`, `generate_actions.sh script`, `patch_activity.sh script` (+4 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **17 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JarvisRepository` connect `Community 3` to `Community 9`, `Community 5`, `Community 14`?**
  _High betweenness centrality (0.186) - this node is a cross-community bridge._
- **Why does `Reminder` connect `Community 3` to `Community 0`, `Community 19`?**
  _High betweenness centrality (0.158) - this node is a cross-community bridge._
- **Why does `JarvisViewModel` connect `Community 0` to `Community 3`, `Community 4`, `Community 5`?**
  _High betweenness centrality (0.099) - this node is a cross-community bridge._
- **What connects `GeminiCandidate`, `GroqChoice`, `fix_compilation.sh script` to the rest of the system?**
  _9 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05310734463276836 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.050314465408805034 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.09158186864014801 - nodes in this community are weakly interconnected._