package com.example.stopchase.screens

import com.example.stopchase.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import java.util.concurrent.Executors
import kotlin.math.abs

class FaceMonitorService : LifecycleService() {

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var faceDetector: FaceDetector
    private var overlayVisible = false

    override fun onCreate() {
        super.onCreate()
        faceDetector = FaceDetection.getClient()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                processImage(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val shouldHide = faces.isEmpty() || faces.any {
                    abs(it.headEulerAngleY) > 20 || abs(it.headEulerAngleZ) > 15
                }

                Log.d("FaceMonitor", "Faces found: ${faces.size}, shouldHide=$shouldHide")

                val broadcastIntent = Intent("com.example.stopchase.OBSCURE_UPDATE")
                broadcastIntent.putExtra("shouldObscure", shouldHide)
                sendBroadcast(broadcastIntent)

                // 🔥 Керуємо OverlayService
                // 🔥 Керуємо OverlayService
                if (shouldHide && !overlayVisible) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                        startService(Intent(this, OverlayService::class.java))
                        overlayVisible = true
                    } else {
                        Log.w("FaceMonitor", "Немає дозволу на накладку — OverlayService не запущено")
                    }
                } else if (!shouldHide && overlayVisible) {
                    stopService(Intent(this, OverlayService::class.java))
                    overlayVisible = false
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceMonitor", "Face detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(1, createNotification())
        Log.d("FaceMonitor", "!**! Service started")
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "face_monitor_channel"
        val channelName = "Face Monitor"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("StopChase активний")
            .setContentText("Слідкуємо за обличчям...")
            .setSmallIcon(R.drawable.ic_shield_eye)
            .build()
    }
}