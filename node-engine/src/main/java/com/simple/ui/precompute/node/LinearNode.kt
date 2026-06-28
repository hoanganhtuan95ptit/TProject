package com.simple.ui.precompute.node

import android.graphics.Canvas
import android.view.View
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext

// ─────────────────────────────────────────────────────────────────────────────
// LinearNode — mô tả một container xếp children theo chiều ngang / dọc.
// GroupSpec  — kết quả sau khi đo: danh sách DrawSpec con đã gán vị trí.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mô tả một container xếp [children] tuần tự theo [orientation].
 *
 * - [gap]: khoảng cách giữa các children trên trục chính.
 * - [crossAlign]: căn chỉnh trên trục phụ (START / CENTER / END).
 * - [padding]: padding bao ngoài toàn bộ container.
 *
 * Đo 2 lượt:
 *   1. Pass 1 — đo tất cả children để biết cross size lớn nhất.
 *   2. Pass 2 — gán vị trí với cross-align qua [com.simple.ui.precompute.DrawSpec.withPosition].
 */
data class LinearNode(
    val orientation: Orientation,
    val children: List<LayoutNode>,
    val gap: Int = 0,
    val crossAlign: CrossAlign = CrossAlign.START,
    override val padding: EdgeInsets = EdgeInsets.ZERO
) : LayoutNode() {

    override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): GroupSpec {
        val p = padding
        val innerMaxW = (c.maxWidth - p.horizontal).coerceAtLeast(0)
        val innerMaxH = (c.maxHeight - p.vertical).coerceAtLeast(0)

        // 1st pass: đo mọi child, chưa cần biết vị trí.
        val measured = ArrayList<DrawSpec>(children.size)
        var mainUsed = 0
        var crossMax = 0

        children.forEachIndexed { i, child ->
            val cc = when (orientation) {
                Orientation.HORIZONTAL ->
                    Constraints((innerMaxW - mainUsed).coerceAtLeast(0), innerMaxH)
                Orientation.VERTICAL ->
                    Constraints(innerMaxW, (innerMaxH - mainUsed).coerceAtLeast(0))
            }
            val s = ctx.measure(child, cc, 0, 0)
            measured.add(s)

            val mainSize = if (orientation == Orientation.HORIZONTAL) s.width else s.height
            val crossSize = if (orientation == Orientation.HORIZONTAL) s.height else s.width
            mainUsed += mainSize
            if (i < children.lastIndex) mainUsed += gap
            if (crossSize > crossMax) crossMax = crossSize
        }

        // 2nd pass: gán vị trí dựa trên cross-align — dùng withPosition(),
        // không cần biết concrete type.
        val placed = ArrayList<DrawSpec>(measured.size)
        var cursor = 0
        for (s in measured) {
            val (cx, cy) = when (orientation) {
                Orientation.HORIZONTAL -> {
                    val offCross = crossOffset(crossMax, s.height, crossAlign)
                    Pair(p.left + cursor, p.top + offCross)
                }
                Orientation.VERTICAL -> {
                    val offCross = crossOffset(crossMax, s.width, crossAlign)
                    Pair(p.left + offCross, p.top + cursor)
                }
            }
            placed.add(s.withPosition(cx, cy))

            val mainSize = if (orientation == Orientation.HORIZONTAL) s.width else s.height
            cursor += mainSize + gap
        }
        if (placed.isNotEmpty()) cursor -= gap

        val (w, h) = when (orientation) {
            Orientation.HORIZONTAL -> Pair(cursor + p.horizontal, crossMax + p.vertical)
            Orientation.VERTICAL -> Pair(crossMax + p.horizontal, cursor + p.vertical)
        }
        return GroupSpec(x, y, w, h, placed)
    }

    private companion object {
        fun crossOffset(parent: Int, child: Int, align: CrossAlign): Int =
            when (align) {
                CrossAlign.START -> 0
                CrossAlign.CENTER -> (parent - child) / 2
                CrossAlign.END -> parent - child
            }
    }
}

/**
 * Kết quả đo của [LinearNode]. Đệ quy: mỗi child tự [draw] trong toạ độ
 * đã translate của GroupSpec — không cần biết concrete type của child.
 */
data class GroupSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val children: List<DrawSpec>
) : DrawSpec() {

    override fun onDrawContent(canvas: Canvas) {
        for (i in children.indices) children[i].draw(canvas)
    }

    override fun onAttachedToWindow(view: View) {
        for (i in children.indices) children[i].onAttachedToWindow(view)
    }

    override fun onDetachedFromWindow(view: View) {
        for (i in children.indices) children[i].onDetachedFromWindow(view)
    }

    override fun withPosition(newLeft: Int, newTop: Int) =
        copy(left = newLeft, top = newTop)
}
