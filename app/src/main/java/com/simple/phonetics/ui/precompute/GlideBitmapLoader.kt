package com.simple.phonetics.ui.precompute

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.simple.phonetics.ui.precompute.node.ImageSource
import com.simple.phonetics.ui.precompute.node.ImageSpec
import java.io.File
import java.util.WeakHashMap

/**
 * BitmapLoader dùng Glide. An toàn để dùng làm singleton ở Application scope.
 *
 * Lưu ý: dùng [Glide.with]`(applicationContext)` để tránh phụ thuộc lifecycle
 * của Activity/Fragment — chu kỳ load được quản lý bởi PrecomputedView qua
 * attach/detach + cancel.
 *
 * Cách dùng (gọi 1 lần ở Application.onCreate):
 *   BitmapLoader.install(GlideBitmapLoader(this))
 */
class GlideBitmapLoader(context: Context) : BitmapLoader {

    private val appContext = context.applicationContext

    /** Tracking target theo spec để cancel chính xác. Key giữ weak. */
    private val targets = WeakHashMap<ImageSpec, CustomTarget<Bitmap>>()

    override fun load(spec: ImageSpec, onReady: () -> Unit) {
        // Có bitmap rồi (BitmapSource hoặc đã load trước đó) → khỏi load.
        if (spec.bitmap != null) return

        val w = spec.dst.width().coerceAtLeast(1)
        val h = spec.dst.height().coerceAtLeast(1)

        val request = Glide.with(appContext)
            .asBitmap()
            .override(w, h)

        val withModel = when (val s = spec.source) {
            is ImageSource.BitmapSource -> request.load(s.bitmap)
            is ImageSource.ResSource -> request.load(s.resId)
            is ImageSource.PathSource -> request.load(File(s.path))
            is ImageSource.UrlSource -> request.load(s.url)
            is ImageSource.DrawableSource -> request.load(s.drawable)
        }

        val target = object : CustomTarget<Bitmap>(w, h) {
            override fun onResourceReady(
                resource: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                spec.bitmap = resource
                onReady()
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                spec.bitmap = null
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
