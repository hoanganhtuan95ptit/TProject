package com.simple.ui.precompute.loader

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.simple.ui.precompute.node.ImageSpec
import java.util.WeakHashMap
import java.util.concurrent.Executor

/**
 * ImageLoader dùng Glide. An toàn singleton ở Application scope.
 *
 * ## Threading
 * - [load] và [cancel] được engine dispatch trên [dispatcher] (single-thread
 *   bg) — toàn bộ setup (build `RequestBuilder`, override size, resolve
 *   transforms, lookup `RequestManager` qua `Glide.with(appContext)`) chạy
 *   off-main.
 * - Glide bắt buộc `RequestBuilder.into(target)` và `RequestManager.clear(target)`
 *   chạy ở main thread → loader post chúng qua [mainHandler].
 * - Vì [dispatcher] là single-thread, cancel của một spec luôn chạy sau load
 *   của chính spec đó (cùng FIFO).
 *
 * Cách dùng (gọi 1 lần ở Application.onCreate):
 *   ImageLoader.install(GlideImageLoader(this))
 */
class GlideImageLoader(context: Context) : ImageLoader {

    private val appContext = context as? Application ?: context.applicationContext

    private val handlerThread = HandlerThread("GlideImageLoader").apply {

        start()
    }

    private val bgHandler = Handler(handlerThread.looper)
    private val mainLooper = Looper.getMainLooper()
    private val mainHandler = Handler(mainLooper)

    override val dispatcher: Executor = Executor { command ->

        bgHandler.post(command)
    }

    /**
     * Cache `RequestManager` app-scope. `Glide.with(appContext)` là hash lookup
     * + validation nội bộ ở mỗi call — không đắt, nhưng nhân với 300+ lần
     * load/cancel trong 1 lần scroll thì cộng dồn. Ref singleton nên có thể
     * cache thẳng (không đổi theo lifecycle).
     */
    private val requestManager: RequestManager by lazy(LazyThreadSafetyMode.NONE) {
        Glide.with(appContext)
    }

    /** Tracking target theo spec để cancel chính xác. Truy cập từ bg thread. */
    private val targets = WeakHashMap<ImageSpec, CustomTarget<Drawable>>()

    /**
     * Chạy [block] trên main. Nếu caller đã ở main (hiếm với loader nhưng có
     * thể xảy ra khi callback Glide chain lại vào load) → gọi thẳng, bỏ round-trip
     * qua Handler queue (1 lần post = 1 lần chờ tới lượt trong MessageQueue).
     */
    private inline fun runOnMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() === mainLooper) block()
        else mainHandler.post { block() }
    }

    private data class RequestSize(
        val width: Int,
        val height: Int
    )

    override fun load(spec: ImageSpec, onReady: () -> Unit) {

        // Chạy trên bg thread (dispatcher).
        if (spec.drawable != null) return

        val size = spec.requestSize()
        val request = createRequest(spec, size)
        val target = createTarget(spec, size, onReady)

        targets[spec] = target

        // Glide yêu cầu into(target) chạy ở main thread.
        runOnMain { request.into(target) }
    }

    override fun cancel(spec: ImageSpec) {

        // Chạy trên bg thread (dispatcher).
        val target = targets.remove(spec) ?: return

        // Glide.clear cũng đụng RequestManager — post về main cho an toàn.
        runOnMain { requestManager.clear(target) }
    }

    private fun ImageSpec.requestSize(): RequestSize =
        RequestSize(
            width = dst.width().coerceAtLeast(1),
            height = dst.height().coerceAtLeast(1)
        )

    private fun createRequest(spec: ImageSpec, size: RequestSize): RequestBuilder<Drawable> {

        var withModel: RequestBuilder<Drawable> = requestManager
            .load(spec.source.source)
            .override(size.width, size.height)

        if (spec.source.placeholder != 0) {

            withModel = withModel.placeholder(spec.source.placeholder)
        }

        if (spec.source.error != 0) {

            withModel = withModel.error(spec.source.error)
        }

        if(spec.source.transforms.isNotEmpty()){

            withModel = withModel.transform(*spec.source.transforms)
        }

        return withModel
    }

    private fun createTarget(
        spec: ImageSpec,
        size: RequestSize,
        onReady: () -> Unit
    ): CustomTarget<Drawable> =
        object : CustomTarget<Drawable>(size.width, size.height) {

            override fun onLoadStarted(placeholder: Drawable?) {

                // Chỉ set placeholder khi spec chưa có gì để vẽ. Tránh clobber
                // drawable cũ (hit cache hoặc đã load lần trước) bằng placeholder
                // — đó là nguồn gốc của flicker.
                spec.setPlaceholderIfEmpty(placeholder, onReady)
            }

            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {

                // Cache trước khi gán drawable: lần attach sau cho cùng [source]
                // sẽ hit cache đồng bộ và bỏ qua hoàn toàn vòng Glide.
                spec.setLoadedDrawable(resource, onReady)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {

                spec.drawable = errorDrawable
                onReady()
            }

            override fun onLoadCleared(placeholder: Drawable?) {

                spec.drawable = null
            }
        }

    private fun ImageSpec.setPlaceholderIfEmpty(placeholder: Drawable?, onReady: () -> Unit) {

        if (drawable != null || placeholder == null) return

        drawable = placeholder
        onReady()
    }

    private fun ImageSpec.setLoadedDrawable(resource: Drawable, onReady: () -> Unit) {

        drawable = resource
        onReady()
    }
}