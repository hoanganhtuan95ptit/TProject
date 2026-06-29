package com.simple.ui.precompute

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.simple.ui.precompute.node.ImageSpec
import java.util.WeakHashMap

/**
 * ImageLoader dùng Glide. An toàn để dùng làm singleton ở Application scope.
 *
 * Lưu ý: dùng [Glide.with]`(applicationContext)` để tránh phụ thuộc lifecycle
 * của Activity/Fragment — chu kỳ load được quản lý bởi PrecomputedView qua
 * attach/detach + cancel.
 *
 * Cách dùng (gọi 1 lần ở Application.onCreate):
 *   ImageLoader.install(GlideImageLoader(this))
 */
class GlideImageLoader(context: Context) : ImageLoader {

    private val appContext = context.applicationContext

    /** Tracking target theo spec để cancel chính xác. Key giữ weak. */
    private val targets = WeakHashMap<ImageSpec, CustomTarget<Drawable>>()

    override fun load(spec: ImageSpec, onReady: () -> Unit) {
        // Có ảnh từ lần load trước thì khỏi load lại.
        if (spec.drawable != null) return

        val w = spec.dst.width().coerceAtLeast(1)
        val h = spec.dst.height().coerceAtLeast(1)

        var withModel = Glide.with(appContext)
            .load(spec.source.source)
            .override(w, h)
        if (spec.source.placeholder != 0) {
            withModel = withModel.placeholder(spec.source.placeholder)
        }
        if (spec.source.error != 0) {
            withModel = withModel.error(spec.source.error)
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
        withModel.into(target)
    }

    override fun cancel(spec: ImageSpec) {
        val target = targets.remove(spec) ?: return
        Glide.with(appContext).clear(target)
    }
}
