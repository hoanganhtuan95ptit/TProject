package com.simple.phonetics.ui.precompute

import com.simple.phonetics.ui.precompute.node.Constraints
import com.simple.phonetics.ui.precompute.node.LayoutNode

/**
 * Pure measurement engine. No View, no Context, no main-thread APIs.
 * Safe to call from Dispatchers.Default.
 *
 *   val spec = LayoutEngine.measure(node, Constraints(screenWidth))
 *
 * Engine không biết Text/Image/Linear là gì — mỗi [com.simple.phonetics.ui.precompute.node.LayoutNode] tự đo qua
 * [com.simple.phonetics.ui.precompute.node.LayoutNode.measure]. Thêm node mới = tạo class kế thừa [com.simple.phonetics.ui.precompute.node.LayoutNode] +
 * [DrawSpec], không cần đụng file này.
 */
object LayoutEngine {

    fun measure(node: LayoutNode, constraints: Constraints): DrawSpec =
        MeasureContext.measure(node, constraints, 0, 0)
}

/**
 * Context truyền xuống [LayoutNode.measure] để node container (vd Linear)
 * có thể đệ quy đo các child mà không cần biết concrete type.
 *
 * Hiện stateless — để dạng object cho gọn. Nếu sau này cần cache / shared
 * resources (paint pool, text-layout cache...) thì chuyển sang class với
 * field tương ứng.
 */
object MeasureContext {

    fun measure(
        node: LayoutNode,
        c: Constraints,
        x: Int = 0,
        y: Int = 0
    ): DrawSpec = node.measure(this, c, x, y)
}
