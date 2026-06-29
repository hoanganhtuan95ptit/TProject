package com.simple.ui.precompute

import com.simple.ui.precompute.node.ImageSpec
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Loader bất đồng bộ cho [com.simple.ui.precompute.node.ImageSpec].
 *
 * ## Threading contract
 * Engine **không** gọi [load]/[cancel] trực tiếp trên UI thread. Mọi yêu cầu
 * được dispatch qua [dispatcher] để mọi setup (build Glide RequestBuilder,
 * resolve transforms, lookup RequestManager, v.v.) chạy **off-main**.
 *
 * - [load] và [cancel] được gọi trên thread của [dispatcher].
 * - Implementation chịu trách nhiệm switch lại main thread cho những API
 *   bắt buộc main (vd Glide's `into(target)`).
 * - [onReady] có thể được gọi từ bất kỳ thread nào, nhưng thường được
 *   implementation post lên main rồi mới gọi.
 *
 * Mặc định [dispatcher] là một single-thread executor — đảm bảo cancel
 * luôn chạy sau load đã queue cùng spec, tránh race.
 *
 * Implementation chuẩn: `GlideImageLoader` ở module :app.
 */
interface ImageLoader {

    /**
     * Executor đảm bảo [load]/[cancel] chạy off-main. Mặc định single-thread
     * để serialize thứ tự load↔cancel cho cùng spec.
     */
    val dispatcher: Executor get() = DEFAULT_DISPATCHER

    fun load(spec: ImageSpec, onReady: () -> Unit)

    fun cancel(spec: ImageSpec)

    companion object {

        private val DEFAULT_DISPATCHER: Executor by lazy {
            Executors.newSingleThreadExecutor { r ->
                Thread(r, "ImageLoader-default").apply { isDaemon = true }
            }
        }

        @Volatile
        private var instance: ImageLoader? = null

        /** Đăng ký loader mặc định (gọi 1 lần ở Application). */
        fun install(loader: ImageLoader) {
            instance = loader
        }

        /** Trả về loader đã đăng ký, hoặc null nếu chưa install. */
        fun get(): ImageLoader? = instance
    }
}
