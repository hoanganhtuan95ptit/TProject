package com.simple.ui.precompute

import android.os.Looper
import android.util.Log
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.LayoutNode
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID

/**
 * LayoutEngine điều phối việc chuyển đổi từ Node sang Spec.
 * Thiết kế theo tài liệu ENGINE_STRUCTURE.md.
 *
 * Nguyên tắc quan trọng về cache:
 * - **Không dùng cache toàn cục cho spec cá nhân**. Trước đây có `nodeCache`
 *   shared giữa mọi build → khi một build release spec cũ, nó có thể xoá entry
 *   mà build khác đang giữ, hoặc release spec mà LayoutResult khác vẫn dùng
 *   → text không hiện.
 * - Chỉ giữ **cache theo (groupName, id)** cho List A và CacheSpec B. Diff/reuse
 *   ở tầng này thực hiện **theo từng item** dựa vào cấu trúc của `spec.node`.
 * - `MeasureContext` chỉ dùng cache **cục bộ trong 1 lần build** (dedupe khi
 *   cùng một node instance xuất hiện nhiều lần trong tree hiện tại).
 */
object LayoutEngine {


    private val groupCache: MutableMap<String, MutableMap<String, MutableMap<Any, DrawSpec>>> = Collections.synchronizedMap(HashMap())

    private val staticGroupCache: MutableMap<String, MutableMap<String, MutableMap<Any, DrawSpec>>> = Collections.synchronizedMap(HashMap())


    private val canReleaseCache: MutableMap<String, MutableMap<String, MutableMap<Any, DrawSpec>>> = Collections.synchronizedMap(HashMap())


    @Volatile
    var staticCoalesceThreshold: Int = 5

    /**
     * Bật log tracking chi tiết cho vòng đời spec trong [build]:
     * - Từng reuse (nodeId, identity cũ → identity copy mới sau withPosition)
     * - Từng spec cũ không tái sử dụng được (sẽ vào canReleaseSpecMap)
     * - CROSS-CHECK cuối build: nếu bất kỳ spec nào trong canReleaseSpecMap
     *   cũng đang có mặt trong cache mới (newDrawSpecCacheMap /
     *   newStaticSpecCacheMap / newListADisplay) → log ERROR "released while
     *   reused" kèm identity + nodeId để truy vết.
     *
     * Tách khỏi [DrawSpec.DEBUG_LOG] để bật riêng khi debug leak/double-release
     * mà không làm ồn log render bình thường.
     */
    @Volatile
    var TRACK_SPEC_LIFECYCLE: Boolean = true

    private const val TRACK_TAG = "LayoutEngine.Track"

    private fun idHex(spec: DrawSpec?): String =
        if (spec == null) "null" else "0x%08x".format(System.identityHashCode(spec))

    /**
     * Build & Compute Layout theo 8 bước trong tài liệu ENGINE_STRUCTURE.md.
     *
     * 0. Chỉ chạy background thread.
     * 1. Build Spec — sinh cây Spec + tính vị trí trong View.
     * 2. Filter — gom List A (Spec thực sự vẽ + Spec có click).
     * 3. Cache Diffing (List A) — so **từng item** với List A' theo (groupName,
     *    id): item nào không đổi thông tin → tận dụng nguyên spec cũ (giữ bitmap
     *    còn sống), item cũ nào biến mất → onRelease + xoá.
     * 4. Save Cache A — lưu List A mới theo (groupName, id).
     * 5. Static Grouping — gom Spec tĩnh trong List A tạo CacheSpec B.
     * 6. Cache Diffing (CacheSpec) — so B với B', tận dụng nếu nội dung giữ
     *    nguyên, giải phóng B' nếu đổi.
     * 7. Save Cache B — lưu CacheSpec B mới theo (groupName, id).
     * 8. Output — trả về kích thước View + List A để hiển thị.
     */
    fun build(
        groupName: String = "default_group",
        id: String = UUID.randomUUID().toString(),
        node: LayoutNode,
        constraints: Constraints
    ): LayoutResult {
        // ── Bước 0: Chỉ cho phép chạy ở background thread ────────────────────
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            error("LayoutEngine.build: Chỉ được phép gọi từ background thread")
        }

