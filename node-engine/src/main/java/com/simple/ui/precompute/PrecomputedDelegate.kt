package com.simple.ui.precompute

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class PrecomputedDelegate(private val view: View, context: Context, attrs: AttributeSet?) {

    var spec: DrawSpec? = null
        set(value) {
            if (field === value) return
            val old = field
            if (view.isAttachedToWindow) old?.onDetachedFromWindow(view)
            field = value
            if (old?.width != value?.width || old?.height != value?.height) {
                view.requestLayout()
            } else {
                view.postInvalidateOnAnimation()
            }
            if (view.isAttachedToWindow) value?.onAttachedToWindow(view)
        }

    fun onDraw(canvas: Canvas) {
        spec?.draw(canvas)
    }

    fun onAttachedToWindow() {
        spec?.onAttachedToWindow(view)
    }

    fun onDetachedFromWindow() {
        spec?.onDetachedFromWindow(view)
    }
}