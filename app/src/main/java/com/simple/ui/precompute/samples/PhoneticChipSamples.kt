package com.simple.ui.precompute.samples

import android.graphics.Color
import com.simple.t.R
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.LoadingNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.span.BigTextSize
import com.simple.ui.precompute.text.toBuilder
import com.simple.ui.precompute.text.with

/**
 * DEMO ⑦ — PhoneticChip từ XML → Node.
 *
 * ```
 * ┌──────────────────────┐
 * │ /həˈloʊ/  [🔊]      │  ← nền xanh #55BB55
 * └──────────────────────┘
 * ```
 */
class PhoneticChipSamples(m: SampleMetrics) : SampleBuilder(m) {

    fun buildChip(phonetic: String): LayoutNode = ConstraintNode(
        children = listOf(
            ConstraintChild(
                id = "bg",
                node = LoadingNode(
                    strokeWidth = 0f,
                    layoutWidth = LayoutDimension.MatchParent,
                    layoutHeight = LayoutDimension.MatchParent
                ),
                startToStartOf = "content",
                endToEndOf = "content",
                topToTopOf = "content",
                bottomToBottomOf = "content",
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "content",
                node = LinearNode(
                    orientation = Orientation.HORIZONTAL,
                    crossAlign = CrossAlign.CENTER,
                    gap = dp(4),
                    padding = EdgeInsets.symmetric(h = dp(8), v = dp(4)),
                    children = listOf(
                        TextNode(
                            text = "tuanha: $phonetic".toBuilder().with(BigTextSize(50), BigForegroundColor(Color.GREEN)).build(),
                        ).linearChild(weight = 1f),
                        ImageNode(
                            source = BigImage(R.mipmap.ic_launcher),
                            layoutWidth = LayoutDimension.Fixed(dp(10)),
                            layoutHeight = LayoutDimension.Fixed(dp(24))
                        ).linearChild()
                    )
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT
            )
        )
    )
}
