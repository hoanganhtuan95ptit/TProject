package com.simple.ui.precompute

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.simple.ui.precompute.node.ImageSpec
import java.io.File
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
        // Có ảnh rồi (BitmapSource, DrawableSource hoặc đã load trước đó) → khỏi load.
        if (spec.drawable != null) return

        val w = spec.dst.width().coerceAtLeast(1)
        val h = spec.dst.height().coerceAtLeast(1)

        val request = Glide.with(appContext)
            .asDrawable()
            .override(w, h)

        val withModel = when (val s = spec.source) {
            is RichImage.BitmapSource -> request.load(s.bitmap)
            is RichImage.ResSource -> request.load(s.resId)
            is RichImage.PathSource -> request.load(File(s.path))
            is RichImage.UrlSource -> request.load(s.url)
            is RichImage.DrawableSource -> request.load(s.drawable)
        }

        val target = object : CustomTarget<Drawable>(w, h) {
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                spec.drawable = resource
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
