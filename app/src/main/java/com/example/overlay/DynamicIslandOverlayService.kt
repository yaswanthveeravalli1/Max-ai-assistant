package com.example.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.service.VoiceSessionManager
import com.example.service.VoiceOutputManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DynamicIslandOverlayService : Service() {
    private lateinit var overlayManager: DynamicIslandManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    companion object {
        private const val TAG = "DynamicIslandService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing DynamicIslandManager")
        overlayManager = DynamicIslandManager(this)
        
        // Observe state from VoiceSessionManager
        scope.launch {
            VoiceSessionManager.state.collect { state ->
                when (state) {
                    VoiceSessionManager.State.LISTENING_WAKE_WORD -> {
                        overlayManager.showIdle()
                    }
                    VoiceSessionManager.State.LISTENING_COMMAND -> {
                        overlayManager.showListening()
                    }
                    
                    VoiceSessionManager.State.ERROR -> {
                        overlayManager.showExpanded("Error occurred")
                        delay(3000)
                        overlayManager.showIdle()
                    }
                    else -> overlayManager.showIdle()
                }
            }
        }
        
        // Observe text from VoiceSessionManager (for expanded card if needed)
        scope.launch {
            VoiceSessionManager.recognizedText.collect { text ->
                if (text.isNotEmpty() && VoiceSessionManager.state.value == VoiceSessionManager.State.LISTENING_COMMAND) {
                    overlayManager.showExpanded(text)
                }
            }
        }

        // We can poll VoiceOutputManager or create a StateFlow for it. 
        // For now, we will track speaking state using a periodic check or better, add a flow to VoiceOutputManager.
        scope.launch {
            while(isActive) {
                if (VoiceOutputManager.isProcessingSpeech) {
                    overlayManager.showSpeaking()
                } else if (VoiceSessionManager.state.value == VoiceSessionManager.State.LISTENING_WAKE_WORD || VoiceSessionManager.state.value == VoiceSessionManager.State.READY) {
                    // Only drop to idle if we aren't listening for command or processing
                    overlayManager.showIdle()
                }
                delay(100)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        overlayManager.hide()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
