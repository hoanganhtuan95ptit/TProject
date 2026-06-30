package com.simple.ui.precompute

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

/**
 * View "dumb tuyệt đối": chỉ giữ một [DrawSpec] và uỷ thác việc vẽ cho nó.
 * Không biết Text / Image / Group là gì, cũng không biết ImageLoader.
 *
 *   view.spec = LayoutEngine.measure(node, Constraints(w))
 *
 * Lifecycle (attach/detach + đổi spec) được forward xuống spec tree;
 * ImageSpec tự lo việc load / cancel ảnh qua loader của chính nó.
 */
class PrecomputedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PrecomputedHost {

    override val delegate: PrecomputedDelegate = PrecomputedDelegate(this, context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = delegate.spec
        setMeasuredDimension(s?.width ?: 0, s?.height ?: 0)
    }

    override fun onDraw(canvas: Canvas) {
        delegate.onDraw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        delegate.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        delegate.onDetachedFromWindow()
        super.onDetachedFromWindow()
    }
}
