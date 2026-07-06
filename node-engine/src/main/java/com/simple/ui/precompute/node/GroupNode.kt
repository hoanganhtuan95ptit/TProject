package com.simple.ui.precompute.node

import android.graphics.Canvas
import android.view.View
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext

// ─────────────────────────────────────────────────────────────────────────────
// GroupNode — container **open** (kế thừa được) xếp children tuyến tính.
//
// Khác với [LinearNode] là `data class` (Kotlin không cho kế thừa), [GroupNode]
// được thiết kế để subclass:
//   1. Override [children] → thay hoàn toàn danh sách con.
//   2. Override [transformChild] → bọc / đổi từng child ngay trước khi đo.
//   3. Override [buildChildren] → build children động dựa vào state subclass.
//
// Kết quả đo là [GroupSpec] (dùng chung với LinearNode) nên tương thích ngược
// với [com.simple.ui.precompute.PrecomputedDelegate], click dispatch, cache…
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Container tuyến tính có khả năng **kế thừa**.
 *
 * ```
 * // (A) — dùng trực tiếp như LinearNode:
 * val row = GroupNode(
 *     orientation = Orientation.HORIZONTAL,
 *     gap = 8,
 *     initialChildren = listOf(text1, text2)
 * )
 *
 * // (B) — kế thừa A, tự sinh children:
 * class TagRow(private val labels: List<String>) : GroupNode(
 *     orientation = Orientation.HORIZONTAL,
 *     gap = 4
 * ) {
 *
 *     override fun buildChildren(): List<LayoutNode> =
 *         labels.map { TextNode(text = BigText(it)) }
 * }
 * ```
 *
 * @param orientation trục chính xếp children.
 * @param gap khoảng cách giữa children trên trục chính.
 * @param crossAlign căn chỉnh trên trục phụ.
 * @param padding padding bao ngoài container.
 * @param layoutWidth chiều rộng khai báo.
 * @param layoutHeight chiều cao khai báo.
 * @param id stable id cho cache — xem [LayoutNode.id].
 * @param onClick click handler — xem [LayoutNode.onClick].
 * @param initialChildren children mặc định nếu subclass không override.
 */
