package com.example.stopchase.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    var protectionEnabled by remember {
        mutableStateOf(loadProtectionStatus(context))
    }


// 🔥 Автоматичний запуск сервісу, якщо захист увімкнено
    LaunchedEffect(protectionEnabled) {
        if (protectionEnabled) {
            val intent = Intent(context, FaceMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    // 🔥 Слухаємо Broadcast з сервісу
    val shouldObscureState = remember { MutableStateFlow(false) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val value = intent?.getBooleanExtra("shouldObscure", false) ?: false
                shouldObscureState.value = value
            }
        }
        context.registerReceiver(receiver, IntentFilter("com.example.stopchase.OBSCURE_UPDATE"))

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FaceMonitorOverlay(shouldObscureFlow = shouldObscureState.asStateFlow())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = if (protectionEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
        Icon(
            imageVector = icon,
            contentDescription = if (protectionEnabled) "Protection On" else "Protection Off",
            modifier = Modifier.size(100.dp)
        )

        Button(onClick = {
            protectionEnabled = !protectionEnabled
            saveProtectionStatus(context, protectionEnabled)

            val status = if (protectionEnabled) "Увімкнено захист" else "Вимкнено захист"
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()

            val intent = Intent(context, FaceMonitorService::class.java)
            if (protectionEnabled) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.stopService(intent)
            }
        }) {
            Text(if (protectionEnabled) "Вимкнути" else "Увімкнути")
        }

        Button(onClick = {
            val file = File(context.filesDir, "Facelogin.id")
            if (file.exists()) {
                file.delete()
                Toast.makeText(context, "Обличчя стерто", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Обличчя ще не збережено", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Скинути обличчя")
        }

        Button(onClick = {
            (context as? Activity)?.finish()
        }) {
            Text("Вийти")
        }
    }
}

private const val PREFS_NAME = "stopchase_prefs"
private const val KEY_PROTECTION_ENABLED = "protection_enabled"

fun saveProtectionStatus(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
}

fun loadProtectionStatus(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_PROTECTION_ENABLED, true)
}