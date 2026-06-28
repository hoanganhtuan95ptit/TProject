package com.simple.ui.precompute

import com.simple.ui.precompute.node.ImageSpec

/**
 * Loader bất đồng bộ cho [com.simple.ui.precompute.node.ImageSpec]. Trách nhiệm:
 *  - Bỏ qua spec đã có bitmap (BitmapSource) hoặc drawable.
 *  - Với các source khác: load → set [com.simple.ui.precompute.node.ImageSpec.drawable] → gọi [onReady]
 *    trên main thread để view invalidate.
 *  - Cancel khi spec bị detach hoặc thay thế.
 *
 * Implementation chuẩn: GlideImageLoader (nằm trong module :app).
 * Inject bằng [ImageLoader.install] một lần ở Application.
 */
interface ImageLoader {
    fun load(spec: ImageSpec, onReady: () -> Unit)
    fun cancel(spec: ImageSpec)

    companion object {
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
