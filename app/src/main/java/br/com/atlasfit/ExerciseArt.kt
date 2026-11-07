package br.com.atlasfit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View
import kotlin.math.min

internal class ExerciseArt(context: Context, private val motion: Motion, private val finish: Boolean) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val equipment = Color.rgb(90, 112, 134)
    private val light = Color.rgb(218, 230, 241)
    private val shade = Color.rgb(115, 137, 161)
    private val focus = Color.rgb(100, 238, 175)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = min(width / 124f, height / 114f)
        canvas.save()
        canvas.translate((width - 124 * scale) / 2, (height - 114 * scale) / 2)
        canvas.scale(scale, scale)
        paint.shader = LinearGradient(0f, 0f, 124f, 114f, intArrayOf(0xff24384b.toInt(), 0xff131e2d.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(0f, 0f, 124f, 114f, 13f, 13f, paint)
        paint.shader = null
        paint.color = 0x225f88af
        canvas.drawOval(15f, 98f, 111f, 106f, paint)
        val pose = pose()
        frame(canvas, pose)
        fun limb(a: Point, b: Point, size: Float, color: Int) {
            paint.strokeWidth = size
            paint.color = color
            canvas.drawLine(a.x, a.y, b.x, b.y, paint)
        }
        fun joint(p: Point, radius: Float, color: Int) {
            paint.color = color
            canvas.drawCircle(p.x, p.y, radius, paint)
        }
        val hip = pose.hip
        val shoulder = pose.shoulder
        val front = motion in setOf(Motion.FLY, Motion.REVERSE_FLY, Motion.ADDUCTOR, Motion.OVERHEAD, Motion.PULLDOWN)
        val leg = motion in setOf(Motion.LEG_EXTENSION, Motion.LEG_PRESS, Motion.SEATED_CURL, Motion.LYING_CURL, Motion.SQUAT, Motion.ADDUCTOR, Motion.CALF, Motion.WALK, Motion.BIKE)
        val arm = motion in setOf(Motion.CURL, Motion.HAMMER, Motion.PUSHDOWN, Motion.EXTENSION)
        val shift = if (front) -10f else -4f
        val otherKnee = if (motion == Motion.ADDUCTOR) Point(2 * hip.x - pose.knee.x, pose.knee.y) else pose.knee + Point(shift, -3f)
        val otherFoot = if (motion == Motion.ADDUCTOR) Point(2 * hip.x - pose.foot.x, pose.foot.y) else pose.foot + Point(shift, -4f)
        limb(hip + Point(shift, 0f), otherKnee, 8f, shade)
        limb(otherKnee, otherFoot, 6f, shade)
        limb(hip, pose.knee, 10f, if (leg && motion != Motion.CALF) focus else light)
        limb(pose.knee, pose.foot, 7f, if (motion == Motion.CALF) focus else light)
        limb(pose.foot, pose.foot + Point(7f, 0f), 4.5f, light)

        val farElbow = if (front) Point(2 * shoulder.x - pose.elbow.x, pose.elbow.y) else pose.elbow + Point(-5f, -3f)
        val farHand = if (front) Point(2 * shoulder.x - pose.hand.x, pose.hand.y) else pose.hand + Point(-6f, -3f)
        limb(shoulder + Point(shift, 1f), farElbow, 7f, shade)
        limb(farElbow, farHand, 5f, shade)
        val offset = hip - shoulder
        val body = Path().apply {
            moveTo(shoulder.x - 10, shoulder.y)
            quadTo(shoulder.x, shoulder.y - 6, shoulder.x + 11, shoulder.y)
            lineTo(hip.x + 7, hip.y)
            quadTo(hip.x, hip.y + 6, hip.x - 8, hip.y)
            close()
        }
        paint.shader = LinearGradient(shoulder.x - 9, shoulder.y, shoulder.x + 11, shoulder.y + 10, shade, light, Shader.TileMode.CLAMP)
        canvas.drawPath(body, paint)
        paint.shader = null
        if (!leg && !arm && motion != Motion.MOBILITY) {
            limb(shoulder + Point(offset.x * .17f - 4, offset.y * .17f + 1), shoulder + Point(offset.x * .32f + 4, offset.y * .32f + 1), 5f, focus)
        }
        limb(shoulder, pose.elbow, 8f, if (arm || motion == Motion.FRONT_RAISE || motion == Motion.OVERHEAD) focus else light)
        limb(pose.elbow, pose.hand, 5.5f, light)
        joint(pose.head, 7f, light)
        joint(pose.head + Point(3.7f, 1f), 2.4f, light)
        joint(pose.hand, 3f, light)
        when (motion) {
            Motion.INCLINE, Motion.BENCH, Motion.SQUAT, Motion.PULLDOWN, Motion.PUSHDOWN, Motion.EXTENSION -> {
                limb(pose.hand + Point(-12f, 0f), pose.hand + Point(12f, 0f), 2.4f, equipment)
                joint(pose.hand + Point(-12f, 0f), 3.6f, equipment)
                joint(pose.hand + Point(12f, 0f), 3.6f, equipment)
            }
            Motion.HAMMER, Motion.FRONT_RAISE, Motion.SHRUG -> {
                limb(pose.hand + Point(-6f, 0f), pose.hand + Point(6f, 0f), 3f, equipment)
                joint(pose.hand + Point(-6f, 0f), 3f, equipment)
                joint(pose.hand + Point(6f, 0f), 3f, equipment)
            }
            else -> Unit
        }
        canvas.restore()
    }

    private fun frame(c: Canvas, p: Pose) {
        paint.color = equipment
        paint.strokeWidth = 3f
        fun bar(x: Float, y: Float, endX: Float, endY: Float) = c.drawLine(x, y, endX, endY, paint)
        when (motion) {
            Motion.WALK -> { bar(23f, 99f, 104f, 99f); bar(94f, 99f, 94f, 57f); bar(94f, 57f, 81f, 57f) }
            Motion.BIKE -> {
                paint.style = Paint.Style.STROKE
                c.drawCircle(76f, 88f, 12f, paint)
                c.drawCircle(48f, 88f, 8f, paint)
                paint.style = Paint.Style.FILL
                bar(48f, 88f, 76f, 88f); bar(76f, 88f, 93f, 50f); bar(93f, 50f, 83f, 50f); bar(48f, 88f, 48f, 60f)
            }
            Motion.BENCH, Motion.INCLINE, Motion.HAMMER, Motion.LYING_CURL, Motion.LEG_PRESS -> {
                bar(p.shoulder.x, p.shoulder.y + 8, p.hip.x + 11, p.hip.y + 7)
                bar(p.hip.x, p.hip.y + 9, p.hip.x, 98f)
                bar(p.hip.x - 12, 98f, p.hip.x + 12, 98f)
                if (motion == Motion.LEG_PRESS) { bar(99f, 89f, 99f, 33f); bar(p.foot.x + 4, p.foot.y - 7, p.foot.x + 4, p.foot.y + 7) }
            }
            Motion.SQUAT, Motion.PULLDOWN, Motion.PUSHDOWN, Motion.EXTENSION -> {
                bar(100f, 16f, 100f, 98f); bar(86f, 98f, 108f, 98f); bar(100f, 16f, 61f, 16f)
                if (motion != Motion.SQUAT) { bar(61f, 16f, p.hand.x, p.hand.y) }
            }
            Motion.FLY, Motion.PRESS, Motion.OVERHEAD, Motion.LEG_EXTENSION, Motion.SEATED_CURL,
            Motion.ADDUCTOR, Motion.CALF, Motion.ROW, Motion.REVERSE_FLY, Motion.CURL -> {
                bar(p.hip.x - 13, p.hip.y + 5, p.hip.x + 14, p.hip.y + 5)
                bar(p.hip.x, p.hip.y + 5, p.hip.x, 98f)
                bar(p.hip.x - 14, 98f, p.hip.x + 14, 98f)
                if (motion == Motion.ROW) { bar(108f, 32f, 108f, 94f); bar(108f, 56f, p.hand.x, p.hand.y) }
            }
            else -> Unit
        }
    }

    private data class Point(val x: Float, val y: Float) {
        operator fun plus(other: Point) = Point(x + other.x, y + other.y)
        operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    }
    private data class Pose(val head: Point, val shoulder: Point, val hip: Point,
                            val elbow: Point, val hand: Point, val knee: Point, val foot: Point)

    private fun pose(): Pose {
        fun body(head: Pair<Int, Int>, shoulder: Pair<Int, Int>, hip: Pair<Int, Int>,
                 knee: Pair<Int, Int>, foot: Pair<Int, Int>, elbow: Pair<Int, Int>, hand: Pair<Int, Int>): Pose {
            fun Pair<Int, Int>.point() = Point(first.toFloat(), second.toFloat())
            return Pose(head.point(), shoulder.point(), hip.point(), elbow.point(), hand.point(), knee.point(), foot.point())
        }
        fun phase(start: Pair<Int, Int>, end: Pair<Int, Int>) = if (finish) end else start
        fun stand(elbow: Pair<Int, Int>, hand: Pair<Int, Int>) =
            body(57 to 28, 55 to 41, 55 to 67, 56 to 84, 57 to 98, elbow, hand)
        fun seat(elbow: Pair<Int, Int>, hand: Pair<Int, Int>, foot: Pair<Int, Int> = 78 to 96) =
            body(49 to 28, 48 to 41, 49 to 68, 73 to 72, foot, elbow, hand)
        return when (motion) {
            Motion.FLY -> seat(phase(83 to 43, 63 to 54), phase(92 to 36, 55 to 55))
            Motion.PRESS -> seat(phase(53 to 56, 71 to 48), phase(70 to 44, 91 to 45))
            Motion.BENCH -> body(32 to 67, 45 to 72, 73 to 75, 90 to 76, 93 to 98,
                phase(52 to 80, 46 to 50), phase(58 to 62, 46 to 31))
            Motion.INCLINE -> body(34 to 42, 44 to 53, 67 to 76, 87 to 77, 90 to 98,
                phase(53 to 64, 53 to 40), phase(60 to 49, 58 to 22))
            Motion.OVERHEAD -> seat(phase(70 to 44, 60 to 29), phase(71 to 31, 60 to 12))
            Motion.FRONT_RAISE -> stand(phase(59 to 57, 72 to 43), phase(61 to 71, 90 to 42))
            Motion.PUSHDOWN -> stand(59 to 55, phase(76 to 48, 62 to 73))
            Motion.EXTENSION -> body(63 to 35, 58 to 46, 45 to 69, 59 to 85, 68 to 98,
                72 to 42, phase(63 to 26, 92 to 40))
            Motion.LEG_EXTENSION -> seat(60 to 59, 60 to 71, phase(76 to 97, 99 to 72))
            Motion.SQUAT -> if (finish) body(66 to 43, 60 to 55, 43 to 75, 72 to 81, 59 to 98, 51 to 64, 65 to 51)
                else stand(44 to 49, 61 to 38)
            Motion.LEG_PRESS -> body(30 to 46, 36 to 57, 52 to 76, phase(73 to 57, 77 to 63),
                phase(94 to 62, 99 to 49), 47 to 68, 62 to 78)
            Motion.SEATED_CURL -> seat(60 to 59, 61 to 70, phase(100 to 72, 59 to 93))
            Motion.LYING_CURL -> body(30 to 66, 45 to 70, 68 to 73, 89 to 74,
                phase(107 to 75, 79 to 55), 37 to 79, 25 to 76)
            Motion.ADDUCTOR -> body(62 to 24, 62 to 39, 62 to 67,
                phase(89 to 76, 72 to 83), phase(95 to 93, 74 to 100), 75 to 55, 78 to 68)
            Motion.CALF -> seat(62 to 56, 73 to 68, phase(77 to 97, 75 to 91))
            Motion.PULLDOWN -> seat(phase(59 to 24, 73 to 55), phase(67 to 12, 65 to 39))
            Motion.ROW -> seat(phase(72 to 52, 46 to 57), phase(96 to 55, 68 to 58), 94 to 87)
            Motion.REVERSE_FLY -> seat(phase(60 to 47, 83 to 43), phase(55 to 49, 99 to 44))
            Motion.SHRUG -> if (finish) body(57 to 28, 55 to 37, 55 to 67, 56 to 84, 57 to 98, 58 to 53, 60 to 68)
                else stand(59 to 57, 61 to 72)
            Motion.HAMMER -> body(40 to 34, 46 to 46, 59 to 72, 79 to 78, 86 to 97,
                53 to 62, phase(55 to 76, 61 to 48))
            Motion.CURL -> seat(68 to 58, phase(87 to 64, 73 to 41))
            Motion.WALK -> body(64 to 26, 60 to 40, 57 to 66, phase(71 to 80, 56 to 85),
                phase(83 to 98, 40 to 98), phase(67 to 50, 53 to 53), phase(79 to 44, 47 to 61))
            Motion.BIKE -> body(71 to 29, 67 to 41, 49 to 61, phase(75 to 67, 70 to 82),
                phase(77 to 83, 69 to 92), 80 to 51, 94 to 51)
            Motion.MOBILITY -> stand(phase(65 to 56, 69 to 30), phase(71 to 69, 79 to 19))
        }
    }
}
