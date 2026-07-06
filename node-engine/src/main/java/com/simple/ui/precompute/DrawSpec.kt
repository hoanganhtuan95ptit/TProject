package com.simple.ui.precompute

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.GroupSpec
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.utils.CompatRenderNode
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// DrawSpec — abstract base class cho mọi "lệnh vẽ" đã đo sẵn.
// Concrete spec types nằm trong file riêng:
//   TextSpec  → TextNode.kt
//   ImageSpec → ImageNode.kt
//   GroupSpec → LinearNode.kt
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Một "lệnh vẽ" đã đo sẵn. Tự biết vị trí, kích thước và cách vẽ chính mình.
 *
 * Base class lo wrap canvas.save() / translate() / restore() — subclass chỉ
 * cần implement [onDrawContent] để vẽ trong toạ độ local (đã translate sẵn).
 *
 * Mở rộng: muốn thêm Border, Divider, Shape... → tạo class mới kế thừa
 * [DrawSpec], không cần sửa View hay LayoutEngine (nếu tự đo).
 */
abstract class DrawSpec {

    companion object {
        internal val drawDepth = ThreadLocal.withInitial { 0 }

        /**
         * Bật log cây vẽ + timing. **Phải tắt ở production**: khi bật, mỗi
         * frame mỗi spec build string indent + 2 lần Log.d — với nhiều spec
         * (vd nhiều TextSpec) chi phí log vượt xa chi phí vẽ thật và gây lag.
         */
        var DEBUG_LOG = true
    }

    /** Chỉ dùng cho log — lazy để copy()/withPosition không tốn UUID alloc. */
    val uuid: String by lazy(LazyThreadSafetyMode.NONE) {
        "${this.javaClass.name}  ${UUID.randomUUID()}"
    }

    abstract val left: Int
    abstract val top: Int
    abstract val width: Int
    abstract val height: Int

    /**
     * [LayoutNode] gốc đã sinh ra spec này.
     *
     * Dùng để so sánh khi cập nhật spec: nếu node mới structurally-equal với
     * node của spec cũ, [PrecomputedDelegate] có thể tái sử dụng spec cũ,
     * tránh requestLayout / mất state runtime (drawable, animator...).
     *
     * `open` + mặc định `null` cho tương thích ngược; concrete spec nên override
     * và trả về node đã tạo ra chính nó (thường qua constructor param).
     */
    open val node: LayoutNode? = null

    /**
     * Click handler có thể được gán động sau khi đo (runtime).
     * Nếu null, engine sẽ fall-back về `node.onClick`.
     */
    open var onClick: (() -> Unit)? = null

    val right: Int get() = left + width
    val bottom: Int get() = top + height

    private val traceSectionName: String by lazy(LazyThreadSafetyMode.NONE) {
        "DrawSpec.${javaClass.simpleName}"
    }

    open var data: String? = null

    /**
     * Độ sâu trong cây draw phẳng.
     *
     * Chỉ tăng khi đi qua một parent có mặt trong draw list. Container thuần
     * không vẽ sẽ bị dissolve nên không làm child sâu thêm.
     */
    var depth: Int = 0

    /**
     * Spec này có trực tiếp thực hiện lệnh vẽ lên Canvas thông qua [onDrawContent] hay không.
     *
     * - `true` (TextSpec, ImageSpec, BackgroundSpec...): Chứa logic vẽ thực sự.
     *   Engine sẽ thực hiện save/translate/restore khi gọi [draw].
     * - `false` (GroupSpec, SizedSpec, LayoutSpacer...): Chỉ là container hoặc
     *   placeholder đo đạc, [onDrawContent] của chúng thường chỉ đệ quy hoặc để trống.
     *
     * Giúp tối ưu performance: bỏ qua các bước setup canvas cho container trung gian.
     */
    open val willDraw: Boolean = true

