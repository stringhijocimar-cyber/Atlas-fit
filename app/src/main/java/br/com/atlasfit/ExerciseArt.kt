package br.com.atlasfit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.LruCache
import android.view.View
import java.io.IOException
import kotlin.math.min

internal class ExerciseArt(
    context: Context,
    private val asset: String,
    private val frameCount: Int = 1,
    firstFrame: Int = 0
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val source = Rect()
    private val target = RectF()
    var frame: Int = firstFrame
        set(value) {
            require(value in 0 until frameCount)
            if (field == value) return
            field = value
            invalidate()
        }

    init {
        background = GradientDrawable().apply {
            setColor(0xff15202f.toInt())
            cornerRadius = 12 * resources.displayMetrics.density
        }
        clipToOutline = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val sample = if (min(width, height) <= 220) 2 else 1
        val bitmap = load(sample) ?: return
        val cellWidth = bitmap.width / frameCount
        val ratio = min(width / cellWidth.toFloat(), height / bitmap.height.toFloat())
        val drawWidth = cellWidth * ratio
        val drawHeight = bitmap.height * ratio
        val left = (width - drawWidth) / 2
        val top = (height - drawHeight) / 2
        source.set(frame * cellWidth, 0, (frame + 1) * cellWidth, bitmap.height)
        target.set(left, top, left + drawWidth, top + drawHeight)
        canvas.drawBitmap(bitmap, source, target, paint)
    }

    private fun load(sample: Int): Bitmap? {
        val key = "$asset/$sample"
        cache.get(key)?.let { return it }
        return try {
            context.assets.open(asset).use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                    inSampleSize = sample
                })
            }?.also { cache.put(key, it) }
        } catch (_: IOException) {
            null
        }
    }

    companion object {
        private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
        }

        fun phase(context: Context, guide: ExerciseGuide, end: Boolean = false) =
            ExerciseArt(context, guide.movementPath, 2, if (end) 1 else 0)
    }
}