open class GroupNode(
    val orientation: Orientation = Orientation.VERTICAL,
    val gap: Int = 0,
    val crossAlign: CrossAlign = CrossAlign.START,
    override val padding: EdgeInsets = EdgeInsets.ZERO,
    override val layoutWidth: LayoutDimension = LayoutDimension.WrapContent,
    override val layoutHeight: LayoutDimension = LayoutDimension.WrapContent,
    override val id: Any? = null,
    override val onClick: (() -> Unit)? = null,
    initialChildren: List<LayoutNode> = emptyList()
) : LayoutNode() {

    private val defaultChildren: List<LayoutNode> = initialChildren

    /**
     * Danh sách children hiện tại. Mặc định = giá trị truyền vào constructor
     * (hoặc kết quả [buildChildren] nếu subclass override).
     *
     * Subclass có thể override property này để trả về list được compute từ
     * state riêng. Được đọc **mỗi lần measure** nên có thể thay đổi runtime,
     * nhưng nhớ cấp `id` mới cho node cha để [MeasureContext] biết cache miss.
     */
    protected open val children: List<LayoutNode>
        get() = buildChildren()

    /**
     * Hook build children — override khi subclass muốn logic sinh children
     * mà không cần override cả property [children].
     *
     * Mặc định trả về [initialChildren] truyền vào constructor.
     */
    protected open fun buildChildren(): List<LayoutNode> = defaultChildren

    /**
     * Hook biến đổi 1 child ngay trước khi đo. Ví dụ:
     * - bọc child trong `BackgroundNode` để có nền / viền,
     * - đổi `padding`, `layoutWidth`,
     * - chèn `SpaceNode` xen kẽ,
     * - giữ nguyên và return `child`.
     *
     * Được gọi 1 lần / child / lượt measure, thứ tự theo [children].
     * Mặc định: giữ nguyên.
     */
    protected open fun transformChild(index: Int, child: LayoutNode): LayoutNode = child

    /**
     * Hook chốt danh sách children sau khi đã transform. Subclass có thể
     * override để thêm / bớt / sắp xếp lại toàn bộ list (vd chèn header,
     * footer, separator dạng SpaceNode).
     *
     * Mặc định: trả nguyên [transformed].
     */
    protected open fun finalizeChildren(transformed: List<LayoutNode>): List<LayoutNode> =
        transformed

    /**
     * Hook tạo spec đầu ra — override để trả về subclass của [GroupSpec]
     * (vd thêm nền, overlay, state runtime). Mặc định: [GroupSpec] chuẩn.
     *
     * Ví dụ:
     * ```
     * override fun createSpec(x, y, w, h, placed) =
     *     ShadedGroupSpec(x, y, w, h, placed, this, shadeColor)
     * ```
     */
    protected open fun createSpec(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        placedChildren: List<DrawSpec>
    ): GroupSpec = GroupSpec(x, y, width, height, this, placedChildren)

    final override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): GroupSpec {

        val resolved = resolveChildren()
        return measureLinear(ctx, c, x, y, resolved)
    }

    private fun resolveChildren(): List<LayoutNode> {

        val raw = children
        if (raw.isEmpty()) return emptyList()
        val transformed = ArrayList<LayoutNode>(raw.size)
        for (i in raw.indices) transformed.add(transformChild(i, raw[i]))
        return finalizeChildren(transformed)
    }

    private fun measureLinear(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int,
        kids: List<LayoutNode>
    ): GroupSpec {

        val p = padding
        val measureMaxW = layoutWidth.maxForMeasure(c.maxWidth)
        val measureMaxH = layoutHeight.maxForMeasure(c.maxHeight)
        val innerMaxW = (measureMaxW - p.horizontal).coerceAtLeast(0)
        val innerMaxH = (measureMaxH - p.vertical).coerceAtLeast(0)

        val pass1 = measureChildren(ctx, kids, innerMaxW, innerMaxH)
        val w = layoutWidth.resolve(naturalWidth(pass1, p), c.maxWidth)
        val h = layoutHeight.resolve(naturalHeight(pass1, p), c.maxHeight)
        val crossSlot = crossSlotFor(w, h, p)
        val placed = placeChildren(x, y, pass1.measured, crossSlot, p)
        return createSpec(x, y, w, h, placed)
    }

    private fun measureChildren(
        ctx: MeasureContext,
        kids: List<LayoutNode>,
        innerMaxW: Int,
        innerMaxH: Int
    ): Pass1Result {

        val measured = ArrayList<DrawSpec>(kids.size)
        var mainUsed = 0
        var crossMax = 0
        for (i in kids.indices) {
            val cc = childConstraints(innerMaxW, innerMaxH, mainUsed)
            val s = ctx.measure(kids[i], cc, 0, 0)
            measured.add(s)
            mainUsed += mainOf(s)
            if (i < kids.lastIndex) mainUsed += gap
            val cs = crossOf(s)
            if (cs > crossMax) crossMax = cs
        }
        return Pass1Result(measured, mainUsed, crossMax)
    }

    private fun childConstraints(innerMaxW: Int, innerMaxH: Int, mainUsed: Int): Constraints =
        when (orientation) {
            Orientation.HORIZONTAL -> Constraints((innerMaxW - mainUsed).coerceAtLeast(0), innerMaxH)
            Orientation.VERTICAL -> Constraints(innerMaxW, (innerMaxH - mainUsed).coerceAtLeast(0))
        }

    private fun placeChildren(
        x: Int,
        y: Int,
        measured: List<DrawSpec>,
        crossSlot: Int,
        p: EdgeInsets
    ): List<DrawSpec> {

        if (measured.isEmpty()) return emptyList()
        val placed = ArrayList<DrawSpec>(measured.size)
        var cursor = 0
        for (i in measured.indices) {
            val s = measured[i]
            val pos = childPosition(s, crossSlot, p, cursor)
            placed.add(s.withPosition(x + pos.first, y + pos.second))
            cursor += mainOf(s) + gap
        }
        return placed
    }

    private fun childPosition(
        s: DrawSpec,
        crossSlot: Int,
        p: EdgeInsets,
        cursor: Int
    ): Pair<Int, Int> =
        when (orientation) {
            Orientation.HORIZONTAL -> Pair(
                p.left + cursor,
                p.top + crossOffset(crossSlot, s.height, crossAlign)
            )

            Orientation.VERTICAL -> Pair(
                p.left + crossOffset(crossSlot, s.width, crossAlign),
                p.top + cursor
            )
        }

    private fun naturalWidth(r: Pass1Result, p: EdgeInsets): Int =
        when (orientation) {
            Orientation.HORIZONTAL -> r.mainUsed + p.horizontal
            Orientation.VERTICAL -> r.crossMax + p.horizontal
        }

    private fun naturalHeight(r: Pass1Result, p: EdgeInsets): Int =
        when (orientation) {
            Orientation.HORIZONTAL -> r.crossMax + p.vertical
            Orientation.VERTICAL -> r.mainUsed + p.vertical
        }

    private fun crossSlotFor(w: Int, h: Int, p: EdgeInsets): Int =
        when (orientation) {
            Orientation.HORIZONTAL -> (h - p.vertical).coerceAtLeast(0)
            Orientation.VERTICAL -> (w - p.horizontal).coerceAtLeast(0)
        }

    private fun mainOf(s: DrawSpec): Int =
        if (orientation == Orientation.HORIZONTAL) s.width else s.height

    private fun crossOf(s: DrawSpec): Int =
        if (orientation == Orientation.HORIZONTAL) s.height else s.width

    private data class Pass1Result(
        val measured: List<DrawSpec>,
        val mainUsed: Int,
        val crossMax: Int
    )

    private companion object {

        fun crossOffset(parent: Int, child: Int, align: CrossAlign): Int =
            when (align) {
                CrossAlign.START -> 0
                CrossAlign.CENTER -> (parent - child).coerceAtLeast(0) / 2
                CrossAlign.END -> (parent - child).coerceAtLeast(0)
            }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GroupSpec — kết quả đo của [GroupNode], [LinearNode], [FlexboxNode],
// [ConstraintNode]. Trước đây là `data class` không kế thừa được; giờ là
// `open class` với hook cho subclass tự vẽ nền / overlay / tuỳ biến state.
//
// Subclass **phải override [withPosition]** nếu có state riêng — nếu không
// clone sẽ mất state (fallback về base [GroupSpec]).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Container spec: giữ danh sách [children] đã đo và tự đệ quy vẽ chúng.
 *
 * Draw pipeline cố định (final [onDrawContent]):
 *   1. [drawBackground] — hook, mặc định no-op.
 *   2. [drawChildren]   — vẽ mọi child theo thứ tự.
 *   3. [drawOverlay]    — hook, mặc định no-op.
 *
 * Toạ độ local đã được [DrawSpec.draw] translate về (0, 0), nên khi vẽ trong
 * [drawBackground] / [drawOverlay] dùng bounds `(0, 0, width, height)`.
 *
 * ## Ví dụ subclass
 * ```
 * class ShadedGroupSpec(...) : GroupSpec(...) {
 *
 *     override fun drawBackground(canvas: Canvas) {
 *         canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shadePaint)
 *     }
 *
 *     override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
 *         ShadedGroupSpec(newLeft, newTop, width, height, children, node, shadePaint)
 * }
 * ```
 */
open class GroupSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    override val node: LayoutNode,

    open val children: List<DrawSpec>
) : DrawSpec() {

    override val willDraw: Boolean = false

    final override fun onDrawContent(canvas: Canvas) {
        drawBackground(canvas)
        // KHÔNG gọi drawChildren(canvas) ở đây.
        // Vì toàn bộ children đã được đưa ra danh sách phẳng (collectDraws)
        // và sẽ được View vẽ độc lập để tối ưu hóa hiệu năng và tránh double translation.
        drawOverlay(canvas)
    }

    /**
     * Hook: vẽ **trước** children — nền, viền, gradient. Mặc định no-op.
     *
     * Toạ độ local: `(0, 0)` = góc trái-trên của spec, `(width, height)` =
     * góc phải-dưới. Canvas đã được base translate + clip sẵn.
     */
    protected open fun drawBackground(canvas: Canvas) = Unit

    /**
     * Hook: vẽ **sau** children — overlay, badge, focus ring, watermark.
     * Mặc định no-op. Toạ độ local giống [drawBackground].
     */
    protected open fun drawOverlay(canvas: Canvas) = Unit

    /**
     * Vẽ children theo thứ tự — subclass hiếm khi cần override.
     * Mở `open` phòng khi cần custom (vd đảo thứ tự, skip 1 số child).
     */
    protected open fun drawChildren(canvas: Canvas) {

        for (i in children.indices) children[i].draw(canvas)
    }

    override fun onAttachedToWindow(view: View) {

        for (i in children.indices) children[i].attach(view)
    }

    override fun onDetachedFromWindow(view: View) {

        for (i in children.indices) children[i].detach(view)
    }

    /**
     * Container: KHÔNG cascade release [children]. Delegate walk tree cũ
     * riêng và quyết định từng child dựa vào alive-filter (identity +
     * node.id) — cascade ở đây sẽ release phải các child mà cache-by-id
     * vẫn giữ dùng chung với tree mới (qua withPosition copy).
     *
     * Subclass có state riêng (paint, gradient shader, cached path...) nên
     * override [onRelease] và dọn state đó — vẫn KHÔNG cascade children.
     */
    override fun onRelease() = Unit

    /**
     * Trả spec mới cùng nội dung tại vị trí ([newLeft], [newTop]).
     *
     * ⚠️ **Subclass có state riêng phải override method này** — bản base tạo
     * lại đúng type [GroupSpec], subclass không override → clone mất state
     * (paint, animator, cached data…).
     */
    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec {
        if (newLeft == left && newTop == top) return this
        val dx = newLeft - left
        val dy = newTop - top
        val shiftedChildren = children.map { it.withPosition(it.left + dx, it.top + dy) }
        return GroupSpec(newLeft, newTop, width, height, node, shiftedChildren)
    }

    override fun hitTest(x: Int, y: Int): DrawSpec? {
        if (x < left || y < top || x >= left + width || y >= top + height) return null
        // Duyệt ngược: child vẽ sau (topmost) thắng.
        for (i in children.indices.reversed()) {
            val hit = children[i].hitTest(x, y)
            if (hit != null) return hit
        }
        return if (onClick != null || node.onClick != null) this else null
    }

    override fun collectDrawAndHitSpecs(
        draws: MutableList<DrawSpec>,
        hits: MutableList<DrawSpec>,
        currentDepth: Int
    ) {

        depth = currentDepth
        if (willDraw) draws.add(this)
        if (isClickable) hits.add(this)

        val childDepth = if (willDraw) currentDepth + 1 else currentDepth
        for (i in children.indices) {
            children[i].collectDrawAndHitSpecs(draws, hits, childDepth)
        }
    }

    override fun collectDraws(out: MutableList<DrawSpec>) {
        val hits = ArrayList<DrawSpec>()
        collectDrawAndHitSpecs(out, hits)
    }

    override fun collectHits(out: MutableList<DrawSpec>) {
        val draws = ArrayList<DrawSpec>()
        collectDrawAndHitSpecs(draws, out)
    }
}
