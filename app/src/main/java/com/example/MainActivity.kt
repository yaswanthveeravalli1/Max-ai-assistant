package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {

    private val binderReceivedListener = rikka.shizuku.Shizuku.OnBinderReceivedListener {
        if (rikka.shizuku.Shizuku.isPreV11()) return@OnBinderReceivedListener
        if (rikka.shizuku.Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            rikka.shizuku.Shizuku.requestPermission(com.example.automation.ShizukuManager.SHIZUKU_PERMISSION_REQUEST_CODE)
        }
    }
    
    private val binderDeadListener = rikka.shizuku.Shizuku.OnBinderDeadListener {
        android.util.Log.w("MainActivity", "Shizuku Binder Dead")
    }
    
    private val permissionResultListener = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == com.example.automation.ShizukuManager.SHIZUKU_PERMISSION_REQUEST_CODE) {
            android.util.Log.d("MainActivity", "Shizuku Permission Result: $grantResult")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rikka.shizuku.Shizuku.addBinderReceivedListener(binderReceivedListener)
        rikka.shizuku.Shizuku.addBinderDeadListener(binderDeadListener)
        rikka.shizuku.Shizuku.addRequestPermissionResultListener(permissionResultListener)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: JarvisViewModel = viewModel()
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        rikka.shizuku.Shizuku.removeBinderReceivedListener(binderReceivedListener)
        rikka.shizuku.Shizuku.removeBinderDeadListener(binderDeadListener)
        rikka.shizuku.Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }
}
