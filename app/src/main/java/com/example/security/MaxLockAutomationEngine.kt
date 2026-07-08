package com.example.security

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScreenType {
    PIN, PATTERN, SWIPE, UNKNOWN
}

class MaxLockAutomationEngine(
    private val service: AccessibilityService
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val displayMetrics = android.content.res.Resources.getSystem().displayMetrics
    private val screenWidth = displayMetrics.widthPixels.toFloat()
    private val screenHeight = displayMetrics.heightPixels.toFloat()
    
    private val scaleX = if (screenWidth > 0) screenWidth / 1080f else 1f
    private val scaleY = if (screenHeight > 0) screenHeight / 1920f else 1f

    private val pinPadMap = mapOf(
        '1' to Pair(180f * scaleX, 820f * scaleY), '2' to Pair(540f * scaleX, 820f * scaleY), '3' to Pair(900f * scaleX, 820f * scaleY),
        '4' to Pair(180f * scaleX, 980f * scaleY), '5' to Pair(540f * scaleX, 980f * scaleY), '6' to Pair(900f * scaleX, 980f * scaleY),
        '7' to Pair(180f * scaleX, 1140f * scaleY), '8' to Pair(540f * scaleX, 1140f * scaleY), '9' to Pair(900f * scaleX, 1140f * scaleY),
        '0' to Pair(540f * scaleX, 1300f * scaleY)
    )

    fun start(pin: String, onComplete: (() -> Unit)? = null) {
        scope.launch {
            Log.d("MaxLockEngine", "Starting MAX UNIVERSAL LOCK AUTOMATION ENGINE with PIN of length ${pin.length}")
            
            // Step 1: Wake and Swipe up to show PIN pad
            swipeUp()
            delay(1500) // Wait 1.5s for transition/animation
            
            val root = service.rootInActiveWindow
            if (root == null) {
                Log.w("MaxLockEngine", "Root window is null. Falling back to classic coordinates.")
                executeClassicCoordinates(pin)
                delay(1000) // Additional time for classic coordinates to complete
                onComplete?.invoke()
                return@launch
            }

            val screenType = detectScreenType(root)
            Log.d("MaxLockEngine", "Detected screen type: $screenType")

            when (screenType) {
                ScreenType.PIN -> {
                    handlePinScreenSmart(root, pin)
                }
                ScreenType.PATTERN -> {
                    Log.w("MaxLockEngine", "Pattern screen detected. Classic gesture fallback.")
                    fallbackGesture()
                }
                ScreenType.SWIPE, ScreenType.UNKNOWN -> {
                    Log.d("MaxLockEngine", "Swipe or unknown screen. Attempting fallback.")
                    executeClassicCoordinates(pin)
                }
            }
            delay(1500) // General delay to ensure device is fully unlocked before callback runs
            onComplete?.invoke()
        }
    }

    private fun detectScreenType(root: AccessibilityNodeInfo): ScreenType {
        // Look for digit text nodes or content descriptions
        var foundDigits = 0
        for (digit in 0..9) {
            val textNodes = root.findAccessibilityNodeInfosByText(digit.toString())
            if (textNodes.isNotEmpty()) {
                foundDigits++
            }
        }

        val drawPatternNodes = root.findAccessibilityNodeInfosByText("Draw pattern") +
                root.findAccessibilityNodeInfosByText("pattern") +
                root.findAccessibilityNodeInfosByText("Pattern")

        return when {
            foundDigits >= 3 -> ScreenType.PIN
            drawPatternNodes.isNotEmpty() -> ScreenType.PATTERN
            else -> ScreenType.SWIPE
        }
    }

    private suspend fun handlePinScreenSmart(root: AccessibilityNodeInfo, pin: String) {
        Log.d("MaxLockEngine", "Executing handlePinScreenSmart for PIN")
        for (digit in pin) {
            val clicked = clickDigitSmart(root, digit.toString())
            if (!clicked) {
                Log.w("MaxLockEngine", "Smart click failed for digit $digit. Tapping fallback coordinates.")
                // Fallback to tap screen coordinates for this digit
                val coord = pinPadMap[digit]
                if (coord != null) {
                    tapScreen(coord.first, coord.second)
                }
            } else {
                Log.d("MaxLockEngine", "Smart clicked digit $digit successfully.")
            }
            delay(300) // Timing delay between typing digits
        }

        // Try to click enter or okay button if it exists
        delay(300)
        val entered = clickEnterButtonSmart(root)
        if (!entered) {
            Log.d("MaxLockEngine", "No explicit Enter button clicked. Tapping fallback enter coordinate.")
            tapScreen(900f * scaleX, 1300f * scaleY) // Classic OK/Enter key
        }
    }

    private fun clickDigitSmart(root: AccessibilityNodeInfo, value: String): Boolean {
        // Layer 1: Precise Text match
        val textNodes = root.findAccessibilityNodeInfosByText(value)
        for (node in textNodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }

        // Layer 2: Content description match or ViewId match
        val searchKeywords = listOf(value, "key_$value", "button_$value", "pin_$value")
        for (keyword in searchKeywords) {
            val found = findNodeByTextOrDescRecursive(root, keyword)
            if (found != null && found.isClickable) {
                found.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }

        // Layer 3: Recursive search in full hierarchy
        return findAndClickRecursive(root, value)
    }

    private fun clickEnterButtonSmart(root: AccessibilityNodeInfo): Boolean {
        val keywords = listOf("enter", "ok", "done", "confirm", "✓", "tick", "next", "search")
        for (kw in keywords) {
            val node = findNodeByTextOrDescRecursive(root, kw)
            if (node != null && node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }

    private fun findNodeByTextOrDescRecursive(node: AccessibilityNodeInfo, keyword: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(keyword, ignoreCase = true) == true ||
            node.contentDescription?.toString()?.contains(keyword, ignoreCase = true) == true ||
            node.viewIdResourceName?.contains(keyword, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByTextOrDescRecursive(child, keyword)
            if (found != null) return found
        }
        return null
    }

    private fun findAndClickRecursive(node: AccessibilityNodeInfo?, value: String): Boolean {
        if (node == null) return false

        if (node.text?.toString() == value || node.contentDescription?.toString() == value) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else {
                // If the node isn't clickable, try to click its clickable parent
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    parent = parent.parent
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (findAndClickRecursive(child, value)) return true
        }

        return false
    }

    private suspend fun executeClassicCoordinates(pin: String) {
        Log.d("MaxLockEngine", "Executing classic coordinate unlock sequence.")
        for (digit in pin) {
            val coord = pinPadMap[digit] ?: continue
            tapScreen(coord.first, coord.second)
            delay(300)
        }
        delay(300)
        tapScreen(900f * scaleX, 1300f * scaleY) // Enter key tap
    }

    private fun swipeUp() {
        val path = Path().apply {
            val midX = screenWidth / 2f
            val startY = screenHeight * 0.85f
            val endY = screenHeight * 0.2f
            moveTo(midX, startY)
            lineTo(midX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    private fun fallbackGesture() {
        swipeUp()
    }

    private fun tapScreen(x: Float, y: Float): Boolean {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }
}
