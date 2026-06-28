package com.simple.phonetics.ui.precompute

/**
 * Loader bất đồng bộ cho [ImageSpec]. Trách nhiệm:
 *  - Bỏ qua spec đã có bitmap (BitmapSource).
 *  - Với các source khác: load → set [ImageSpec.bitmap] → gọi [onReady]
 *    trên main thread để view invalidate.
 *  - Cancel khi spec bị detach hoặc thay thế.
 *
 * Implementation chuẩn: GlideBitmapLoader (nằm trong module :app).
 * Inject bằng [BitmapLoader.install] một lần ở Application.
 */
interface BitmapLoader {
    fun load(spec: ImageSpec, onReady: () -> Unit)
    fun cancel(spec: ImageSpec)

    companion object {
        @Volatile
        private var instance: BitmapLoader? = null

        /** Đăng ký loader mặc định (gọi 1 lần ở Application). */
        fun install(loader: BitmapLoader) {
            instance = loader
        }

        /** Trả về loader đã đăng ký, hoặc null nếu chưa install. */
        fun get(): BitmapLoader? = instance
    }
}
