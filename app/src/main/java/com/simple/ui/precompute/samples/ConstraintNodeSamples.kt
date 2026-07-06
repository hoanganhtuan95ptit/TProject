package com.simple.ui.precompute.samples

import android.graphics.Color
import android.graphics.Typeface
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
import com.simple.ui.precompute.node.LinearChild
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.span.BigTextSize
import com.simple.ui.precompute.text.with

/**
 * DEMO ②③④ ⑪ — [ConstraintNode].
 *
 * ② `buildCard`          — icon + badge + title/ipa/meaning liên kết theo constraint.
 * ③ `buildProfileCard`   — view con leo nhau (view-to-view, cả ngang lẫn dọc).
 * ④ `buildWrapContent*`  — WrapContent chaining + center-in-parent.
 * ⑪ `buildTrySpeakChip`  — dựng lại giao diện ảnh mẫu (mic icon + text) bằng node.
 */
class ConstraintNodeSamples(m: SampleMetrics) : SampleBuilder(m) {

    /**
     * ```
     * ┌──────────────────────────────────┐
     * │ [icon]  title (MatchParent) [EN]│
     * │         /ipa/                   │
     * │         meaning                 │
     * └──────────────────────────────────┘
     * ```
     */
    fun buildCard(
        word: String,
        ipa: String,
        meaning: String,
        iconSource: BigImage,
        iconSizePx: Int
    ): LayoutNode = ConstraintNode(
        padding = EdgeInsets.all(dp(12)),
        children = listOf(
            ConstraintChild(
                id = "icon",
                node = ImageNode(
                    source = iconSource,
                    layoutWidth = LayoutDimension.Fixed(iconSizePx),
                    layoutHeight = LayoutDimension.Fixed(iconSizePx)
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT
            ),
            ConstraintChild(
                id = "badge",
                node = TextNode(
                    text = BigText("EN"),
                    textSizePx = sp(10f),
                    color = 0xFF6200EE.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(6), v = dp(3))
                ),
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT
            ),
            ConstraintChild(
                id = "title",
                node = TextNode(BigText(word), sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 2),
                startToEndOf = "icon", marginStart = dp(12),
                endToStartOf = "badge", marginEnd = dp(8),
                topToTopOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "ipa",
                node = TextNode(BigText(ipa), sp(14f), 0xFF6200EE.toInt(), maxLines = 1),
                startToStartOf = "title",
                endToEndOf = "title",
                topToBottomOf = "title", marginTop = dp(4),
                width = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "meaning",
                node = TextNode(BigText(meaning), sp(12f), Color.GRAY, maxLines = 2),
                startToStartOf = "ipa",
                endToEndOf = "ipa",
                topToBottomOf = "ipa", marginTop = dp(4),
                width = LayoutDimension.MatchParent
            )
        )
    )

