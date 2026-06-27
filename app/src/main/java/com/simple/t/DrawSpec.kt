package com.simple.phonetics.ui.precompute

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.StaticLayout

/**
 * Một "lệnh vẽ" đã đo sẵn. Tự biết vị trí, kích thước và cách vẽ chính mình.
 *
 * Base class lo wrap canvas.save() / translate() / restore() — subclass chỉ
 * cần implement [onDrawContent] để vẽ trong toạ độ local (đã translate sẵn).
 *
 * Mở rộng: muốn thêm Border, Divider, Shape... → tạo class mới kế thừa
 * [DrawSpec], không cần sửa View hay LayoutEngine (nếu tự đo).
 */
abstract class DrawSpec {

    abstract val left: Int
    abstract val top: Int
    abstract val width: Int
    abstract val height: Int

    val right: Int get() = left + width
    val bottom: Int get() = top + height

    /** Gọi từ parent (View hoặc GroupSpec). Toạ độ canvas hiện tại = parent. */
    fun draw(canvas: Canvas) {
        val saved = canvas.save()
        canvas.translate(left.toFloat(), top.toFloat())
        onDrawContent(canvas)
        canvas.restoreToCount(saved)
    }

    /** Vẽ nội dung trong toạ độ local của spec này. */
    protected abstract fun onDrawContent(canvas: Canvas)

    /** Trả về copy với (left, top) mới — phục vụ layout engine khi assign vị trí. */
    abstract fun withPosition(newLeft: Int, newTop: Int): DrawSpec
}

// -----------------------------------------------------------------------

data class TextSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val contentLeft: Int,
    val contentTop: Int,
    val layout: StaticLayout
) : DrawSpec() {

    override fun onDrawContent(canvas: Canvas) {
        if (contentLeft != 0 || contentTop != 0) {
            canvas.translate(contentLeft.toFloat(), contentTop.toFloat())
        }
        layout.draw(canvas)
    }

    override fun withPosition(newLeft: Int, newTop: Int) =
        copy(left = newLeft, top = newTop)
}

/**
 * Spec ảnh. Không phải data class vì [bitmap] có thể được loader cập nhật
 * sau khi spec đã measure xong (cho UrlSource / ResSource / ...).
 *
 * - Nếu source là [ImageSource.BitmapSource]: [bitmap] có ngay từ đầu, vẽ được luôn.
 * - Ngược lại: [bitmap] = null cho tới khi [BitmapLoader] gọi setter; trong khi
 *   chờ, spec chỉ chiếm chỗ chứ không vẽ gì.
 *
 * [withPosition] giữ lại bitmap hiện có để không phải load lại sau khi
 * layout assign vị trí.
 */
class ImageSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val source: ImageSource,
    val dst: Rect,
    initialBitmap: Bitmap? = null
) : DrawSpec() {

    @Volatile
    var bitmap: Bitmap? = initialBitmap
        ?: (source as? ImageSource.BitmapSource)?.bitmap

    override fun onDrawContent(canvas: Canvas) {
        val b = bitmap ?: return
        canvas.drawBitmap(b, null, dst, SHARED_PAINT)
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
        ImageSpec(newLeft, newTop, width, height, source, dst, bitmap)

    companion object {
        private val SHARED_PAINT =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
}

data class GroupSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val children: List<DrawSpec>
) : DrawSpec() {

    override fun onDrawContent(canvas: Canvas) {
        // Đệ quy: mỗi child tự draw, tự wrap save/translate.
        for (i in children.indices) children[i].draw(canvas)
    }

    override fun withPosition(newLeft: Int, newTop: Int) =
        copy(left = newLeft, top = newTop)
}
