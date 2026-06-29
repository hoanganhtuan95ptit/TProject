package com.simple.ui.precompute.node

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import com.simple.launcher.retirement.utils.image.RichImage
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.ImageLoader
import com.simple.ui.precompute.MeasureContext
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// RichImage   — nguồn ảnh (sealed, immutable, thread-safe).
// ImageNode   — mô tả một ảnh cần layout.
// ImageSpec   — kết quả sau khi đo; load ảnh async qua ImageLoader.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mô tả một ảnh cần layout.
 *
 * - Với [RichImage.BitmapSource] hoặc [RichImage.DrawableSource] có intrinsic
 *   size, [LayoutDimension.WrapContent] sẽ lấy theo kích thước ảnh.
 * - Với các source async (Res/Path/Url/Drawable), cần có kích thước trước khi
 *   bitmap về: đặt [layoutWidth]/[layoutHeight] thành [LayoutDimension.Fixed]
 *   hoặc bounded [LayoutDimension.MatchParent].
 */
data class ImageNode(
    val source: RichImage,
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

        val rawW = layoutWidth.contentSizeFrom(c.maxWidth, p.horizontal)
            ?: 0
        val rawH = layoutHeight.contentSizeFrom(c.maxHeight, p.vertical)
            ?: 0

        val w = layoutWidth.resolve(rawW + p.horizontal, c.maxWidth)
        val h = layoutHeight.resolve(rawH + p.vertical, c.maxHeight)
        val dstW = (w - p.horizontal).coerceAtLeast(0)
        val dstH = (h - p.vertical).coerceAtLeast(0)

        val dst = Rect(p.left, p.top, p.left + dstW, p.top + dstH)
        return ImageSpec(x, y, w, h, source, dst)
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
 * - Nếu source là [RichImage.BitmapSource]: [drawable] sẽ là `BitmapDrawable` và có ngay từ đầu.
 * - Nếu source là [RichImage.DrawableSource]: [drawable] có ngay từ đầu.
 * - Ngược lại: [drawable] có thể null cho tới khi [com.simple.ui.precompute.ImageLoader] gọi setter;
 *   trong khi chờ, spec chỉ chiếm chỗ chứ không vẽ gì.
 */
class ImageSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val source: RichImage,
    val dst: Rect
) : DrawSpec() {

    @Volatile
    var drawable: Drawable? = null
        set(value) {
            field?.callback = null
            field = value?.apply {
                bounds = centerInside(dst)
                if (attachedView != null) {
                    callback = this@ImageSpec.callback
                }
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
        drawable?.draw(canvas)
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

    private fun Drawable.centerInside(container: Rect): Rect {
        val containerW = container.width()
        val containerH = container.height()
        val sourceW = intrinsicWidth
        val sourceH = intrinsicHeight

        if (containerW <= 0 || containerH <= 0) {
            return Rect(container.left, container.top, container.left, container.top)
        }

        if (sourceW <= 0 || sourceH <= 0) {
            return Rect(container)
        }

        val scale = minOf(
            1f,
            containerW.toFloat() / sourceW.toFloat(),
            containerH.toFloat() / sourceH.toFloat()
        )
        val drawW = (sourceW * scale).roundToInt().coerceIn(1, containerW)
        val drawH = (sourceH * scale).roundToInt().coerceIn(1, containerH)
        val left = container.left + (containerW - drawW) / 2
        val top = container.top + (containerH - drawH) / 2

        return Rect(left, top, left + drawW, top + drawH)
    }
}
