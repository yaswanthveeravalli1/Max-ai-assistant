package com.example

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object CustomAppIconManager {

    fun createShortcut(context: Context, imageUri: Uri, shortcutName: String) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return
        }

        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            // Scale and crop bitmap to 108dp equivalent roughly (e.g. 256x256 for a shortcut icon is good)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)

            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
            }

            val icon = IconCompat.createWithAdaptiveBitmap(scaledBitmap)
            
            val shortcut = ShortcutInfoCompat.Builder(context, "custom_icon_${System.currentTimeMillis()}")
                .setShortLabel(shortcutName)
                .setIcon(icon)
                .setIntent(intent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