    /**
     * Spec này có nội dung **không đổi giữa các frame** hay không.
     *
     * - `true` (Background, Text, Line, Space...): pixels chỉ phụ thuộc measure
     *   input, không animate, không load async. Có thể gom vào [CachedDrawSpec]
     *   để pre-render một lần ra bitmap → nhiều spec = 1 lệnh `drawBitmap`.
     * - `false` (Image, Outline animation, ProgressBar...): cần vẽ lại mỗi
     *   frame hoặc pixels đến sau measure (async load, animator). Không được
     *   gom cache, luôn tự vẽ.
     *
     * Mặc định `false` để **an toàn** — subclass tự khẳng định static thì
     * override thành `true`. Yêu cầu bổ sung khi bật static:
     * - `onDrawContent` phải deterministic theo state hiện tại (không phụ thuộc
     *   frame counter, thời gian, animator).
     * - Mọi mutation runtime (vd `BackgroundSpec.backgroundColor = ...`) sẽ
     *   **không tự invalidate bitmap cache** — nếu cần đổi thì phải rebuild
     *   qua measure lại.
     */
    open val isStatic: Boolean = false

    /**
     * Gọi từ parent (View hoặc GroupSpec). Toạ độ canvas hiện tại = parent.
     *
     * **Leaf giữ `clipRect`**: đo trên perfetto trace thấy bỏ clip thì
     * `Drawing` trên RenderThread tăng ~2× và `CircularRRectOp` avg 3× —
     * do AA spill của `drawRoundRect` / text rasterization tràn ra rìa,
     * overlap với sibling gây overdraw + HWUI mất cơ hội cull sớm. Tiết
     * kiệm 1 op display list không bù lại được chi phí GPU thêm.
     *
     * **Container [GroupSpec]** thì clip là dư (children đã tự clip qua
     * leaf path).
     */
    open fun draw(canvas: Canvas) {
        if (!willDraw) {
            onDrawContent(canvas)
            return
        }

        if (!DEBUG_LOG) {
            val saved = canvas.save()
            canvas.translate(left.toFloat(), top.toFloat())
            try {
                onDrawContent(canvas)
            } finally {
                canvas.restoreToCount(saved)
            }
            return
        }

        val depth = drawDepth.get() ?: 0
        val indent = "│  ".repeat(depth)
        val start = System.nanoTime()

        val idString = node?.id?.let { " [id=$it]" } ?: " [uuid=${uuid.takeLast(4)}]"
        Log.d("DrawTree", "$indent┌── ${javaClass.simpleName}$idString ($width x $height) @($left, $top) data:$data")

        drawDepth.set(depth + 1)
        val saved = canvas.save()
        canvas.translate(left.toFloat(), top.toFloat())
        try {
            onDrawContent(canvas)
        } finally {
            canvas.restoreToCount(saved)
            drawDepth.set(depth)
        }

        val end = System.nanoTime()
        val timeStr = " took ${(end - start) / 1_000_000f} ms"
        Log.d("DrawTree", "$indent└── ${javaClass.simpleName}$idString$timeStr")
    }

    /** Vẽ nội dung trong toạ độ local của spec này. */
    protected abstract fun onDrawContent(canvas: Canvas)

    /** Trả về copy với (left, top) mới — phục vụ layout engine khi assign vị trí. */
    abstract fun withPosition(newLeft: Int, newTop: Int): DrawSpec

    /**
     * Trả về copy với kích thước mới. Mặc định bọc spec hiện tại trong một
     * measured box; spec nào phụ thuộc trực tiếp vào bounds có thể override.
     */
    open fun withSize(newWidth: Int, newHeight: Int): DrawSpec {
        val w = newWidth.coerceAtLeast(0)
        val h = newHeight.coerceAtLeast(0)
        return if (width == w && height == h) {
            this
        } else {
            SizedSpec(left, top, w, h, withPosition(left, top))
        }
    }

