package com.example.stopchase.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.StateFlow

@Composable
fun FaceMonitorOverlay(
    shouldObscureFlow: StateFlow<Boolean>
) {
    val shouldObscure by shouldObscureFlow.collectAsState()

    if (shouldObscure) {
        // 🔄 Пульсуюча анімація для фону
        val infiniteTransition = rememberInfiniteTransition()
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = alpha))
                .zIndex(10f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Захист активний\nОбличчя не виявлено",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}