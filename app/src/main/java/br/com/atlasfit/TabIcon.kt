package br.com.atlasfit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

internal class TabIcon(context: Context, private val tab: String, color: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = 1.65f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.scale(width / 24f, height / 24f)
        fun line(x: Float, y: Float, x2: Float, y2: Float) = canvas.drawLine(x, y, x2, y2, paint)
        fun box(x: Float, y: Float, x2: Float, y2: Float) = canvas.drawRoundRect(x, y, x2, y2, 2f, 2f, paint)
        when (tab) {
            "home" -> {
                box(4f, 7f, 8f, 17f)
                box(16f, 7f, 20f, 17f)
                line(8f, 12f, 16f, 12f)
                line(2f, 10f, 2f, 14f)
                line(22f, 10f, 22f, 14f)
            }
            "library" -> {
                box(3f, 3f, 10f, 10f)
                box(14f, 3f, 21f, 10f)
                box(3f, 14f, 10f, 21f)
                box(14f, 14f, 21f, 21f)
            }
            "calendar" -> {
                box(3f, 6f, 21f, 21f)
                line(3f, 11f, 21f, 11f)
                line(8f, 4f, 8f, 8f)
                line(16f, 4f, 16f, 8f)
                line(7f, 15f, 10f, 15f)
                line(14f, 15f, 17f, 15f)
            }
            "report" -> {
                box(3f, 13f, 7f, 21f)
                box(10f, 9f, 14f, 21f)
                box(17f, 4f, 21f, 21f)
            }
            else -> {
                line(3f, 20f, 21f, 20f)
                line(4f, 16f, 9f, 10f)
                line(9f, 10f, 14f, 13f)
                line(14f, 13f, 21f, 5f)
                line(17f, 5f, 21f, 5f)
                line(21f, 5f, 21f, 9f)
            }
        }
        canvas.restore()
    }
}
