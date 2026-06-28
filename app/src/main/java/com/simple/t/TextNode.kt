package com.simple.phonetics.ui.precompute

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

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
    val text: CharSequence,
    val textSizePx: Float,
    val color: Int,
    val maxLines: Int = Int.MAX_VALUE,
    val typeface: Typeface? = null,
    val lineSpacingMul: Float = 1f,
    val lineSpacingAdd: Float = 0f,
    override val padding: EdgeInsets = EdgeInsets.ZERO
) : LayoutNode() {

    override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): TextSpec {
        val p = padding
        val innerWidth = (c.maxWidth - p.horizontal).coerceAtLeast(0)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            color = this@TextNode.color
            this@TextNode.typeface?.let { typeface = it }
        }

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, innerWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(lineSpacingAdd, lineSpacingMul)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()

        val usedWidth = (0 until layout.lineCount)
            .maxOfOrNull { layout.getLineWidth(it) }
            ?.toInt()
            ?: 0

        val w = usedWidth.coerceAtMost(innerWidth) + p.horizontal
        val h = layout.height + p.vertical
        return TextSpec(x, y, w, h, p.left, p.top, layout)
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
    val layout: StaticLayout
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
