package com.simple.ui.precompute.node

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext
import com.simple.ui.precompute.text.BigText
import kotlin.math.ceil

// ─────────────────────────────────────────────────────────────────────────────
// TextNode — mô tả một đoạn text cần đo/vẽ.
// TextSpec  — kết quả sau khi đo: giữ StaticLayout đã build sẵn, vẽ 0-alloc.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mô tả một đoạn text cần layout.
 *
 * Mọi tham số phải resolve sẵn trước khi truyền vào (color là Int,
 * typeface đã load) — engine không được đụng Context.
 */
data class TextNode(
    val text: BigText,
    val textSizePx: Float = 1f,
    val color: Int = Color.TRANSPARENT,
    val maxLines: Int = Int.MAX_VALUE,
    val typeface: Typeface? = null,
    val lineSpacingMul: Float = 1f,
    val lineSpacingAdd: Float = 0f,
    override val padding: EdgeInsets = EdgeInsets.ZERO,
    override val layoutWidth: LayoutDimension = LayoutDimension.WrapContent,
    override val layoutHeight: LayoutDimension = LayoutDimension.WrapContent,
    val textPaintDensity: Float = Resources.getSystem().displayMetrics.density
) : LayoutNode() {

    override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): TextSpec {
        val p = padding
        val measureWidth = layoutWidth.maxForMeasure(c.maxWidth)
        val innerWidth = (measureWidth - p.horizontal).coerceAtLeast(0)
        val textChar = text.textChar

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            density = textPaintDensity
            color = this@TextNode.color
            this@TextNode.typeface?.let { typeface = it }
        }

        val layout = StaticLayout.Builder
            .obtain(textChar, 0, textChar.length, paint, innerWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(lineSpacingAdd, lineSpacingMul)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()

        val usedWidth = (0 until layout.lineCount)
            .maxOfOrNull { layout.getLineWidth(it) }
            ?.let { ceil(it.toDouble()).toInt() }
            ?: 0

        val contentW = usedWidth.coerceAtMost(innerWidth) + p.horizontal
        val contentH = layout.height + p.vertical
        val w = layoutWidth.resolve(contentW, c.maxWidth)
        val h = layoutHeight.resolve(contentH, c.maxHeight)
        return TextSpec(x, y, w, h, p.left, p.top, layout, this)
    }
}

/**
 * Kết quả đo của [TextNode]. Giữ [StaticLayout] đã build sẵn;
 * [onDrawContent] chỉ gọi [StaticLayout.draw] — zero allocation.
 */
data class TextSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val contentLeft: Int,
    val contentTop: Int,
    val layout: StaticLayout,
    override val node: TextNode
) : DrawSpec() {

    override fun onDrawContent(canvas: Canvas) {
        if (contentLeft != 0 || contentTop != 0) {
            canvas.translate(contentLeft.toFloat(), contentTop.toFloat())
        }
        layout.draw(canvas)
    }

    override fun withPosition(newLeft: Int, newTop: Int) =
        copy(left = newLeft, top = newTop)
}
