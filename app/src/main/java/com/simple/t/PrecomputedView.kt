package com.simple.phonetics.ui.precompute

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

/**
 * View "dumb tuyệt đối": chỉ giữ một [DrawSpec] và uỷ thác việc vẽ cho nó.
 * Không biết Text / Image / Group là gì.
 *
 *   view.bitmapLoader = GlideBitmapLoader(context)            // 1 lần
 *   view.spec = LayoutEngine.measure(node, Constraints(w))   // gán từ bg
 *
 * View tự walk spec tree để start/cancel load ảnh async theo lifecycle attach.
 */
class PrecomputedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Loader dùng cho các ImageSpec có source async (Url/Path/Res/Drawable).
     * Có thể bỏ trống nếu spec chỉ chứa BitmapSource.
     */
    var bitmapLoader: BitmapLoader? = null
        set(value) {
            if (field === value) return
            stopLoads()
            field = value
            if (isAttachedToWindow) startLoads()
        }

    var spec: DrawSpec? = null
        set(value) {
            if (field === value) return
            stopLoads()
            field = value
            requestLayout()
            invalidate()
            if (isAttachedToWindow) startLoads()
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
        startLoads()
    }

    override fun onDetachedFromWindow() {
        stopLoads()
        super.onDetachedFromWindow()
    }

    // -----------------------------------------------------------------

    private fun startLoads() {
        val loader = bitmapLoader ?: return
        val s = spec ?: return
        forEachImage(s) { img ->
            loader.load(img) {
                // onReady được Glide gọi trên main thread → invalidate ngay.
                invalidate()
            }
        }
    }

    private fun stopLoads() {
        val loader = bitmapLoader ?: return
        val s = spec ?: return
        forEachImage(s) { img -> loader.cancel(img) }
    }

    private fun forEachImage(s: DrawSpec, action: (ImageSpec) -> Unit) {
        when (s) {
            is ImageSpec -> action(s)
            is GroupSpec -> {
                val children = s.children
                for (i in children.indices) forEachImage(children[i], action)
            }
            else -> Unit
        }
    }
}
