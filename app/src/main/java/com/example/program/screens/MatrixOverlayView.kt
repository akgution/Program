package com.example.stopchase.screens

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class MatrixOverlayView(context: Context) : View(context) {
    private val paintGrid = Paint().apply {
        color = Color.argb(100, 0, 255, 0) // напівпрозора зелена сітка
        strokeWidth = 1f
    }

    private val paintPulse = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val gridSize = 40
    private val pulsePoints = mutableListOf<PulsePoint>()
    private var frame = 0

    init {
        generatePulsePoints()
    }

    private fun generatePulsePoints() {
        pulsePoints.clear()
        for (i in 0 until 50) {
            val x = Random.nextInt(0, 100)
            val y = Random.nextInt(0, 100)
            val speed = Random.nextDouble(0.05, 0.15)
            pulsePoints.add(PulsePoint(x, y, speed))
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        val cols = width / gridSize
        val rows = height / gridSize

        // Малюємо сітку
        for (i in 0..cols) {
            val x = i * gridSize.toFloat()
            canvas.drawLine(x, 0f, x, height.toFloat(), paintGrid)
        }
        for (j in 0..rows) {
            val y = j * gridSize.toFloat()
            canvas.drawLine(0f, y, width.toFloat(), y, paintGrid)
        }

        // Малюємо пульсуючі точки
        for (point in pulsePoints) {
            val x = point.x / 100f * width
            val y = point.y / 100f * height
            val radius = 6f + 4f * sin(frame * point.speed).toFloat()
            canvas.drawCircle(x, y, radius, paintPulse)
        }

        frame++
        postInvalidateDelayed(40)
    }

    data class PulsePoint(val x: Int, val y: Int, val speed: Double)
}
