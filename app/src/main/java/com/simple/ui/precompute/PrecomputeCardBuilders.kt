package com.simple.ui.precompute

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import com.simple.t.R
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.CircleCrop
import com.simple.ui.precompute.image.RoundedCorners
import com.simple.ui.precompute.image.addTransform
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.toBuilder
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.FlexAlignContent
import com.simple.ui.precompute.node.FlexAlignItems
import com.simple.ui.precompute.node.FlexChild
import com.simple.ui.precompute.node.FlexDirection
import com.simple.ui.precompute.node.FlexJustifyContent
import com.simple.ui.precompute.node.FlexWrap
import com.simple.ui.precompute.node.FlexboxNode
import com.simple.ui.precompute.node.GaugeArcNode
import com.simple.ui.precompute.node.GaugeScoreNode
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LineNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.OutlineNode
import com.simple.ui.precompute.node.OutlineState
import com.simple.ui.precompute.node.ProgressBarNode
import com.simple.ui.precompute.node.SpaceNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.span.BigTextSize
import com.simple.ui.precompute.text.with

class PrecomputeCardBuilders(
    private val activity: androidx.appcompat.app.AppCompatActivity,
) {

    private val dp by lazy { activity.resources.displayMetrics.density }

    fun buildLinearCard(
        word: String,
        ipa: String,
        meaning: String,
        iconSource: BigImage,
        iconSizePx: Int,
    ): LayoutNode = LinearNode(
        orientation = com.simple.ui.precompute.node.Orientation.HORIZONTAL,
        crossAlign = CrossAlign.CENTER,
        gap = dp(12),
        padding = EdgeInsets.all(dp(12)),
        children = listOf(
            ImageNode(source = iconSource, layoutWidth = LayoutDimension.Fixed(iconSizePx), layoutHeight = LayoutDimension.Fixed(iconSizePx)),
            LinearNode(
                orientation = com.simple.ui.precompute.node.Orientation.VERTICAL,
                gap = dp(4),
                children = listOf(
                    TextNode(BigText(word), sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 2),
                    TextNode(BigText(ipa), sp(14f), 0xFF6200EE.toInt(), maxLines = 1),
                    TextNode(BigText(meaning), sp(12f), Color.GRAY, maxLines = 2),
                )
            )
        )
    )

    fun buildNoteRowFromXml(title: String, note: String, iconSource: BigImage): LayoutNode = LinearNode(
        orientation = com.simple.ui.precompute.node.Orientation.HORIZONTAL,
        crossAlign = CrossAlign.START,
        padding = EdgeInsets.symmetric(h = dp(16), v = dp(8)),
        layoutWidth = LayoutDimension.MatchParent,
        children = listOf(
            ImageNode(source = iconSource, layoutWidth = LayoutDimension.Fixed(dp(28)), layoutHeight = LayoutDimension.Fixed(dp(28))),
            SpaceNode.horizontal(dp(16)),
            LinearNode(
                orientation = com.simple.ui.precompute.node.Orientation.VERTICAL,
                layoutWidth = LayoutDimension.MatchParent,
                children = listOf(
                    TextNode(text = BigText(title), textSizePx = sp(14f), color = 0xFF202124.toInt(), layoutWidth = LayoutDimension.MatchParent),
                    SpaceNode.vertical(dp(8)),
                    TextNode(text = BigText(note), textSizePx = sp(14f), color = 0xFF5F6368.toInt(), layoutWidth = LayoutDimension.MatchParent)
                )
            )
        )
    )

    fun buildFlexboxTagsCard(): LayoutNode { /* unchanged body moved below */ 
        val tags = listOf(
            "Kotlin" to 0xFFE91E63.toInt(),
            "Android" to 0xFF4CAF50.toInt(),
            "Precompute" to 0xFF2196F3.toInt(),
            "LayoutEngine" to 0xFF795548.toInt(),
            "FlexboxLayout" to 0xFF6200EE.toInt(),
            "wrapBefore" to 0xFFFF9800.toInt(),
            "space_between" to 0xFF009688.toInt(),
            "alignItems" to 0xFF3F51B5.toInt(),
            "DrawSpec" to 0xFF607D8B.toInt(),
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
                FlexChild(node = buildTagChip(label, color), order = index, wrapBefore = label == "space_between")
            }
        )
    }

    fun buildColorChangingFlexboxCard(): LayoutNode {
        val labels = listOf(
            "normal" to "node-1",
            "aaa" to "aaa",
            "reused measure" to "node-2",
            "custom spec" to "node-3",
            "5s ticker" to "node-4",
        )
        return ColorChangingFlexboxNode(
            targetId = "aaa",
            colors = listOf(0xFFE91E63.toInt(), 0xFF4CAF50.toInt(), 0xFF2196F3.toInt(), 0xFFFF9800.toInt()),
            intervalMs = 5_000L,
            flexDirection = FlexDirection.ROW,
            flexWrap = FlexWrap.WRAP,
            justifyContent = FlexJustifyContent.FLEX_START,
            alignItems = FlexAlignItems.CENTER,
            gap = dp(10),
            padding = EdgeInsets.all(dp(16)),
            layoutWidth = LayoutDimension.MatchParent,
            children = labels.mapIndexed { index, (label, id) ->
                FlexChild(
                    node = RuntimeColorTextNode(
                        id = id,
                        text = BigText(label),
                        textSizePx = sp(15f),
                        color = if (id == "aaa") 0xFFE91E63.toInt() else 0xFF5F6368.toInt(),
                        typeface = if (id == "aaa") Typeface.DEFAULT_BOLD else null,
                        maxLines = 1,
                        padding = EdgeInsets.symmetric(h = dp(10), v = dp(6)),
                    ),
                    order = index
                )
            }
        )
    }

    fun buildConstraintCard(word: String, ipa: String, meaning: String, iconSource: BigImage, iconSizePx: Int): LayoutNode = ConstraintNode(
        padding = EdgeInsets.all(dp(12)),
        children = listOf(
            ConstraintChild(id = "icon", node = ImageNode(source = iconSource, layoutWidth = LayoutDimension.Fixed(iconSizePx), layoutHeight = LayoutDimension.Fixed(iconSizePx)), startToStartOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT),
            ConstraintChild(id = "badge", node = TextNode(text = BigText("EN"), textSizePx = sp(10f), color = 0xFF6200EE.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(6), v = dp(3))), endToEndOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT),
            ConstraintChild(id = "title", node = TextNode(BigText(word), sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 2), startToEndOf = "icon", marginStart = dp(12), endToStartOf = "badge", marginEnd = dp(8), topToTopOf = ConstraintNode.PARENT, width = LayoutDimension.MatchParent),
            ConstraintChild(id = "ipa", node = TextNode(BigText(ipa), sp(14f), 0xFF6200EE.toInt(), maxLines = 1), startToStartOf = "title", endToEndOf = "title", topToBottomOf = "title", marginTop = dp(4), width = LayoutDimension.MatchParent),
            ConstraintChild(id = "meaning", node = TextNode(BigText(meaning), sp(12f), Color.GRAY, maxLines = 2), startToStartOf = "ipa", endToEndOf = "ipa", topToBottomOf = "ipa", marginTop = dp(4), width = LayoutDimension.MatchParent),
        )
    )

    fun buildProfileConstraintCard(name: String, tag: String, role: String, avatarSource: BigImage, avatarSizePx: Int): LayoutNode = ConstraintNode(
        padding = EdgeInsets(left = dp(12), top = dp(12), right = dp(12), bottom = dp(12)),
        children = listOf(
            ConstraintChild(id = "avatar", node = ImageNode(source = avatarSource, layoutWidth = LayoutDimension.Fixed(avatarSizePx), layoutHeight = LayoutDimension.Fixed(avatarSizePx)), startToStartOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT),
            ConstraintChild(id = "name", node = TextNode(BigText(name), sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 1), startToEndOf = "avatar", marginStart = dp(12), endToEndOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT, width = LayoutDimension.MatchParent),
            ConstraintChild(id = "tag", node = TextNode(text = BigText(tag), textSizePx = sp(11f), color = 0xFFFFFFFF.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(6), v = dp(3))), startToEndOf = "avatar", marginStart = dp(12), topToBottomOf = "name", marginTop = dp(4)),
            ConstraintChild(id = "role", node = TextNode(BigText(role), sp(11f), Color.DKGRAY, maxLines = 1), startToEndOf = "tag", marginStart = dp(6), endToEndOf = ConstraintNode.PARENT, topToTopOf = "tag", width = LayoutDimension.MatchParent),
            ConstraintChild(id = "bio", node = TextNode(BigText("Tapped: view ③ leo ④ (ngang) → ⑤ leo ④ (dọc)"), sp(10f), 0xFF9E9E9E.toInt(), maxLines = 2), startToEndOf = "avatar", marginStart = dp(12), endToEndOf = ConstraintNode.PARENT, topToBottomOf = "role", marginTop = dp(4), width = LayoutDimension.MatchParent),
            ConstraintChild(id = "btn_like", node = TextNode(text = BigText("♥  Like"), textSizePx = sp(12f), color = 0xFFFFFFFF.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(14), v = dp(7))), startToStartOf = ConstraintNode.PARENT, topToBottomOf = "bio", marginTop = dp(10)),
            ConstraintChild(id = "btn_share", node = TextNode(text = BigText("↗  Share"), textSizePx = sp(12f), color = 0xFF6200EE.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(14), v = dp(7))), startToEndOf = "btn_like", marginStart = dp(8), topToTopOf = "btn_like"),
        )
    )

    fun buildWrapContentTagsCard(): LayoutNode = ConstraintNode(
        padding = EdgeInsets.all(dp(16)),
        children = listOf(
            ConstraintChild(id = "tag_1", node = TextNode("Kotlin".with(BigForegroundColor(Color.GREEN), BigTextSize(20)).build(), sp(1f), 0xFFE91E63.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(12), v = dp(6))), startToStartOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT),
            ConstraintChild(id = "tag_2", node = TextNode(BigText("Android"), sp(14f), 0xFF4CAF50.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(12), v = dp(6))), startToEndOf = "tag_1", marginStart = dp(8), topToTopOf = "tag_1"),
            ConstraintChild(id = "tag_3", node = TextNode(BigText("WrapContent"), sp(14f), 0xFF2196F3.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(12), v = dp(6))), startToEndOf = "tag_2", marginStart = dp(8), topToTopOf = "tag_2")
        )
    )

    fun buildWrapContentCenterCard(): LayoutNode = ConstraintNode(
        layoutWidth = LayoutDimension.MatchParent,
        padding = EdgeInsets.all(dp(16)),
        children = listOf(
            ConstraintChild(id = "btn_confirm", node = TextNode(text = BigText("NÚT CĂN GIỮA MÀN HÌNH"), textSizePx = sp(14f), color = 0xFF6200EE.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(24), v = dp(12))), startToStartOf = ConstraintNode.PARENT, endToEndOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT)
        )
    )

    fun buildTransformCard(iconSizePx: Int): LayoutNode {
        val baseSource = R.mipmap.ic_launcher
        val rounded = dp(12)
        fun item(label: String, image: BigImage) = LinearNode(
            orientation = com.simple.ui.precompute.node.Orientation.VERTICAL,
            crossAlign = CrossAlign.CENTER,
            gap = dp(6),
            children = listOf(ImageNode(source = image, layoutWidth = LayoutDimension.Fixed(iconSizePx), layoutHeight = LayoutDimension.Fixed(iconSizePx)), TextNode(text = BigText(label), textSizePx = sp(11f), color = Color.DKGRAY, typeface = Typeface.DEFAULT_BOLD, maxLines = 1))
        )
        return LinearNode(
            orientation = com.simple.ui.precompute.node.Orientation.HORIZONTAL,
            crossAlign = CrossAlign.CENTER,
            gap = dp(20),
            padding = EdgeInsets.all(dp(16)),
            children = listOf(
                item("Original", BigImage(source = baseSource)),
                item("CircleCrop", R.drawable.image.toBuilder().addTransform(CircleCrop).build()),
                item("Rounded ${rounded}px", R.drawable.img1.toBuilder().addTransform(RoundedCorners(rounded)).build()),
            )
        )
    }

    fun buildPhoneticChip(phonetic: String): LayoutNode = ConstraintNode(
        children = listOf(
            ConstraintChild(id = "bg", node = OutlineNode(backgroundColor = Color.parseColor("#55BB55"), strokeWidth = 0f, layoutWidth = LayoutDimension.MatchParent, layoutHeight = LayoutDimension.MatchParent), startToStartOf = "content", endToEndOf = "content", topToTopOf = "content", bottomToBottomOf = "content", width = LayoutDimension.MatchParent, height = LayoutDimension.MatchParent),
            ConstraintChild(id = "content", node = LinearNode(orientation = com.simple.ui.precompute.node.Orientation.HORIZONTAL, crossAlign = CrossAlign.CENTER, gap = dp(4), padding = EdgeInsets.symmetric(h = dp(8), v = dp(4)), children = listOf(TextNode(text = BigText(phonetic), textSizePx = sp(14f), color = Color.WHITE), ImageNode(source = BigImage(R.mipmap.ic_launcher), layoutWidth = LayoutDimension.Fixed(dp(10)), layoutHeight = LayoutDimension.Fixed(dp(24))))), startToStartOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT)
        )
    )

    fun buildTrySpeakChipFromImage(): LayoutNode = ConstraintNode(
        children = listOf(
            ConstraintChild(id = "outline", node = OutlineNode(backgroundColor = Color.WHITE, strokeColor = 0xFF19D96B.toInt(), strokeWidth = dp(2).toFloat(), cornerRadius = dp(24).toFloat(), dashWidth = dp(5).toFloat(), dashGap = dp(5).toFloat(), layoutWidth = LayoutDimension.MatchParent, layoutHeight = LayoutDimension.MatchParent), startToStartOf = "content", endToEndOf = "content", topToTopOf = "content", bottomToBottomOf = "content", width = LayoutDimension.MatchParent, height = LayoutDimension.MatchParent),
            ConstraintChild(id = "content", node = LinearNode(orientation = com.simple.ui.precompute.node.Orientation.HORIZONTAL, crossAlign = CrossAlign.CENTER, gap = dp(10), padding = EdgeInsets(left = dp(16), top = dp(10), right = dp(18), bottom = dp(10)), children = listOf(ImageNode(source = BigImage(R.drawable.ic_try_speak_mic), layoutWidth = LayoutDimension.Fixed(dp(22)), layoutHeight = LayoutDimension.Fixed(dp(22))), TextNode(text = BigText("Thử nói"), textSizePx = sp(20f), color = 0xFF19D96B.toInt(), maxLines = 1))), startToStartOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT)
        )
    )

    fun buildDashedLineTextFromImage(): LayoutNode = LinearNode(
        orientation = com.simple.ui.precompute.node.Orientation.VERTICAL,
        crossAlign = CrossAlign.CENTER,
        gap = dp(12),
        padding = EdgeInsets(top = dp(14)),
        layoutWidth = LayoutDimension.MatchParent,
        layoutHeight = LayoutDimension.Fixed(dp(86)),
        children = listOf(LineNode(color = 0xFFB8B8B8.toInt(), strokeWidth = 1.5f * dp, dashWidth = dp(6).toFloat(), dashGap = dp(6).toFloat(), layoutWidth = LayoutDimension.Fixed(dp(240)), layoutHeight = LayoutDimension.Fixed(dp(2))), TextNode(text = BigText("rất vui được quen biết bạn"), textSizePx = sp(20f), color = 0xFF202124.toInt(), maxLines = 1))
    )

    fun buildLoadingOutlineCard(): LayoutNode = ConstraintNode(
        layoutWidth = LayoutDimension.MatchParent,
        layoutHeight = LayoutDimension.Fixed(dp(92)),
        children = listOf(
            ConstraintChild(id = "outline", node = OutlineNode(layoutWidth = LayoutDimension.MatchParent, layoutHeight = LayoutDimension.MatchParent, backgroundColor = 0x11E91E63, strokeColor = 0xFFE91E63.toInt(), strokeWidth = dp(2).toFloat(), cornerRadius = dp(16).toFloat(), dashWidth = dp(10).toFloat(), dashGap = dp(6).toFloat(), loadingSegmentRatio = 0.35f, loadingDurationMs = 10000L, state = OutlineState.LOADING), startToStartOf = ConstraintNode.PARENT, endToEndOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT, bottomToBottomOf = ConstraintNode.PARENT, width = LayoutDimension.MatchParent, height = LayoutDimension.MatchParent),
            ConstraintChild(id = "content", node = LinearNode(orientation = com.simple.ui.precompute.node.Orientation.VERTICAL, gap = dp(4), padding = EdgeInsets.all(dp(16)), layoutWidth = LayoutDimension.MatchParent, children = listOf(TextNode(text = BigText("Outline đang xử lý"), textSizePx = sp(16f), color = Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 1), TextNode(text = BigText("OutlineNode chỉ vẽ effect; content nằm ở sibling khác."), textSizePx = sp(12f), color = Color.DKGRAY, maxLines = 2))), startToStartOf = ConstraintNode.PARENT, endToEndOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT, bottomToBottomOf = ConstraintNode.PARENT, width = LayoutDimension.MatchParent, verticalBias = 0.5f)
        )
    )

    fun buildProgressBarCard(title: String, progress: Int, max: Int, progressColor: Int): LayoutNode {
        val safeMax = max.coerceAtLeast(1)
        val safeProgress = progress.coerceIn(0, safeMax)
        return LinearNode(
            orientation = com.simple.ui.precompute.node.Orientation.VERTICAL,
            gap = dp(8),
            padding = EdgeInsets.all(dp(16)),
            layoutWidth = LayoutDimension.MatchParent,
            children = listOf(TextNode(text = BigText("$title  $safeProgress/$safeMax"), textSizePx = sp(14f), color = 0xFF202124.toInt(), typeface = Typeface.DEFAULT_BOLD, maxLines = 1), ProgressBarNode(progress = safeProgress, max = safeMax, trackColor = 0xFFE8EAED.toInt(), progressColor = progressColor, layoutWidth = LayoutDimension.MatchParent, layoutHeight = LayoutDimension.Fixed(dp(8))))
        )
    }

    fun buildScoreGaugeSpec(progress: Int, grade: String = "", label: String = "ĐIỂM", sizePx: Int, strokeWidthPx: Float = dp(1).toFloat()): LayoutNode = ConstraintNode(
        layoutWidth = LayoutDimension.Fixed(sizePx),
        layoutHeight = LayoutDimension.Fixed(sizePx),
        children = listOf(
            ConstraintChild(id = "arc", node = GaugeArcNode(progress = progress, progressColor = Color.BLACK, strokeWidthPx = strokeWidthPx), startToStartOf = ConstraintNode.PARENT, endToEndOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT, bottomToBottomOf = ConstraintNode.PARENT, width = LayoutDimension.MatchParent, height = LayoutDimension.MatchParent),
            ConstraintChild(id = "score", node = GaugeScoreNode(progress = progress, label = label, grade = grade, gradeColor = Color.BLACK), startToStartOf = ConstraintNode.PARENT, endToEndOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT, bottomToBottomOf = ConstraintNode.PARENT, width = LayoutDimension.MatchParent, height = LayoutDimension.MatchParent),
        )
    )

    private fun buildTagChip(label: String, color: Int): LayoutNode = ConstraintNode(
        children = listOf(
            ConstraintChild(id = "bg", node = OutlineNode(backgroundColor = color, strokeWidth = 0f, cornerRadius = dp(18).toFloat(), layoutWidth = LayoutDimension.MatchParent, layoutHeight = LayoutDimension.MatchParent), startToStartOf = "text", endToEndOf = "text", topToTopOf = "text", bottomToBottomOf = "text", width = LayoutDimension.MatchParent, height = LayoutDimension.MatchParent),
            ConstraintChild(id = "text", node = TextNode(text = BigText(label), textSizePx = sp(12f), color = Color.WHITE, typeface = Typeface.DEFAULT_BOLD, maxLines = 1, padding = EdgeInsets.symmetric(h = dp(10), v = dp(6))), startToStartOf = ConstraintNode.PARENT, topToTopOf = ConstraintNode.PARENT)
        )
    )

    private fun dp(value: Int): Int = (value * dp).toInt()

    private fun sp(value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, activity.resources.displayMetrics)
}
