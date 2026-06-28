package com.simple.ui.precompute

import android.graphics.Canvas
import android.view.View

// ─────────────────────────────────────────────────────────────────────────────
// DrawSpec — abstract base class cho mọi "lệnh vẽ" đã đo sẵn.
// Concrete spec types nằm trong file riêng:
//   TextSpec  → TextNode.kt
//   ImageSpec → ImageNode.kt
//   GroupSpec → LinearNode.kt
// ─────────────────────────────────────────────────────────────────────────────

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
        canvas.clipRect(0, 0, width, height)
        onDrawContent(canvas)
        canvas.restoreToCount(saved)
    }

    /** Vẽ nội dung trong toạ độ local của spec này. */
    protected abstract fun onDrawContent(canvas: Canvas)

    /** Trả về copy với (left, top) mới — phục vụ layout engine khi assign vị trí. */
    abstract fun withPosition(newLeft: Int, newTop: Int): DrawSpec

    open fun onAttachedToWindow(view: View) {}

    open fun onDetachedFromWindow(view: View) {}
}

/**
 * A measured box that delegates drawing/lifecycle to a child spec.
 * Used when parent layout rules force a size different from the child's
 * natural measured size.
 */
internal data class SizedSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    val child: DrawSpec
) : DrawSpec() {

    override fun onDrawContent(canvas: Canvas) {
        child.draw(canvas)
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
        copy(left = newLeft, top = newTop)

    override fun onAttachedToWindow(view: View) {
        child.onAttachedToWindow(view)
    }

    override fun onDetachedFromWindow(view: View) {
        child.onDetachedFromWindow(view)
    }
}
