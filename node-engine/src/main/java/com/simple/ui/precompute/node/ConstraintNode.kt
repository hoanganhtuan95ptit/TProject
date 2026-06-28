package com.simple.ui.precompute.node

import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext

// ─────────────────────────────────────────────────────────────────────────────
// ConstraintDim  — chế độ kích thước của child.
// ConstraintChild — mô tả 1 child với các constraint liên kết.
// ConstraintNode  — container layout theo constraint, tương tự ConstraintLayout.
// Kết quả trả về  — GroupSpec (cùng DrawSpec type với LinearNode).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Kích thước của một child trong [ConstraintNode].
 *
 * - [Fixed]           — cố định (pixel).
 * - [WrapContent]     — đo theo nội dung (mặc định).
 * - [MatchConstraint] — lấp đầy khoảng giữa 2 constraint cùng chiều (= 0dp XML).
 *                       Yêu cầu cả 2 constraint cùng trục phải được set.
 */
sealed class ConstraintDim {
    data class Fixed(val px: Int) : ConstraintDim()
    object WrapContent : ConstraintDim()
    object MatchConstraint : ConstraintDim()
}

/**
 * Một child trong [ConstraintNode].
 *
 * Quy ước đặt tên constraint giống ConstraintLayout XML:
 *   [startToStartOf] = "This.start snaps to target.start"
 *   [startToEndOf]   = "This.start snaps to target.end"
 *   … v.v.
 *
 * Dùng [ConstraintNode.PARENT] làm target ID để trỏ đến container.
 *
 * **Bias** (0f = hugged start/top, 1f = hugged end/bottom, 0.5f = center) chỉ
 * có hiệu lực khi **cả 2** constraint cùng trục được set.
 */
data class ConstraintChild(
    val id: String,
    val node: LayoutNode,
    // ── Horizontal constraints ────────────────────────────────────────────
    /** This.start = target.start */
    val startToStartOf: String? = null,
    /** This.start = target.end   */
    val startToEndOf: String? = null,
    /** This.end   = target.end   */
    val endToEndOf: String? = null,
    /** This.end   = target.start */
    val endToStartOf: String? = null,
    // ── Vertical constraints ──────────────────────────────────────────────
    /** This.top    = target.top    */
    val topToTopOf: String? = null,
    /** This.top    = target.bottom */
    val topToBottomOf: String? = null,
    /** This.bottom = target.bottom */
    val bottomToBottomOf: String? = null,
    /** This.bottom = target.top    */
    val bottomToTopOf: String? = null,
    // ── Margins (px) ─────────────────────────────────────────────────────
    val marginStart: Int = 0,
    val marginEnd: Int = 0,
    val marginTop: Int = 0,
    val marginBottom: Int = 0,
    // ── Dimension mode ────────────────────────────────────────────────────
    val width: ConstraintDim = ConstraintDim.WrapContent,
    val height: ConstraintDim = ConstraintDim.WrapContent,
    // ── Bias (chỉ active khi cả 2 constraint cùng trục được set) ─────────
    val horizontalBias: Float = 0.5f,
    val verticalBias: Float = 0.5f,
)

/**
 * Container layout theo kiểu constraint — tương tự [ConstraintLayout].
 *
 * ## Thuật toán
 * **Iterative topological sort**: mỗi vòng lặp, chọn các child mà tất cả
 * target ID trong constraint đã resolved, đo + gán vị trí, rồi tiếp tục.
 * Worst case **O(n²)** với n children — phù hợp cho < 50 views trong 1 màn.
 *
 * ## Hạn chế hiện tại
 * - Không hỗ trợ chain (horizontal/vertical chain).
 * - Không hỗ trợ Guideline / Barrier.
 * - [ConstraintDim.MatchConstraint] yêu cầu cả 2 constraint cùng trục được set.
 * - Chiều cao container = wrap-to-content (max bottom của children + padding).
 * - Dependency cycle → child bị đặt tại (0, 0) làm fallback.
 *
 * ## Kết quả
 * Trả về [GroupSpec] — cùng kiểu với [LinearNode] để dùng chung [com.simple.ui.precompute.PrecomputedView].
 *
 * ## Ví dụ
 * ```kotlin
 * ConstraintNode(
 *     children = listOf(
 *         ConstraintChild(
 *             id = "title",
 *             node = TextNode("Hello", sp18, Color.BLACK),
 *             startToStartOf = PARENT, endToEndOf = PARENT,
 *             topToTopOf = PARENT, marginTop = dp16,
 *             width = ConstraintDim.MatchConstraint,
 *         ),
 *         ConstraintChild(
 *             id = "subtitle",
 *             node = TextNode("World", sp14, Color.GRAY),
 *             startToStartOf = "title",
 *             topToBottomOf = "title", marginTop = dp8,
 *         ),
 *     )
 * )
 * ```
 */
