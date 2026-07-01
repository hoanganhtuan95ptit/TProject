package com.simple.ui.precompute.node

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext

// ─────────────────────────────────────────────────────────────────────────────
// ProgressBarNode — horizontal progress bar tương đương:
//
//   <ProgressBar
//       style="?android:attr/progressBarStyleHorizontal"
//       android:layout_width="match_parent"
//       android:layout_height="6dp"
//       android:max="100"
//       android:progress="68"
//       android:progressDrawable="@drawable/progressbar_score_orange" />
//
// Track + progress đều là rounded-rect, fill màu thuần — engine không đụng
// Context / Resources nên mọi màu phải resolve sẵn (Int ARGB).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mô tả một horizontal progress bar.
 *
 * Mặc định [layoutWidth] = [LayoutDimension.MatchParent] (giống `match_parent`),
 * [layoutHeight] = [LayoutDimension.WrapContent] và sẽ dùng [Fixed]/Fixed-height
 * khi bạn truyền vào — đặt `layoutHeight = LayoutDimension.fixed(heightPx)` để
 * khớp với `android:layout_height="6dp"`.
 *
 * [cornerRadius] < 0 (mặc định) ⇒ pill (bo tròn nửa chiều cao). Truyền 0f để
 * vẽ vuông, hoặc giá trị px cụ thể.
 */
data class ProgressBarNode(
    val progress: Int,
    val max: Int = 100,
    val trackColor: Int = 0xFFE6E5DE.toInt(),
    val progressColor: Int = 0xFFFFA726.toInt(),
    /** Bán kính bo góc (px). < 0 ⇒ pill (= height/2). */
    val cornerRadius: Float = -1f,
    override val padding: EdgeInsets = EdgeInsets.ZERO,
    override val layoutWidth: LayoutDimension = LayoutDimension.MatchParent,
    override val layoutHeight: LayoutDimension = LayoutDimension.WrapContent
) : LayoutNode() {

    override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): ProgressBarSpec {
        val p = padding
        val w = layoutWidth.resolve(p.horizontal, c.maxWidth)
        val h = layoutHeight.resolve(p.vertical, c.maxHeight)
        return ProgressBarSpec(
            left = x,
            top = y,
            width = w,
            height = h,
            padding = p,
            progress = progress,
            max = max.coerceAtLeast(1),
            trackColor = trackColor,
            progressColor = progressColor,
            cornerRadius = cornerRadius,
            node = this
        )
    }
}

/**
 * Kết quả đo của [ProgressBarNode]. Paints + RectF pre-allocate, [onDrawContent]
 * zero-allocation.
 */
class ProgressBarSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val padding: EdgeInsets,
    progress: Int,
    max: Int,
    trackColor: Int,
    progressColor: Int,
    cornerRadius: Float,
    override val node: ProgressBarNode
) : DrawSpec() {

    var max: Int = max.coerceAtLeast(1)
        set(value) {
            field = value.coerceAtLeast(1)
        }

    var progress: Int = progress.coerceIn(0, max)
        set(value) {
            field = value.coerceIn(0, this.max)
        }

    var trackColor: Int = trackColor
        set(value) {
            field = value
            trackPaint.color = value
        }

    var progressColor: Int = progressColor
        set(value) {
            field = value
            progressPaint.color = value
        }

    var cornerRadius: Float = cornerRadius
        set(value) {
            field = value
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = this@ProgressBarSpec.trackColor
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = this@ProgressBarSpec.progressColor
    }

    private val trackRect = RectF()
    private val progressRect = RectF()

    private val innerL: Float = padding.left.toFloat()
    private val innerT: Float = padding.top.toFloat()
    private val innerR: Float = (width - padding.right).toFloat()
    private val innerB: Float = (height - padding.bottom).toFloat()
    private val innerW: Float = (innerR - innerL).coerceAtLeast(0f)
    private val innerH: Float = (innerB - innerT).coerceAtLeast(0f)

    init {
        trackRect.set(innerL, innerT, innerR, innerB)
    }

    override fun onDrawContent(canvas: Canvas) {
        if (innerW <= 0f || innerH <= 0f) return

        val radius = resolveRadius()

        // 1. Track nền.
        if (Color.alpha(trackColor) != 0) {
            canvas.drawRoundRect(trackRect, radius, radius, trackPaint)
        }

        // 2. Progress fill.
        if (progress <= 0 || Color.alpha(progressColor) == 0) return
        val ratio = progress.toFloat() / max.toFloat()
        val fillW = innerW * ratio.coerceIn(0f, 1f)
        if (fillW <= 0f) return

        progressRect.set(innerL, innerT, innerL + fillW, innerB)
        canvas.drawRoundRect(progressRect, radius, radius, progressPaint)
    }

    private fun resolveRadius(): Float {
        val maxR = minOf(innerW, innerH) / 2f
        return if (cornerRadius < 0f) maxR else cornerRadius.coerceIn(0f, maxR)
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
        copyTo(newLeft, newTop, width, height)

    override fun withSize(newWidth: Int, newHeight: Int): DrawSpec =
        copyTo(left, top, newWidth.coerceAtLeast(0), newHeight.coerceAtLeast(0))

    private fun copyTo(newLeft: Int, newTop: Int, newWidth: Int, newHeight: Int): ProgressBarSpec =
        ProgressBarSpec(
            left = newLeft,
            top = newTop,
            width = newWidth,
            height = newHeight,
            padding = padding,
            progress = progress,
            max = max,
            trackColor = trackColor,
            progressColor = progressColor,
            cornerRadius = cornerRadius,
            node = node
        )
}
