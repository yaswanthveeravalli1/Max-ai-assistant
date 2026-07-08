with open("app/src/main/java/com/example/ui/screens/SettingsTab.kt", "r") as f:
    text = f.read()

replacement = """                HorizontalDivider(color = Color(0xFFFFFFFF).copy(alpha = 0.05f), thickness = 1.dp)

                // Dynamic Island / Draw Overlays Permission
                val canDrawOverlays = android.provider.Settings.canDrawOverlays(context)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!canDrawOverlays) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Island Overlay",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (canDrawOverlays) "Granted ✅" else "Tap to grant permission ⚠️",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (canDrawOverlays) Color(0xFF00FF7F) else Color(0xFFFF003C)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFFFFFFF).copy(alpha = 0.05f), thickness = 1.dp)

                // Service & Mic Capture Status Indicator Rows"""

text = text.replace(
    '                HorizontalDivider(color = Color(0xFFFFFFFF).copy(alpha = 0.05f), thickness = 1.dp)\n\n                // Service & Mic Capture Status Indicator Rows',
    replacement
)

with open("app/src/main/java/com/example/ui/screens/SettingsTab.kt", "w") as f:
    f.write(text)
