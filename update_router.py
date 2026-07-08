import sys

with open('app/src/main/java/com/example/service/AIBrainRouter.kt', 'r') as f:
    code = f.read()

code = code.replace("suspend fun process(context: Context, text: String) {", "suspend fun process(context: Context, text: String): Boolean {")
code = code.replace("GroqBrainEngine.processCommand(context, text)\n                return // Important: return on success to avoid falling back to Gemini", "return GroqBrainEngine.processCommand(context, text)")
code = code.replace("GeminiBrainEngine.processCommand(context, text)\n        } catch (e: Exception) {", "return GeminiBrainEngine.processCommand(context, text)\n        } catch (e: Exception) {")

with open('app/src/main/java/com/example/service/AIBrainRouter.kt', 'w') as f:
    f.write(code)
