package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.utils.NotificationHelper

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
        var pendingTask: AutomationTask? = null
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isWaiting = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("JarvisAccessibility", "Accessibility service connected successfully.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UNLOCK_WITH_PIN") {
            val pin = intent.getStringExtra("pin")
            if (!pin.isNullOrEmpty()) {
                Log.d("JarvisAccessibility", "Received request to unlock with PIN")
                unlockWithPin(pin)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val task = pendingTask ?: return
        if (isWaiting) {
            return
        }
        val packageName = event?.packageName?.toString() ?: return

        // 1. Failsafe timeout check (20 seconds)
        if (System.currentTimeMillis() - task.startTime > 20000L) {
            Log.w("JarvisAccessibility", "Failsafe: Automation has timed out (20s limit). Cancelling.")
            Toast.makeText(this, "Jarvis Automation Timed Out ⚠️", Toast.LENGTH_SHORT).show()
            pendingTask = null
            return
        }

        // 2. Event Optimization: Wake up on window state, window list, or window content changes
        val eventType = event?.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && 
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        Log.d("JarvisAccessibility", "Processing event for package: $packageName, type: $eventType, task: ${task.type}")

        if (task.type == "GENERIC") {
            handleGenericAutomation(task)
            return
        }

        if (task.type == "SEND_MESSAGE_DEEP_LINK" && (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b")) {
            handleWhatsAppDeepLinkAutomation(task, packageName)
            return
        }

        if ((packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") && (task.appName == "WhatsApp" || task.appName == "WhatsApp Business")) {
            handleWhatsAppAutomation(task, packageName)
        } else if (packageName == "org.telegram.messenger" && task.appName == "Telegram") {
            handleTelegramAutomation(task)
        } else if (packageName == "com.instagram.android" && task.appName == "Instagram") {
            handleInstagramAutomation(task)
        }
    }

    private fun handleWhatsAppDeepLinkAutomation(task: AutomationTask, pkgName: String) {
        val root = rootInActiveWindow ?: return
        val contact = task.contact ?: return
        val message = task.message ?: return

        Log.d("JarvisAccessibility", "WhatsApp Deep Link Automation: step=${task.step}")

        when (task.step) {
            0 -> {
                // Wait for the chat UI to load
                val entryNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/entry")
                if (entryNodes.isEmpty()) {
                    if (System.currentTimeMillis() - task.startTime < 7000L) {
                        return
                    }
                }
                
                // Verify we are in the correct chat
                if (isCorrectChatOpen(root, contact, pkgName)) {
                    Log.d("JarvisAccessibility", "Correct chat detected for $contact, proceeding to send.")
                    val sendNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/send")
                    if (sendNodes.isNotEmpty()) {
                        sendNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d("JarvisAccessibility", "Deep link Send clicked successfully.")
                        pendingTask = null
                    } else {
                        Log.w("JarvisAccessibility", "Send button not found, falling back to notification.")
                        NotificationHelper.showSendFallbackNotification(this, contact, message)
                        pendingTask = null
                    }
                } else {
                    Log.w("JarvisAccessibility", "Safety Check Failed: Expected chat for '$contact' not confirmed. Aborting auto-send.")
                    NotificationHelper.showSendFallbackNotification(this, contact, message)
                    pendingTask = null
                }
            }
        }
    }

    private fun isCorrectChatOpen(root: AccessibilityNodeInfo, contact: String, pkgName: String): Boolean {
        val entryNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/entry")
        if (entryNodes.isEmpty()) {
            return false
        }
        
        // Remove trailing punctuation that speech-to-text might add
        val normalizedContact = contact.replace(Regex("[.,!?]\\s*$"), "").trim()
        val variants = mutableListOf(normalizedContact, contact)
        
        val cleanContact = contact.replace(Regex("[^0-9]"), "")
        if (cleanContact.isNotEmpty() && cleanContact.length >= 7) {
            variants.add(cleanContact)
            variants.add(cleanContact.takeLast(10))
            variants.add("+$cleanContact")
            variants.add("+91${cleanContact.takeLast(10)}")
            if (cleanContact.takeLast(10).length == 10) {
                val last10 = cleanContact.takeLast(10)
                variants.add("+91 ${last10.substring(0, 5)} ${last10.substring(5)}")
            }
        }

        for (variant in variants.distinct()) {
            if (variant.isBlank()) continue
            val textNodes = root.findAccessibilityNodeInfosByText(variant)
            for (node in textNodes) {
                if (node.packageName == pkgName) {
                    return true
                }
            }
        }
        
        Log.w("JarvisAccessibility", "Could not strictly verify contact name in header, but entry box found. Proceeding.")
        return true
    }

    private fun tapNodeWithGesture(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.isEmpty) return false

        // Screen width ni dynamically get chesthunnam
        val screenWidth = resources.displayMetrics.widthPixels
        
        // Nuvvu red mark pettina exact X coordinate (screen width lo 55%)
        val x = screenWidth * 0.55f 
        // Y coordinate vachi contact name text exact middle lo untundi
        val y = rect.centerY().toFloat()

        Log.d("GESTURE_DEBUG", "Tap x=$x y=$y bounds=$rect")
        
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(path, 0, 100)
            )
            .build()
        dispatchGesture(gesture, null, null)
        return true
    }

    private fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // --- WHATSAPP PROFILE PICTURE FIX ---
        // Parent row ACTION_CLICK chesthe WhatsApp profile picture open chesthundi.
        // Andhuke, direct ga contact name text node meedha exact screen coordinates tho 
        // gesture tap (nee custom function) ni first force chesthunnam.
        if (tapNodeWithGesture(node)) {
            Log.d("JarvisAccessibility", "Used precise gesture tap on text node directly.")
            return true
        }

        // --- ORIGINAL FALLBACK LOGIC ---
        if (node.isClickable) {
            val normalClick = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (normalClick) {
                return true
            }
            return tapNodeWithGesture(node)
        }
        
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val normalClick = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (normalClick) {
                    return true
                }
                return tapNodeWithGesture(parent)
            }
            parent = parent.parent
        }
        return false
    }

    private fun performClickOnNode(node: AccessibilityNodeInfo, forceGesture: Boolean = false): Boolean {
        if (forceGesture) {
            val gestureSuccess = tapNodeWithGesture(node)
            Log.d("CLICK_DEBUG", "Forced gesture tap directly on node: $gestureSuccess")
            if (gestureSuccess) return true
        }

        var current: AccessibilityNodeInfo? = node
        var depth = 0

        while (current != null && depth < 15) {
            val id = current.viewIdResourceName ?: "NO_ID"

            Log.d(
                "CLICK_DEBUG",
                "Depth=$depth clickable=${current.isClickable} id=$id text=${current.text}"
            )

            if (current.isClickable) {
                val normalClick = current.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )

                Log.d("CLICK_DEBUG", "CLICK RESULT = $normalClick at depth=$depth")
                if (normalClick) {
                    return true
                }
                
                return tapNodeWithGesture(current)
            }

            current = current.parent
            depth++
        }

        Log.e("CLICK_DEBUG", "No clickable parent found")
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        pendingTask = null
        handler.removeCallbacksAndMessages(null)
        Log.d("JarvisAccessibility", "Accessibility service destroyed and cleaned up.")
    }

    private fun findNodeByDescRecursive(node: AccessibilityNodeInfo, keyword: String, depth: Int = 0): AccessibilityNodeInfo? {
        if (depth > 25) return null // Safety depth limit
        if (node.contentDescription?.toString()?.contains(keyword, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByDescRecursive(child, keyword, depth + 1)
            if (found != null) return found
        }
        return null
    }

    private fun handleWhatsAppAutomation(task: AutomationTask, pkgName: String) {
        val root = rootInActiveWindow ?: return
        val contact = task.contact ?: return
        val message = task.message ?: return

        Log.d("JarvisAccessibility", "WhatsApp Automation State: pkg=$pkgName, step=${task.step}, contact=$contact")

        when (task.step) {
            0 -> { // Step 0: Find and click search icon
                // Initial delay to let the app load
                val timeElapsed = System.currentTimeMillis() - task.startTime
                if (timeElapsed < 1500L) {
                    return
                }

                var searchNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/menuitem_search")
                if (searchNodes.isEmpty()) {
                    searchNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/menu_item_search")
                }
                if (searchNodes.isEmpty()) {
                    searchNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/search")
                }
                if (searchNodes.isEmpty()) {
                    searchNodes = root.findAccessibilityNodeInfosByText("Search")
                }
                
                var targetNode = if (searchNodes.isNotEmpty()) searchNodes[0] else findNodeByDescRecursive(root, "search")

                if (targetNode != null) {
                    Log.d("JarvisAccessibility", "Step 0: Clicking WhatsApp search icon")
                    val clicked = performClickOnNode(targetNode)
                    if (clicked) {
                        task.step = 1
                    }
                } else {
                    // Check if search bar is already open (from a previous event)
                    var searchInputNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/search_src_text")
                    if (searchInputNodes.isEmpty()) {
                        searchInputNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/search_input")
                    }
                    if (searchInputNodes.isNotEmpty()) {
                        Log.d("JarvisAccessibility", "Step 0: Search bar already open, moving to Step 1")
                        task.step = 1
                    } else {
                        // Maybe we are already in the chat?
                        val messageBoxNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/entry")
                        if (messageBoxNodes.isNotEmpty()) {
                            // Check if it's the correct chat
                            if (isCorrectChatOpen(root, contact, pkgName)) {
                                Log.d("JarvisAccessibility", "Step 0: Correct chat already open, jumping to Step 3")
                                task.step = 3
                            } else {
                                // Wrong chat open, need to go back
                                val backNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/back")
                                var backButton = if (backNodes.isNotEmpty()) backNodes[0] else findNodeByDescRecursive(root, "Navigate up")
                                if (backButton == null) backButton = findNodeByDescRecursive(root, "Back")
                                if (backButton == null) {
                                    val navNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/navigation_icon")
                                    if (navNodes.isNotEmpty()) backButton = navNodes[0]
                                }
                                
                                if (backButton != null) {
                                    Log.d("JarvisAccessibility", "Step 0: Wrong chat open, clicking back UI button")
                                    performClickOnNode(backButton)
                                } else {
                                    Log.d("JarvisAccessibility", "Step 0: Back button not found, using global back action")
                                    performGlobalAction(GLOBAL_ACTION_BACK)
                                }
                            }
                        } else {
                            // If we've been here for more than 4 seconds and still no search, try to navigate back or switch tabs
                            if (timeElapsed > 4000L) {
                                // Try to find the "Chats" tab (sometimes we land on Updates or Calls)
                                val chatTabNodes = root.findAccessibilityNodeInfosByText("Chats")
                                if (chatTabNodes.isNotEmpty()) {
                                    Log.d("JarvisAccessibility", "Step 0: Stuck in non-home tab? Clicking 'Chats' tab")
                                    performClickOnNode(chatTabNodes[0])
                                } else {
                                    // Not in home screen and not in a chat? Try to go back anyway
                                    val backButton = findNodeByDescRecursive(root, "Navigate up") ?: 
                                                   findNodeByDescRecursive(root, "Back") ?:
                                                   root.findAccessibilityNodeInfosByViewId("$pkgName:id/back").firstOrNull() ?:
                                                   root.findAccessibilityNodeInfosByViewId("$pkgName:id/navigation_icon").firstOrNull()
                                    
                                    if (backButton != null) {
                                        Log.d("JarvisAccessibility", "Step 0: Search missing, attempting to navigate back to home screen")
                                        performClickOnNode(backButton)
                                    } else {
                                        // Last resort: check if we are in some other screen (Settings, etc.)
                                        Log.d("JarvisAccessibility", "Step 0: UI unknown, trying global back")
                                        performGlobalAction(GLOBAL_ACTION_BACK)
                                    }
                                }
                                // Reset start time slightly to avoid spamming back every event
                                task.startTime = System.currentTimeMillis() - 2000L 
                            }
                        }
                    }
                }
            }
            1 -> { // Step 1: Enter contact name in search box
                var searchInputNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/search_src_text")
                if (searchInputNodes.isEmpty()) {
                    searchInputNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/search_input")
                }
                if (searchInputNodes.isNotEmpty()) {
                    val currentText = searchInputNodes[0].text?.toString()
                    if (currentText == contact) {
                        Log.d("JarvisAccessibility", "Step 1: Contact name already entered, moving to Step 2")
                        task.step = 2
                    } else {
                        val args = Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, contact)
                        }
                        searchInputNodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        Log.d("JarvisAccessibility", "Step 1: Entered contact name: $contact")
                        task.step = 2 
                    }
                }
            }
            2 -> { // Step 2: Find and click the contact in search results
                Log.d("JarvisAccessibility", "Step 2: Searching for results for '$contact'")
                
                val currentRoot = rootInActiveWindow ?: return
                var clickedResult = false
                
                val nodes = currentRoot.findAccessibilityNodeInfosByText(contact)
                for (node in nodes) {
                    Log.d(
                        "RESULT_DEBUG",
                        "text=${node.text}, id=${node.viewIdResourceName}, clickable=${node.isClickable}"
                    )
                }
                
                for (node in nodes) {
                    val text = node.text?.toString()?.lowercase() ?: ""
                    val id = (node.viewIdResourceName ?: "").lowercase()
                    if (!id.contains("search_src_text") && !id.contains("search_input")) {
                        if (text.contains(contact.lowercase()) || contact.lowercase().contains(text)) {
                            val clicked = performClickOnNode(node, forceGesture = true)
                            if (clicked) {
                                Log.d("JarvisAccessibility", "Step 2: Clicked search result via text match for $contact (ID: ${node.viewIdResourceName})")
                                task.step = 3
                                clickedResult = true
                                break
                            }
                        }
                    }
                }
                
                if (clickedResult) return
                
                // Fallback: full screen scan (IMPORTANT)
                Log.d("JarvisAccessibility", "Step 2: Text search failed or didn't click. Running recursive full screen scan fallback.")
                var scanClicked = false
                fun scan(node: AccessibilityNodeInfo?) {
                    if (node == null || scanClicked) return

                    val text = node.text?.toString()?.lowercase() ?: ""
                    val id = (node.viewIdResourceName ?: "").lowercase()
                    if (!id.contains("search_src_text") && !id.contains("search_input")) {
                        if (text.contains(contact.lowercase())) {
                            if (performClickOnNode(node, forceGesture = true)) {
                                Log.d("JarvisAccessibility", "Step 2 Fallback: Clicked node via recursive scan: $text")
                                scanClicked = true
                                return
                            }
                        }
                    }

                    for (i in 0 until node.childCount) {
                        if (scanClicked) break
                        scan(node.getChild(i))
                    }
                }

                scan(currentRoot)
                if (scanClicked) {
                    task.step = 3
                    return
                }
                
                // Fallback 1: Specific WhatsApp Name IDs
                val nameIds = listOf("$pkgName:id/contact_name", "$pkgName:id/name", "$pkgName:id/conversations_row_contact_name")
                for (id in nameIds) {
                    val nameNodes = currentRoot.findAccessibilityNodeInfosByViewId(id)
                    if (nameNodes.isNotEmpty()) {
                        Log.d("JarvisAccessibility", "Step 2 Fallback 1: Clicking node with name ID: $id")
                        if (performClickOnNode(nameNodes[0], forceGesture = true)) {
                            task.step = 3
                            return
                        }
                    }
                }
                
                // Fallback 2: Try finding contact row but click the right side (name area) if possible
                val rowNodes = currentRoot.findAccessibilityNodeInfosByViewId("$pkgName:id/contact_row_container")
                if (rowNodes.isNotEmpty()) {
                    Log.d("JarvisAccessibility", "Step 2 Fallback 2: Examining contact row container")
                    val row = rowNodes[0]
                    // Try to find a child that isn't a photo
                    var clickedChild = false
                    for (i in 0 until row.childCount) {
                        val child = row.getChild(i) ?: continue
                        val childId = (child.viewIdResourceName ?: "").lowercase()
                        if (!childId.contains("photo") && !childId.contains("image") && !childId.contains("avatar")) {
                            if (performClickOnNode(child, forceGesture = true)) {
                                Log.d("JarvisAccessibility", "Step 2 Fallback 2: Clicked non-photo child of row")
                                clickedChild = true
                                break
                            }
                        }
                    }
                    if (clickedChild) {
                        task.step = 3
                        return
                    }
                    // If no child worked, click the row itself
                    if (performClickOnNode(row, forceGesture = true)) {
                        task.step = 3
                        return
                    }
                }
                
                // Fallback 2: Try finding any list item in the search results
                val listNodes = currentRoot.findAccessibilityNodeInfosByViewId("$pkgName:id/search_results_list")
                if (listNodes.isNotEmpty()) {
                    val list = listNodes[0]
                    Log.d("JarvisAccessibility", "Step 2 Fallback 2: Searching children of search_results_list (count: ${list.childCount})")
                    for (i in 0 until list.childCount) {
                        val child = list.getChild(i)
                        if (child != null && performClickOnNode(child, forceGesture = true)) {
                            Log.d("JarvisAccessibility", "Step 2 Fallback 2: Clicked child $i of search results list")
                            task.step = 3
                            return
                        }
                    }
                }

                // Fallback 3: Broad search for anything clickable that isn't the search bar
                // Sometimes results are just text views in a recycler view
                val searchListId = "$pkgName:id/list"
                val genericListNodes = currentRoot.findAccessibilityNodeInfosByViewId(searchListId)
                if (genericListNodes.isNotEmpty()) {
                    val list = genericListNodes[0]
                    for (i in 0 until list.childCount) {
                        val child = list.getChild(i)
                        if (child != null && performClickOnNode(child, forceGesture = true)) {
                            Log.d("JarvisAccessibility", "Step 2 Fallback 3: Clicked child $i of generic list")
                            task.step = 3
                            return
                        }
                    }
                }
                
                // Last ditch effort: if we see the message box, we somehow skipped to the chat
                if (currentRoot.findAccessibilityNodeInfosByViewId("$pkgName:id/entry").isNotEmpty()) {
                    Log.d("JarvisAccessibility", "Step 2: Message box found, moving to Step 3")
                    task.step = 3
                }
            }
            3 -> { // Step 3: Enter message and click send
                val messageBoxNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/entry")
                if (messageBoxNodes.isNotEmpty()) {
                    // Safety check: Ensure the correct contact is open before typing/sending
                    if (!isCorrectChatOpen(root, contact, pkgName)) {
                        Log.d("JarvisAccessibility", "Step 3 Verification: Expected chat for '$contact' not confirmed. Waiting for correct UI.")
                        return
                    }
                    
                    val currentText = messageBoxNodes[0].text?.toString()
                    if (currentText != message) {
                        val args = Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                        }
                        messageBoxNodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        Log.d("JarvisAccessibility", "Step 3: Entered message text")
                    }
                    
                    val sendButtonNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/send")
                    if (sendButtonNodes.isNotEmpty() && performClickOnNode(sendButtonNodes[0])) {
                        Log.d("JarvisAccessibility", "Step 3: Clicking WhatsApp send button")
                        completeTask(task)
                    } else {
                        val sendTextNodes = root.findAccessibilityNodeInfosByText("Send")
                        if (sendTextNodes.isNotEmpty()) {
                            if (performClickOnNode(sendTextNodes[0])) {
                                completeTask(task)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleTelegramAutomation(task: AutomationTask) {
        val root = rootInActiveWindow ?: return
        val contact = task.contact ?: return
        val message = task.message ?: return
        val pkgName = "org.telegram.messenger"

        Log.d("JarvisAccessibility", "Telegram Automation State: step=${task.step}, contact=$contact")

        when (task.step) {
            0 -> {
                val searchNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/search_button")
                if (searchNodes.isNotEmpty()) {
                    Log.d("JarvisAccessibility", "Step 0: Clicking Telegram search button")
                    searchNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    task.step = 1
                } else {
                    val searchEdit = root.findAccessibilityNodeInfosByViewId("$pkgName:id/search_edit")
                    if (searchEdit.isNotEmpty()) {
                        task.step = 1
                    }
                }
            }
            1 -> {
                val searchEdit = root.findAccessibilityNodeInfosByViewId("$pkgName:id/search_edit")
                if (searchEdit.isNotEmpty()) {
                    val currentText = searchEdit[0].text?.toString()
                    if (currentText == contact) {
                        task.step = 2
                    } else {
                        val args = Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, contact)
                        }
                        searchEdit[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        Log.d("JarvisAccessibility", "Step 1: Entered contact name in Telegram")
                        task.step = 2
                    }
                }
            }
            2 -> {
                val resultNodes = root.findAccessibilityNodeInfosByText(contact)
                if (resultNodes.isNotEmpty()) {
                    for (node in resultNodes) {
                        Log.d(
                            "RESULT_DEBUG",
                            "text=${node.text}, id=${node.viewIdResourceName}, clickable=${node.isClickable}"
                        )
                    }
                    for (node in resultNodes) {
                        val isInput = node.viewIdResourceName == "$pkgName:id/search_edit"
                        if (!isInput) {
                            var target = node
                            var depth = 0
                            while (!target.isClickable && depth < 3) {
                                target = target.parent ?: break
                                depth++
                            }
                            if (target.isClickable) {
                                Log.d("JarvisAccessibility", "Step 2: Clicking Telegram search result for $contact")
                                target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                task.step = 3
                                return
                            }
                        }
                    }
                }
                if (root.findAccessibilityNodeInfosByViewId("$pkgName:id/message_box").isNotEmpty()) {
                    task.step = 3
                }
            }
            3 -> {
                val messageBox = root.findAccessibilityNodeInfosByViewId("$pkgName:id/message_box")
                if (messageBox.isNotEmpty()) {
                    val currentText = messageBox[0].text?.toString()
                    if (currentText != message) {
                        val args = Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                        }
                        messageBox[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        Log.d("JarvisAccessibility", "Step 3: Message entered in Telegram")
                    }
                    val sendBtn = root.findAccessibilityNodeInfosByViewId("$pkgName:id/send_button")
                    if (sendBtn.isNotEmpty()) {
                        Log.d("JarvisAccessibility", "Step 3: Clicking Telegram send button")
                        sendBtn[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        completeTask(task)
                    }
                }
            }
        }
    }

    private fun handleInstagramAutomation(task: AutomationTask) {
        val root = rootInActiveWindow ?: return
        val contact = task.contact ?: return
        val message = task.message ?: return
        val pkgName = "com.instagram.android"

        Log.d("JarvisAccessibility", "Instagram Automation State: step=${task.step}, contact=$contact")

        when (task.step) {
            0 -> {
                val searchNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/search_row")
                if (searchNodes.isNotEmpty()) {
                    Log.d("JarvisAccessibility", "Step 0: Clicking Instagram search row")
                    searchNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    task.step = 1
                } else {
                    val textEditNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/row_thread_composer_edittext")
                    if (textEditNodes.isNotEmpty()) {
                        task.step = 3
                    }
                }
            }
            1 -> {
                task.step = 2
            }
            2 -> {
                val textEditNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/row_thread_composer_edittext")
                if (textEditNodes.isNotEmpty()) {
                    task.step = 3
                }
            }
            3 -> {
                val textEditNodes = root.findAccessibilityNodeInfosByViewId("$pkgName:id/row_thread_composer_edittext")
                if (textEditNodes.isNotEmpty()) {
                    val currentText = textEditNodes[0].text?.toString()
                    if (currentText != message) {
                        val args = Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                        }
                        textEditNodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        Log.d("JarvisAccessibility", "Step 3: Message entered in Instagram")
                    }
                    val sendButton = root.findAccessibilityNodeInfosByViewId("$pkgName:id/row_thread_composer_button_send")
                    if (sendButton.isNotEmpty()) {
                        Log.d("JarvisAccessibility", "Step 3: Clicking Instagram send button")
                        sendButton[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        completeTask(task)
                    }
                }
            }
        }
    }

    private fun findNodesWithFallback(root: AccessibilityNodeInfo, viewId: String): List<AccessibilityNodeInfo> {
        var nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        if (nodes.isEmpty()) {
            val altId = when {
                viewId.contains("search_input") -> viewId.replace("search_input", "search_src_text")
                viewId.contains("search_src_text") -> viewId.replace("search_src_text", "search_input")
                viewId.contains("menuitem_search") -> viewId.replace("menuitem_search", "menu_item_search")
                viewId.contains("menu_item_search") -> viewId.replace("menu_item_search", "menuitem_search")
                else -> null
            }
            if (altId != null) {
                nodes = root.findAccessibilityNodeInfosByViewId(altId)
            }
        }
        if (nodes.isEmpty() && viewId.contains("search", ignoreCase = true)) {
            nodes = root.findAccessibilityNodeInfosByText("Search")
        }
        return nodes
    }

    private fun triggerPostStepDelay(task: AutomationTask, delayMs: Long) {
        isWaiting = true
        handler.postDelayed({
            isWaiting = false
            // Trigger evaluation of the next step
            handleGenericAutomation(task)
        }, delayMs)
    }

    private fun handleGenericAutomation(task: AutomationTask) {
        if (isWaiting) return
        val root = rootInActiveWindow ?: return
        if (task.steps.isEmpty() || task.currentStepIndex >= task.steps.size) {
            Log.d("JarvisAccessibility", "Generic automation: No more steps or empty.")
            pendingTask = null
            return
        }

        val step = task.steps[task.currentStepIndex]
        Log.d("JarvisAccessibility", "Generic Step [${task.currentStepIndex}]: action=${step.action}, text=${step.text}, id=${step.id}")

        when (step.action.uppercase()) {
            "OPEN_APP" -> {
                val pkgName = step.pkg ?: return
                val intent = packageManager.getLaunchIntentForPackage(pkgName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    task.currentStepIndex++
                    triggerPostStepDelay(task, 1500L) // Wait 1.5s for app to open
                } else {
                    Log.e("JarvisAccessibility", "Failed to open package: $pkgName")
                    pendingTask = null
                }
            }
            "CLICK_TEXT" -> {
                val textToClick = step.text ?: return
                val nodes = root.findAccessibilityNodeInfosByText(textToClick)
                if (nodes.isNotEmpty()) {
                    var clicked = false
                    for (node in nodes) {
                        if (performClickOnNode(node)) {
                            clicked = true
                            Log.d("JarvisAccessibility", "Clicked text '$textToClick' successfully")
                            break
                        }
                    }
                    if (clicked) {
                        task.currentStepIndex++
                        triggerPostStepDelay(task, 800L) // Wait 800ms for UI to settle
                    }
                }
            }
            "CLICK_ID" -> {
                val idToClick = step.id ?: return
                val nodes = findNodesWithFallback(root, idToClick)
                if (nodes.isNotEmpty()) {
                    var clicked = false
                    for (node in nodes) {
                        if (performClickOnNode(node)) {
                            clicked = true
                            Log.d("JarvisAccessibility", "Clicked ID '$idToClick' successfully")
                            break
                        }
                    }
                    if (clicked) {
                        task.currentStepIndex++
                        triggerPostStepDelay(task, 800L) // Wait 800ms for UI to settle
                    }
                }
            }
            "TYPE_TEXT" -> {
                val textToType = step.text ?: return
                val idToType = step.id
                val nodes = if (idToType != null) {
                    findNodesWithFallback(root, idToType)
                } else {
                    findFocusedEditText(root)
                }
                if (nodes.isNotEmpty()) {
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
                    }
                    nodes[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    Log.d("JarvisAccessibility", "Typed text '$textToType' successfully")
                    task.currentStepIndex++
                    triggerPostStepDelay(task, 800L) // Wait 800ms for text field to update
                }
            }
            "WAIT" -> {
                val delay = if (step.delayMs > 0) step.delayMs else 1000L
                task.currentStepIndex++
                triggerPostStepDelay(task, delay)
            }
            else -> {
                Log.w("JarvisAccessibility", "Unknown step action: ${step.action}")
                task.currentStepIndex++
            }
        }

        if (task.currentStepIndex >= task.steps.size) {
            Log.d("JarvisAccessibility", "Generic automation: Completed all steps.")
            pendingTask = null
        }
    }

    private fun findFocusedEditText(node: AccessibilityNodeInfo, depth: Int = 0): List<AccessibilityNodeInfo> {
        if (depth > 25) return emptyList()
        val list = mutableListOf<AccessibilityNodeInfo>()
        if (node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
            list.add(node)
            return list
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFocusedEditText(child, depth + 1)
            if (found.isNotEmpty()) {
                return found
            }
        }
        return emptyList()
    }

    fun dumpScreenToJson(): String {
        val root = rootInActiveWindow ?: return "{\"error\": \"No active window found or permission is not granted.\"}"
        val json = StringBuilder()
        json.append("[\n")
        traverseAndDump(root, json, 0)
        // Remove trailing comma if exists
        if (json.endsWith(",")) {
            json.setLength(json.length - 1)
        }
        json.append("\n]")
        return json.toString()
    }

    private fun traverseAndDump(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (depth > 30) return // Safety depth limit
        val text = node.text?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""
        val isClickable = node.isClickable

        if (text.isNotEmpty() || viewId.isNotEmpty()) {
            val indent = "  ".repeat(depth + 1)
            sb.append(indent).append("{\n")
            sb.append(indent).append("  \"class\": \"").append(className.replace("\"", "\\\"")).append("\",\n")
            sb.append(indent).append("  \"id\": \"").append(viewId.replace("\"", "\\\"")).append("\",\n")
            sb.append(indent).append("  \"text\": \"").append(text.replace("\"", "\\\"")).append("\",\n")
            sb.append(indent).append("  \"clickable\": ").append(isClickable).append("\n")
            sb.append(indent).append("},\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseAndDump(child, sb, depth + 1)
        }
    }

    private fun tapScreen(x: Float, y: Float): Boolean {
        val path = android.graphics.Path()
        path.moveTo(x, y)

        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(
                android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 100)
            )
            .build()

        return dispatchGesture(gesture, null, null)
    }

    fun unlockWithPin(pin: String, onComplete: (() -> Unit)? = null): Boolean {
        com.example.security.MaxLockAutomationEngine(this).start(pin, onComplete)
        return true
    }

    private fun completeTask(task: AutomationTask) {
        task.step = 4
        pendingTask = null
        if (task.isScheduled) {
            val securitySettings = com.example.security.SecuritySettings(this)
            if (securitySettings.autoRelockEnabled) {
                handler.postDelayed({
                    lockPhone()
                }, securitySettings.lockDelayMs)
            }
        }
    }
    fun lockPhone() {
        val dpm = getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val component = android.content.ComponentName(this, com.example.receiver.MyDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(component)) {
            dpm.lockNow()
        }
    }

    override fun onInterrupt() {
        Log.d("JarvisAccessibility", "Accessibility service interrupted.")
    }

}
