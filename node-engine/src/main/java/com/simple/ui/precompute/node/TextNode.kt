package com.simple.ui.precompute.node

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.utils.CompatRenderNode
import kotlin.math.ceil

// ─────────────────────────────────────────────────────────────────────────────
// TextNode — mô tả một đoạn text cần đo/vẽ.
// TextSpec  — kết quả sau khi đo: giữ Bitmap đã render sẵn, vẽ cực nhanh.
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

        // Pre-render text vào CompatRenderNode để onDrawContent đạt hiệu năng cao nhất.
        val renderNode = CompatRenderNode("TextSpec")
        renderNode.setPosition(0, 0, w.coerceAtLeast(1), h.coerceAtLeast(1))
        renderNode.record { canvas ->
            // Vẽ background nhạt để debug vị trí/kích thước (optional)
            // canvas.drawColor(0x10FF0000)
            canvas.translate(p.left.toFloat(), p.top.toFloat())
            layout.draw(canvas)
        }

        return TextSpec(x, y, w, h, this, renderNode)
    }
}

/**
 * Kết quả đo của [TextNode]. Giữ [CompatRenderNode] đã record sẵn;
 * [onDrawContent] chỉ gọi [CompatRenderNode.draw] — cực nhanh và tối ưu memory.
 */
data class TextSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    override val node: TextNode,
    val renderNode: CompatRenderNode
) : DrawSpec() {

    override var data: String? = node.text.text

    /** Đã pre-render vào renderNode ở measure → luôn static, coalesce-friendly. */
    override val isStatic: Boolean = true

    override fun onDrawContent(canvas: Canvas) {
        renderNode.draw(canvas)
    }

    override fun withPosition(newLeft: Int, newTop: Int) =
        copy(left = newLeft, top = newTop)

    /**
     * Giải phóng display list của renderNode.
     */
    override fun onRelease() {
        Log.d("tuanha", "onRelease: ${node.text.text}  ")
        renderNode.discardDisplayList()
    }
}
