package com.simple.phonetics.ui.precompute

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes

// ─────────────────────────────────────────────────────────────────────────────
// ImageSource — nguồn ảnh (sealed, immutable, thread-safe).
// ImageNode   — mô tả một ảnh cần layout.
// ImageSpec   — kết quả sau khi đo; load bitmap async qua BitmapLoader.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Nguồn ảnh cho [ImageNode].
 *
 * - [BitmapSource] đã có sẵn bitmap → vẽ ngay, không cần loader.
 * - Các source còn lại cần [BitmapLoader] load async ở runtime;
 *   trước khi bitmap về, [ImageSpec] sẽ chiếm chỗ trống đúng kích thước.
 *
 * Tất cả source đều immutable & thread-safe để pass qua background thread.
 * Riêng [DrawableSource] giữ reference Drawable — chỉ dùng nếu chắc chắn
 * drawable đó không bị mutate ngoài luồng (Glide sẽ rasterize hộ).
 */
sealed class ImageSource {
    data class BitmapSource(val bitmap: Bitmap) : ImageSource()
    data class ResSource(@DrawableRes val resId: Int) : ImageSource()
    /** Đường dẫn file trên thiết bị (absolute path). */
    data class PathSource(val path: String) : ImageSource()
    data class UrlSource(val url: String) : ImageSource()
    data class DrawableSource(val drawable: Drawable) : ImageSource()
}

/**
 * Mô tả một ảnh cần layout.
 *
 * - Với [ImageSource.BitmapSource], [width]/[height] có thể bỏ trống —
 *   sẽ tự lấy theo bitmap.
 * - Với các source async (Res/Path/Url/Drawable), **bắt buộc** truyền
 *   [width] và [height] vì engine phải đo trước khi bitmap về.
 *   Sẽ throw [IllegalArgumentException] khi build nếu thiếu.
 */
data class ImageNode(
    val source: ImageSource,
    /** null chỉ được phép khi source là BitmapSource. */
    val width: Int? = null,
    /** null chỉ được phép khi source là BitmapSource. */
    val height: Int? = null,
    override val padding: EdgeInsets = EdgeInsets.ZERO
) : LayoutNode() {

    init {
        if (source !is ImageSource.BitmapSource) {
            require(width != null && height != null) {
                "width/height bắt buộc cho ImageSource async: $source"
            }
        }
    }

    override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): ImageSpec {
        val p = padding
        // Với BitmapSource: lấy width/height theo bitmap nếu node không truyền.
        // Với source khác: init {} đã đảm bảo width/height != null.
        val bitmapSize = (source as? ImageSource.BitmapSource)?.bitmap
        val rawW = width ?: bitmapSize?.width ?: 0
        val rawH = height ?: bitmapSize?.height ?: 0
        val w = rawW + p.horizontal
        val h = rawH + p.vertical
        val dst = Rect(p.left, p.top, p.left + rawW, p.top + rawH)
        return ImageSpec(x, y, w, h, source, dst)
    }

    companion object {
        /** Tiện ích: tạo ImageNode từ Bitmap có sẵn. */
        fun fromBitmap(
            bitmap: Bitmap,
            width: Int? = null,
            height: Int? = null,
            padding: EdgeInsets = EdgeInsets.ZERO
        ) = ImageNode(ImageSource.BitmapSource(bitmap), width, height, padding)
    }
}

/**
 * Kết quả đo của [ImageNode].
 *
 * Không phải data class vì [bitmap] có thể được loader cập nhật
 * sau khi spec đã measure xong (cho UrlSource / ResSource / ...).
 *
 * - Nếu source là [ImageSource.BitmapSource]: [bitmap] có ngay từ đầu.
 * - Ngược lại: [bitmap] = null cho tới khi [BitmapLoader] gọi setter;
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
    var bitmap: Bitmap? = (source as? ImageSource.BitmapSource)?.bitmap

    override fun onDrawContent(canvas: Canvas) {
        val b = bitmap ?: return
        canvas.drawBitmap(b, null, dst, SHARED_PAINT)
    }

    override fun onAttachedToWindow(view: View) {
        // BitmapSource đã có bitmap sẵn — khỏi cần loader.
        if (source is ImageSource.BitmapSource) return
        val loader = BitmapLoader.get() ?: return
        loader.load(this) { view.invalidate() }
    }

    override fun onDetachedFromWindow(view: View) {
        if (source is ImageSource.BitmapSource) return
        BitmapLoader.get()?.cancel(this)
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
        ImageSpec(newLeft, newTop, width, height, source, dst)

    companion object {
        private val SHARED_PAINT =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
}
