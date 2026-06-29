package com.simple.ui.precompute

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.simple.ui.precompute.image.RichTransformConverters
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

    private val appContext = context.applicationContext

    private val handlerThread = HandlerThread("GlideImageLoader").apply { start() }
    private val bgHandler = Handler(handlerThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())

    override val dispatcher: Executor = Executor { bgHandler.post(it) }

    /** Tracking target theo spec để cancel chính xác. Truy cập từ bg thread. */
    private val targets = WeakHashMap<ImageSpec, CustomTarget<Drawable>>()

    override fun load(spec: ImageSpec, onReady: () -> Unit) {
        // Chạy trên bg thread (dispatcher).
        if (spec.drawable != null) return

        val w = spec.dst.width().coerceAtLeast(1)
        val h = spec.dst.height().coerceAtLeast(1)

        // Glide.with(applicationContext) an toàn từ bg thread (lifecycle
        // gắn với application, không phải Activity/Fragment).
        var withModel: RequestBuilder<Drawable> = Glide.with(appContext)
            .load(spec.source.source)
            .override(w, h)
        if (spec.source.placeholder != 0) {
            withModel = withModel.placeholder(spec.source.placeholder)
        }
        if (spec.source.error != 0) {
            withModel = withModel.error(spec.source.error)
        }
        RichTransformConverters.build(spec.source.transforms)?.let {
            withModel = withModel.transform(it)
        }

        val target = object : CustomTarget<Drawable>(w, h) {
            override fun onLoadStarted(placeholder: Drawable?) {
                spec.drawable = placeholder
                onReady()
            }

            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                spec.drawable = resource
                onReady()
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                spec.drawable = errorDrawable
                onReady()
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                spec.drawable = null
            }
        }

        targets[spec] = target

        // Glide yêu cầu into(target) chạy ở main thread.
        mainHandler.post { withModel.into(target) }
    }

    override fun cancel(spec: ImageSpec) {
        // Chạy trên bg thread (dispatcher).
        val target = targets.remove(spec) ?: return
        // Glide.clear cũng đụng RequestManager — post về main cho an toàn.
        mainHandler.post { Glide.with(appContext).clear(target) }
    }
}
