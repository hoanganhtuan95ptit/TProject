package com.simple.ui.precompute

import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.LayoutNode

/**
 * Thuật toán đo của một [com.simple.ui.precompute.node.LayoutNode].
 *
 * Node giữ vai trò data mô tả layout, còn policy giữ phần behavior chuyển data
 * đó thành [DrawSpec]. Concrete policy dùng `open class` để node custom có thể
 * kế thừa và chỉ override phần cần đổi.
 */
abstract class MeasurePolicy<N : LayoutNode> {

    abstract fun measure(
        node: N,
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): DrawSpec
}