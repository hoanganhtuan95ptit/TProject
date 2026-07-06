package com.simple.ui.precompute

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner

class PrecomputedDelegate(private val view: View, context: Context, attrs: AttributeSet?) {

    /**
     * Kết quả đo phẳng từ [LayoutEngine.build]. Delegate chỉ biết
     * [LayoutResult.draws] / [LayoutResult.hits] — không biết tree.
     *
     * Khi swap result, delegate thực hiện:
     * 1. Attach result mới (tăng ref counter).
     * 2. Cập nhật LayoutParams (kích hoạt requestLayout nếu size đổi).
     * 3. Detach result cũ (giảm ref counter).
     *
     * Việc giải phóng (release) spec không còn dùng đã được [LayoutEngine.build]
     * tự động xử lý qua Cache Diffing.
     */
    var result: LayoutResult? = null
        set(value) {
            if (field === value) return

            val start = System.currentTimeMillis()
            val old = field
            if (view.isAttachedToWindow) value?.let { attachAll(it) }
            field = value

            val newW = value?.width ?: 0
            val newH = value?.height ?: 0

            val lp = view.layoutParams
            var layoutRequested = false
            if (lp != null && (lp.width != newW || lp.height != newH)) {
                lp.width = newW
                lp.height = newH
                view.layoutParams = lp
                layoutRequested = true
            }

            if (!layoutRequested) {
                if (view.width != newW || view.height != newH) {
                    view.requestLayout()
                } else {
                    view.postInvalidateOnAnimation()
                }
            }

            if (view.isAttachedToWindow) old?.let { detachAll(it) }
            
            // Releasing is now handled inside LayoutEngine.build or via LayoutEngine.release(groupName)

            if (DrawSpec.DEBUG_LOG) {
                val end = System.currentTimeMillis()
                Log.d("PrecomputedDelegate", "result swap: ${end - start}ms, view: $view")
            }
        }

    /**
     * GestureDetector chuyên trách dispatch tap → onClick của node trúng
     * hit-test. Dùng detector chuẩn Android để tự động lo tap-slop,
     * double-tap window, cancel khi kéo ra ngoài.
     */
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            // Chỉ "claim" chuỗi event khi điểm chạm rơi trên một spec
            // clickable — trả false để parent (nếu có) xử lý các case
            // trống. Kết quả: view chỉ intercept khi thực sự có target.
            return result?.hitTest(e.x.toInt(), e.y.toInt()) != null
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val hit = result?.hitTest(e.x.toInt(), e.y.toInt()) ?: return false
            val cb = hit.onClick ?: hit.node?.onClick ?: return false
            view.playSoundEffect(SoundEffectConstants.CLICK)
            cb.invoke()
            return true
        }
    })

    /**
     * Observer bám vào [LifecycleOwner] của tree — bắn khi host Fragment /
     * Activity vào DESTROY. Trigger release toàn bộ current result và null
     * hoá `result` để không còn ref nào giữ spec.
     *
     * Cần nhớ owner đã register để có thể tự tháo observer khi view detach
     * (RecyclerView holder có thể được attach/detach nhiều lần — mỗi lần
     * attach lại vào cùng lifecycle sẽ add thêm observer nếu không tháo).
     */
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            releaseAll()
            detachLifecycle()
        }
    }
    private var observedOwner: LifecycleOwner? = null

    /** Vẽ = for-loop phẳng theo painter's order, không đệ quy tree. */
    fun onDraw(canvas: Canvas) {
        val r = result ?: return
        val draws = r.draws
        for (i in draws.indices) draws[i].draw(canvas)
    }

    fun onAttachedToWindow() {
        result?.let { attachAll(it) }
        attachLifecycle()
    }

    fun onDetachedFromWindow() {
        result?.let { detachAll(it) }
        detachLifecycle()
    }

    /**
     * Trả về `true` nếu event đã được tiêu thụ bởi node clickable. Caller
     * (PrecomputedView) fall back về `super.onTouchEvent` khi false.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    /**
     * Lifecycle chỉ chạy trên [LayoutResult.draws]: spec nào cần attach hook
     * (animator, image load, callback) đều là spec có vẽ. Container thuần đã
     * dissolve và không có state riêng. Composite spec (subclass GroupSpec tự
     * vẽ) nằm nguyên trong draws → tự đệ quy children qua hook của nó như cũ.
     */
    private fun attachAll(r: LayoutResult) {
        val draws = r.draws
        for (i in draws.indices) draws[i].attach(view)
    }

    private fun detachAll(r: LayoutResult) {
        val draws = r.draws
        for (i in draws.indices) draws[i].detach(view)
    }

    /**
     * Clear result — dùng khi host lifecycle DESTROY.
     * Detach (nếu view còn attached) trước để reference counter đúng.
     * Lưu ý: Việc giải phóng cache theo groupName nên được thực hiện bởi Caller
     * thông qua LayoutEngine.release(name).
     */
    private fun releaseAll() {
        val r = result ?: return
        if (view.isAttachedToWindow) detachAll(r)
        result = null
    }

    private fun attachLifecycle() {
        if (observedOwner != null) return
        val owner = view.findViewTreeLifecycleOwner() ?: return
        // Nếu owner đã DESTROYED sẵn, addObserver sẽ không bắn onDestroy nữa
        // (Lifecycle spec) → release ngay tại chỗ.
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            releaseAll()
            return
        }
        owner.lifecycle.addObserver(lifecycleObserver)
        observedOwner = owner
    }

    private fun detachLifecycle() {
        val owner = observedOwner ?: return
        owner.lifecycle.removeObserver(lifecycleObserver)
        observedOwner = null
    }
}
