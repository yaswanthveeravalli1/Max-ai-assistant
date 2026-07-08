import sys

with open('app/src/main/java/com/example/service/ActionExecutor.kt', 'r') as f:
    code = f.read()

code = code.replace("when (action) {", "return when (action) {")

with open('app/src/main/java/com/example/service/ActionExecutor.kt', 'w') as f:
    f.write(code)
