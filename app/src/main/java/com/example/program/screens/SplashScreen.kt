package com.example.stopchase.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun SplashScreen() {
    val context = LocalContext.current

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

            Button(
                onClick = {
                    val file = File(context.filesDir, "Facelogin.id")
                    if (!file.exists()) {
                        file.writeText("FAKE_FACE_DATA")
                        Toast.makeText(context, "Обличчя збережено", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Файл FaceID вже існує", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(text = "Face ID / Вхід", color = Color.White)
            }
        }
    }
}