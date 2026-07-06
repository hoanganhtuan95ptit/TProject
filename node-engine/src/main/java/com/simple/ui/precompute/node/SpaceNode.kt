package com.simple.ui.precompute.node

import android.graphics.Canvas
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext
import com.simple.ui.precompute.node.SpaceNode.Companion.fixed
import com.simple.ui.precompute.node.SpaceNode.Companion.horizontal
import com.simple.ui.precompute.node.SpaceNode.Companion.vertical

// ─────────────────────────────────────────────────────────────────────────────
// SpaceNode — node chỉ chiếm chỗ, không vẽ gì.
// Dùng khi cần khoảng trống explicit trong LinearNode / ConstraintNode thay vì
// tăng gap chung cho mọi child.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mô tả một khoảng trống không vẽ nội dung.
 *
 * Mặc định node có kích thước `0x0`. Dùng [horizontal], [vertical], [fixed],
 * hoặc truyền trực tiếp [layoutWidth] / [layoutHeight] để tạo khoảng trống cụ
 * thể theo pixel.
 */
data class SpaceNode(
    override val padding: EdgeInsets = EdgeInsets.ZERO,
    override val layoutWidth: LayoutDimension = LayoutDimension.WrapContent,
    override val layoutHeight: LayoutDimension = LayoutDimension.WrapContent
) : LayoutNode() {

    override fun measure(ctx: MeasureContext, c: Constraints, x: Int, y: Int): SpaceSpec {

        val p = padding
        val w = layoutWidth.resolve(p.horizontal, c.maxWidth)
        val h = layoutHeight.resolve(p.vertical, c.maxHeight)

        return SpaceSpec(
            left = x,
            top = y,
            width = w,
            height = h,
            node = this
        )
    }

    companion object {
        fun horizontal(widthPx: Int): SpaceNode = fixed(
            widthPx = widthPx,
            heightPx = 0
        )

        fun vertical(heightPx: Int): SpaceNode = fixed(
            widthPx = 0,
            heightPx = heightPx
        )

        fun fixed(widthPx: Int, heightPx: Int): SpaceNode = SpaceNode(
            layoutWidth = LayoutDimension.Fixed(widthPx.coerceAtLeast(0)),
            layoutHeight = LayoutDimension.Fixed(heightPx.coerceAtLeast(0))
        )
    }
}

/**
 * Kết quả đo của [SpaceNode]. Không vẽ gì, chỉ giữ bounds để parent layout tính
 * đúng kích thước và vị trí.
 */
data class SpaceSpec(
    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    override val node: SpaceNode
) : DrawSpec() {

    /** Chỉ vẽ debug rect cố định — pixel không đổi giữa các frame. */
    override val isStatic: Boolean = true

    override fun onDrawContent(canvas: Canvas) {
    }

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec = copy(
        left = newLeft,
        top = newTop
    )

    override fun withSize(newWidth: Int, newHeight: Int): DrawSpec = copy(
        width = newWidth.coerceAtLeast(0),
        height = newHeight.coerceAtLeast(0)
    )
}
