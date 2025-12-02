package com.example.stopchase.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.graphics.Color
import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.view.ViewGroup
import androidx.camera.core.Preview
import androidx.core.app.ActivityCompat
import java.io.ByteArrayOutputStream
import androidx.navigation.NavHostController
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.sqrt
import org.tensorflow.lite.support.image.ImageProcessor
import java.nio.ByteOrder


// Ми були вимушені додати ці анотації бо без них компілятор бачив помилку в використанні ImageProxy

fun preprocessFaceBitmap(context: Context, bitmap: Bitmap): ByteBuffer {
    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(112, 112, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()

    val tensorImage = TensorImage(DataType.FLOAT32)
    tensorImage.load(bitmap)
    val processedImage = imageProcessor.process(tensorImage)

    val singleImageBuffer = processedImage.buffer

    // Створюємо буфер для batch із 2 зображень
    val batchBuffer = ByteBuffer.allocateDirect(2 * singleImageBuffer.capacity())
    batchBuffer.order(ByteOrder.nativeOrder())

    // Копіюємо одне зображення двічі (можна замінити другим, якщо є)
    batchBuffer.put(singleImageBuffer)
    batchBuffer.put(singleImageBuffer)
    batchBuffer.rewind()

    return batchBuffer
}

@Composable
fun FaceCaptureScreen(navController: NavHostController) {
    val context = LocalContext.current
    val imageProxyRef = remember {
        mutableStateOf<ImageProxy?>(null)
    }
}

// ... решта UI


fun extractEmbedding(context: Context, bitmap: Bitmap): FloatArray {
    val model = FileUtil.loadMappedFile(context, "MobileFaceNet.tflite")
    val interpreter = Interpreter(model)

    val inputBuffer = preprocessFaceBitmap(context, bitmap)
    val output = Array(2) { FloatArray(192) }

    val inputTensor = interpreter.getInputTensor(0)
    Log.d("Model", "Input shape: ${inputTensor.shape().joinToString()}")
    Log.d("Model", "Input type: ${inputTensor.dataType()}")

    interpreter.run(inputBuffer, output)
    return output[0]
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

fun loadEmbeddingFromFile(context: Context): FloatArray? {
        return try {
            val file = File(context.filesDir, "Facelogin.id")
            if (!file.exists()) return null

            val content = file.readText()
            content.split(",")
                .mapNotNull { it.toFloatOrNull() }
                .toFloatArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size) return -1f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            normA += vec1[i] * vec1[i]
            normB += vec2[i] * vec2[i]
        }

        return if (normA != 0f && normB != 0f) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            -1f
        }
    }

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
fun processImageProxy(
    imageProxy: ImageProxy,
    context: Context,
    onResult: (Boolean) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val detector = FaceDetection.getClient()

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    if (bitmap != null) {
                        val newEmbedding = extractEmbedding(context, bitmap)
                        val file = File(context.filesDir, "Facelogin.id")

                        if (!file.exists()) {
                            // Зберігаємо перше обличчя
                            file.writeText(newEmbedding.joinToString(","))
                            Log.d("FaceCapture", "Еталонне обличчя збережено")
                            onResult(true) // Вважаємо, що це успішний вхід
                        } else {
                            val savedEmbedding = file.readText()
                                .split(",")
                                .map { it.toFloat() }
                                .toFloatArray()

                            val similarity = cosineSimilarity(savedEmbedding, newEmbedding)
                            Log.d("FaceCapture", "Cosine similarity: $similarity")

                            val isRecognized = similarity > 0.75f // Можна налаштувати поріг
                            onResult(isRecognized)
                        }
                    } else {
                        Log.e("FaceCapture", "Не вдалося конвертувати зображення")
                        onResult(false)
                    }
                } else {
                    Log.d("FaceCapture", "Обличчя не знайдено")
                    onResult(false)
                }
            }
            .addOnFailureListener {
                Log.e("FaceCapture", "Помилка розпізнавання: ${it.message}")
                onResult(false)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
        onResult(false)
    }
}

@Composable
fun FaceCaptureScreen(onFaceCaptured: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // 🔧 Створюємо PreviewView один раз
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // 🔍 Запускаємо камеру
    LaunchedEffect(Unit) {
        val permission = Manifest.permission.CAMERA
        val activity = context as? Activity

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            activity?.let {
                ActivityCompat.requestPermissions(it, arrayOf(permission), 0)
            }
            Toast.makeText(context, "Потрібен дозвіл на камеру", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        try {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider) // ✅ Тепер працює
            }

            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(executor) { imageProxy ->
                    processImageProxy(
                        imageProxy = imageProxy,
                        context = context,
                        onResult = { isRecognized ->
                            if (isRecognized) {
                                Toast.makeText(context, "Обличчя впізнано!", Toast.LENGTH_SHORT).show()
                                onFaceCaptured()
                            } else {
                                Toast.makeText(context, "Обличчя не впізнано!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
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
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Помилка запуску камери: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 📺 Виводимо PreviewView
    AndroidView(factory = { previewView })
}

val cameraSelector = CameraSelector.Builder()
    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
    .build()

val options = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    .build()
val detector = FaceDetection.getClient(options)

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun analyze(imageProxy: ImageProxy) {
    val mediaImage = imageProxy.image ?: return
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val context = LocalContext.current

    detector.process(image)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val bitmap = imageProxyToBitmap(imageProxy)
                if (bitmap != null) {
                    val embedding = extractEmbedding(context, bitmap)

                    // 🔽 Тут можна зберегти embedding або порівняти
                    Log.d("FaceEmbedding", embedding.joinToString())
                }
            }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
