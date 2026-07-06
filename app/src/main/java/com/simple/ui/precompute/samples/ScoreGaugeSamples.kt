package com.simple.ui.precompute.samples

import android.graphics.Color
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.GaugeArcNode
import com.simple.ui.precompute.node.GaugeScoreNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode

/**
 * DEMO ⑧ — ScoreGauge.
 *
 * [GaugeArcNode] + [GaugeScoreNode] overlay trong cùng 1 [ConstraintNode] vuông.
 */
class ScoreGaugeSamples(m: SampleMetrics) : SampleBuilder(m) {

    fun buildGauge(
        progress: Int,
        grade: String = "",
        label: String = "ĐIỂM",
        sizePx: Int,
        strokeWidthPx: Float = dp(1).toFloat()
    ): LayoutNode = ConstraintNode(
        layoutWidth = LayoutDimension.Fixed(sizePx),
        layoutHeight = LayoutDimension.Fixed(sizePx),
        children = listOf(
            ConstraintChild(
                id = "arc",
                node = GaugeArcNode(
                    progress = progress,
                    progressColor = Color.BLACK,
                    strokeWidthPx = strokeWidthPx
                ),
                startToStartOf = ConstraintNode.PARENT,
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
                bottomToBottomOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "score",
                node = GaugeScoreNode(
                    progress = progress,
                    label = label,
                    grade = grade,
                    gradeColor = Color.BLACK
                ),
                startToStartOf = ConstraintNode.PARENT,
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
                bottomToBottomOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent
            )
        )
    )
}
