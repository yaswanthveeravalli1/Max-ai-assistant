import sys

with open('app/src/main/java/com/example/service/VoiceCommandParser.kt', 'r') as f:
    code = f.read()

code = code.replace("private fun toggleFlashlight(context: Context, state: Boolean) {", "private fun toggleFlashlight(context: Context, state: Boolean): Boolean {")
code = code.replace("""            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, state)
        }""", """            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, state)
            return false
        }""")
code = code.replace("""        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
        }""", """        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
            return false
        }""")

with open('app/src/main/java/com/example/service/VoiceCommandParser.kt', 'w') as f:
    f.write(code)
