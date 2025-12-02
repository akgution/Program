package com.example.stopchase.screens

import android.content.Intent
import android.os.Build
import com.example.stopchase.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

import android.provider.Settings
import android.net.Uri

@Composable
fun SplashScreen(
    onFaceLogin: () -> Unit // ⬅️ новий параметр
) {
    val context = LocalContext.current
    val imageProxyRef = remember { mutableStateOf<ImageProxy?>(null) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + context.packageName)
        )
        context.startActivity(intent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shield_eye),
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = "StopChase",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Захисти своє приватне життя!",
                fontSize = 16.sp,
                color = Color.LightGray
            )

            Button(onClick = {
                val imageProxy = imageProxyRef.value
                if (imageProxy != null) {
                    processImageProxy(imageProxy, context) { isRecognized ->
                        if (isRecognized) {
                            Toast.makeText(context, "Обличчя впізнано!", Toast.LENGTH_SHORT).show()
                            onFaceLogin()
                        } else {
                            Toast.makeText(context, "Обличчя не впізнано!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Камера запускається для зчитування обличчя", Toast.LENGTH_SHORT).show()
                    onFaceLogin() // ⬅️ Переходимо на екран з камерою
                }
            }) {
                Text("Підтвердити обличчя")
            }

        }
    }
}