package com.example.stopchase.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun FaceCaptureScreen(onFaceCaptured: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Немає дозволу на камеру", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        val cameraProvider = cameraProviderFuture.get()
        val previewView = PreviewView(context)

        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(executor) { imageProxy ->
                processImageProxy(imageProxy, onFaceCaptured)
            }
        }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )
    }

    AndroidView(
        factory = { PreviewView(it) },
        modifier = Modifier.fillMaxSize()
    )
}
// Ми були вимушені додати ці анотації бо без них компілятор бачив помилку в використанні ImageProxy
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onFaceDetected: () -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val detector = FaceDetection.getClient()

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    Log.d("FaceCapture", "Обличчя знайдено!")
                    onFaceDetected()
                }
            }
            .addOnFailureListener {
                Log.e("FaceCapture", "Помилка розпізнавання: ${it.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}