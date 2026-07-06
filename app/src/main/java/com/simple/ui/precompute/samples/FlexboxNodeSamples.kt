package com.simple.ui.precompute.samples

import android.graphics.Color
import android.graphics.Typeface
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.FlexAlignContent
import com.simple.ui.precompute.node.FlexAlignItems
import com.simple.ui.precompute.node.FlexChild
import com.simple.ui.precompute.node.FlexDirection
import com.simple.ui.precompute.node.FlexJustifyContent
import com.simple.ui.precompute.node.FlexWrap
import com.simple.ui.precompute.node.FlexboxNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LoadingNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.text.BigText

/**
 * DEMO ⑬ — [FlexboxNode].
 *
 * Wrap tags giống FlexboxLayout.
 */
class FlexboxNodeSamples(m: SampleMetrics) : SampleBuilder(m) {

    fun buildTagsCard(): LayoutNode {
        val tags = listOf(
            "Kotlin" to 0xFFE91E63.toInt(),
            "Android" to 0xFF4CAF50.toInt(),
            "Precompute" to 0xFF2196F3.toInt(),
            "LayoutEngine" to 0xFF795548.toInt(),
            "FlexboxLayout" to 0xFF6200EE.toInt(),
            "wrapBefore" to 0xFFFF9800.toInt(),
            "space_between" to 0xFF009688.toInt(),
            "alignItems" to 0xFF3F51B5.toInt(),
            "DrawSpec" to 0xFF607D8B.toInt()
        )

        return FlexboxNode(
            flexDirection = FlexDirection.ROW,
            flexWrap = FlexWrap.WRAP,
            justifyContent = FlexJustifyContent.SPACE_BETWEEN,
            alignItems = FlexAlignItems.CENTER,
            alignContent = FlexAlignContent.FLEX_START,
            gap = dp(8),
            padding = EdgeInsets.all(dp(16)),
            layoutWidth = LayoutDimension.MatchParent,
            children = tags.mapIndexed { index, (label, color) ->
                FlexChild(
                    node = buildTagChip(label, color),
                    order = index,
                    wrapBefore = label == "space_between"
                )
            }
        )
    }

    private fun buildTagChip(label: String, color: Int): LayoutNode = ConstraintNode(
        children = listOf(
            ConstraintChild(
                id = "bg",
                node = LoadingNode(
                    strokeWidth = 0f,
                    cornerRadius = dp(18).toFloat(),
                    layoutWidth = LayoutDimension.MatchParent,
                    layoutHeight = LayoutDimension.MatchParent
                ),
                startToStartOf = "text",
                endToEndOf = "text",
                topToTopOf = "text",
                bottomToBottomOf = "text",
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "text",
                node = TextNode(
                    text = BigText(label),
                    textSizePx = sp(12f),
                    color = Color.WHITE,
                    typeface = Typeface.DEFAULT_BOLD,
                    maxLines = 1,
                    padding = EdgeInsets.symmetric(h = dp(10), v = dp(6))
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT
            )
        )
    )
}
