with open("app/src/main/AndroidManifest.xml", "r") as f:
    text = f.read()

text = text.replace(
    '<uses-permission android:name="android.permission.RECORD_AUDIO" />',
    '<uses-permission android:name="android.permission.RECORD_AUDIO" />\n    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />'
)

text = text.replace(
    '</application>',
    '    <service android:name=".overlay.DynamicIslandOverlayService" android:exported="false" />\n    </application>'
)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(text)
