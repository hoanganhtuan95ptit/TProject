package com.simple.phonetics.ui.precompute

import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

/**
 * Pure measurement engine. No View, no Context, no main-thread APIs.
 * Safe to call from Dispatchers.Default.
 *
 *   val spec = LayoutEngine.measure(node, Constraints(screenWidth))
 *
 * Trả về [DrawSpec] (đa hình) — caller / View không cần biết là Text/Image/Group.
 */
object LayoutEngine {

    fun measure(node: LayoutNode, constraints: Constraints): DrawSpec =
        measureAt(node, constraints, 0, 0)

    // -----------------------------------------------------------------

    private fun measureAt(
        node: LayoutNode,
        c: Constraints,
        x: Int,
        y: Int
    ): DrawSpec = when (node) {
        is LayoutNode.Text -> measureText(node, c, x, y)
        is LayoutNode.Image -> measureImage(node, c, x, y)
        is LayoutNode.Linear -> measureLinear(node, c, x, y)
    }

    // -----------------------------------------------------------------

    private fun measureText(
        node: LayoutNode.Text,
        c: Constraints,
        x: Int,
        y: Int
    ): TextSpec {
        val p = node.padding
        val innerWidth = (c.maxWidth - p.horizontal).coerceAtLeast(0)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = node.textSizePx
            color = node.color
            node.typeface?.let { typeface = it }
        }

        val layout = StaticLayout.Builder
            .obtain(node.text, 0, node.text.length, paint, innerWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(node.lineSpacingAdd, node.lineSpacingMul)
            .setMaxLines(node.maxLines)
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

    private fun measureImage(
        node: LayoutNode.Image,
        c: Constraints,
        x: Int,
        y: Int
    ): ImageSpec {
        val p = node.padding
        // Với BitmapSource: lấy width/height theo bitmap nếu node không truyền.
        // Với source khác: init {} đã đảm bảo width/height != null.
        val bitmapSize = (node.source as? ImageSource.BitmapSource)?.bitmap
        val rawW = node.width ?: bitmapSize?.width ?: 0
        val rawH = node.height ?: bitmapSize?.height ?: 0
        val w = rawW + p.horizontal
        val h = rawH + p.vertical
        val dst = Rect(p.left, p.top, p.left + rawW, p.top + rawH)
        return ImageSpec(x, y, w, h, node.source, dst)
    }

    private fun measureLinear(
        node: LayoutNode.Linear,
        c: Constraints,
        x: Int,
        y: Int
    ): GroupSpec {
        val p = node.padding
        val innerMaxW = (c.maxWidth - p.horizontal).coerceAtLeast(0)
        val innerMaxH = (c.maxHeight - p.vertical).coerceAtLeast(0)

        // 1st pass: đo mọi child, chưa cần biết vị trí.
        val measured = ArrayList<DrawSpec>(node.children.size)
        var mainUsed = 0
        var crossMax = 0

        node.children.forEachIndexed { i, child ->
            val cc = when (node.orientation) {
                Orientation.HORIZONTAL ->
                    Constraints((innerMaxW - mainUsed).coerceAtLeast(0), innerMaxH)
                Orientation.VERTICAL ->
                    Constraints(innerMaxW, (innerMaxH - mainUsed).coerceAtLeast(0))
            }
            val s = measureAt(child, cc, 0, 0)
            measured.add(s)

            val mainSize = if (node.orientation == Orientation.HORIZONTAL) s.width else s.height
            val crossSize = if (node.orientation == Orientation.HORIZONTAL) s.height else s.width
            mainUsed += mainSize
            if (i < node.children.lastIndex) mainUsed += node.gap
            if (crossSize > crossMax) crossMax = crossSize
        }

        // 2nd pass: gán vị trí dựa trên cross-align — dùng withPosition(),
        // không cần biết concrete type.
        val placed = ArrayList<DrawSpec>(measured.size)
        var cursor = 0
        for (s in measured) {
            val (cx, cy) = when (node.orientation) {
                Orientation.HORIZONTAL -> {
                    val offCross = crossOffset(crossMax, s.height, node.crossAlign)
                    Pair(p.left + cursor, p.top + offCross)
                }
                Orientation.VERTICAL -> {
                    val offCross = crossOffset(crossMax, s.width, node.crossAlign)
                    Pair(p.left + offCross, p.top + cursor)
                }
            }
            placed.add(s.withPosition(cx, cy))

            val mainSize = if (node.orientation == Orientation.HORIZONTAL) s.width else s.height
            cursor += mainSize + node.gap
        }
        if (placed.isNotEmpty()) cursor -= node.gap

        val (w, h) = when (node.orientation) {
            Orientation.HORIZONTAL -> Pair(cursor + p.horizontal, crossMax + p.vertical)
            Orientation.VERTICAL -> Pair(crossMax + p.horizontal, cursor + p.vertical)
        }
        return GroupSpec(x, y, w, h, placed)
    }

    private fun crossOffset(parent: Int, child: Int, align: CrossAlign): Int =
        when (align) {
            CrossAlign.START -> 0
            CrossAlign.CENTER -> (parent - child) / 2
            CrossAlign.END -> parent - child
        }
}
