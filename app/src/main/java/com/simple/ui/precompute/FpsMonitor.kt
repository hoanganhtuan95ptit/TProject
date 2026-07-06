package com.simple.ui.precompute

import android.util.Log
import android.view.Choreographer
import android.widget.TextView
import java.util.concurrent.TimeUnit

/**
 * Đo FPS + jank bằng [Choreographer]. Gọi [attach] khi Activity resume,
 * [detach] khi pause. Mỗi khoảng [reportIntervalMs], log ra tag [TAG] và
 * cập nhật [label] (nếu có) — dùng để đo hiệu năng scroll.
 *
 * Cách tính:
 * - Mỗi vsync tick tính `dt = now - lastNs`.
 * - `frames++`, cộng `dt` vào tổng để suy avg fps trong cửa sổ report.
 * - `dt > slowThresholdNs` → tính là jank frame (mặc định > 32ms ~ trễ 2
 *   vsync ở 60Hz). `dt > freezeThresholdNs` → freeze frame (mặc định >
 *   700ms — theo Android Vitals).
 */
class FpsMonitor(
    private val label: TextView? = null,
    private val reportIntervalMs: Long = 1000L,
    slowThresholdMs: Long = 32L,
    freezeThresholdMs: Long = 700L
) : Choreographer.FrameCallback {

    private val slowThresholdNs = TimeUnit.MILLISECONDS.toNanos(slowThresholdMs)
    private val freezeThresholdNs = TimeUnit.MILLISECONDS.toNanos(freezeThresholdMs)
    private val reportIntervalNs = TimeUnit.MILLISECONDS.toNanos(reportIntervalMs)

    private var attached = false
    private var lastFrameNs = 0L
    private var windowStartNs = 0L
    private var frames = 0
    private var slowFrames = 0
    private var freezeFrames = 0
    private var maxDtNs = 0L

    fun attach() {
        if (attached) return
        attached = true
        lastFrameNs = 0L
        windowStartNs = 0L
        frames = 0
        slowFrames = 0
        freezeFrames = 0
        maxDtNs = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun detach() {
        if (!attached) return
        attached = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!attached) return

        if (lastFrameNs == 0L) {
            lastFrameNs = frameTimeNanos
            windowStartNs = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
            return
        }

        val dt = frameTimeNanos - lastFrameNs
        lastFrameNs = frameTimeNanos
        frames++
        if (dt > maxDtNs) maxDtNs = dt
        if (dt > freezeThresholdNs) freezeFrames++
        else if (dt > slowThresholdNs) slowFrames++

        val windowElapsed = frameTimeNanos - windowStartNs
        if (windowElapsed >= reportIntervalNs) {
            val fps = frames * 1_000_000_000.0 / windowElapsed
            val maxDtMs = maxDtNs / 1_000_000.0
            val line = String.format(
                "fps=%.1f  jank=%d  freeze=%d  maxFrame=%.1fms",
                fps, slowFrames, freezeFrames, maxDtMs
            )
            Log.d(TAG, line)
            label?.post { label.text = line }

            windowStartNs = frameTimeNanos
            frames = 0
            slowFrames = 0
            freezeFrames = 0
            maxDtNs = 0L
        }

        Choreographer.getInstance().postFrameCallback(this)
    }

    companion object {
        private const val TAG = "FpsMonitor"
    }
}
