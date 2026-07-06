package com.simple.ui.precompute.samples

import android.graphics.Typeface
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.ProgressBarNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.LinearChild
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.BigText

/**
 * DEMO ⑨ — [ProgressBarNode].
 *
 * Bar ngang có chiều cao cố định, rộng theo card.
 */
class ProgressBarSamples(m: SampleMetrics) : SampleBuilder(m) {

    fun buildCard(
        title: String,
        progress: Int,
        max: Int,
        progressColor: Int
    ): LayoutNode {

        val safeMax = max.coerceAtLeast(1)
        val safeProgress = progress.coerceIn(0, safeMax)

        return LinearNode(
            orientation = Orientation.VERTICAL,
            gap = dp(8),
            padding = EdgeInsets.all(dp(16)),
            layoutWidth = LayoutDimension.MatchParent,
            children = listOf(
                LinearChild(
                    node = TextNode(
                        text = BigText("$title  $safeProgress/$safeMax"),
                        textSizePx = sp(14f),
                        color = 0xFF202124.toInt(),
                        typeface = Typeface.DEFAULT_BOLD,
                        maxLines = 1
                    )
                ),
                LinearChild(
                    node = ProgressBarNode(
                        progress = safeProgress,
                        max = safeMax,
                        trackColor = 0xFFE8EAED.toInt(),
                        progressColor = progressColor,
                        layoutWidth = LayoutDimension.MatchParent,
                        layoutHeight = LayoutDimension.Fixed(dp(8))
                    )
                )
            )
        )
    }
}
