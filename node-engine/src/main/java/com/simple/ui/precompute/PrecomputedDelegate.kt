package com.simple.ui.precompute

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class PrecomputedDelegate(private val view: View, context: Context, attrs: AttributeSet?) {

    var spec: DrawSpec? = null
        set(value) {
            // Identity swap thuần: mọi tối ưu tái sử dụng subtree (skip measure,
            // giữ drawable / animator / StaticLayout...) đã được xử lý ở
            // background qua cache-by-id trong [LayoutEngine] + [MeasureContext].
            // Main thread ở đây chỉ làm swap + attach/detach — không diff cây.
            //
            // Cache hit ở root → LayoutEngine trả về đúng spec cũ mà delegate
            // đang giữ → `field === value` bắt ngay, thoát sớm, không đụng gì
            // (không invalidate, không detach/attach).
            if (field === value) return

            val old = field
            // Thứ tự: attach new TRƯỚC, detach old SAU.
            //
            // Cache-by-id có thể trả về cây new chứa những [DrawSpec] cùng
            // reference với cây old (subtree tận dụng lại). Reference counter
            // trong [DrawSpec.attach] / [DrawSpec.detach] cần shared ref
            // không rơi về 0 ở giữa chừng — nếu detach trước, counter đi
            // 1→0 → onDetached chạy, animator stop / scope cancel; sau đó
            // attach lại đi 0→1 → onAttached chạy, phải setup lại từ đầu.
            //
            // Đảo thứ tự: shared ref counter 1→2 (attach no-op) → 2→1
            // (detach no-op) — hook lifecycle không bị đụng, state giữ
            // nguyên. Non-shared spec (old-only / new-only) counter đi
            // đúng 0↔1 như thường.
            if (view.isAttachedToWindow) value?.attach(view)
            field = value
            if (old?.width != value?.width || old?.height != value?.height) {
                view.requestLayout()
            } else {
                view.postInvalidateOnAnimation()
            }
            if (view.isAttachedToWindow) old?.detach(view)
        }

    fun onDraw(canvas: Canvas) {
        spec?.draw(canvas)
    }

    fun onAttachedToWindow() {
        spec?.attach(view)
    }

    fun onDetachedFromWindow() {
        spec?.detach(view)
    }
}