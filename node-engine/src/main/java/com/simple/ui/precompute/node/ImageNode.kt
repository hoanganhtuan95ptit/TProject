package com.simple.ui.precompute.node

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.ImageLoader
import com.simple.ui.precompute.MeasureContext

// ─────────────────────────────────────────────────────────────────────────────
// ImageSource — nguồn ảnh (sealed, immutable, thread-safe).
// ImageNode   — mô tả một ảnh cần layout.
// ImageSpec   — kết quả sau khi đo; load ảnh async qua ImageLoader.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Nguồn ảnh cho [ImageNode].
 *
 * - [BitmapSource] đã có sẵn bitmap → vẽ ngay, không cần loader.
 * - Các source còn lại cần [com.simple.ui.precompute.ImageLoader] load async ở runtime;
 *   trước khi bitmap về, [ImageSpec] sẽ chiếm chỗ trống đúng kích thước.
 *
 * Tất cả source đều immutable & thread-safe để pass qua background thread.
 * Riêng [DrawableSource] giữ reference Drawable — chỉ dùng nếu chắc chắn
 * drawable đó không bị mutate ngoài luồng (Glide sẽ rasterize hộ).
 */
sealed class ImageSource {
    data class BitmapSource(val bitmap: Bitmap) : ImageSource()
    data class ResSource(@param:DrawableRes val resId: Int) : ImageSource()
    /** Đường dẫn file trên thiết bị (absolute path). */
    data class PathSource(val path: String) : ImageSource()
    data class UrlSource(val url: String) : ImageSource()
    data class DrawableSource(val drawable: Drawable) : ImageSource()
}

/**
 * Mô tả một ảnh cần layout.
 *
 * - Với [ImageSource.BitmapSource] hoặc [ImageSource.DrawableSource] có intrinsic
 *   size, [LayoutDimension.WrapContent] sẽ lấy theo kích thước ảnh.
 * - Với các source async (Res/Path/Url/Drawable), cần có kích thước trước khi
 *   bitmap về: đặt [layoutWidth]/[layoutHeight] thành [LayoutDimension.Fixed]
 *   hoặc bounded [LayoutDimension.MatchParent].
 */
data class ImageNode(
    val source: ImageSource,
    override val layoutWidth: LayoutDimension = LayoutDimension.WrapContent,
    override val layoutHeight: LayoutDimension = LayoutDimension.WrapContent,
    override val padding: EdgeInsets = EdgeInsets.ZERO
) : LayoutNode() {

    override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): ImageSpec {
        val p = padding
        val bitmap = (source as? ImageSource.BitmapSource)?.bitmap
        val drawable = (source as? ImageSource.DrawableSource)?.drawable

        val rawW = bitmap?.width
            ?: drawable?.intrinsicWidth?.takeIf { it > 0 }
            ?: layoutWidth.contentSizeFrom(c.maxWidth, p.horizontal)
            ?: 0
        val rawH = bitmap?.height
            ?: drawable?.intrinsicHeight?.takeIf { it > 0 }
            ?: layoutHeight.contentSizeFrom(c.maxHeight, p.vertical)
            ?: 0

        if (source !is ImageSource.BitmapSource && (rawW <= 0 || rawH <= 0)) {
            throw IllegalArgumentException(
                "ImageSource async requires fixed/bounded layoutWidth/layoutHeight: $source"
            )
        }

        val w = layoutWidth.resolve(rawW + p.horizontal, c.maxWidth)
        val h = layoutHeight.resolve(rawH + p.vertical, c.maxHeight)
        val dstW = (w - p.horizontal).coerceAtLeast(0)
        val dstH = (h - p.vertical).coerceAtLeast(0)

        val dst = Rect(p.left, p.top, p.left + dstW, p.top + dstH)
        return ImageSpec(x, y, w, h, source, dst).apply {
            this.drawable = when (source) {
                is ImageSource.BitmapSource -> BitmapDrawable(Resources.getSystem(), source.bitmap)
                is ImageSource.DrawableSource -> source.drawable
                else -> null
            }
        }
    }

    companion object {
        /** Tiện ích: tạo ImageNode từ Bitmap có sẵn. */
        fun fromBitmap(
            bitmap: Bitmap,
            layoutWidth: LayoutDimension = LayoutDimension.WrapContent,
            layoutHeight: LayoutDimension = LayoutDimension.WrapContent,
            padding: EdgeInsets = EdgeInsets.ZERO
        ) = ImageNode(ImageSource.BitmapSource(bitmap), layoutWidth, layoutHeight, padding)
    }

    private fun LayoutDimension.contentSizeFrom(parentMax: Int, padding: Int): Int? =
        when (this) {
            is LayoutDimension.Fixed -> (maxForMeasure(parentMax) - padding).coerceAtLeast(0)
            LayoutDimension.MatchParent -> {
                if (parentMax == Int.MAX_VALUE) null else (parentMax - padding).coerceAtLeast(0)
            }
            LayoutDimension.WrapContent -> null
        }
}

/**
 * Kết quả đo của [ImageNode].
 *
 * Không phải data class vì [bitmap] có thể được loader cập nhật
 * sau khi spec đã measure xong (cho UrlSource / ResSource / ...).
 *
 * - Nếu source là [ImageSource.BitmapSource]: [drawable] sẽ là `BitmapDrawable` và có ngay từ đầu.
 * - Nếu source là [ImageSource.DrawableSource]: [drawable] có ngay từ đầu.
 * - Ngược lại: [drawable] có thể null cho tới khi [com.simple.ui.precompute.ImageLoader] gọi setter;
 *   trong khi chờ, spec chỉ chiếm chỗ chứ không vẽ gì.
 */
class ImageSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val source: ImageSource,
    val dst: Rect
) : DrawSpec() {

    @Volatile
    var drawable: Drawable? = null
        set(value) {
            field = value
            if (attachedView != null) {
                value?.callback = callback
            }
        }

    private var attachedView: View? = null

    private val callback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) {
            attachedView?.postInvalidateOnAnimation()
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            val delay = `when` - android.os.SystemClock.uptimeMillis()
            attachedView?.postDelayed(what, delay)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            attachedView?.removeCallbacks(what)
        }
    }

    override fun onDrawContent(canvas: Canvas) {
        drawable?.let {
            it.bounds = dst
            it.draw(canvas)
        }
    }

    override fun onAttachedToWindow(view: View) {
        attachedView = view
        drawable?.callback = callback
        (drawable as? Animatable)?.start()

        // Đã có ảnh (từ Bitmap/DrawableSource hoặc đã load xong từ trước) thì không cần load nữa.
        if (drawable != null) return
        val loader = ImageLoader.get() ?: return
        loader.load(this) { view.postInvalidateOnAnimation() }
    }

    override fun onDetachedFromWindow(view: View) {
        attachedView = null
        drawable?.callback = null
        (drawable as? Animatable)?.stop()

        ImageLoader.get()?.cancel(this)
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
        ImageSpec(newLeft, newTop, width, height, source, dst).also {
            it.drawable = drawable
        }

    companion object {
        // Shared paint if needed for other places, but Drawables usually handle their own paint
        private val SHARED_PAINT =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
}