    fun buildProfileCard(
        name: String,
        tag: String,
        role: String,
        avatarSource: BigImage,
        avatarSizePx: Int
    ): LayoutNode = ConstraintNode(
        padding = EdgeInsets(left = dp(12), top = dp(12), right = dp(12), bottom = dp(12)),
        children = listOf(
            ConstraintChild(
                id = "avatar",
                node = ImageNode(
                    source = avatarSource,
                    layoutWidth = LayoutDimension.Fixed(avatarSizePx),
                    layoutHeight = LayoutDimension.Fixed(avatarSizePx)
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT
            ),
            ConstraintChild(
                id = "name",
                node = TextNode(BigText(name), sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 1),
                startToEndOf = "avatar", marginStart = dp(12),
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "tag",
                node = TextNode(
                    text = BigText(tag),
                    textSizePx = sp(11f),
                    color = 0xFFFFFFFF.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(6), v = dp(3))
                ),
                startToEndOf = "avatar", marginStart = dp(12),
                topToBottomOf = "name", marginTop = dp(4)
            ),
            ConstraintChild(
                id = "role",
                node = TextNode(BigText(role), sp(11f), Color.DKGRAY, maxLines = 1),
                startToEndOf = "tag", marginStart = dp(6),
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = "tag",
                width = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "bio",
                node = TextNode(BigText("Tapped: view ③ leo ④ (ngang) → ⑤ leo ④ (dọc)"), sp(10f), 0xFF9E9E9E.toInt(), maxLines = 2),
                startToEndOf = "avatar", marginStart = dp(12),
                endToEndOf = ConstraintNode.PARENT,
                topToBottomOf = "role", marginTop = dp(4),
                width = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "btn_like",
                node = TextNode(
                    text = BigText("♥  Like"),
                    textSizePx = sp(12f),
                    color = 0xFFFFFFFF.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(14), v = dp(7))
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToBottomOf = "bio", marginTop = dp(10)
            ),
            ConstraintChild(
                id = "btn_share",
                node = TextNode(
                    text = BigText("↗  Share"),
                    textSizePx = sp(12f),
                    color = 0xFF6200EE.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(14), v = dp(7))
                ),
                startToEndOf = "btn_like", marginStart = dp(8),
                topToTopOf = "btn_like"
            )
        )
    )

    fun buildWrapContentTagsCard(): LayoutNode = ConstraintNode(
        padding = EdgeInsets.all(dp(16)),
        children = listOf(
            ConstraintChild(
                id = "tag_1",
                node = TextNode(
                    "Kotlin".with(BigForegroundColor(Color.GREEN), BigTextSize(20)).build(),
                    sp(1f),
                    0xFFE91E63.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(12), v = dp(6))
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT
            ),
            ConstraintChild(
                id = "tag_2",
                node = TextNode(BigText("Android"), sp(14f), 0xFF4CAF50.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(12), v = dp(6))),
                startToEndOf = "tag_1", marginStart = dp(8),
                topToTopOf = "tag_1"
            ),
            ConstraintChild(
                id = "tag_3",
                node = TextNode(BigText("WrapContent"), sp(14f), 0xFF2196F3.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(12), v = dp(6))),
                startToEndOf = "tag_2", marginStart = dp(8),
                topToTopOf = "tag_2"
            )
        )
    )

    fun buildWrapContentCenterCard(): LayoutNode = ConstraintNode(
        layoutWidth = LayoutDimension.MatchParent,
        padding = EdgeInsets.all(dp(16)),
        children = listOf(
            ConstraintChild(
                id = "btn_confirm",
                node = TextNode(
                    text = BigText("NÚT CĂN GIỮA MÀN HÌNH"),
                    textSizePx = sp(14f),
                    color = 0xFF6200EE.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(24), v = dp(12))
                ),
                startToStartOf = ConstraintNode.PARENT,
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT
            )
        )
    )

    /**
     * TrySpeakChip — dựng lại giao diện trong ảnh bằng các node cũ.
     */
    fun buildTrySpeakChip(): LayoutNode {
        val green = 0xFF19D96B.toInt()

        return ConstraintNode(
            children = listOf(
                ConstraintChild(
                    id = "outline",
                    node = LoadingNode(
                        strokeColor = green,
                        strokeWidth = dp(2).toFloat(),
                        cornerRadius = dp(24).toFloat(),
                        dashWidth = dp(5).toFloat(),
                        dashGap = dp(5).toFloat(),
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
                        gap = dp(10),
                        padding = EdgeInsets(left = dp(16), top = dp(10), right = dp(18), bottom = dp(10)),
                        children = listOf(
                            LinearChild(
                                node = ImageNode(
                                    source = BigImage(R.drawable.ic_try_speak_mic),
                                    layoutWidth = LayoutDimension.Fixed(dp(22)),
                                    layoutHeight = LayoutDimension.Fixed(dp(22))
                                )
                            ),
                            LinearChild(
                                node = TextNode(
                                    text = BigText("Thử nói"),
                                    textSizePx = sp(20f),
                                    color = green,
                                    maxLines = 1
                                )
                            )
                        )
                    ),
                    startToStartOf = ConstraintNode.PARENT,
                    topToTopOf = ConstraintNode.PARENT
                )
            )
        )
    }
}
