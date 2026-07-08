import sys

with open('app/src/main/java/com/example/service/ActionExecutor.kt', 'r') as f:
    code = f.read()

# Revert signatures
code = code.replace("fun execute(context: Context, json: JSONObject): Boolean {", "fun execute(context: Context, json: JSONObject) {")
code = code.replace("private fun performSystemAction(actionName: String): Boolean {", "private fun performSystemAction(actionName: String) {")
code = code.replace("private fun openApp(context: Context, appName: String): Boolean {", "private fun openApp(context: Context, appName: String) {")
code = code.replace("private fun toggleFlashlight(context: Context, state: Boolean): Boolean {", "private fun toggleFlashlight(context: Context, state: Boolean) {")
code = code.replace("private fun sendWhatsApp(context: Context, contact: String, msg: String): Boolean {", "private fun sendWhatsApp(context: Context, contact: String, msg: String) {")

# Revert execute
code = code.replace("return openApp(context, app)", "openApp(context, app)")
code = code.replace("return performSystemAction(sysAction)", "performSystemAction(sysAction)")
code = code.replace("return toggleFlashlight(context, true)", "toggleFlashlight(context, true)")
code = code.replace("return toggleFlashlight(context, false)", "toggleFlashlight(context, false)")
code = code.replace("return performSystemAction(\"back\")", "performSystemAction(\"back\")")
code = code.replace("return performSystemAction(\"home\")", "performSystemAction(\"home\")")
code = code.replace("return performSystemAction(\"recent\")", "performSystemAction(\"recent\")")
code = code.replace("return sendWhatsApp(context, contact, message)", "sendWhatsApp(context, contact, message)")
code = code.replace("return false", "")
code = code.replace("return true", "return")

# Fix empty returns in openApp that were supposed to just be return
code = code.replace("context.startActivity(dialIntent)\n                return", "context.startActivity(dialIntent)\n                return")
code = code.replace("context.startActivity(smsIntent)\n                    return", "context.startActivity(smsIntent)\n                    return")

# Cleanup any formatting issues from the replaces
import re
# Remove bare 'return' at end of blocks if they are unnecessary, but it's safe to leave them or clean them up.
# Actually, the original code had `return` in openApp for early exit.
# Let's just restore the file from the original content I can see in the history.
