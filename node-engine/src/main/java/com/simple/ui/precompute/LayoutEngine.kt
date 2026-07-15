package com.simple.ui.precompute

import android.os.Looper
import com.simple.ui.precompute.LayoutEngine.clearCache
import com.simple.ui.precompute.LayoutEngine.evict
import com.simple.ui.precompute.LayoutEngine.measure
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.LayoutNode
import java.util.concurrent.ConcurrentHashMap

/**
 * Pure measurement engine. No View, no Context, no main-thread APIs.
 * Safe to call from Dispatchers.Default.
 *
 * ## Basic
 *
 *   val spec = LayoutEngine.measure(node, Constraints(screenWidth))
 *
 * ## Cache
 *
 * Engine giữ một map phẳng `Map<LayoutNode.id, DrawSpec>` (per-JVM). Khi
 * measure đệ quy vào một node có [LayoutNode.id]:
 * - Lookup trong cache. Nếu hit + spec cũ là cùng ref (`===`) với node
 *   mới + kích thước còn valid dưới constraint mới → **trả nguyên spec
 *   cũ, bỏ qua `node.measure()`** (tiết kiệm text Picture record, constraint
 *   solver, image intrinsic math...).
 * - Miss (không có id, id không khớp, ref khác, hoặc constraint đổi) →
 *   chạy `node.measure()` như thường và ghi kết quả vào cache dưới id
 *   của node đó.
 *
 * Cache **tự populate incremental** — mỗi lần measure vừa đọc vừa ghi,
 * không cần bước walk-old-tree riêng.
 *
 * Tham số [id] của [measure] chỉ để **gán cache slot cho root spec** —
 * hữu ích khi root node chưa có [LayoutNode.id] mà caller muốn evict theo
 * key riêng sau này. Không truyền [id] và cả root cũng không có id →
 * root không lưu cache (nhưng subtree con vẫn được cache theo id của
 * chúng nếu có).
 *
 * ## Lifecycle
 *
 * Caller chủ động [evict] khi view/màn hình bị huỷ hẳn để tránh giữ
 * [DrawSpec] cùng Picture / Drawable / Bitmap quá lâu. [clearCache]
 * dùng cho config change (theme/density) hoặc low-memory.
 *
 * ## Thread-safety
 *
 * Cache là [ConcurrentHashMap] → an toàn giữa các thread. Với cùng một id
 * caller nên serialize (thường mỗi view đã có dispatcher riêng nên tự
 * nhiên đã serialize).
 *
 * Engine không biết Text/Image/Linear là gì — mỗi [com.simple.ui.precompute.node.LayoutNode] tự đo qua
 * [com.simple.ui.precompute.node.LayoutNode.measure]. Thêm node mới = tạo class kế thừa [com.simple.ui.precompute.node.LayoutNode] +
 * [DrawSpec], không cần đụng file này.
 */
object LayoutEngine {

    private val cache = ConcurrentHashMap<Any, DrawSpec>()

    fun measure(
        node: LayoutNode,
        constraints: Constraints,
        id: Any? = null
    ): DrawSpec {

        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            error("luồng tính toán cần phải xử lý ở background")
        }

        val ctx = MeasureContext(cache)
        val spec = ctx.measure(node, constraints, 0, 0)
        // Root có thể chưa có LayoutNode.id — nếu caller truyền id riêng cho
        // slot cache thì gán ở đây. Nếu root cũng có node.id, ctx đã lưu rồi.
        if (id != null) cache[id] = spec
        return spec
    }

    /**
     * Xoá entry [id] khỏi cache. Gọi khi view/màn hình gắn với id bị huỷ hẳn.
     * Lưu ý: nếu tree đó còn nhiều subtree khác cũng cache theo [LayoutNode.id]
     * riêng của chúng thì các entry đó phải evict riêng — hoặc dùng [clearCache].
     */
    fun evict(id: Any) {
        cache.remove(id)
    }

    /** Xoá toàn bộ cache. Dùng khi config change hoặc low-memory. */
    fun clearCache() {
        cache.clear()
    }
}

/**
 * Context truyền xuống [LayoutNode.measure] để node container (vd Linear)
 * có thể đệ quy đo các child mà không cần biết concrete type.
 *
 * Có tham chiếu tới [cache] chung của [LayoutEngine]. Mỗi lần đo một node
 * có [LayoutNode.id]:
 * - Đọc cache. Hit + identity + [DrawSpec.canReuseUnder] → skip measure.
 * - Miss → chạy `node.measure()`, ghi kết quả vào cache.
 *
 * Vì đọc-ghi cùng một map, cache được **populate incremental** trong lúc
 * đo — không có bước build-map riêng.
 */
class MeasureContext(
    private val cache: MutableMap<Any, DrawSpec> = HashMap()
) {

    fun measure(
        node: LayoutNode,
        c: Constraints,
        x: Int = 0,
        y: Int = 0
    ): DrawSpec {
        val id = node.id
        if (id != null) {
            val cached = cache[id]
            // Identity-only match: caller giữ ref = tín hiệu subtree không đổi.
            // == structural sẽ tốn O(subtree) → phá tinh thần fast-path.
            if (cached != null && cached.node === node && cached.canReuseUnder(c)) {
                return if (cached.left == x && cached.top == y) cached
                else cached.withPosition(x, y)
            }
        }
        val spec = node.measure(this, c, x, y)
        if (id != null) cache[id] = spec
        return spec
    }
}
