package com.simple.ui.precompute

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.simple.t.R
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.loader.GlideImageLoader
import com.simple.ui.precompute.loader.ImageLoader
import com.simple.ui.precompute.node.Constraints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrecomputeDemoScreen(
    private val activity: AppCompatActivity,
) {

    private val dp by lazy { activity.resources.displayMetrics.density }
    private val cardWidth by lazy { activity.resources.displayMetrics.widthPixels - (32 * dp).toInt() }

    fun render() {
        val container = activity.findViewById<LinearLayout>(R.id.container)
        val items = PrecomputeDemoData.items
        val profiles = PrecomputeDemoData.profiles
        val notes = PrecomputeDemoData.notes
        val iconSource = BigImage(R.mipmap.ic_launcher)
        val dp48 = (48 * dp).toInt()

        ImageLoader.install(GlideImageLoader(activity))

        activity.lifecycleScope.launch {
            PrecomputeUiSectionRenderer(
                activity = activity,
                container = container,
                cardWidth = cardWidth,
                iconSource = iconSource,
                iconSizePx = dp48,
                items = items,
                profiles = profiles,
                notes = notes,
            ).render()

            container.addView(TextView(activity).apply {
                text = "${items.size * 4 + profiles.size + 4 + notes.size + 6} cards — LinearNode + ConstraintNode + OutlineNode + ImageTransform + PhoneticChip + ScoreGauge + ProgressBarNode + XML NoteRow + TrySpeakChip + LineNode + FlexboxNode, measured on bg thread"
                setTextColor(Color.GRAY)
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, (16 * dp).toInt(), 0, (24 * dp).toInt())
            })
        }
    }
}

private class PrecomputeUiSectionRenderer(
    private val activity: AppCompatActivity,
    private val container: LinearLayout,
    private val cardWidth: Int,
    private val iconSource: BigImage,
    private val iconSizePx: Int,
    private val items: List<PrecomputeDemoData.WordItem>,
    private val profiles: List<PrecomputeDemoData.ProfileItem>,
    private val notes: List<PrecomputeDemoData.NoteItem>,
) {

    private val builders = PrecomputeCardBuilders(activity)

    suspend fun render() {
        val helper = PrecomputeUiHelpers(activity)

        helper.addSectionLabel(container, "① LinearNode  —  Row / Column")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                items.map { item ->
                    LayoutEngine.measure(
                        builders.buildLinearCard(item.word, item.ipa, item.meaning, iconSource, iconSizePx),
                        Constraints(cardWidth)
                    )
                }
            }
        )

        helper.addSectionLabel(container, "② ConstraintNode  —  tương tự ConstraintLayout")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                items.map { item ->
                    LayoutEngine.measure(
                        builders.buildConstraintCard(item.word, item.ipa, item.meaning, iconSource, iconSizePx),
                        Constraints(cardWidth)
                    )
                }
            }
        )

        helper.addSectionLabel(container, "③ ConstraintNode  —  view con leo nhau (view-to-view)")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                profiles.map { profile ->
                    LayoutEngine.measure(
                        builders.buildProfileConstraintCard(profile.name, profile.tag, profile.role, iconSource, iconSizePx),
                        Constraints(cardWidth)
                    )
                }
            }
        )

        helper.addSectionLabel(container, "④ ConstraintNode  —  WrapContent (vừa khít nội dung)")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                listOf(
                    LayoutEngine.measure(builders.buildWrapContentTagsCard(), Constraints(cardWidth)),
                    LayoutEngine.measure(builders.buildWrapContentCenterCard(), Constraints(cardWidth))
                )
            }
        )

        helper.addSectionLabel(container, "⑤ OutlineNode  —  viền loading bo góc")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                listOf(LayoutEngine.measure(builders.buildLoadingOutlineCard(), Constraints(cardWidth)))
            }
        )

        helper.addSectionLabel(container, "⑥ ImageNode  —  Glide transform (Circle / Rounded)")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                listOf(LayoutEngine.measure(builders.buildTransformCard(iconSizePx), Constraints(cardWidth)))
            }
        )

        helper.addSectionLabel(container, "⑦ PhoneticChip  —  từ XML → Node")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                items.map { item ->
                    LayoutEngine.measure(builders.buildPhoneticChip("${item.word}\n${item.ipa}"), Constraints(cardWidth))
                }
            }
        )

        helper.addSectionLabel(container, "⑧ ScoreGauge  —  GaugeArcNode + GaugeScoreNode")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                items.map {
                    LayoutEngine.measure(builders.buildScoreGaugeSpec(progress = 90, sizePx = helper.dp(160)), Constraints(cardWidth))
                }
            }
        )

        helper.addSectionLabel(container, "⑨ ProgressBarNode  —  thanh tiến độ ngang")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                listOf(
                    LayoutEngine.measure(builders.buildProgressBarCard("Listening accuracy", 68, 100, 0xFF1B998B.toInt()), Constraints(cardWidth)),
                    LayoutEngine.measure(builders.buildProgressBarCard("Speaking fluency", 42, 100, 0xFFE76F51.toInt()), Constraints(cardWidth)),
                    LayoutEngine.measure(builders.buildProgressBarCard("Daily goal", 9, 12, 0xFF5B7CFA.toInt()), Constraints(cardWidth))
                )
            }
        )

        helper.addSectionLabel(container, "⑩ XML NoteRow  —  LinearLayout → Node")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                notes.map { note ->
                    LayoutEngine.measure(builders.buildNoteRowFromXml(note.title, note.note, iconSource), Constraints(cardWidth))
                }
            }
        )

        helper.addSectionLabel(container, "⑪ TrySpeakChip  —  ảnh mẫu → node cũ")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                listOf(LayoutEngine.measure(builders.buildTrySpeakChipFromImage(), Constraints(cardWidth)))
            }
        )

        helper.addSectionLabel(container, "⑫ DashedLineText  —  LineNode + TextNode")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                listOf(LayoutEngine.measure(builders.buildDashedLineTextFromImage(), Constraints(cardWidth)))
            }
        )

        helper.addSectionLabel(container, "⑬ FlexboxNode  —  wrap tags giống FlexboxLayout")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                listOf(LayoutEngine.measure(builders.buildFlexboxTagsCard(), Constraints(cardWidth)))
            }
        )

        helper.addSectionLabel(container, "⑭ Custom FlexboxNode  —  đổi màu node id aaa mỗi 5s")
        helper.addCards(
            container,
            withContext(Dispatchers.Default) {
                listOf(LayoutEngine.measure(builders.buildColorChangingFlexboxCard(), Constraints(cardWidth)))
            }
        )
    }
}