    /**
     * Reference-counted attach.
     *
     * Cùng một [DrawSpec] có thể nằm trong **cả cây spec cũ lẫn cây spec mới**
     * (do cache-by-id trong [MeasureContext] tái sử dụng). Nếu container cứ
     * gọi thẳng [onAttachedToWindow] / [onDetachedFromWindow] khi đệ quy vào
     * children, shared ref sẽ chịu chu kỳ detach→attach vô ích — animator
     * (OutlineSpec) restart, scope (ImageSpec) huỷ+tái tạo, callback re-set.
     *
     * Counter đảm bảo:
     * - [attach]: chỉ gọi [onAttachedToWindow] khi counter đi từ 0→1
     *   (lần đầu spec này gắn vào view).
     * - [detach]: chỉ gọi [onDetachedFromWindow] khi counter đi từ 1→0
     *   (spec không còn nằm trong tree nào của view).
     *
     * Container spec (GroupSpec, SizedSpec) đệ quy children qua **[attach] /
     * [detach]** (không phải [onAttachedToWindow] / [onDetachedFromWindow]).
     * Delegate cũng gọi [attach] / [detach] chứ không gọi thẳng hook.
     */
    fun attach(view: View) {
        val wasZero = attachCount == 0
        attachCount++
        if (wasZero) onAttachedToWindow(view)
    }

    fun detach(view: View) {
        if (attachCount == 0) return
        attachCount--
        if (attachCount == 0) onDetachedFromWindow(view)
    }

    private var attachCount: Int = 0

    /**
     * Hook cho subclass setup (start animator, kick off async load...).
     *
     * KHÔNG gọi trực tiếp từ container hay delegate — dùng [attach] để đi
     * qua reference counter. Container recurse vào children cũng phải qua
     * `child.attach(view)`.
     */
    open fun onAttachedToWindow(view: View) {}

    /**
     * Hook cho subclass teardown (stop animator, cancel scope, clear callback...).
     *
     * KHÔNG gọi trực tiếp — dùng [detach] để đi qua reference counter.
     */
    open fun onDetachedFromWindow(view: View) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Release — giải phóng resource nặng (bitmap, drawable, scope, animator...)
    //
    // Khác với [detach]: detach chỉ "tạm nghỉ" (view rời window, có thể quay
    // lại). Release là **chấm dứt** — spec sẽ không còn được vẽ, không còn
    // được reuse. Sau [release] mọi tương tác đều là no-op / undefined.
    //
    // Hai chỗ gọi tiêu chuẩn (do [PrecomputedDelegate] đảm nhận):
    //
    //  1. **Bị thay thế bởi spec/result khác**: khi `delegate.result` được set
    //     giá trị mới, delegate diff old vs new. Spec chỉ có trong old (không
    //     shared với new qua identity) → release. Spec dùng chung nhờ
    //     cache-by-id vẫn sống trong new → **không release**.
    //
    //  2. **Lifecycle owner ON_DESTROY**: khi
    //     `view.findViewTreeLifecycleOwner()` bắn DESTROY, delegate release
    //     toàn bộ current result và null hoá `field` → spec ra khỏi mọi ref
    //     giữ bởi delegate.
    //
    // Subclass override [onRelease] để dọn resource của riêng mình
    // (bitmap.recycle, scope.cancel, drawable.callback = null...).
    // Container spec (SizedSpec, GroupSpec, CachedDrawSpec) đệ quy children
    // qua [release] để guard idempotent lan xuống.
    // ─────────────────────────────────────────────────────────────────────────

    /** Đã gọi [release] chưa. `true` = mọi resource nặng đã dọn, không tái sử dụng. */
    @Volatile
    var isReleased: Boolean = false
        private set

    /**
     * Idempotent double-release guard. Chỉ gọi [onRelease] đúng một lần cho
     * mỗi instance — spec share ref (cache hit) không bị release chồng.
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        onRelease()
    }

    /**
     * Hook cho subclass giải phóng resource nặng.
     *
     * Gọi sau khi flag [isReleased] đã set → an toàn để re-entrancy check.
     * Mặc định no-op — leaf spec không có resource ngoài GC-managed refs
     * không cần override.
     *
     * Sau khi return, spec KHÔNG cần đảm bảo vẽ được nữa — nhưng nếu vẫn còn
     * nguy cơ bị gọi [onDrawContent] (vd race với UI thread), subclass nên
     * tự guard bên trong onDrawContent (xem [CachedDrawSpec.onDrawContent]
     * check [android.graphics.Bitmap.isRecycled]).
     */
    protected open fun onRelease() {}

