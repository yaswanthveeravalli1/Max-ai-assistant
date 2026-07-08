import sys

with open('app/src/main/java/com/example/service/GeminiBrainEngine.kt', 'r') as f:
    code = f.read()

code = code.replace("suspend fun processCommand(context: Context, command: String) = withContext(Dispatchers.IO) {", "suspend fun processCommand(context: Context, command: String): Boolean = withContext(Dispatchers.IO) {")
code = code.replace("""            withContext(Dispatchers.Main) {
                ActionExecutor.execute(context, json)
            }
            
        } catch (e: Exception) {""", """            withContext(Dispatchers.Main) {
                ActionExecutor.execute(context, json)
            }
        } catch (e: Exception) {""")

with open('app/src/main/java/com/example/service/GeminiBrainEngine.kt', 'w') as f:
    f.write(code)
