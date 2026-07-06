package com.simple.ui.precompute.samples

import android.graphics.Color
import android.graphics.Typeface
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.LoadingNode
import com.simple.ui.precompute.node.OutlineState
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.LinearChild
import com.simple.ui.precompute.text.BigText

/**
 * DEMO ⑤ — [LoadingNode] loading card.
 *
 * Effect là sibling phủ lên content (không phải wrapper) — nhờ [ConstraintNode]
 * cho phép 2 child overlap trên cùng bounds.
 */
class OutlineNodeSamples(m: SampleMetrics) : SampleBuilder(m) {

    fun buildLoadingCard(): LayoutNode = ConstraintNode(
        layoutWidth = LayoutDimension.MatchParent,
        layoutHeight = LayoutDimension.Fixed(dp(92)),
        children = listOf(
            ConstraintChild(
                id = "outline",
                node = LoadingNode(
                    strokeColor = 0xFFE91E63.toInt(),
                    strokeWidth = dp(2).toFloat(),
                    cornerRadius = dp(16).toFloat(),
                    dashWidth = dp(10).toFloat(),
                    dashGap = dp(6).toFloat(),
                    loadingSegmentRatio = 0.35f,
                    loadingDurationMs = 10000L,
                    state = OutlineState.LOADING,
                    layoutWidth = LayoutDimension.MatchParent,
                    layoutHeight = LayoutDimension.MatchParent
                ),
                startToStartOf = ConstraintNode.PARENT,
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
                bottomToBottomOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "content",
                node = LinearNode(
                    orientation = Orientation.VERTICAL,
                    gap = dp(4),
                    padding = EdgeInsets.all(dp(16)),
                    layoutWidth = LayoutDimension.MatchParent,
                    children = listOf(
                        LinearChild(
                            node = TextNode(
                                text = BigText("Outline đang xử lý"),
                                textSizePx = sp(16f),
                                color = Color.BLACK,
                                typeface = Typeface.DEFAULT_BOLD,
                                maxLines = 1
                            )
                        ),
                        LinearChild(
                            node = TextNode(
                                text = BigText("OutlineNode chỉ vẽ effect; content nằm ở sibling khác."),
                                textSizePx = sp(12f),
                                color = Color.DKGRAY,
                                maxLines = 2
                            )
                        )
                    )
                ),
                startToStartOf = ConstraintNode.PARENT,
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
                bottomToBottomOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent,
                verticalBias = 0.5f
            )
        )
    )
}
