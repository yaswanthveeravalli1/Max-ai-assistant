package com.example.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.AnimatedVisibility

class DynamicIslandManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private val customLifecycleOwner = CustomLifecycleOwner()
    
    private val _overlayState = MutableStateFlow(OverlayState.IDLE)
    private val _expandedText = MutableStateFlow("")
    
    private var isAdded = false

    init {
        customLifecycleOwner.onCreate()
        customLifecycleOwner.onStart()
        customLifecycleOwner.onResume()
        setupComposeView()
    }

    private fun setupComposeView() {
        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(customLifecycleOwner)
            setViewTreeViewModelStoreOwner(customLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(customLifecycleOwner)
            
            setContent {
                val state by _overlayState.collectAsState()
                val expandedText by _expandedText.collectAsState()
                
                if (state != OverlayState.HIDDEN) {
                    DynamicIslandUI(state, expandedText, onDismiss = { showIdle() })
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 50 // Padding from top
        }

        try {
            windowManager.addView(composeView, params)
            isAdded = true
        } catch (e: Exception) {
            Log.e("DynamicIsland", "Failed to add window", e)
        }
    }

    fun showIdle() { _overlayState.value = OverlayState.IDLE }
    fun showListening() { _overlayState.value = OverlayState.LISTENING }
    fun showProcessing() { _overlayState.value = OverlayState.PROCESSING }
    fun showSpeaking() { _overlayState.value = OverlayState.SPEAKING }
    fun showExpanded(text: String) { 
        _expandedText.value = text
        _overlayState.value = OverlayState.EXPANDED 
    }
    fun hide() {
        _overlayState.value = OverlayState.HIDDEN
        if (isAdded && composeView != null) {
            try {
                windowManager.removeView(composeView)
                isAdded = false
                customLifecycleOwner.onPause()
                customLifecycleOwner.onStop()
                customLifecycleOwner.onDestroy()
            } catch (e: Exception) {}
        }
    }
}

@Composable
fun DynamicIslandUI(state: OverlayState, expandedText: String, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val glowColor by androidx.compose.animation.animateColorAsState(
        targetValue = when (state) {
            OverlayState.LISTENING -> Color(0xFF00FF7F).copy(alpha = 0.5f)
            OverlayState.PROCESSING -> Color(0xFFA855F7).copy(alpha = 0.5f)
            OverlayState.SPEAKING -> Color(0xFFFF003C).copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        animationSpec = tween(500, easing = LinearEasing),
        label = "glowColor"
    )

    Box(
        modifier = Modifier
            .padding(16.dp) // Leave space for shadow/glow
            .animateContentSize(animationSpec = tween(durationMillis = 0))
            .background(Color(0xFF121212), RoundedCornerShape(40.dp))
            .border(
                width = if (state == OverlayState.IDLE) 1.dp else 2.dp,
                color = if (state == OverlayState.IDLE) Color(0xFF333333) else glowColor,
                shape = RoundedCornerShape(40.dp)
            )
            .clip(RoundedCornerShape(40.dp))
            .clickable(enabled = state != OverlayState.IDLE) { onDismiss() }
    ) {
        Crossfade(targetState = state, animationSpec = tween(0)) { targetState ->
            when (targetState) {
                OverlayState.IDLE -> CompactState()
                OverlayState.LISTENING -> ListeningState()
                OverlayState.PROCESSING -> ProcessingState()
                OverlayState.SPEAKING -> SpeakingState()
                OverlayState.EXPANDED -> ExpandedState(expandedText, onDismiss)
                OverlayState.HIDDEN -> {}
            }
        }
    }
}

@Composable
fun CompactState() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).background(Color.White, RoundedCornerShape(50)))
        Spacer(modifier = Modifier.width(8.dp))
        Text("MAX", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ListeningState() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Mic, contentDescription = "Listening", tint = Color(0xFF00FF7F), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Listening...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(16.dp))
        WaveformAnimation(color = Color(0xFF00FF7F))
    }
}

@Composable
fun ProcessingState() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).background(Color(0xFFA855F7), RoundedCornerShape(50)))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Thinking...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(16.dp))
        DotsAnimation(color = Color(0xFFA855F7))
    }
}

@Composable
fun SpeakingState() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).background(Color(0xFFFF003C), RoundedCornerShape(50)))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Speaking...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(16.dp))
        WaveformAnimation(color = Color(0xFFFF003C))
    }
}

@Composable
fun ExpandedState(text: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 300.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(Color(0xFFFF003C), RoundedCornerShape(50)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("MAX", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text, 
            color = Color.White, 
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaveformAnimation(color = Color(0xFFFF003C))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun WaveformAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val heights = List(5) { 
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 300 + it * 100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }
    
    Canvas(modifier = Modifier.size(width = 40.dp, height = 24.dp)) {
        val barWidth = size.width / 9f
        val maxBarHeight = size.height
        
        heights.forEachIndexed { index, heightState ->
            val barHeight = maxBarHeight * heightState.value
            val x = index * (barWidth * 2)
            val y = (maxBarHeight - barHeight) / 2
            
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

@Composable
fun DotsAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val alphas = List(3) { 
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, delayMillis = it * 200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }
    
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        alphas.forEach { alpha ->
            Box(modifier = Modifier.size(6.dp).background(color.copy(alpha = alpha.value), RoundedCornerShape(50)))
        }
    }
}
