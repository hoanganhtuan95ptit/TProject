package com.simple.ui.precompute.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Rect
import android.os.Build
import android.util.Log
import kotlin.math.roundToInt

/**
 * A small RenderNode-like compatibility wrapper.
 *
 * Backend:
 * - API 23+: Picture
 * - API <=22: Bitmap cache, because Picture cannot be replayed on a
 *   hardware-accelerated Canvas before API 23.
 *
 * Native android.graphics.RenderNode is intentionally not used here. Layout
 * recording in this engine happens on a background thread, while framework
 * RenderNode is a HWUI primitive whose display-list state is only reliable in
 * the UI rendering pipeline.
 *
 * This class intentionally supports the common 2D subset only:
 * position, alpha, translation, scale, rotationZ, pivot and clipToBounds.
 * It is not thread-safe. Finish recording before publishing the instance to
 * the thread that will draw it; do not record and draw concurrently.
 */
class CompatRenderNode(
    name: String,
    private val invalidateCallback: (() -> Unit)? = null
) {

    enum class Implementation {
        RENDER_NODE,
        PICTURE,
        BITMAP
    }

    val implementation: Implementation

    private val backend: Backend

    var left: Int = 0
        private set

    var top: Int = 0
        private set

    var right: Int = 0
        private set

    var bottom: Int = 0
        private set

    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top

    var alpha: Float = 1f
        set(value) {
            require(value.isFinite() && value in 0f..1f) {
                "alpha must be finite and between 0 and 1: $value"
            }
            if (field == value) return
            field = value
            backend.setAlpha(value)
            notifyChanged()
        }

    var translationX: Float = 0f
        set(value) {
            require(value.isFinite()) { "translationX must be finite: $value" }
            if (field == value) return
            field = value
            backend.setTranslationX(value)
            notifyChanged()
        }

    var translationY: Float = 0f
        set(value) {
            require(value.isFinite()) { "translationY must be finite: $value" }
            if (field == value) return
            field = value
            backend.setTranslationY(value)
            notifyChanged()
        }

    var scaleX: Float = 1f
        set(value) {
            require(value.isFinite()) { "scaleX must be finite: $value" }
            if (field == value) return
            field = value
            backend.setScaleX(value)
            notifyChanged()
        }

    var scaleY: Float = 1f
        set(value) {
            require(value.isFinite()) { "scaleY must be finite: $value" }
            if (field == value) return
            field = value
            backend.setScaleY(value)
            notifyChanged()
        }

    /** Rotation in degrees around the Z axis. */
    var rotationZ: Float = 0f
        set(value) {
            require(value.isFinite()) { "rotationZ must be finite: $value" }
            if (field == value) return
            field = value
            backend.setRotationZ(value)
            notifyChanged()
        }

    var clipToBounds: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            backend.setClipToBounds(value)
            notifyChanged()
        }

    var hasOverlappingRendering: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            backend.setHasOverlappingRendering(value)
            notifyChanged()
        }

    private val legacyTransform = Matrix()

    private var explicitPivotX: Float? = null
    private var explicitPivotY: Float? = null
    private var recording = false

    val pivotX: Float
        get() = explicitPivotX ?: width / 2f

    val pivotY: Float
        get() = explicitPivotY ?: height / 2f

    val isPivotExplicitlySet: Boolean
        get() = explicitPivotX != null

    val hasDisplayList: Boolean
        get() = backend.hasDisplayList

    init {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                implementation = Implementation.PICTURE
                backend = PictureBackend()
            }

            else -> {
                implementation = Implementation.BITMAP
                backend = BitmapBackend()
            }
        }

        backend.setAlpha(alpha)
        backend.setTranslationX(translationX)
        backend.setTranslationY(translationY)
        backend.setScaleX(scaleX)
        backend.setScaleY(scaleY)
        backend.setRotationZ(rotationZ)
        backend.setClipToBounds(clipToBounds)
        backend.setHasOverlappingRendering(hasOverlappingRendering)
    }

    /**
     * Sets the node's position in its parent Canvas coordinate space.
     * Returns true only when the bounds changed.
     */
    fun setPosition(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        require(right >= left) { "right ($right) must be >= left ($left)" }
        require(bottom >= top) { "bottom ($bottom) must be >= top ($top)" }

        if (
            this.left == left &&
            this.top == top &&
            this.right == right &&
            this.bottom == bottom
        ) {
            return false
        }

        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom

        backend.setPosition(left, top, right, bottom)
        notifyChanged()
        return true
    }

    fun offsetLeftAndRight(offset: Int): Boolean {
        if (offset == 0) return false
        return setPosition(left + offset, top, right + offset, bottom)
    }

    fun offsetTopAndBottom(offset: Int): Boolean {
        if (offset == 0) return false
        return setPosition(left, top + offset, right, bottom + offset)
    }

    fun setPivot(pivotX: Float, pivotY: Float) {
        require(pivotX.isFinite()) { "pivotX must be finite: $pivotX" }
        require(pivotY.isFinite()) { "pivotY must be finite: $pivotY" }

        if (explicitPivotX == pivotX && explicitPivotY == pivotY) return

        explicitPivotX = pivotX
        explicitPivotY = pivotY
        backend.setPivot(pivotX, pivotY)
        notifyChanged()
    }

    fun resetPivot(): Boolean {
        if (explicitPivotX == null) return false

        explicitPivotX = null
        explicitPivotY = null
        backend.resetPivot()
        notifyChanged()
        return true
    }

    /** Starts recording using the current position's width and height. */
    fun beginRecording(): Canvas {
        check(!recording) { "A recording is already in progress" }
        require(width > 0 && height > 0) {
            "Call setPosition() with positive width and height before beginRecording()"
        }

        val canvas = backend.beginRecording(width, height)
        recording = true
        return canvas
    }

    /**
     * Changes the node size while preserving left/top, then starts recording.
     */
    fun beginRecording(width: Int, height: Int): Canvas {
        require(width > 0) { "width must be > 0: $width" }
        require(height > 0) { "height must be > 0: $height" }

        val newRight = left.toLong() + width
        val newBottom = top.toLong() + height
        require(newRight <= Int.MAX_VALUE && newBottom <= Int.MAX_VALUE) {
            "Node bounds overflow Int"
        }

        setPosition(left, top, newRight.toInt(), newBottom.toInt())
        return beginRecording()
    }

    fun endRecording() {
        check(recording) { "No recording is in progress" }
        try {
            backend.endRecording()
        } finally {
            recording = false
        }
        notifyChanged()
    }

    /**
     * Safe helper that always finishes recording. A failed block discards the
     * partially recorded display list/cache.
     */
    fun record(block: (Canvas) -> Unit) {
        val canvas = beginRecording()
        try {
            block(canvas)
            endRecording()
        } catch (error: Throwable) {
            if (recording) {
                try {
                    endRecording()
                } catch (endError: Throwable) {
                    error.addSuppressed(endError)
                }
            }
            backend.discardDisplayList()
            throw error
        }
    }

    fun record(width: Int, height: Int, block: (Canvas) -> Unit) {
        val canvas = beginRecording(width, height)
        try {
            block(canvas)
            endRecording()
        } catch (error: Throwable) {
            if (recording) {
                try {
                    endRecording()
                } catch (endError: Throwable) {
                    error.addSuppressed(endError)
                }
            }
            backend.discardDisplayList()
            throw error
        }
    }

    /** Draws the cached content into the supplied Canvas. */
    fun draw(canvas: Canvas) {
        if (!backend.hasDisplayList || alpha <= 0f) return

        if (backend.hasNativeProperties) {
            backend.drawContent(
                canvas = canvas,
                alpha = alpha,
                width = width,
                height = height,
                clipToBounds = clipToBounds
            )
            return
        }

        drawLegacy(canvas)
    }

    fun discardDisplayList() {
        Log.d("tuanha", "discardDisplayList: ")
        check(!recording) { "Cannot discard while recording" }
        if (!backend.hasDisplayList) return
        backend.discardDisplayList()
        notifyChanged()
    }

    private fun drawLegacy(canvas: Canvas) {
        val saveCount = canvas.save()
        try {
            // Match the transform order documented by RenderNode:
            // setTranslate -> preRotate -> preScale. The owning Canvas is first
            // translated to the node's left/top position.
            canvas.translate(left.toFloat(), top.toFloat())

            legacyTransform.setTranslate(translationX, translationY)
            legacyTransform.preRotate(rotationZ, pivotX, pivotY)
            legacyTransform.preScale(scaleX, scaleY, pivotX, pivotY)
            canvas.concat(legacyTransform)

            if (clipToBounds) {
                canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
            }

            backend.drawContent(
                canvas = canvas,
                alpha = alpha,
                width = width,
                height = height,
                clipToBounds = clipToBounds
            )
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    private fun notifyChanged() {
        invalidateCallback?.invoke()
    }

    private interface Backend {
        val hasNativeProperties: Boolean
        val hasDisplayList: Boolean

        fun setPosition(left: Int, top: Int, right: Int, bottom: Int)
        fun setAlpha(value: Float)
        fun setTranslationX(value: Float)
        fun setTranslationY(value: Float)
        fun setScaleX(value: Float)
        fun setScaleY(value: Float)
        fun setRotationZ(value: Float)
        fun setPivot(pivotX: Float, pivotY: Float)
        fun resetPivot()
        fun setClipToBounds(value: Boolean)
        fun setHasOverlappingRendering(value: Boolean)

        fun beginRecording(width: Int, height: Int): Canvas
        fun endRecording()

        fun drawContent(
            canvas: Canvas,
            alpha: Float,
            width: Int,
            height: Int,
            clipToBounds: Boolean
        )

        fun discardDisplayList()
    }

    private class PictureBackend : Backend {
        private var picture: Picture? = null
        private var recordingPicture: Picture? = null
        private val clipBounds = Rect()

        override val hasNativeProperties: Boolean = false

        override val hasDisplayList: Boolean
            get() = picture != null

        override fun setPosition(left: Int, top: Int, right: Int, bottom: Int) = Unit
        override fun setAlpha(value: Float) = Unit
        override fun setTranslationX(value: Float) = Unit
        override fun setTranslationY(value: Float) = Unit
        override fun setScaleX(value: Float) = Unit
        override fun setScaleY(value: Float) = Unit
        override fun setRotationZ(value: Float) = Unit
        override fun setPivot(pivotX: Float, pivotY: Float) = Unit
        override fun resetPivot() = Unit
        override fun setClipToBounds(value: Boolean) = Unit
        override fun setHasOverlappingRendering(value: Boolean) = Unit

        override fun beginRecording(width: Int, height: Int): Canvas {
            check(recordingPicture == null) { "Picture recording already in progress" }
            return Picture().also { recordingPicture = it }.beginRecording(width, height)
        }

        override fun endRecording() {
            val current = checkNotNull(recordingPicture) {
                "Picture recording is not in progress"
            }
            try {
                current.endRecording()
                picture = current
            } finally {
                recordingPicture = null
            }
        }

        @Suppress("DEPRECATION")
        override fun drawContent(
            canvas: Canvas,
            alpha: Float,
            width: Int,
            height: Int,
            clipToBounds: Boolean
        ) {
            val current = picture ?: return

            if (alpha >= 1f) {
                current.draw(canvas)
                return
            }

            val alphaInt = (alpha * 255f).roundToInt().coerceIn(0, 255)
            if (alphaInt == 0) return

            val layer = if (clipToBounds) {
                canvas.saveLayerAlpha(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    alphaInt,
                    Canvas.ALL_SAVE_FLAG
                )
            } else {
                if (!canvas.getClipBounds(clipBounds)) return
                canvas.saveLayerAlpha(
                    clipBounds.left.toFloat(),
                    clipBounds.top.toFloat(),
                    clipBounds.right.toFloat(),
                    clipBounds.bottom.toFloat(),
                    alphaInt,
                    Canvas.ALL_SAVE_FLAG
                )
            }

            try {
                current.draw(canvas)
            } finally {
                canvas.restoreToCount(layer)
            }
        }

        override fun discardDisplayList() {
            Log.d("tuanha", "discardDisplayList: ")
            picture = null
            recordingPicture = null
        }
    }

    private class BitmapBackend : Backend {
        private var bitmap: Bitmap? = null
        private var recordingBitmap: Bitmap? = null
        private var recordingCanvas: Canvas? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        override val hasNativeProperties: Boolean = false

        override val hasDisplayList: Boolean
            get() = bitmap != null

        override fun setPosition(left: Int, top: Int, right: Int, bottom: Int) = Unit
        override fun setAlpha(value: Float) = Unit
        override fun setTranslationX(value: Float) = Unit
        override fun setTranslationY(value: Float) = Unit
        override fun setScaleX(value: Float) = Unit
        override fun setScaleY(value: Float) = Unit
        override fun setRotationZ(value: Float) = Unit
        override fun setPivot(pivotX: Float, pivotY: Float) = Unit
        override fun resetPivot() = Unit
        override fun setClipToBounds(value: Boolean) = Unit
        override fun setHasOverlappingRendering(value: Boolean) = Unit

        override fun beginRecording(width: Int, height: Int): Canvas {
            check(recordingBitmap == null) { "Bitmap recording already in progress" }

            val requiredBytes = width.toLong() * height.toLong() * 4L
            require(requiredBytes <= Int.MAX_VALUE.toLong()) {
                "Bitmap fallback is too large: $width x $height, approximately $requiredBytes bytes"
            }

            val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                density = Bitmap.DENSITY_NONE
            }
            val newCanvas = Canvas(newBitmap)

            recordingBitmap = newBitmap
            recordingCanvas = newCanvas
            return newCanvas
        }

        override fun endRecording() {
            val current = checkNotNull(recordingBitmap) {
                "Bitmap recording is not in progress"
            }
            bitmap = current
            recordingBitmap = null
            recordingCanvas = null
        }

        override fun drawContent(
            canvas: Canvas,
            alpha: Float,
            width: Int,
            height: Int,
            clipToBounds: Boolean
        ) {
            val current = bitmap ?: return
            paint.alpha = (alpha * 255f).roundToInt().coerceIn(0, 255)
            canvas.drawBitmap(current, 0f, 0f, paint)
        }

        override fun discardDisplayList() {
            Log.d("tuanha", "discardDisplayList: ")
            // Do not call Bitmap.recycle() here. A previous frame/display list may
            // still reference the bitmap. Let GC release it when safe.
            bitmap = null
            recordingBitmap = null
            recordingCanvas = null
        }
    }
}
