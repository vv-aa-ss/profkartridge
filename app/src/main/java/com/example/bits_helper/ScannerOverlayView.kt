package com.example.bits_helper

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val overlayPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.BLACK
        alpha = 150 // Полупрозрачный черный
    }

    private val framePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 4f
    }

    private val cornerPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val scanLinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.GREEN
        alpha = 200
    }

    private var scanLineY = 0f
    private var scanLineDirection = 1f
    private var animationSpeed = 3f

    private val scanRect = RectF()
    private val cornerLength = 40f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Вычисляем размеры области сканирования (60% от ширины экрана)
        val scanWidth = (w * 0.6f).toInt()
        val scanHeight = (scanWidth * 0.3f).toInt() // Прямоугольная область для штрихкодов
        
        val left = (w - scanWidth) / 2f
        val top = (h - scanHeight) / 2f
        val right = left + scanWidth
        val bottom = top + scanHeight
        
        scanRect.set(left, top, right, bottom)
        scanLineY = top
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Рисуем затемненный фон
        canvas.drawRect(0f, 0f, width, height, overlayPaint)
        
        // Вырезаем область сканирования
        canvas.drawRect(scanRect, paint.apply { 
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        })
        
        // Рисуем рамку области сканирования
        canvas.drawRect(scanRect, framePaint)
        
        // Рисуем углы рамки
        drawCorners(canvas)
        
        // Анимированная сканирующая линия
        drawScanLine(canvas)
        
        // Обновляем анимацию
        updateScanLine()
        invalidate()
    }

    private fun drawCorners(canvas: Canvas) {
        val left = scanRect.left
        val top = scanRect.top
        val right = scanRect.right
        val bottom = scanRect.bottom
        
        // Верхний левый угол
        canvas.drawLine(left, top, left + cornerLength, top, cornerPaint)
        canvas.drawLine(left, top, left, top + cornerLength, cornerPaint)
        
        // Верхний правый угол
        canvas.drawLine(right - cornerLength, top, right, top, cornerPaint)
        canvas.drawLine(right, top, right, top + cornerLength, cornerPaint)
        
        // Нижний левый угол
        canvas.drawLine(left, bottom - cornerLength, left, bottom, cornerPaint)
        canvas.drawLine(left, bottom, left + cornerLength, bottom, cornerPaint)
        
        // Нижний правый угол
        canvas.drawLine(right - cornerLength, bottom, right, bottom, cornerPaint)
        canvas.drawLine(right, bottom, right, bottom - cornerLength, cornerPaint)
    }

    private fun drawScanLine(canvas: Canvas) {
        val lineHeight = 4f
        canvas.drawRect(
            scanRect.left,
            scanLineY - lineHeight / 2,
            scanRect.right,
            scanLineY + lineHeight / 2,
            scanLinePaint
        )
    }

    private fun updateScanLine() {
        scanLineY += scanLineDirection * animationSpeed
        
        if (scanLineY >= scanRect.bottom) {
            scanLineY = scanRect.bottom
            scanLineDirection = -1f
        } else if (scanLineY <= scanRect.top) {
            scanLineY = scanRect.top
            scanLineDirection = 1f
        }
    }
}
