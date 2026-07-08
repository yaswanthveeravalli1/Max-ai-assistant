import sys

with open('app/src/main/java/com/example/service/ActionExecutor.kt', 'r') as f:
    code = f.read()

code = code.replace("""            "GET_TIME" -> {
                val time = SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
                ).format(Date())
                JarvisSpeaker.speak("Time is $time")
            }""", """            "GET_TIME" -> {
                val time = SimpleDateFormat(
                    "h:mm a",
                    Locale.getDefault()
                ).format(Date())
                JarvisSpeaker.speak("Time is $time")
                return false
            }""")
            
with open('app/src/main/java/com/example/service/ActionExecutor.kt', 'w') as f:
    f.write(code)