        // ── Bước 1: Build Spec ───────────────────────────────────────────────
        // Xây dựng danh sách Spec + vị trí trong View. `MeasureContext` chỉ có
        // cache cục bộ cho lần build này — không dùng cache toàn cục.
        val ctx = MeasureContext()
        val rootSpec = ctx.measure(node, constraints, 0, 0)
        val viewWidth = rootSpec.width
        val viewHeight = rootSpec.height

        // ── Bước 2: Filter ───────────────────────────────────────────────────
        // Lọc ra danh sách các Spec thực sự vẽ + có xử lý click (List A).
        val drawSpecList = ArrayList<DrawSpec>()
        val hitSpecList = ArrayList<DrawSpec>()
        rootSpec.collectDrawAndHitSpecs(drawSpecList, hitSpecList)

        // todo bước 3

        // ── Bước 5: Static Grouping ──────────────────────────────────────────
        // `coalesceResult.list` đã là MutableList (ArrayList) — dùng trực tiếp,
        // không cần `.toMutableList()` copy thừa.
        val newListADisplay = coalesceStatic(drawSpecList, staticCoalesceThreshold).list

        // log theo sơ đồ case giúp tôi

        // ── Bước 8: Output ───────────────────────────────────────────────────
        // Trả về kích thước View + danh sách Spec A (đã coalesce) để hiển thị.
        return LayoutResult(viewWidth, viewHeight, newListADisplay, hitSpecList)
    }

    /**
     * Diff List A theo từng item.
     *
     * Key match: `spec.node` (data class → structural equality). Với mỗi key có
     * thể có nhiều spec trùng, xếp thành queue tiêu thụ theo thứ tự xuất hiện.
     * Điều kiện tận dụng: cùng node structural + cùng width/height + cùng vị trí
     * (tránh phải copy). Nếu vị trí khác → dùng spec mới, spec cũ được release
     * ở cuối bước 3.
     *
     * Spec mới bị loại bỏ (do tận dụng được spec cũ) sẽ được release để thu hồi
     * bitmap thừa mà `node.measure()` vừa allocate.
     */
    private fun reconcileListA(
        rawListA: List<DrawSpec>,
        oldListA: List<DrawSpec>,
        reusedOldSpecs: IdentityHashMap<DrawSpec, Boolean>
    ): List<DrawSpec> {
        if (oldListA.isEmpty()) return rawListA

        val oldByKey: HashMap<Any, ArrayDeque<DrawSpec>> = HashMap()
        for (old in oldListA) {
            val key: Any = old.node ?: continue
            oldByKey.getOrPut(key) { ArrayDeque() }.addLast(old)
        }
        if (oldByKey.isEmpty()) return rawListA

        val reconciled = ArrayList<DrawSpec>(rawListA.size)
        for (newSpec in rawListA) {
            val key: Any? = newSpec.node
            val queue = if (key != null) oldByKey[key] else null
            val reused = queue?.let { pickReusable(it, newSpec) }
            if (reused != null) {
                reconciled.add(reused)
                reusedOldSpecs[reused] = true
                // Bitmap của newSpec là bản render thừa (nội dung trùng với
                // reused). Release ngay để thu hồi native memory.
                if (newSpec !== reused) newSpec.release()
            } else {
                reconciled.add(newSpec)
            }
        }
        return reconciled
    }

    /**
     * Chọn spec cũ tận dụng được cho [newSpec] trong [queue].
     * Ưu tiên spec cùng vị trí (không cần copy); nếu không có, trả null để
     * caller dùng luôn newSpec (không tận dụng vị trí khác vì `withPosition`
     * tạo copy chia sẻ bitmap — dễ gây double-release).
     */
    private fun pickReusable(queue: ArrayDeque<DrawSpec>, newSpec: DrawSpec): DrawSpec? {
        val it = queue.iterator()
        while (it.hasNext()) {
            val candidate = it.next()
            if (candidate.isReleased) {
                it.remove()
                continue
            }
            if (candidate.width == newSpec.width &&
                candidate.height == newSpec.height &&
                candidate.left == newSpec.left &&
                candidate.top == newSpec.top
            ) {
                it.remove()
                return candidate
            }
        }
        return null
    }

    /**
     * Diff CacheSpec B với B'. Nếu children mới === children cũ (từng item
     * identity match — nghĩa là bước 3 đã tận dụng nguyên tất cả children thành
     * spec cũ) thì tận dụng luôn B' (giữ nguyên bitmap composite). Ngược lại
     * release B' và trả về B mới.
     */
    private fun reconcileCacheSpecB(
        newCacheSpec: CachedDrawSpec?,
        oldCacheSpec: CachedDrawSpec?,
        displayList: List<DrawSpec>
    ): CachedDrawSpec? {
        if (newCacheSpec == null) {
            oldCacheSpec?.release()
            return null
        }
        if (oldCacheSpec == null) return newCacheSpec

        if (isSameChildren(oldCacheSpec.childrenView, newCacheSpec.childrenView)) {
            // Thay thế newCacheSpec bằng oldCacheSpec ngay trong displayList.
            @Suppress("UNCHECKED_CAST")
            val mutableDisplay = displayList as? MutableList<DrawSpec>
            if (mutableDisplay != null) {
                val idx = mutableDisplay.indexOf(newCacheSpec)
                if (idx >= 0) mutableDisplay[idx] = oldCacheSpec
            }
            // Bitmap của newCacheSpec là thừa (composite của cùng children).
            newCacheSpec.release()
            return oldCacheSpec
        }

        oldCacheSpec.release()
        return newCacheSpec
    }

    private fun isSameChildren(oldChildren: List<DrawSpec>, newChildren: List<DrawSpec>): Boolean {
        if (oldChildren.size != newChildren.size) return false
        for (i in oldChildren.indices) {
            if (oldChildren[i] !== newChildren[i]) return false
        }
        return true
    }

    /**
     * Giải phóng toàn bộ các Spec liên quan dựa vào groupName.
     * Dọn cả 3 tầng cache: List A (groupCache), CacheSpec B (staticGroupCache),
     * và pending-release pool (canReleaseCache). `DrawSpec.release()` idempotent
     * nên spec xuất hiện ở nhiều cache không bị release chồng.
     */
    fun release(groupName: String) {
//        releaseWholeGroup(groupCache.remove(groupName))
//        releaseWholeGroup(staticGroupCache.remove(groupName))
//        releaseWholeGroup(canReleaseCache.remove(groupName))
    }

    /**
     * Giải phóng các Spec thuộc groupName, ngoại trừ các id có trong keepIds.
     * Áp dụng cho cả 3 tầng cache. `canReleaseCache` luôn được drain hoàn toàn
     * (không tôn trọng keepIds) vì đó là các spec đã bị thay thế ở build gần
     * nhất, không còn được LayoutResult mới tham chiếu.
     */
    fun release(groupName: String, keepIds: List<String>) {
        val keepSet: Set<String> = if (keepIds is Set<*>) {
            @Suppress("UNCHECKED_CAST") (keepIds as Set<String>)
        } else HashSet(keepIds)

//        releaseGroupExcept(groupCache, groupName, keepSet)
//        releaseGroupExcept(staticGroupCache, groupName, keepSet)
//         Pool pending-release là "spec cũ đã bị thay thế" — drain hết luôn.
//        releaseWholeGroup(canReleaseCache.remove(groupName))
    }

    private fun releaseWholeGroup(groupMap: MutableMap<String, MutableMap<Any, DrawSpec>>?) {
        groupMap ?: return
        synchronized(groupMap) {
            for (specMap in groupMap.values) {
                for (spec in specMap.values) spec.release()
            }
            groupMap.clear()
        }
    }

    private fun releaseGroupExcept(
        parent: MutableMap<String, MutableMap<String, MutableMap<Any, DrawSpec>>>,
        groupName: String,
        keepIds: Set<String>
    ) {
        val groupMap = parent[groupName] ?: return
        synchronized(groupMap) {
            val iterator = groupMap.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key !in keepIds) {
                    for (spec in entry.value.values) spec.release()
                    iterator.remove()
                }
            }
            if (groupMap.isEmpty()) parent.remove(groupName)
        }
    }

    /**
     * Tương thích ngược với code cũ.
     */
    fun measure(
        id: Any? = null,
        node: LayoutNode,
        constraints: Constraints
    ): LayoutResult {
        return build(id = id?.toString() ?: UUID.randomUUID().toString(), node = node, constraints = constraints)
    }

    private fun releaseAllSpecs(specs: List<DrawSpec>) {
        for (spec in specs) spec.release()
    }

    private data class CoalesceResult(val list: MutableList<DrawSpec>, val cacheSpecB: CachedDrawSpec?)

    private enum class CacheSlotConstraint {

        Any,
        CacheBeforeDynamic,
        CacheAfterDynamic,
        Impossible
    }

    private fun coalesceStatic(draws: List<DrawSpec>, threshold: Int): CoalesceResult {

        if (threshold <= 0 || draws.size < threshold) {

            return CoalesceResult(ArrayList(draws), null)
        }

        val staticIndexes = collectStaticIndexes(draws)
        if (staticIndexes.size < threshold) {

            return CoalesceResult(ArrayList(draws), null)
        }

        val insertionSlot = findSingleCacheInsertionSlot(draws, staticIndexes)
        if (insertionSlot != null) {

            val result = coalesceStaticIndexes(draws, staticIndexes, insertionSlot)
            if (result != null) return result
        }

        return coalesceLargestStaticRun(draws, threshold)
    }

    private fun collectStaticIndexes(draws: List<DrawSpec>): List<Int> {

        val indexes = ArrayList<Int>(draws.size)
        for (i in draws.indices) {
            if (draws[i].isStatic) indexes.add(i)
        }
        return indexes
    }

    /**
     * Một [CachedDrawSpec] duy nhất chỉ đúng khi có thể chèn nó vào danh sách
     * dynamic mà không đảo thứ tự vẽ của mọi cặp static/dynamic đang overlap.
     */
    private fun findSingleCacheInsertionSlot(
        draws: List<DrawSpec>,
        staticIndexes: List<Int>
    ): Int? {

        val dynamicIndexes = collectDynamicIndexes(draws)
        var minSlot = 0
        var maxSlot = dynamicIndexes.size

        for (slot in dynamicIndexes.indices) {

            val dynamicIndex = dynamicIndexes[slot]
            when (cacheSlotConstraint(draws, staticIndexes, dynamicIndex)) {
                CacheSlotConstraint.CacheBeforeDynamic -> maxSlot = minOf(maxSlot, slot)
                CacheSlotConstraint.CacheAfterDynamic -> minSlot = maxOf(minSlot, slot + 1)
                CacheSlotConstraint.Impossible -> return null
                CacheSlotConstraint.Any -> Unit
            }
            if (minSlot > maxSlot) return null
        }

        return minSlot
    }

    private fun collectDynamicIndexes(draws: List<DrawSpec>): List<Int> {

        val indexes = ArrayList<Int>(draws.size)
        for (i in draws.indices) {
            if (!draws[i].isStatic) indexes.add(i)
        }
        return indexes
    }

    private fun cacheSlotConstraint(
        draws: List<DrawSpec>,
        staticIndexes: List<Int>,
        dynamicIndex: Int
    ): CacheSlotConstraint {

        var cacheMustDrawBeforeDynamic = false
        var cacheMustDrawAfterDynamic = false
        val dynamic = draws[dynamicIndex]

        for (staticIndex in staticIndexes) {

            if (!overlaps(dynamic, draws[staticIndex])) continue

            cacheMustDrawBeforeDynamic = cacheMustDrawBeforeDynamic || staticIndex < dynamicIndex
            cacheMustDrawAfterDynamic = cacheMustDrawAfterDynamic || staticIndex > dynamicIndex
            if (cacheMustDrawBeforeDynamic && cacheMustDrawAfterDynamic) {

                return CacheSlotConstraint.Impossible
            }
        }

        if (cacheMustDrawBeforeDynamic) return CacheSlotConstraint.CacheBeforeDynamic
        if (cacheMustDrawAfterDynamic) return CacheSlotConstraint.CacheAfterDynamic
        return CacheSlotConstraint.Any
    }

    private fun overlaps(first: DrawSpec, second: DrawSpec): Boolean =
        first.left < second.right &&
                second.left < first.right &&
                first.top < second.bottom &&
                second.top < first.bottom

    private fun coalesceStaticIndexes(
        draws: List<DrawSpec>,
        staticIndexes: List<Int>,
        insertionSlot: Int
    ): CoalesceResult? {

        val staticSpecs = ArrayList<DrawSpec>(staticIndexes.size)
        val staticIndexSet = HashSet<Int>(staticIndexes.size)
        for (index in staticIndexes) {

            staticSpecs.add(draws[index])
            staticIndexSet.add(index)
        }

        val cached = CachedDrawSpec.from(staticSpecs) ?: return null
        val out = ArrayList<DrawSpec>(draws.size - staticIndexes.size + 1)
        var dynamicSlot = 0
        var inserted = false

        for (i in draws.indices) {

            if (staticIndexSet.contains(i)) continue

            if (!inserted && dynamicSlot == insertionSlot) {

                out.add(cached)
                inserted = true
            }

            out.add(draws[i])
            dynamicSlot++
        }

        if (!inserted) out.add(cached)
        return CoalesceResult(out, cached)
    }

    private fun coalesceLargestStaticRun(draws: List<DrawSpec>, threshold: Int): CoalesceResult {

        val range = findLargestStaticRun(draws, threshold)
            ?: return CoalesceResult(ArrayList(draws), null)
        val cached = CachedDrawSpec.from(draws.subList(range.first, range.second))
            ?: return CoalesceResult(ArrayList(draws), null)
        val out = ArrayList<DrawSpec>(draws.size - (range.second - range.first) + 1)

        for (i in 0 until range.first) out.add(draws[i])
        out.add(cached)
        for (i in range.second until draws.size) out.add(draws[i])

        return CoalesceResult(out, cached)
    }

    private fun findLargestStaticRun(draws: List<DrawSpec>, threshold: Int): Pair<Int, Int>? {

        var bestStart = -1
        var bestEnd = -1
        var i = 0

        while (i < draws.size) {

            val runEnd = findStaticRunEnd(draws, i)
            if (runEnd == i) {

                i++
                continue
            }

            if (runEnd - i >= threshold && runEnd - i > bestEnd - bestStart) {

                bestStart = i
                bestEnd = runEnd
            }
            i = runEnd
        }

        if (bestStart < 0) return null
        return bestStart to bestEnd
    }

    private fun findStaticRunEnd(draws: List<DrawSpec>, start: Int): Int {

        if (!draws[start].isStatic) return start

        var end = start + 1
        while (end < draws.size && draws[end].isStatic) end++
        return end
    }

    /**
     * Xoá toàn bộ 3 tầng cache, release tất cả spec đang giữ. Dùng khi cần
     * reset engine (ví dụ đổi theme, low-memory).
     */
    fun clearCache() {
        clearAndReleaseAll(groupCache)
        clearAndReleaseAll(staticGroupCache)
        clearAndReleaseAll(canReleaseCache)
    }

    private fun clearAndReleaseAll(
        root: MutableMap<String, MutableMap<String, MutableMap<Any, DrawSpec>>>
    ) {
        synchronized(root) {
            for (groupMap in root.values) {
                synchronized(groupMap) {
                    for (specMap in groupMap.values) {
                        for (spec in specMap.values) spec.release()
                    }
                }
            }
            root.clear()
        }
    }
}

class LayoutResult(
    val width: Int,
    val height: Int,
    val draws: List<DrawSpec>,
    val hits: List<DrawSpec>
) {
    fun hitTest(x: Int, y: Int): DrawSpec? {
        for (i in hits.indices.reversed()) {
            val hit = hits[i].hitTest(x, y)
            if (hit != null) return hit
        }
        return null
    }
}

/**
 * MeasureContext chỉ cache **trong 1 lần build**. Dedupe khi cùng một node
 * instance xuất hiện nhiều lần trong tree hiện tại; không cross-build.
 */
class MeasureContext {

    fun measure(node: LayoutNode, c: Constraints, x: Int = 0, y: Int = 0): DrawSpec {
        return node.measure(this, c, x, y)
    }
}
