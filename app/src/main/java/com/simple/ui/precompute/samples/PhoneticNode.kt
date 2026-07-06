package com.simple.ui.precompute.samples

import android.graphics.Typeface
import android.util.Log
import android.view.View
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.emptyImage
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.GroupSpec
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.LoadingNode
import com.simple.ui.precompute.node.LoadingSpec
import com.simple.ui.precompute.node.OutlineState
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.GroupNode
import com.simple.ui.precompute.node.LinearChild
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText

/**
 * PhoneticNode — Ví dụ node tự dựng cây phonetic:
 * - Tận dụng [GroupNode] để làm container chính (Linear VERTICAL).
 * - Tận dụng [GroupSpec] để quản lý danh sách children phẳng (bao gồm Outline + Content).
 * - Tránh lồng thêm ConstraintNode trung gian.
 */
class PhoneticNode(
    override val id: String = "",
    private val text: String = "",
    private val contentColor: Int = 0xFF19D96B.toInt(),
    private val strokeShow: Boolean = true,
    private val onlyReading: Boolean = true,
    private val textDisplay: BigText = emptyText(),
    private val phoneticDisplay: BigText = emptyText(),
    private val iconShow: Boolean = false,
    private val iconDisplay: BigImage = emptyImage(),
    private val textSizePx: Float = 16f,
    private val phoneticTextSizePx: Float = 13f,
    private val gapPx: Int = 4,
    private val iconSizePx: Int = 12,
    private val strokeWidthPx: Float = 1f,
    private val cornerRadiusPx: Float = 16f,
    private val dashWidthPx: Float = 4f,
    private val dashGapPx: Float = 4f,
    private val contentPadding: EdgeInsets = EdgeInsets.symmetric(h = 8, v = 4),
    layoutWidth: LayoutDimension = LayoutDimension.WrapContent,
    layoutHeight: LayoutDimension = LayoutDimension.WrapContent
) : GroupNode(
    orientation = Orientation.VERTICAL,
    crossAlign = CrossAlign.START,
    padding = contentPadding,
    layoutWidth = layoutWidth,
    layoutHeight = layoutHeight
) {

    private val outlineNode = LoadingNode(
        strokeColor = contentColor,
        strokeWidth = strokeWidthPx,
        cornerRadius = cornerRadiusPx,
        dashWidth = dashWidthPx,
        dashGap = dashGapPx,
        id = "${id}_outline"
    )

    override fun buildChildren(): List<LayoutNode> = listOfNotNull(
        LinearNode(
            orientation = Orientation.HORIZONTAL,
            crossAlign = CrossAlign.CENTER,
            gap = gapPx,
            children = listOfNotNull(
                LinearChild(
                    node = TextNode(
                        text = textDisplay,
                        textSizePx = textSizePx,
                        color = contentColor,
                        maxLines = 1,
                        typeface = Typeface.DEFAULT_BOLD
                    )
                ),
                iconShow.takeIf { it }?.let {
                    LinearChild(
                        node = ImageNode(
                            source = iconDisplay,
                            layoutWidth = LayoutDimension.Fixed(iconSizePx),
                            layoutHeight = LayoutDimension.Fixed(iconSizePx)
                        )
                    )
                }
            )
        ),
        TextNode(
            text = phoneticDisplay,
            textSizePx = phoneticTextSizePx,
            color = contentColor,
            maxLines = 1
        ).takeIf { phoneticDisplay != emptyText() }
    )

    override fun createSpec(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        placedChildren: List<DrawSpec>
    ): GroupSpec {
        // Outline bao phủ toàn bộ diện tích của PhoneticNode (bao gồm cả padding)
        val outline = LoadingSpec(
            left = x,
            top = y,
            width = width,
            height = height,
            padding = EdgeInsets.ZERO,
            strokeColor = contentColor,
            strokeWidth = strokeWidthPx,
            cornerRadius = cornerRadiusPx,
            dashWidth = dashWidthPx,
            dashGap = dashGapPx,
            loadingSegmentRatio = 0.5f,
            loadingDurationMs = 1200L,
            state = if (strokeShow) OutlineState.IDLE else OutlineState.HIDDEN,
            node = outlineNode
        )

        return PhoneticSpec(
            id = id,
            text = text,
            strokeShow = strokeShow,
            onlyReading = onlyReading,
            left = x,
            top = y,
            width = width,
            height = height,
            node = this,
            // Outline được đưa lên đầu để vẽ như background
            children = listOf(outline) + placedChildren,
            outlineSpec = outline
        )
    }
}

/**
 * PhoneticSpec — Spec bọc cây phonetic và tự xử lý click.
 *
 * Tận dụng cơ chế của [GroupSpec]:
 * - Tự động quản lý lifecycle (attach/detach) cho children.
 * - Tự động hỗ trợ shift position (withPosition) cho children.
 * - findOutlineSpec trở nên đơn giản hơn do OutlineSpec là child trực tiếp.
 */
class PhoneticSpec(
    val id: String = "",
    val text: String = "",
    val strokeShow: Boolean = true,
    val onlyReading: Boolean = true,
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    override val node: LayoutNode,
    override val children: List<DrawSpec>,
    private var outlineSpec: LoadingSpec? = null
) : GroupSpec(left, top, width, height, node, children) {

    private var isReading: Boolean = false

    init {
        onClick = ::handleClick
        if (outlineSpec == null) {
            outlineSpec = children.filterIsInstance<LoadingSpec>().firstOrNull()
        }
    }

    override fun onAttachedToWindow(view: View) {
        super.onAttachedToWindow(view)
        outlineSpec?.setLoading(isReading, show = strokeShow || isReading, animate = false)
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec {
        val superSpec = super.withPosition(newLeft, newTop) as GroupSpec
        return PhoneticSpec(
            id = id,
            text = text,
            strokeShow = strokeShow,
            onlyReading = onlyReading,
            left = superSpec.left,
            top = superSpec.top,
            width = superSpec.width,
            height = superSpec.height,
            node = node,
            children = superSpec.children
        ).also {
            it.isReading = isReading
        }
    }

    private fun handleClick() {
        Log.d("tuanha", "onClick: $this")
        isReading = if (onlyReading) !isReading else true
        outlineSpec?.setLoading(isReading, show = true, animate = true)
    }
}
