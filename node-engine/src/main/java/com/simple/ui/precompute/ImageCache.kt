package com.simple.ui.precompute

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.simple.ui.precompute.image.BigImage

/**
 * Cache toàn cục cho drawable đã load của [com.simple.ui.precompute.node.ImageSpec].
 *
 * Mục tiêu: cho phép [com.simple.ui.precompute.node.ImageSpec] truy vấn **đồng bộ**
 * trên main thread khi attach — nếu hit, bỏ qua hoàn toàn vòng load qua loader/Glide
 * và tránh frame nháy (vốn xảy ra do callback của Glide bị post sang frame sau).
 *
 * ## Thiết kế
 * - Lưu [Drawable.ConstantState] thay vì [Drawable] trực tiếp → mỗi spec lấy
 *   một instance riêng qua [Drawable.ConstantState.newDrawable] + [Drawable.mutate],
 *   tránh share state có thể bị mutate (bounds, callback) giữa các spec.
 * - Bitmap bên dưới vẫn share qua ConstantState — rẻ, không tốn thêm memory.
 * - Animated drawable ([Animatable]) bị skip khi put vì:
 *     · newDrawable() thường tạo instance mới có animation reset frame 0;
 *     · animation state share-being-mutated giữa các view rất khó debug.
 *
 * ## Threading
 * [LruCache] đã synchronized nội bộ. Get/put có thể gọi từ bất kỳ thread nào.
 * Trong thực tế, get được gọi trên main (khi attach), put được gọi trên main
 * sau khi Glide callback về (CustomTarget.onResourceReady).
 *
 * ## Kích thước
 * Mặc định 128 entry — phù hợp UI thông thường (chip, icon, avatar). Nếu app
 * load nhiều ảnh lớn, cân nhắc subclass với [LruCache.sizeOf] đếm theo byte.
 *
 * ## Eviction
 * Khi entry bị evict, các spec đang giữ drawable cũ không bị ảnh hưởng vì
 * chúng nắm reference riêng. Lần load tiếp theo cho cùng [BigImage] sẽ lại
 * đi qua loader.
 */
object ImageCache {

    private const val DEFAULT_MAX_ENTRIES = 128

    @Volatile
    private var lru: LruCache<BigImage, Drawable.ConstantState> =
        LruCache(DEFAULT_MAX_ENTRIES)

    /**
     * Đổi kích thước cache. Gọi ở Application nếu cần override default.
     * Resize tạo cache mới và bỏ entries cũ — chỉ nên gọi 1 lần lúc init.
     */
    fun setMaxEntries(max: Int) {
        require(max > 0) { "max must be > 0" }
        lru = LruCache(max)
    }

    /**
     * Trả về drawable mới cho [source] nếu cache có; null nếu miss.
     * Mỗi lần get cấp phát instance riêng qua [Drawable.ConstantState.newDrawable]
     * + [Drawable.mutate] — caller có thể tự do set bounds / callback.
     */
    fun get(source: BigImage): Drawable? =
        lru.get(source)?.newDrawable()?.mutate()

    /**
     * Lưu [drawable] vào cache theo [source].
     * - Bỏ qua nếu drawable không expose [Drawable.constantState] (vd custom drawable không support).
     * - Bỏ qua [Animatable] (xem note ở header) — animated content luôn đi qua loader.
     */
    fun put(source: BigImage, drawable: Drawable) {
        if (drawable is Animatable) return
        val state = drawable.constantState ?: return
        lru.put(source, state)
    }

    /** Xoá toàn bộ cache. Dùng khi config change (theme/density) hoặc low-memory. */
    fun clear() {
        lru.evictAll()
    }
}
