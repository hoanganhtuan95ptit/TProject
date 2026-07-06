package com.simple.ui.precompute

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * View "dumb tuyệt đối": chỉ giữ một [LayoutResult] — danh sách phẳng các
 * spec **thực sự vẽ**. Không biết Text / Image là gì, không biết ImageLoader,
 * và cũng không biết cấu trúc node/tree (container thuần đã bị engine
 * dissolve trước khi trả ra).
 *
 *   view.result = LayoutEngine.measure(node, Constraints(w))
 *
 * Lifecycle (attach/detach + đổi result) chạy phẳng trên từng spec vẽ;
 * ImageSpec tự lo việc load / cancel ảnh qua loader của chính nó.
 */
class PrecomputedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PrecomputedHost {

    override val delegate: PrecomputedDelegate = PrecomputedDelegate(this, context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val r = delegate.result
        setMeasuredDimension(r?.width ?: 0, r?.height ?: 0)
    }

    override fun onDraw(canvas: Canvas) {
        delegate.onDraw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        delegate.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        delegate.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Delegate hit-test qua spec tree; nếu không có node clickable trúng,
        // fall back về hành vi View mặc định.
        if (delegate.onTouchEvent(event)) return true
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        // Onclick thực tế đã invoke ở node level trong delegate; ở đây chỉ
        // super để giữ accessibility contract (không nhân đôi callback).
        return super.performClick()
    }
}