    /**
     * Spec này (đã đo trước) có còn dùng được dưới constraint mới [c] không.
     *
     * Dùng bởi [MeasureContext] khi tra cache theo `node.id` — trước khi bỏ
     * qua `node.measure()`, ta phải chắc kết quả nếu đo lại vẫn ra đúng
     * kích thước hiện tại.
     *
     * Logic suy từ [LayoutDimension.resolve] của mỗi axis:
     * - **Fixed(px)**: `resolve = min(px, max)`. Reuse an toàn khi
     *   `cached == px && px <= max` (cached chưa từng bị cap và max mới đủ chỗ).
     * - **MatchParent**: `resolve = max` (unbounded → wrap). Reuse an toàn
     *   khi `cached == max`. Trường hợp max mới unbounded thì skip cache
     *   vì kết quả sẽ phụ thuộc contentSize không lưu ở đây.
     * - **WrapContent**: `resolve = min(content, max)`. Bảo thủ: chỉ reuse
     *   khi `cached < max` — vì nếu `cached == max` thì có thể đã bị cap
     *   (content > max), lần đo mới với max khác sẽ ra khác. Trường hợp
     *   `cached < max` giả định content chính bằng cached (uncapped).
     *
     * Trả về `false` khi [node] null (spec ẩn danh — không đủ metadata suy).
     */
    open fun canReuseUnder(c: Constraints): Boolean {
        val n = node ?: return false
        return axisReusable(n.layoutWidth, width, c.maxWidth) &&
                axisReusable(n.layoutHeight, height, c.maxHeight)
    }

    /** Spec này có nhận click không (runtime [onClick] hoặc [LayoutNode.onClick]). */
    val isClickable: Boolean
        get() = onClick != null || node?.onClick != null

    /**
     * Flatten pass — gom draw + hit trong cùng một lượt duyệt cây.
     *
     * [currentDepth] là số ancestor có mặt trong draw list. Parent không vẽ
     * không làm tăng depth của child vì nó bị dissolve khỏi list phẳng.
     */
    open fun collectDrawAndHitSpecs(
        draws: MutableList<DrawSpec>,
        hits: MutableList<DrawSpec>,
        currentDepth: Int = 0
    ) {

        depth = currentDepth
        if (willDraw) draws.add(this)
        if (isClickable) hits.add(this)
    }

    /**
     * Flatten pass — gom các spec **thực sự vẽ** vào [out] theo đúng thứ tự vẽ
     * (painter's order). Toạ độ spec đã là absolute (container không translate
     * canvas) nên list phẳng vẽ ra pixel giống hệt vẽ theo tree.
     *
     * - Leaf (mặc định): add chính nó nếu [willDraw] là true.
     * - Container thuần (GroupSpec/FlexboxSpec/SizedSpec): **không add chính
     *   nó**, đệ quy vào children — [com.simple.ui.precompute.PrecomputedView]
     *   không cần biết các spec không vẽ.
     * - Subclass container có tự vẽ (drawBackground/drawOverlay/custom draw):
     *   giữ nguyên add-self → spec tự vẽ subtree của nó như cũ.
     *
     * Lifecycle attach/detach của delegate chạy trên list này — spec nào cần
     * attach hook (animator, image load...) đều là spec có vẽ nên luôn có mặt.
     */
    open fun collectDraws(out: MutableList<DrawSpec>) {
        val hits = ArrayList<DrawSpec>()
        collectDrawAndHitSpecs(out, hits)
    }

    /**
     * Flatten pass — gom các spec **có thể nhận click** vào [out] theo
     * pre-order (parent trước children). Hit-test duyệt **ngược** list này →
     * thứ tự trùng khớp đệ quy [hitTest] cũ: child vẽ sau (topmost) thắng,
     * parent sau cùng.
     *
     * Mặc định: add nếu [isClickable]. Container thuần đệ quy children;
     * subclass có [hitTest] tuỳ biến nên add chính nó (xem GroupSpec).
     */
    open fun collectHits(out: MutableList<DrawSpec>) {
        val draws = ArrayList<DrawSpec>()
        collectDrawAndHitSpecs(draws, out)
    }

