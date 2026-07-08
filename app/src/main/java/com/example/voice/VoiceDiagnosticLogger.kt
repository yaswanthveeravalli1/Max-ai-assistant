package com.example.voice

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VoiceDiagnosticLogger {
    private const val TAG = "VoiceDiagnostics"

    fun logAudioError(context: Context, errorCode: Int) {
        val errorDesc = getErrorText(errorCode)
        val logMessage = "Voice Recognition Error [$errorCode - $errorDesc]"
        Log.e(TAG, logMessage)
        saveLogToFile(context, logMessage)
    }

    fun logTriggerFailure(context: Context, command: String, reason: String) {
        val logMessage = "Trigger Failure for command '$command': $reason"
        Log.e(TAG, logMessage)
        saveLogToFile(context, logMessage)
    }

    fun logTriggerSuccess(context: Context, command: String) {
        val logMessage = "Trigger Success for command '$command'"
        Log.i(TAG, logMessage)
        saveLogToFile(context, logMessage)
    }

    fun logRawAudioInput(context: Context, command: String) {
        val logMessage = "Raw Audio Input Recognized: '$command'"
        Log.i(TAG, logMessage)
        saveLogToFile(context, logMessage)
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            android.speech.SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            android.speech.SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            android.speech.SpeechRecognizer.ERROR_NETWORK -> "Network error"
            android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            android.speech.SpeechRecognizer.ERROR_SERVER -> "error from server"
            android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            android.speech.SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
            android.speech.SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable"
            android.speech.SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
            android.speech.SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server disconnected"
            else -> "Unknown Error"
        }
    }

    private fun saveLogToFile(context: Context, message: String) {
        try {
            val logDir = File(context.filesDir, "diagnostics")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            val logFile = File(logDir, "voice_diagnostics.txt")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            logFile.appendText("[$timestamp] $message\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save diagnostic log", e)
        }
    }
}
