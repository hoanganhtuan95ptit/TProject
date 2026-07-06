package com.simple.ui.precompute.node

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext
import kotlin.math.ceil

/**
 * Draws a single horizontal or vertical line.
 *
 * Use [dashWidth] + [dashGap] for dashed separators. All sizes are resolved in
 * pixels before constructing the node; the engine does not touch resources.
 */
data class LineNode(
    val color: Int = Color.BLACK,
    val strokeWidth: Float = 1f,
    val orientation: Orientation = Orientation.HORIZONTAL,
    val dashWidth: Float = 0f,
    val dashGap: Float = 0f,
    val strokeCap: Paint.Cap = Paint.Cap.ROUND,
    override val padding: EdgeInsets = EdgeInsets.ZERO,
    override val layoutWidth: LayoutDimension = LayoutDimension.MatchParent,
    override val layoutHeight: LayoutDimension = LayoutDimension.WrapContent
) : LayoutNode() {

    override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): LineSpec {

        val p = padding
        val thickness = ceil(strokeWidth.coerceAtLeast(0f).toDouble()).toInt()
        val contentW = if (orientation == Orientation.HORIZONTAL) 0 else thickness
        val contentH = if (orientation == Orientation.HORIZONTAL) thickness else 0
        val w = layoutWidth.resolve(contentW + p.horizontal, c.maxWidth)
        val h = layoutHeight.resolve(contentH + p.vertical, c.maxHeight)

        return LineSpec(
            left = x,
            top = y,
            width = w,
            height = h,
            padding = p,
            color = color,
            strokeWidth = strokeWidth,
            orientation = orientation,
            dashWidth = dashWidth,
            dashGap = dashGap,
            strokeCap = strokeCap,
            node = this
        )
    }
}

class LineSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val padding: EdgeInsets,
    color: Int,
    strokeWidth: Float,
    val orientation: Orientation,
    dashWidth: Float,
    dashGap: Float,
    strokeCap: Paint.Cap,
    override val node: LineNode
) : DrawSpec() {

    /** Vẽ 1 line với paint bất biến giữa các frame → static. */
    override val isStatic: Boolean = true

    var color: Int = color
        set(value) {
            field = value
            paint.color = value
        }

    var strokeWidth: Float = strokeWidth.coerceAtLeast(0f)
        set(value) {
            field = value.coerceAtLeast(0f)
            paint.strokeWidth = field
        }

    var dashWidth: Float = dashWidth.coerceAtLeast(0f)
        set(value) {
            field = value.coerceAtLeast(0f)
            updateDashEffect()
        }

    var dashGap: Float = dashGap.coerceAtLeast(0f)
        set(value) {
            field = value.coerceAtLeast(0f)
            updateDashEffect()
        }

    var strokeCap: Paint.Cap = strokeCap
        set(value) {
            field = value
            paint.strokeCap = value
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.color = this@LineSpec.color
        this.strokeWidth = this@LineSpec.strokeWidth
        this.strokeCap = this@LineSpec.strokeCap
    }

    init {
        updateDashEffect()
    }

    override fun onDrawContent(canvas: Canvas) {

        if (strokeWidth <= 0f || Color.alpha(color) == 0) return

        when (orientation) {
            Orientation.HORIZONTAL -> drawHorizontal(canvas)
            Orientation.VERTICAL -> drawVertical(canvas)
        }
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
        copyTo(newLeft, newTop, width, height)

    override fun withSize(newWidth: Int, newHeight: Int): DrawSpec =
        copyTo(left, top, newWidth.coerceAtLeast(0), newHeight.coerceAtLeast(0))

    private fun drawHorizontal(canvas: Canvas) {

        val startX = padding.left.toFloat()
        val endX = (width - padding.right).toFloat()
        if (endX <= startX) return

        val innerTop = padding.top.toFloat()
        val innerBottom = (height - padding.bottom).toFloat()
        val y = innerTop + (innerBottom - innerTop).coerceAtLeast(0f) / 2f
        canvas.drawLine(startX, y, endX, y, paint)
    }

    private fun drawVertical(canvas: Canvas) {

        val startY = padding.top.toFloat()
        val endY = (height - padding.bottom).toFloat()
        if (endY <= startY) return

        val innerLeft = padding.left.toFloat()
        val innerRight = (width - padding.right).toFloat()
        val x = innerLeft + (innerRight - innerLeft).coerceAtLeast(0f) / 2f
        canvas.drawLine(x, startY, x, endY, paint)
    }

    private fun updateDashEffect() {

        paint.pathEffect = if (dashWidth > 0f && dashGap > 0f) {
            DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
        } else {
            null
        }
    }

    private fun copyTo(newLeft: Int, newTop: Int, newWidth: Int, newHeight: Int): LineSpec =
        LineSpec(
            left = newLeft,
            top = newTop,
            width = newWidth,
            height = newHeight,
            padding = padding,
            color = color,
            strokeWidth = strokeWidth,
            orientation = orientation,
            dashWidth = dashWidth,
            dashGap = dashGap,
            strokeCap = strokeCap,
            node = node
        )
}
