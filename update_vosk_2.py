import sys

with open('app/src/main/java/com/example/service/VoskManager.kt', 'r') as f:
    code = f.read()

code = code.replace("""        try {
            // Wake word grammar: Only listen for "hey bittu" variations or unknowns
            val recognizer = Recognizer(model, 16000.0f, "[\"hey\", \"heyy\", \"bittu\", \"bitu\", \"beta\", \"bit\", \"too\", \"[unk]\"]")
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(object : RecognitionListener {""", """        try {
            if (speechService == null) {
                // Wake word grammar: Only listen for "hey bittu" variations or unknowns
                val recognizer = Recognizer(model, 16000.0f, "[\"hey\", \"heyy\", \"bittu\", \"bitu\", \"beta\", \"bit\", \"too\", \"[unk]\"]")
                speechService = SpeechService(recognizer, 16000.0f)
            }
            speechService?.startListening(object : RecognitionListener {""")

code = code.replace("""    fun stopListening() {
        Log.d(TAG, "Wake word stopped")
        speechService?.let {
            it.stop()
            it.shutdown()
        }
        speechService = null""", """    fun stopListening() {
        Log.d(TAG, "Wake word stopped")
        speechService?.let {
            it.stop()
        }""")

code = code.replace("""    fun destroy() {
        stopListening()
        mainHandler.post {
            androidSpeechRecognizer?.let {
                Log.d(TAG, "SpeechRecognizer destroyed")
                it.destroy()
            }
            androidSpeechRecognizer = null
        }
    }""", """    fun destroy() {
        stopListening()
        speechService?.shutdown()
        speechService = null
        mainHandler.post {
            androidSpeechRecognizer?.let {
                Log.d(TAG, "SpeechRecognizer destroyed")
                it.destroy()
            }
            androidSpeechRecognizer = null
        }
    }""")

code = code.replace("""                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }""", """                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }""")


with open('app/src/main/java/com/example/service/VoskManager.kt', 'w') as f:
    f.write(code)