    /**
     * Hit-test tại điểm ([x], [y]) trong hệ toạ độ **local của parent**
     * (view space nếu spec này là root).
     *
     * Trả về spec sâu nhất (top-most con) có `node?.onClick != null` bao phủ
     * điểm này. Không có → null.
     *
     * Base impl chỉ check bounds + [LayoutNode.onClick] của [node]. Container
     * spec (GroupSpec, SizedSpec) override để đệ quy vào children — child vẽ
     * sau (topmost) được ưu tiên.
     */
    open fun hitTest(x: Int, y: Int): DrawSpec? {
        val lx = x - left
        val ly = y - top
        if (lx < 0 || ly < 0 || lx >= width || ly >= height) return null
        return if (this.onClick != null || node?.onClick != null) this else null
    }

    private fun axisReusable(mode: LayoutDimension, cached: Int, maxAvail: Int): Boolean {
        if (maxAvail == Int.MAX_VALUE) {
            // Unbounded parent: Fixed & WrapContent chỉ phụ thuộc node content,
            // cached vẫn đúng. MatchParent unbounded falls back to wrap semantics
            // → không đủ info để so, skip cache cho case này.
            return mode !is LayoutDimension.MatchParent
        }
        return when (mode) {
            is LayoutDimension.Fixed -> mode.px == cached && cached <= maxAvail
            LayoutDimension.MatchParent -> cached == maxAvail
            LayoutDimension.WrapContent -> cached < maxAvail
        }
    }
}

/**
 * A measured box that delegates drawing/lifecycle to a child spec.
 * Used when parent layout rules force a size different from the child's
 * natural measured size.
 */
