package com.simple.ui.precompute.samples

import android.graphics.Color
import android.graphics.Typeface
import com.simple.t.R
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearChild
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.LineNode
import com.simple.ui.precompute.node.SpaceNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.BigText

/**
 * DEMO ① ⑩ ⑫ — [LinearNode].
 *
 * ① `buildCard`          — Row(icon | Column(title, ipa, meaning)).
 * ⑩ `buildNoteRow`       — LinearLayout horizontal (icon + vertical text column).
 * ⑫ `buildDashedLineText` — Vertical column (DashedLine + Text).
 */
class LinearNodeSamples(m: SampleMetrics) : SampleBuilder(m) {

    fun buildCard(
        word: String,
        ipa: String,
        meaning: String,
        iconSource: BigImage,
        iconSizePx: Int
    ): LayoutNode = LinearNode(
        orientation = Orientation.HORIZONTAL,
        crossAlign = CrossAlign.CENTER,
        gap = dp(12),
        padding = EdgeInsets.all(dp(12)),
        children = listOf(
            ImageNode(
                source = iconSource,
                layoutWidth = LayoutDimension.Fixed(iconSizePx),
                layoutHeight = LayoutDimension.Fixed(iconSizePx)
            ).linearChild(),
            LinearNode(
                orientation = Orientation.VERTICAL,
                gap = dp(4),
                children = listOf(
                    TextNode(BigText(word), sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 2).linearChild(),
                    TextNode(BigText(ipa), sp(14f), 0xFF6200EE.toInt(), maxLines = 1).linearChild(),
                    TextNode(BigText(meaning), sp(12f), Color.GRAY, maxLines = 2).linearChild()
                )
            ).linearChild()
        )
    )

    /**
     * XML NoteRow — chuyển đổi trực tiếp từ LinearLayout trong XML.
     */
    fun buildNoteRow(
        title: String,
        note: String,
        iconSource: BigImage
    ): LayoutNode = LinearNode(
        orientation = Orientation.HORIZONTAL,
        crossAlign = CrossAlign.START,
        padding = EdgeInsets.symmetric(h = dp(16), v = dp(8)),
        layoutWidth = LayoutDimension.MatchParent,
        children = listOf(
            ImageNode(
                source = iconSource,
                layoutWidth = LayoutDimension.Fixed(dp(28)),
                layoutHeight = LayoutDimension.Fixed(dp(28))
            ).linearChild(),
            SpaceNode.horizontal(dp(16)).linearChild(),
            LinearNode(
                orientation = Orientation.VERTICAL,
                layoutWidth = LayoutDimension.MatchParent,
                children = listOf(
                    TextNode(
                        text = BigText(title),
                        textSizePx = sp(14f),
                        color = 0xFF202124.toInt(),
                        layoutWidth = LayoutDimension.MatchParent
                    ).linearChild(),
                    SpaceNode.vertical(dp(8)).linearChild(),
                    TextNode(
                        text = BigText(note),
                        textSizePx = sp(14f),
                        color = 0xFF5F6368.toInt(),
                        layoutWidth = LayoutDimension.MatchParent
                    ).linearChild()
                )
            ).linearChild()
        )
    )

    /**
     * DashedLineText — dựng lại ảnh mẫu bằng [LineNode] + [TextNode].
     */
    fun buildDashedLineText(): LayoutNode = LinearNode(
        orientation = Orientation.VERTICAL,
        crossAlign = CrossAlign.CENTER,
        gap = dp(12),
        padding = EdgeInsets(top = dp(14)),
        layoutWidth = LayoutDimension.MatchParent,
        layoutHeight = LayoutDimension.Fixed(dp(86)),
        children = listOf(
            LineNode(
                color = 0xFFB8B8B8.toInt(),
                strokeWidth = 1.5f * m.density,
                dashWidth = dp(6).toFloat(),
                dashGap = dp(6).toFloat(),
                layoutWidth = LayoutDimension.Fixed(dp(240)),
                layoutHeight = LayoutDimension.Fixed(dp(2))
            ).linearChild(),
            TextNode(
                text = BigText("rất vui được quen biết bạn"),
                textSizePx = sp(20f),
                color = 0xFF202124.toInt(),
                maxLines = 1
            ).linearChild()
        )
    )

    /**
     * Build a node like the requested image:
     * Bold text + speaker icon
     * Phonetic text below
     */
    fun buildWordPhonetic(word: String, ipa: String): LayoutNode = LinearNode(
        orientation = Orientation.VERTICAL,
        gap = dp(2),
        padding = EdgeInsets.all(dp(16)),
        children = listOf(
            LinearNode(
                orientation = Orientation.HORIZONTAL,
                crossAlign = CrossAlign.CENTER,
                gap = dp(4),
                children = listOf(
                    TextNode(
                        text = BigText(word),
                        textSizePx = sp(18f),
                        color = Color.BLACK,
                        typeface = Typeface.DEFAULT_BOLD
                    ).linearChild(),
                    ImageNode(
                        source = BigImage(R.drawable.ic_try_speak_mic),
                        layoutWidth = LayoutDimension.Fixed(dp(14)),
                        layoutHeight = LayoutDimension.Fixed(dp(14))
                    ).linearChild()
                )
            ).linearChild(),
            TextNode(
                text = BigText(ipa),
                textSizePx = sp(14f),
                color = 0xFFA52A2A.toInt() // Reddish brown
            ).linearChild()
        )
    )
}
