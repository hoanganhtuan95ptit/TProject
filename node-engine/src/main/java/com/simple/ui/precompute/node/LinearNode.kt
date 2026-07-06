package com.simple.ui.precompute.node

import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// LinearNode — mô tả một container xếp children theo chiều ngang / dọc.
// LinearChild — wrapper để gán weight cho child trong LinearNode.
// GroupSpec  — kết quả sau khi đo: danh sách DrawSpec con đã gán vị trí.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wrapper cho [LayoutNode] để gán thêm các thuộc tính chỉ có ý nghĩa trong [LinearNode].
 * Tương tự [ConstraintChild] hay [com.simple.ui.precompute.node.FlexChild].
 */
data class LinearChild(
    val node: LayoutNode,
    val weight: Float = 0f
)

/** Extension helper để tạo LinearChild nhanh. */
fun LayoutNode.linearChild(weight: Float = 0f): LinearChild = LinearChild(this, weight)

/**
 * Mô tả một container xếp [children] tuần tự theo [orientation].
 *
 * - [gap]: khoảng cách giữa các children trên trục chính.
 * - [crossAlign]: căn chỉnh trên trục phụ (START / CENTER / END).
 * - [padding]: padding bao ngoài toàn bộ container.
 *
 * Hỗ trợ `weight` thông qua [LinearChild].
 */
data class LinearNode(
    val orientation: Orientation,
    val children: List<LinearChild>,
    val gap: Int = 0,
    val crossAlign: CrossAlign = CrossAlign.START,
    override val padding: EdgeInsets = EdgeInsets.ZERO,
    override val layoutWidth: LayoutDimension = LayoutDimension.WrapContent,
    override val layoutHeight: LayoutDimension = LayoutDimension.WrapContent
) : LayoutNode() {

    override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): GroupSpec {
        val p = padding
        val measureMaxW = layoutWidth.maxForMeasure(c.maxWidth)
        val measureMaxH = layoutHeight.maxForMeasure(c.maxHeight)
        val innerMaxW = (measureMaxW - p.horizontal).coerceAtLeast(0)
        val innerMaxH = (measureMaxH - p.vertical).coerceAtLeast(0)

        val isHorizontal = orientation == Orientation.HORIZONTAL
        val mainMax = if (isHorizontal) innerMaxW else innerMaxH
        val crossMaxLimit = if (isHorizontal) innerMaxH else innerMaxW

        val totalWeight = children.sumOf { it.weight.toDouble() }.toFloat()
        val measured = arrayOfNulls<DrawSpec>(children.size)
        var mainUsed = 0
        var crossMax = 0

        // Pass 1: Đo các child không có weight
        children.forEachIndexed { i, childWrap ->
            if (childWrap.weight <= 0f) {
                val remainingMain = (mainMax - mainUsed).coerceAtLeast(0)
                val cc = if (isHorizontal) {
                    Constraints(remainingMain, crossMaxLimit)
                } else {
                    Constraints(crossMaxLimit, remainingMain)
                }
                val s = ctx.measure(childWrap.node, cc, 0, 0)
                measured[i] = s
                mainUsed += if (isHorizontal) s.width else s.height
                if (i < children.lastIndex) mainUsed += gap
                val crossSize = if (isHorizontal) s.height else s.width
                if (crossSize > crossMax) crossMax = crossSize
            } else {
                if (i < children.lastIndex) mainUsed += gap
            }
        }

        // Pass 2: Phân phối khoảng trống còn lại cho các child có weight
        if (totalWeight > 0f) {
            val remainingMain = (mainMax - mainUsed).coerceAtLeast(0)
            var distributedMain = 0
            val weightedIndices = children.indices.filter { children[it].weight > 0f }

            weightedIndices.forEachIndexed { i, idx ->
                val weight = children[idx].weight
                val share = if (i == weightedIndices.lastIndex) {
                    remainingMain - distributedMain
                } else {
                    (remainingMain * weight / totalWeight).roundToInt()
                }.coerceAtLeast(0)

                distributedMain += share

                val cc = if (isHorizontal) {
                    Constraints(share, crossMaxLimit)
                } else {
                    Constraints(crossMaxLimit, share)
                }

                val s = ctx.measure(children[idx].node, cc, 0, 0)
                // Ép size theo share
                val finalS = if (isHorizontal) {
                    s.withSize(share, s.height)
                } else {
                    s.withSize(s.width, share)
                }

                measured[idx] = finalS
                val crossSize = if (isHorizontal) finalS.height else finalS.width
                if (crossSize > crossMax) crossMax = crossSize
            }
            mainUsed += distributedMain
        }

        val finalMeasured = measured.filterNotNull()

        // Pass 3: Gán vị trí
        val placed = ArrayList<DrawSpec>(finalMeasured.size)
        var cursor = 0
        val naturalMain = if (finalMeasured.isEmpty()) 0 else mainUsed
        val naturalW = if (isHorizontal) naturalMain + p.horizontal else crossMax + p.horizontal
        val naturalH = if (isHorizontal) crossMax + p.vertical else naturalMain + p.vertical

        val w = layoutWidth.resolve(naturalW, c.maxWidth)
        val h = layoutHeight.resolve(naturalH, c.maxHeight)
        val crossSlot = if (isHorizontal) (h - p.vertical).coerceAtLeast(0) else (w - p.horizontal).coerceAtLeast(0)

        children.indices.forEach { i ->
            val s = measured[i] ?: return@forEach
            val (cx, cy) = if (isHorizontal) {
                val offCross = crossOffset(crossSlot, s.height, crossAlign)
                Pair(p.left + cursor, p.top + offCross)
            } else {
                val offCross = crossOffset(crossSlot, s.width, crossAlign)
                Pair(p.left + offCross, p.top + cursor)
            }
            placed.add(s.withPosition(x + cx, y + cy))

            val mainSize = if (isHorizontal) s.width else s.height
            cursor += mainSize + gap
        }

        return GroupSpec(x, y, w, h, this, placed)
    }

    private companion object {
        fun crossOffset(parent: Int, child: Int, align: CrossAlign): Int =
            when (align) {
                CrossAlign.START -> 0
                CrossAlign.CENTER -> (parent - child).coerceAtLeast(0) / 2
                CrossAlign.END -> (parent - child).coerceAtLeast(0)
            }
    }
}
