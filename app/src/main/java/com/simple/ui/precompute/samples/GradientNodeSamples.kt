package com.simple.ui.precompute.samples

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.GroupNode
import com.simple.ui.precompute.node.GroupSpec
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.Orientation

/**
 * Tùy biến Node hỗ trợ vẽ nền Gradient.
 */
class GradientGroup(
    initialChildren: List<LayoutNode>,
    val startColor: Int,
    val endColor: Int,
    val cornerRadius: Float = 0f,
    padding: EdgeInsets = EdgeInsets.ZERO,
    override val id: Any? = null,
    override val onClick: (() -> Unit)? = null
) : GroupNode(
    orientation = Orientation.VERTICAL,
    padding = padding,
    initialChildren = initialChildren,
    layoutWidth = LayoutDimension.MatchParent,
    onClick = onClick
) {

    override fun createSpec(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        placedChildren: List<DrawSpec>
    ): GroupSpec = GradientGroupSpec(
        left = x,
        top = y,
        width = width,
        height = height,
        children = placedChildren,
        node = this,
        startColor = startColor,
        endColor = endColor,
        cornerRadius = cornerRadius
    )
}

/**
 * Spec xử lý logic vẽ Gradient thực tế.
 */
class GradientGroupSpec(
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    children: List<DrawSpec>,
    node: LayoutNode,
    private val startColor: Int,
    private val endColor: Int,
    private val cornerRadius: Float
) : GroupSpec(left, top, width, height, node, children) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var attachedView: View? = null
    private var currentStartColor = startColor
    private var currentEndColor = endColor
    private var isGradientReversed = false

    // Lưu lại size để tránh khởi tạo Shader liên tục khi không thay đổi
    private var lastWidth = -1
    private var lastHeight = -1

    init {
        onClick = ::handleClick
    }

    fun setGradientColors(startColor: Int, endColor: Int) {
        if (currentStartColor == startColor && currentEndColor == endColor) return
        currentStartColor = startColor
        currentEndColor = endColor
        resetShader()
        invalidate()
    }

    fun invalidate() {
        attachedView?.postInvalidateOnAnimation()
    }

    override fun drawBackground(canvas: Canvas) {
        if (width <= 0 || height <= 0) return

        // Chỉ tạo lại Shader khi kích thước thay đổi
        if (width != lastWidth || height != lastHeight) {
            paint.shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                currentStartColor, currentEndColor,
                Shader.TileMode.CLAMP
            )
            lastWidth = width
            lastHeight = height
        }

        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        if (cornerRadius > 0) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        } else {
            canvas.drawRect(rect, paint)
        }
    }

    override fun onAttachedToWindow(view: View) {
        attachedView = view
        super.onAttachedToWindow(view)
    }

    override fun onDetachedFromWindow(view: View) {
        super.onDetachedFromWindow(view)
        if (attachedView === view) attachedView = null
    }

    /**
     * Tùy biến logic nhận click.
     * Child vẫn được ưu tiên; nếu không child nào trúng, spec có thể tự nhận click.
     */
    override fun hitTest(x: Int, y: Int): DrawSpec? {
        val lx = x - left
        val ly = y - top
        if (lx < 0 || ly < 0 || lx >= width || ly >= height) return null

        for (i in children.indices.reversed()) {
            val hit = children[i].hitTest(lx, ly)
            if (hit != null) return hit
        }

        return if (onClick != null || node.onClick != null) this else null
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec =
        GradientGroupSpec(
            left = newLeft,
            top = newTop,
            width = width,
            height = height,
            children = children,
            node = node,
            startColor = startColor,
            endColor = endColor,
            cornerRadius = cornerRadius
        ).also {
            it.setGradientColors(currentStartColor, currentEndColor)
            it.isGradientReversed = isGradientReversed
        }

    private fun handleClick() {
        isGradientReversed = !isGradientReversed
        if (isGradientReversed) {
            setGradientColors(endColor, startColor)
        } else {
            setGradientColors(startColor, endColor)
        }
        node.onClick?.invoke()
    }

    private fun resetShader() {
        paint.shader = null
        lastWidth = -1
        lastHeight = -1
    }
}
