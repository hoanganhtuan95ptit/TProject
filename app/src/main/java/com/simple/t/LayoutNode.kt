package com.simple.phonetics.ui.precompute

import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes

/**
 * Nguồn ảnh cho [LayoutNode.Image].
 *
 * - [BitmapSource] đã có sẵn bitmap → vẽ ngay, không cần loader.
 * - Các source còn lại cần [BitmapLoader] (vd Glide) load async ở runtime;
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
 * Hard constraints from the parent (in pixels).
 * Use [Int.MAX_VALUE] to express "unbounded".
 */
data class Constraints(
    val maxWidth: Int,
    val maxHeight: Int = Int.MAX_VALUE
)

data class EdgeInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val horizontal: Int get() = left + right
    val vertical: Int get() = top + bottom

    companion object {
        val ZERO = EdgeInsets()
        fun all(v: Int) = EdgeInsets(v, v, v, v)
        fun symmetric(h: Int = 0, v: Int = 0) = EdgeInsets(h, v, h, v)
    }
}

enum class Orientation { HORIZONTAL, VERTICAL }

enum class CrossAlign { START, CENTER, END }

/**
 * Immutable description of a sub-tree to be laid out.
 * Everything here must be safe to read from a background thread:
 * no references to View, Context, Resources, etc.
 *
 * Pre-load Bitmap / resolve color / resolve typeface BEFORE building.
 */
sealed class LayoutNode {

    abstract val padding: EdgeInsets

    data class Text(
        val text: CharSequence,
        val textSizePx: Float,
        val color: Int,
        val maxLines: Int = Int.MAX_VALUE,
        val typeface: Typeface? = null,
        val lineSpacingMul: Float = 1f,
        val lineSpacingAdd: Float = 0f,
        override val padding: EdgeInsets = EdgeInsets.ZERO
    ) : LayoutNode()

    /**
     * Node ảnh.
     *
     * - Với [ImageSource.BitmapSource], [width]/[height] có thể bỏ trống —
     *   sẽ tự lấy theo bitmap.
     * - Với các source async (Res/Path/Url/Drawable), **bắt buộc** truyền
     *   [width] và [height] vì engine phải đo trước khi bitmap về.
     *   Sẽ throw [IllegalArgumentException] khi build nếu thiếu.
     */
    data class Image(
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

        companion object {
            /** Tiện ích: giữ tương thích với call-site cũ. */
            fun fromBitmap(
                bitmap: Bitmap,
                width: Int? = null,
                height: Int? = null,
                padding: EdgeInsets = EdgeInsets.ZERO
            ) = Image(ImageSource.BitmapSource(bitmap), width, height, padding)
        }
    }

    data class Linear(
        val orientation: Orientation,
        val children: List<LayoutNode>,
        val gap: Int = 0,
        val crossAlign: CrossAlign = CrossAlign.START,
        override val padding: EdgeInsets = EdgeInsets.ZERO
    ) : LayoutNode()
}
