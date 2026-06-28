package com.simple.ui.precompute.node

import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext

// ─────────────────────────────────────────────────────────────────────────────
// Shared types dùng chung cho toàn bộ engine.
// Concrete node types nằm trong file riêng:
//   TextNode   → TextNode.kt
//   ImageNode  → ImageNode.kt
//   LinearNode → LinearNode.kt
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Hard constraints from the parent (in pixels).
 * Use [Int.MAX_VALUE] to express "unbounded".
 */
data class Constraints(
    val maxWidth: Int,
    val maxHeight: Int = Int.MAX_VALUE
)

data class EdgeInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val horizontal: Int get() = left + right
    val vertical: Int get() = top + bottom

    companion object {
        val ZERO = EdgeInsets()
        fun all(v: Int) = EdgeInsets(v, v, v, v)
        fun symmetric(h: Int = 0, v: Int = 0) = EdgeInsets(h, v, h, v)
    }
}

enum class Orientation { HORIZONTAL, VERTICAL }

enum class CrossAlign { START, CENTER, END }

/**
 * Immutable description of a sub-tree to be laid out.
 * Everything here must be safe to read from a background thread:
 * no references to View, Context, Resources, etc.
 *
 * Pre-load Bitmap / resolve color / resolve typeface BEFORE building.
 *
 * **Mở rộng**: thêm 1 loại node mới = tạo class kế thừa [LayoutNode] + 1 class
 * kế thừa [com.simple.ui.precompute.DrawSpec], implement [measure]. Không cần đụng [com.simple.ui.precompute.LayoutEngine] hay
 * các node có sẵn.
 */
abstract class LayoutNode {

    abstract val padding: EdgeInsets

    /**
     * Tự đo và trả về [com.simple.ui.precompute.DrawSpec] tại vị trí ([x], [y]).
     * Dùng [ctx] để đệ quy đo các child nếu là node container.
     */
    abstract fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): DrawSpec
}