data class ConstraintNode(
    val children: List<ConstraintChild>,
    override val padding: EdgeInsets = EdgeInsets.ZERO
) : LayoutNode() {

    companion object {
        /** ID reserved trỏ đến container — dùng làm target trong constraint. */
        const val PARENT = "parent"
    }

    override fun measure(ctx: MeasureContext, c: Constraints, x: Int, y: Int): GroupSpec {
        val p = padding
        val innerW = (c.maxWidth - p.horizontal).coerceAtLeast(0)
        val innerH = (c.maxHeight - p.vertical).coerceAtLeast(0)

        // bounds[id] = [left, top, right, bottom] trong toạ độ inner
        val bounds = HashMap<String, IntArray>(children.size + 1)
        bounds[PARENT] = intArrayOf(0, 0, innerW, innerH)

        val specs = HashMap<String, DrawSpec>(children.size)
        val remaining = children.toMutableList()

        // Iterative resolve: mỗi pass giải những child đã đủ dependency
        repeat(children.size + 1) {
            for (child in remaining.toList()) {         // snapshot để an toàn khi remove
                if (!canResolve(child, bounds)) continue

                // 1. Tính kích thước khả dụng để truyền vào đo
                val avW = availW(child, bounds, innerW)
                val avH = availH(child, bounds, innerH)

                // 2. Đo child node (background thread safe)
                val spec = ctx.measure(
                    child.node,
                    Constraints(avW.coerceAtLeast(0), avH.coerceAtLeast(0)),
                    0, 0
                )
                specs[child.id] = spec

                // 3. Kích thước thực của child sau khi biết spec
                val cw = when (val d = child.width) {
                    is ConstraintDim.Fixed -> d.px
                    ConstraintDim.MatchConstraint -> avW
                    ConstraintDim.WrapContent -> spec.width
                }
                val ch = when (val d = child.height) {
                    is ConstraintDim.Fixed -> d.px
                    ConstraintDim.MatchConstraint -> avH
                    ConstraintDim.WrapContent -> spec.height
                }

                // 4. Giải vị trí dựa trên constraint + bias
                val l = resolveLeft(child, bounds, cw, innerW)
                val t = resolveTop(child, bounds, ch, innerH)
                bounds[child.id] = intArrayOf(l, t, l + cw, t + ch)
                remaining.remove(child)
            }
        }

        // Fallback: child còn lại (dependency cycle hoặc thiếu constraint)
        for (child in remaining) {
            val spec = ctx.measure(child.node, Constraints(innerW, innerH), 0, 0)
            specs[child.id] = spec
            bounds[child.id] = intArrayOf(0, 0, spec.width, spec.height)
        }

        // Gán vị trí thực (thêm padding container) và trả GroupSpec
        val placed = children.mapNotNull { child ->
            val b = bounds[child.id] ?: return@mapNotNull null
            specs[child.id]?.withPosition(p.left + b[0], p.top + b[1])
        }

        // Chiều rộng = toàn bộ constraint width; chiều cao = wrap children
        val totalH = (children.mapNotNull { bounds[it.id]?.get(3) }.maxOrNull() ?: 0) + p.vertical
        return GroupSpec(x, y, c.maxWidth, totalH.coerceAtLeast(p.vertical), placed)
    }

    // ── Dependency check ────────────────────────────────────────────────────

    /**
     * Tất cả ID được tham chiếu trong constraint của [child] đã resolved chưa.
     */
    private fun canResolve(child: ConstraintChild, bounds: Map<String, IntArray>): Boolean =
        listOfNotNull(
            child.startToStartOf, child.startToEndOf,
            child.endToEndOf,     child.endToStartOf,
            child.topToTopOf,     child.topToBottomOf,
            child.bottomToBottomOf, child.bottomToTopOf
        ).all { it in bounds }

    // ── Available size ──────────────────────────────────────────────────────

    /**
     * Width khả dụng truyền vào [MeasureContext.measure]:
     * - [ConstraintDim.MatchConstraint] → khoảng giữa 2 anchor ngang.
     * - [ConstraintDim.Fixed]           → giá trị cố định.
     * - [ConstraintDim.WrapContent]     → innerW (child tự co lại sau khi đo).
     */
    private fun availW(child: ConstraintChild, bounds: Map<String, IntArray>, innerW: Int): Int =
        when (child.width) {
            is ConstraintDim.Fixed -> child.width.px
            ConstraintDim.MatchConstraint ->
                (endAnchor(child, bounds, innerW) - startAnchor(child, bounds)).coerceAtLeast(0)
            ConstraintDim.WrapContent -> innerW
        }

    private fun availH(child: ConstraintChild, bounds: Map<String, IntArray>, innerH: Int): Int =
        when (child.height) {
            is ConstraintDim.Fixed -> child.height.px
            ConstraintDim.MatchConstraint ->
                (bottomAnchor(child, bounds, innerH) - topAnchor(child, bounds)).coerceAtLeast(0)
            ConstraintDim.WrapContent -> innerH
        }

    // ── Anchor helpers (tọa độ inner) ──────────────────────────────────────

    private fun startAnchor(c: ConstraintChild, b: Map<String, IntArray>): Int =
        when {
            c.startToStartOf != null -> b[c.startToStartOf]!![0] + c.marginStart
            c.startToEndOf   != null -> b[c.startToEndOf]!![2]   + c.marginStart
            else                     -> c.marginStart
        }

    private fun endAnchor(c: ConstraintChild, b: Map<String, IntArray>, innerW: Int): Int =
        when {
            c.endToEndOf   != null -> b[c.endToEndOf]!![2]   - c.marginEnd
            c.endToStartOf != null -> b[c.endToStartOf]!![0] - c.marginEnd
            else                   -> innerW - c.marginEnd
        }

    private fun topAnchor(c: ConstraintChild, b: Map<String, IntArray>): Int =
        when {
            c.topToTopOf    != null -> b[c.topToTopOf]!![1]    + c.marginTop
            c.topToBottomOf != null -> b[c.topToBottomOf]!![3] + c.marginTop
            else                    -> c.marginTop
        }

    private fun bottomAnchor(c: ConstraintChild, b: Map<String, IntArray>, innerH: Int): Int =
        when {
            c.bottomToBottomOf != null -> b[c.bottomToBottomOf]!![3] - c.marginBottom
            c.bottomToTopOf    != null -> b[c.bottomToTopOf]!![1]    - c.marginBottom
            else                       -> innerH - c.marginBottom
        }

    // ── Position resolution ─────────────────────────────────────────────────

    /**
     * Giải left position (toạ độ inner).
     *
     * - Có cả start + end → dùng [ConstraintChild.horizontalBias] để phân phối
     *   khoảng trống giữa 2 anchor.
     * - Chỉ có start → hugged start.
     * - Chỉ có end   → hugged end.
     * - Không có gì  → 0.
     */
    private fun resolveLeft(c: ConstraintChild, b: Map<String, IntArray>, cw: Int, innerW: Int): Int {
        val hasStart = c.startToStartOf != null || c.startToEndOf != null
        val hasEnd   = c.endToEndOf     != null || c.endToStartOf != null
        val sa = if (hasStart) startAnchor(c, b) else null
        val ea = if (hasEnd) endAnchor(c, b, innerW) else null
        return when {
            hasStart && hasEnd ->
                sa!! + ((ea!! - sa - cw).coerceAtLeast(0) * c.horizontalBias).toInt()
            hasStart -> sa!!
            hasEnd   -> ea!! - cw
            else     -> 0
        }.coerceAtLeast(0)
    }

    /**
     * Giải top position (toạ độ inner).
     *
     * Logic đối xứng với [resolveLeft] theo trục dọc.
     */
    private fun resolveTop(c: ConstraintChild, b: Map<String, IntArray>, ch: Int, innerH: Int): Int {
        val hasTop    = c.topToTopOf       != null || c.topToBottomOf    != null
        val hasBottom = c.bottomToBottomOf != null || c.bottomToTopOf    != null
        val ta = if (hasTop) topAnchor(c, b) else null
        val ba = if (hasBottom) bottomAnchor(c, b, innerH) else null
        return when {
            hasTop && hasBottom ->
                ta!! + ((ba!! - ta - ch).coerceAtLeast(0) * c.verticalBias).toInt()
            hasTop    -> ta!!
            hasBottom -> ba!! - ch
            else      -> 0
        }.coerceAtLeast(0)
    }
}
