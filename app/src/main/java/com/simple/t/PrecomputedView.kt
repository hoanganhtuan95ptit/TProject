package com.simple.phonetics.ui.precompute

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

/**
 * View "dumb tuyệt đối": chỉ giữ một [DrawSpec] và uỷ thác việc vẽ cho nó.
 * Không biết Text / Image / Group là gì, cũng không biết BitmapLoader.
 *
 *   view.spec = LayoutEngine.measure(node, Constraints(w))
 *
 * Lifecycle (attach/detach + đổi spec) được forward xuống spec tree;
 * ImageSpec tự lo việc load / cancel bitmap qua loader của chính nó.
 */
class PrecomputedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var spec: DrawSpec? = null
        set(value) {
            if (field === value) return
            val old = field
            if (isAttachedToWindow) old?.onDetachedFromWindow(this)
            field = value
            if (old?.width != value?.width || old?.height != value?.height) {
                requestLayout()
            } else {
                postInvalidateOnAnimation()
            }
            if (isAttachedToWindow) value?.onAttachedToWindow(this)
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = spec
        setMeasuredDimension(s?.width ?: 0, s?.height ?: 0)
    }

    override fun onDraw(canvas: Canvas) {
        spec?.draw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        spec?.onAttachedToWindow(this)
    }

    override fun onDetachedFromWindow() {
        spec?.onDetachedFromWindow(this)
        super.onDetachedFromWindow()
    }
}