internal data class SizedSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val child: DrawSpec
) : DrawSpec() {

    override val willDraw: Boolean = false

    /** Delegate node cho child — SizedSpec chỉ là size adapter, không phải node riêng. */
    override val node: LayoutNode?
        get() = child.node

    override fun draw(canvas: Canvas) {
        if (!willDraw || !DEBUG_LOG) {
            onDrawContent(canvas)
            return
        }
        val depth = drawDepth.get() ?: 0
        val indent = "│  ".repeat(depth)
        val idString = node?.id?.let { " [id=$it]" } ?: " [uuid=${uuid.takeLast(4)}]"
        Log.d("DrawTree", "$indent┌── ${javaClass.simpleName}$idString ($width x $height) @($left, $top) - skip isolation")
        drawDepth.set(depth + 1)
        try {
            onDrawContent(canvas)
        } finally {
            drawDepth.set(depth)
            Log.d("DrawTree", "$indent└── ${javaClass.simpleName}$idString")
        }
    }

    override fun onDrawContent(canvas: Canvas) {
        child.draw(canvas)
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
        copy(left = newLeft, top = newTop, child = child.withPosition(newLeft, newTop))

    override fun withSize(newWidth: Int, newHeight: Int): DrawSpec {
        val w = newWidth.coerceAtLeast(0)
        val h = newHeight.coerceAtLeast(0)
        return if (width == w && height == h) this else copy(width = w, height = h)
    }

    override fun onAttachedToWindow(view: View) {
        child.attach(view)
    }

    override fun onDetachedFromWindow(view: View) {
        child.detach(view)
    }

    /**
     * KHÔNG cascade release cho [child]: delegate tự walk tree cũ và quyết
     * định từng spec dựa trên identity + node.id có còn xuất hiện trong tree
     * mới hay không. Nếu ta cascade ở đây, spec [child] có thể bị release
     * dù nó (hoặc bitmap của nó) vẫn được `withPosition` copy trong tree
     * mới dùng chung — pixel biến mất.
     *
     * Trên lifecycle DESTROY delegate cũng dùng cùng walk (release-all,
     * không cần filter), nên vẫn dọn được [child].
     */
    override fun onRelease() = Unit

    override fun hitTest(x: Int, y: Int): DrawSpec? {
        if (x < left || y < top || x >= left + width || y >= top + height) return null
        val hit = child.hitTest(x, y)
        if (hit != null) return hit
        return if (this.onClick != null || node?.onClick != null) this else null
    }

    /** Size adapter không vẽ gì — dissolve, chỉ child xuất hiện trong draw list. */
    override fun collectDrawAndHitSpecs(
        draws: MutableList<DrawSpec>,
        hits: MutableList<DrawSpec>,
        currentDepth: Int
    ) {

        depth = currentDepth
        if (isClickable) hits.add(this)
        child.collectDrawAndHitSpecs(draws, hits, currentDepth)
    }

    override fun collectDraws(out: MutableList<DrawSpec>) {
        val hits = ArrayList<DrawSpec>()
        collectDrawAndHitSpecs(out, hits)
    }

    /**
     * Add chính nó khi clickable để giữ hit-bounds = sized box (có thể lớn hơn
     * child), rồi đệ quy child. Duyệt ngược → child entries được check trước.
     */
    override fun collectHits(out: MutableList<DrawSpec>) {
        val draws = ArrayList<DrawSpec>()
        collectDrawAndHitSpecs(draws, out)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CachedDrawSpec — gom một nhóm spec **static** theo painter's order
// và pre-render chúng vào một Bitmap duy nhất. Khi vẽ, chỉ một lệnh
// `Canvas.drawBitmap` thay cho N lệnh vẽ con — tiết kiệm setup cost của mỗi
// spec (Paint, Path, StaticLayout draw...) và giảm HWUI display-list ops.
//
// Coalesce pass ở [LayoutEngine] dựng spec này từ list phẳng của
// [DrawSpec.collectDraws]. Bitmap tạo ngay trong constructor (chạy background)
// nên frame đầu không bị hitch. Bitmap sống theo lifetime của instance —
// GC lo khi LayoutResult mới thay thế result cũ.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Composite của N spec [DrawSpec.isStatic] `= true` trong draw list.
 *
 * Bounds ([left]/[top]/[width]/[height]) = bounding rect của [children]. Bitmap
 * chứa pixel đã render sẵn của toàn bộ subgroup, đúng thứ tự painter.
 *
 * Lifecycle attach/detach vẫn được forward vào [children] — hầu hết static spec
 * không có hook, nhưng vẫn giữ hợp đồng cho subclass tương lai.
 *
 * ⚠️ **Bitmap chỉ dựng 1 lần.** Nếu ai đó mutate property của child sau khi
 * cache (vd đổi màu BackgroundSpec runtime), pixel trong bitmap sẽ cũ. Cách
 * đúng: rebuild qua measure lại — engine sẽ tạo CachedDrawSpec mới.
 */
class CachedDrawSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val children: List<DrawSpec>,
    private val renderNode: CompatRenderNode
) : DrawSpec() {

    /**
     * Bản thân composite là kết quả cache tĩnh — coi như static để nếu vòng
     * coalesce sau này gặp lại vẫn có thể xử lý đồng nhất.
     */
    override val isStatic: Boolean get() = true

    override fun onDrawContent(canvas: Canvas) {
        renderNode.draw(canvas)
    }

    override fun onAttachedToWindow(view: View) {
        // Static spec hiếm khi có hook, nhưng vẫn forward cho đúng contract —
        // đi qua reference counter của [attach] để share-ref không bị ping-pong.
        for (i in children.indices) children[i].attach(view)
    }

    override fun onDetachedFromWindow(view: View) {
        for (i in children.indices) children[i].detach(view)
        // Không recycle bitmap ở detach: cùng một LayoutResult có thể được
        // attach lại khi view rời rồi trở lại window (fragment swap,
        // RecyclerView bind). Việc recycle chuyển hẳn sang [onRelease] —
        // gọi khi spec bị thay thế hoặc lifecycle DESTROY, tức chắc chắn
        // không còn draw nữa.
    }

    /**
     * Chỉ recycle **composite bitmap** của chính mình. Không cascade release
     * cho [children]: children ownership thuộc measure-cache-by-id
     * ([com.simple.ui.precompute.LayoutEngine.cache]), CachedDrawSpec chỉ là
     * đại lý blit — sau khi composite được vẽ xong, pixel của composite độc
     * lập với bitmap của child.
     *
     * Ví dụ nguy hiểm nếu cascade: khi tree mới không đạt threshold để
     * coalesce, TextSpec đang là con của CachedDrawSpec_old sẽ xuất hiện
     * **thẳng** trong new.draws (nhờ cache hit). Cascade release ở đây sẽ
     * recycle bitmap của TextSpec đang vẫn được dùng ở tree mới.
     *
     * Delegate walk tree cũ và quyết định release children riêng theo alive
     * filter — cách này an toàn cho mọi mô hình sharing.
     */
    override fun onRelease() {
        renderNode.discardDisplayList()
    }

    /** Cho phép delegate walk vào [children] khi diff-release. */
    internal val childrenView: List<DrawSpec> get() = children

    /**
     * Chỉ dịch bounds — bitmap giữ nguyên. `draw()` của base tự translate
     * canvas về (left, top) trước khi gọi [onDrawContent], nên đặt bitmap tại
     * (0, 0) local vẫn đúng vị trí absolute mới.
     *
     * Trong pipeline hiện tại coalesce chạy sau layout, [withPosition] gần
     * như không được gọi trên CachedDrawSpec — nhưng vẫn giữ implement đúng
     * để tránh surprise.
     */
    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec {
        if (newLeft == left && newTop == top) return this
        return CachedDrawSpec(newLeft, newTop, width, height, children, renderNode).also {

            it.depth = depth
        }
    }

    /**
     * Composite tự vẽ 1 lệnh — không dissolve, phải xuất hiện trong draws.
     * Children đã "hoà" vào bitmap → không đệ quy vào chúng nữa.
     */
    override fun collectDraws(out: MutableList<DrawSpec>) {
        out.add(this)
    }

    /**
     * Cache không thay đổi hit-target: static spec thường không clickable, nhưng
     * nếu có thì hit list gốc đã ghi nhận qua [DrawSpec.collectHits] từ tree —
     * CachedDrawSpec không cần add gì thêm.
     */
    override fun collectHits(out: MutableList<DrawSpec>) = Unit

    override fun collectDrawAndHitSpecs(
        draws: MutableList<DrawSpec>,
        hits: MutableList<DrawSpec>,
        currentDepth: Int
    ) {

        depth = currentDepth
        draws.add(this)
    }

    companion object {
        /**
         * Dựng [CachedDrawSpec] cho một nhóm [staticChildren] toàn spec static.
         *
         * Tính bounding rect từ absolute positions của children, tạo bitmap
         * đúng size và vẽ từng child vào bitmap canvas (đã translate về
         * bounding origin). Trả `null` nếu bounding rect rỗng — caller
         * fallback về vẽ trực tiếp children.
         *
         * Chạy trên background thread (chỗ coalesce trong LayoutEngine) —
         * Bitmap allocation + software canvas draw đều an toàn off-main.
         */
        fun from(staticChildren: List<DrawSpec>): CachedDrawSpec? {
            if (staticChildren.isEmpty()) return null

            var minLeft = Int.MAX_VALUE
            var minTop = Int.MAX_VALUE
            var maxRight = Int.MIN_VALUE
            var maxBottom = Int.MIN_VALUE
            for (i in staticChildren.indices) {
                val s = staticChildren[i]
                if (s.width <= 0 || s.height <= 0) continue
                if (s.left < minLeft) minLeft = s.left
                if (s.top < minTop) minTop = s.top
                if (s.right > maxRight) maxRight = s.right
                if (s.bottom > maxBottom) maxBottom = s.bottom
            }
            if (minLeft == Int.MAX_VALUE) return null

            val w = (maxRight - minLeft).coerceAtLeast(0)
            val h = (maxBottom - minTop).coerceAtLeast(0)
            if (w == 0 || h == 0) return null

            val node = CompatRenderNode("CachedDrawSpec")
            node.setPosition(0, 0, w, h)
            node.record { canvas ->
                // Dịch canvas về gốc bounding-rect: child.draw() sẽ tự translate
                // theo (child.left, child.top) trong hệ absolute, kết hợp với dịch
                // này thành offset trong bitmap.
                canvas.translate(-minLeft.toFloat(), -minTop.toFloat())
                for (i in staticChildren.indices) staticChildren[i].draw(canvas)
            }

            val minDepth = staticChildren.minOf { it.depth }
            return CachedDrawSpec(
                left = minLeft,
                top = minTop,
                width = w,
                height = h,
                children = staticChildren,
                renderNode = node
            ).also {

                it.depth = minDepth
            }
        }
    }
}
