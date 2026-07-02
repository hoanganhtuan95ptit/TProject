package com.simple.ui.precompute

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.simple.t.R
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.CircleCrop
import com.simple.ui.precompute.image.RoundedCorners
import com.simple.ui.precompute.image.addTransform
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.toBuilder
import com.simple.ui.precompute.loader.GlideImageLoader
import com.simple.ui.precompute.loader.ImageLoader
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.GaugeArcNode
import com.simple.ui.precompute.node.GaugeScoreNode
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LineNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val dp by lazy { resources.displayMetrics.density }
    private val cardWidth by lazy { resources.displayMetrics.widthPixels - (32 * dp).toInt() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<LinearLayout>(R.id.container)

        // Dữ liệu mẫu — dùng chung cho cả 2 demo
        val items = listOf(
            Triple("Hello World — a very long word to test text wrapping behavior", "/həˈloʊ wɜːrld/", "Xin chào thế giới"),
            Triple("Google nói Advertising ID là mã định danh do Google Play services cung cấp, người dùng có thể reset hoặc xóa. Khi bị xóa, app đọc ID có thể nhận chuỗi toàn số 0",   "/ˈændrɔɪd/",   "Hệ điều hành"),
            Triple("Kotlin",    "/ˈkɒtlɪn/",    "Ngôn ngữ lập trình"),
            Triple("Precompute","/priːkəmˈpjuːt/","Tính trước"),
        )

        val dp48 = (48 * dp).toInt()
        val iconSource = BigImage(R.mipmap.ic_launcher)

        ImageLoader.install(GlideImageLoader(this))

        lifecycleScope.launch {

            // ════════════════════════════════════════════════════════════════
            // DEMO 1 — LinearNode
            //   Layout: Row(icon | Column(title, ipa, meaning))
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "① LinearNode  —  Row / Column")

            val linearSpecs = withContext(Dispatchers.Default) {
                items.map { (word, ipa, meaning) ->
                    LayoutEngine.measure(
                        buildLinearCard(word, ipa, meaning, iconSource, dp48),
                        Constraints(cardWidth)
                    )
                }
            }
            addCards(container, linearSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 2 — ConstraintNode
            //   Layout:
            //     icon  ← anchored start-top to PARENT
            //     badge ← anchored end-top  to PARENT  ("EN" label)
            //     title ← startToEndOf(icon), endToStartOf(badge), MatchParent
            //     ipa   ← topToBottomOf(title), same horizontal span
            //     meaning ← topToBottomOf(ipa),  same horizontal span
            //
            //   Dependency graph (không có cycle):
            //     PARENT → icon, badge
            //     icon + badge → title
            //     title → ipa → meaning
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "② ConstraintNode  —  tương tự ConstraintLayout")

            val constraintSpecs = withContext(Dispatchers.Default) {
                items.map { (word, ipa, meaning) ->
                    LayoutEngine.measure(
                        buildConstraintCard(word, ipa, meaning, iconSource, dp48),
                        Constraints(cardWidth)
                    )
                }
            }
            addCards(container, constraintSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 3 — ConstraintNode: views leo nhau (view-to-view chaining)
            //
            //  Profile card layout:
            //  ┌─────────────────────────────────────────────────────┐
            //  │ [avatar]      [name]  (bold, lớn)                   │
            //  │               [tag]──►[role]   ← tag.end→role.start │
            //  │               [bio]             ← topToBottomOf(role)│
            //  │ [btn_like]──►[btn_share]        ← start2endOf chain  │
            //  └─────────────────────────────────────────────────────┘
            //
            //  Dependency graph:
            //   PARENT → avatar, name, btn_like
            //   avatar → tag (startToEndOf)
            //   name   → tag (topToBottomOf)
            //   tag    → role (startToEndOf)
            //   role   → bio (topToBottomOf)
            //   btn_like → btn_share (startToEndOf)
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "③ ConstraintNode  —  view con leo nhau (view-to-view)")

            val profiles = listOf(
                Triple("Alice Nguyen",   "#Android",  "Senior Engineer  ·  Google"),
                Triple("Bob Tran",       "#Kotlin",   "Staff Engineer  ·  JetBrains"),
                Triple("Carol Le",       "#Compose",  "UI Engineer  ·  Meta"),
            )

            val profileSpecs = withContext(Dispatchers.Default) {
                profiles.map { (name, tag, role) ->
                    LayoutEngine.measure(
                        buildProfileConstraintCard(name, tag, role, iconSource, dp48),
                        Constraints(cardWidth)
                    )
                }
            }
            addCards(container, profileSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 4 — ConstraintNode: WrapContent mặc định
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "④ ConstraintNode  —  WrapContent (vừa khít nội dung)")

            val wrapContentSpecs = withContext(Dispatchers.Default) {
                listOf(
                    LayoutEngine.measure(buildWrapContentTagsCard(), Constraints(cardWidth)),
                    LayoutEngine.measure(buildWrapContentCenterCard(), Constraints(cardWidth))
                )
            }
            addCards(container, wrapContentSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 5 — OutlineNode: dashed rounded loading outline
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "⑤ OutlineNode  —  viền loading bo góc")

            val outlineSpecs = withContext(Dispatchers.Default) {
                listOf(
                    LayoutEngine.measure(buildLoadingOutlineCard(), Constraints(cardWidth))
                )
            }
            addCards(container, outlineSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 6 — ImageNode + BigTransform (CircleCrop / RoundedCorners)
            //   Cùng 1 nguồn ảnh, 3 cách biến đổi khác nhau xếp ngang.
            //   Engine không biết Glide — transforms được resolve ở loader.
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "⑥ ImageNode  —  Glide transform (Circle / Rounded)")

            val transformSpecs = withContext(Dispatchers.Default) {
                listOf(
                    LayoutEngine.measure(buildTransformCard(dp48), Constraints(cardWidth))
                )
            }
            addCards(container, transformSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 7 — PhoneticChip
            //   Chuyển đổi từ XML:
            //     ConstraintLayout (wrap_content)
            //       └─ View background #55BB55 (fill toàn bộ)
            //       └─ LinearLayout horizontal (padding 8dp/4dp)
            //            ├─ TextView  (phonetic text)
            //            └─ ImageView (10×24dp, marginStart 4dp)
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "⑦ PhoneticChip  —  từ XML → Node")

            val chipSpecs = withContext(Dispatchers.Default) {
                items.map { (text, ipa, _) ->
                    LayoutEngine.measure(buildPhoneticChip("$text\n$ipa"), Constraints(cardWidth))
                }
            }
            addCards(container, chipSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 8 — ScoreGauge
            //   GaugeArcNode + GaugeScoreNode overlay trong cùng 1 ConstraintNode.
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "⑧ ScoreGauge  —  GaugeArcNode + GaugeScoreNode")

            val scoreGaugeSpecs = withContext(Dispatchers.Default) {
                items.map {
                    LayoutEngine.measure(buildScoreGaugeSpec(progress = 90, sizePx = dp(160)), Constraints(cardWidth))
                }
            }
            addCards(container, scoreGaugeSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 9 — ProgressBarNode
            //   Horizontal progress bar: track + fill đều là rounded-rect.
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "⑨ ProgressBarNode  —  thanh tiến độ ngang")

            val progressBarSpecs = withContext(Dispatchers.Default) {
                listOf(
                    LayoutEngine.measure(
                        buildProgressBarCard(
                            title = "Listening accuracy",
                            progress = 68,
                            max = 100,
                            progressColor = 0xFF1B998B.toInt()
                        ),
                        Constraints(cardWidth)
                    ),
                    LayoutEngine.measure(
                        buildProgressBarCard(
                            title = "Speaking fluency",
                            progress = 42,
                            max = 100,
                            progressColor = 0xFFE76F51.toInt()
                        ),
                        Constraints(cardWidth)
                    ),
                    LayoutEngine.measure(
                        buildProgressBarCard(
                            title = "Daily goal",
                            progress = 9,
                            max = 12,
                            progressColor = 0xFF5B7CFA.toInt()
                        ),
                        Constraints(cardWidth)
                    )
                )
            }
            addCards(container, progressBarSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 10 — XML NoteRow: LinearLayout → Node
            //   Chuyển đổi từ XML người dùng đưa:
            //     LinearLayout horizontal match_parent / wrap_content
            //       ├─ ImageView 28×28dp
            //       └─ LinearLayout vertical match_parent + marginStart 16dp
            //            ├─ TextView title
            //            └─ TextView note + marginTop 8dp
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "⑩ XML NoteRow  —  LinearLayout → Node")

            val notes = listOf(
                "Meet" to "Meet",
                "Design sync" to "Review node spacing from the XML version",
                "Long title wraps inside the remaining width" to
                    "The text column uses MatchParent after icon width and marginStart are reserved."
            )

            val noteSpecs = withContext(Dispatchers.Default) {
                notes.map { (title, note) ->
                    LayoutEngine.measure(
                        buildNoteRowFromXml(title, note, iconSource),
                        Constraints(cardWidth)
                    )
                }
            }
            addCards(container, noteSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 11 — TrySpeak chip từ ảnh mẫu
            //   Không thêm node mới:
            //     ConstraintNode  → stack outline dưới content
            //     OutlineNode     → viền xanh nét đứt bo tròn
            //     LinearNode      → icon + text nằm ngang
            //     ImageNode       → icon micro
            //     TextNode        → "Thử nói"
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "⑪ TrySpeakChip  —  ảnh mẫu → node cũ")

            val trySpeakSpecs = withContext(Dispatchers.Default) {
                listOf(
                    LayoutEngine.measure(buildTrySpeakChipFromImage(), Constraints(cardWidth))
                )
            }
            addCards(container, trySpeakSpecs)

            // ════════════════════════════════════════════════════════════════
            // DEMO 12 — DashedLineText từ ảnh mẫu
            //   LineNode  → đường gạch ngang nét đứt
            //   TextNode  → text căn giữa phía dưới line
            // ════════════════════════════════════════════════════════════════
            addSectionLabel(container, "⑫ DashedLineText  —  LineNode + TextNode")

            val dashedLineTextSpecs = withContext(Dispatchers.Default) {
                listOf(
                    LayoutEngine.measure(buildDashedLineTextFromImage(), Constraints(cardWidth))
                )
            }
            addCards(container, dashedLineTextSpecs)

            // Footer
            container.addView(TextView(this@MainActivity).apply {
                text = "${items.size * 4 + profiles.size + 4 + progressBarSpecs.size + noteSpecs.size + trySpeakSpecs.size + dashedLineTextSpecs.size} cards — LinearNode + ConstraintNode + OutlineNode + ImageTransform + PhoneticChip + ScoreGauge + ProgressBarNode + XML NoteRow + TrySpeakChip + LineNode, measured on bg thread"
                setTextColor(Color.GRAY)
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, dp(24))
            })
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun addSectionLabel(container: LinearLayout, label: String) {
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(20) }

        container.addView(TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setBackgroundColor(0xFF6200EE.toInt())
        }, lp)
    }

    private fun addCards(container: LinearLayout, specs: List<DrawSpec>) {
        specs.forEachIndexed { index, spec ->
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = if (index == 0) dp(10) else dp(10) }

            container.addView(PrecomputedView(this@MainActivity).apply {
                this.spec = spec
                setBackgroundResource(R.drawable.card_background)
                elevation = 2 * dp
            }, lp)
        }
    }

    // ── Card builders ─────────────────────────────────────────────────────────

    /**
     * **LinearNode** card: icon bên trái, cột text bên phải.
     *
     * ```
     * ┌──────────────────────────────────┐
     * │ [icon]  title (bold)             │
     * │         /ipa/                    │
     * │         meaning                  │
     * └──────────────────────────────────┘
     * ```
     */
    private fun buildLinearCard(
        word: String,
        ipa: String,
        meaning: String,
        iconSource: BigImage,
        iconSizePx: Int,
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
            ),
            LinearNode(
                orientation = Orientation.VERTICAL,
                gap = dp(4),
                children = listOf(
                    TextNode(BigText(word), sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 2),
                    TextNode(BigText(ipa), sp(14f), 0xFF6200EE.toInt(), maxLines = 1),
                    TextNode(BigText(meaning), sp(12f), Color.GRAY, maxLines = 2),
                )
            )
        )
    )

    /**
     * **XML NoteRow** — chuyển đổi trực tiếp từ LinearLayout trong XML:
     *
     * - root: horizontal, `match_parent` × `wrap_content`, padding 16dp/8dp
     * - icon: 28dp × 28dp
     * - text column: `match_parent`, marginStart 16dp
     * - note text: marginTop 8dp
     */
    private fun buildNoteRowFromXml(
        title: String,
        note: String,
        iconSource: BigImage,
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
            ),
            SpaceNode.horizontal(dp(16)),
            LinearNode(
                orientation = Orientation.VERTICAL,
                layoutWidth = LayoutDimension.MatchParent,
                children = listOf(
                    TextNode(
                        text = BigText(title),
                        textSizePx = sp(14f),
                        color = 0xFF202124.toInt(),
                        layoutWidth = LayoutDimension.MatchParent
                    ),
                    SpaceNode.vertical(dp(8)),
                    TextNode(
                        text = BigText(note),
                        textSizePx = sp(14f),
                        color = 0xFF5F6368.toInt(),
                        layoutWidth = LayoutDimension.MatchParent
                    )
                )
            )
        )
    )

    /**
     * **ConstraintNode** card: icon + badge nằm cạnh nhau trên cùng,
     * title / ipa / meaning tự căn theo constraint.
     *
     * ```
     * ┌──────────────────────────────────┐
     * │ [icon]  title (MatchParent) [EN]│
     * │         /ipa/                   │
     * │         meaning                 │
     * └──────────────────────────────────┘
     * ```
     *
     * Thứ tự resolve (không có cycle):
     *   1. icon, badge  → phụ thuộc PARENT (resolve ngay vòng đầu)
     *   2. title        → phụ thuộc icon + badge
     *   3. ipa          → phụ thuộc title
     *   4. meaning      → phụ thuộc ipa
     */
    private fun buildConstraintCard(
        word: String,
        ipa: String,
        meaning: String,
        iconSource: BigImage,
        iconSizePx: Int,
    ): LayoutNode = ConstraintNode(
        padding = EdgeInsets.all(dp(12)),
        children = listOf(

            // ① Icon — anchor start-top vào PARENT
            ConstraintChild(
                id = "icon",
                node = ImageNode(
                    source = iconSource,
                    layoutWidth = LayoutDimension.Fixed(iconSizePx),
                    layoutHeight = LayoutDimension.Fixed(iconSizePx)
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            ),

            // ② Badge "EN" — anchor end-top vào PARENT
            ConstraintChild(
                id = "badge",
                node = TextNode(
                    text = BigText("EN"),
                    textSizePx = sp(10f),
                    color = 0xFF6200EE.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(6), v = dp(3)),
                ),
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            ),

            // ③ Title — nằm giữa icon (start) và badge (end), fill hết chiều rộng
            ConstraintChild(
                id = "title",
                node = TextNode(BigText(word), sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 2),
                startToEndOf = "icon", marginStart = dp(12),
                endToStartOf = "badge", marginEnd = dp(8),
                topToTopOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent,
            ),

            // ④ IPA — ngay bên dưới title, căn trái/phải theo title
            ConstraintChild(
                id = "ipa",
                node = TextNode(BigText(ipa), sp(14f), 0xFF6200EE.toInt(), maxLines = 1),
                startToStartOf = "title",
                endToEndOf = "title",
                topToBottomOf = "title", marginTop = dp(4),
                width = LayoutDimension.MatchParent,
            ),

            // ⑤ Meaning — ngay bên dưới ipa, căn trái/phải theo ipa
            ConstraintChild(
                id = "meaning",
                node = TextNode(BigText(meaning), sp(12f), Color.GRAY, maxLines = 2),
                startToStartOf = "ipa",
                endToEndOf = "ipa",
                topToBottomOf = "ipa", marginTop = dp(4),
                width = LayoutDimension.MatchParent,
            ),
        )
    )

    /**
     * **ConstraintNode** profile card — minh họa view con leo nhau theo cả 2 chiều.
     *
     * ```
     * ┌─────────────────────────────────────────────────────┐
     * │ [avatar]    [name]          (bold, sp16)            │
     * │             [tag]──►[role]  tag.end → role.start    │
     * │             [bio]           topToBottomOf(role)      │
     * │ [btn_like]──►[btn_share]   btn_share.start = btn_like.end │
     * └─────────────────────────────────────────────────────┘
     * ```
     *
     * Thứ tự resolve:
     *   Pass 1: avatar (PARENT), name (PARENT), btn_like (PARENT)
     *   Pass 2: tag    (avatar + name), btn_share (btn_like)
     *   Pass 3: role   (tag)
     *   Pass 4: bio    (role)
     */
    private fun buildProfileConstraintCard(
        name: String,
        tag: String,
        role: String,
        avatarSource: BigImage,
        avatarSizePx: Int,
    ): LayoutNode = ConstraintNode(
        padding = EdgeInsets(left = dp(12), top = dp(12), right = dp(12), bottom = dp(12)),
        children = listOf(

            // ① Avatar — góc trên-trái của PARENT
            ConstraintChild(
                id = "avatar",
                node = ImageNode(
                    source = avatarSource,
                    layoutWidth = LayoutDimension.Fixed(avatarSizePx),
                    layoutHeight = LayoutDimension.Fixed(avatarSizePx)
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            ),

            // ② Name — start leo vào END của avatar, top leo vào TOP của PARENT
            ConstraintChild(
                id = "name",
                node = TextNode(BigText(name), sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 1),
                startToEndOf = "avatar", marginStart = dp(12),
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent,
            ),

            // ③ Tag badge — start leo vào END của avatar, top leo vào BOTTOM của name
            //   → phụ thuộc cả avatar lẫn name: resolve ở pass 2
            ConstraintChild(
                id = "tag",
                node = TextNode(
                    text = BigText(tag),
                    textSizePx = sp(11f),
                    color = 0xFFFFFFFF.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(6), v = dp(3)),
                ),
                startToEndOf = "avatar", marginStart = dp(12),
                topToBottomOf = "name", marginTop = dp(4),
            ),

            // ④ Role — start leo vào END của tag (chaining ngang)
            //   → phụ thuộc tag: resolve ở pass 3
            ConstraintChild(
                id = "role",
                node = TextNode(BigText(role), sp(11f), Color.DKGRAY, maxLines = 1),
                startToEndOf = "tag", marginStart = dp(6),
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = "tag",
                width = LayoutDimension.MatchParent,
            ),

            // ⑤ Bio (dòng sub-text) — top leo vào BOTTOM của role (chaining dọc)
            //   → phụ thuộc role: resolve ở pass 4
            ConstraintChild(
                id = "bio",
                node = TextNode(BigText("Tapped: view ③ leo ④ (ngang) → ⑤ leo ④ (dọc)"), sp(10f), 0xFF9E9E9E.toInt(), maxLines = 2),
                startToEndOf = "avatar", marginStart = dp(12),
                endToEndOf = ConstraintNode.PARENT,
                topToBottomOf = "role", marginTop = dp(4),
                width = LayoutDimension.MatchParent,
            ),

            // ⑥ Button Like — anchor start-bottom vào PARENT
            ConstraintChild(
                id = "btn_like",
                node = TextNode(
                    text = BigText("♥  Like"),
                    textSizePx = sp(12f),
                    color = 0xFFFFFFFF.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(14), v = dp(7)),
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToBottomOf = "bio", marginTop = dp(10),
            ),

            // ⑦ Button Share — start leo vào END của btn_like (chaining ngang)
            //   → phụ thuộc btn_like: resolve sau btn_like
            ConstraintChild(
                id = "btn_share",
                node = TextNode(
                    text = BigText("↗  Share"),
                    textSizePx = sp(12f),
                    color = 0xFF6200EE.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    padding = EdgeInsets.symmetric(h = dp(14), v = dp(7)),
                ),
                startToEndOf = "btn_like", marginStart = dp(8),
                topToTopOf = "btn_like",
            ),
        )
    )

    // ── Dimension helpers ─────────────────────────────────────────────────────

    private fun dp(value: Int): Int = (value * dp).toInt()

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    // ── WrapContent Demo builders ─────────────────────────────────────────────

    /**
     * **ConstraintNode** WrapContent: Các thẻ xếp hàng (chaining ngang) vừa khít nội dung.
     */
    private fun buildWrapContentTagsCard(): LayoutNode = ConstraintNode(
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
                topToTopOf = ConstraintNode.PARENT,
            ),
            ConstraintChild(
                id = "tag_2",
                node = TextNode(BigText("Android"), sp(14f), 0xFF4CAF50.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(12), v = dp(6))),
                startToEndOf = "tag_1", marginStart = dp(8),
                topToTopOf = "tag_1",
            ),
            ConstraintChild(
                id = "tag_3",
                node = TextNode(BigText("WrapContent"), sp(14f), 0xFF2196F3.toInt(), typeface = Typeface.DEFAULT_BOLD, padding = EdgeInsets.symmetric(h = dp(12), v = dp(6))),
                startToEndOf = "tag_2", marginStart = dp(8),
                topToTopOf = "tag_2",
            )
        )
    )

    /**
     * **ConstraintNode** WrapContent: Nút nằm chính giữa màn hình.
     */
    private fun buildWrapContentCenterCard(): LayoutNode = ConstraintNode(
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
     * **ImageNode + BigTransform** card.
     *
     * Cùng `R.mipmap.ic_launcher` nhưng 3 cách biến đổi khác nhau, xếp ngang:
     *  ① không transform → ảnh nguyên dạng.
     *  ② [CircleCrop]    → crop tròn (avatar).
     *  ③ [RoundedCorners]→ bo góc theo bán kính.
     *
     * Engine vẫn đo từng [ImageNode] với `Fixed(iconSizePx)` ở background.
     * Lúc attach view, `GlideImageLoader` đọc `BigImage.transforms` rồi
     * dịch sang `Transformation<Bitmap>` của Glide qua `BigTransformConverters`.
     */
    private fun buildTransformCard(iconSizePx: Int): LayoutNode {
        val baseSource = R.mipmap.ic_launcher
        val rounded = dp(12)

        fun item(label: String, image: BigImage) = LinearNode(
            orientation = Orientation.VERTICAL,
            crossAlign = CrossAlign.CENTER,
            gap = dp(6),
            children = listOf(
                ImageNode(
                    source = image,
                    layoutWidth = LayoutDimension.Fixed(iconSizePx),
                    layoutHeight = LayoutDimension.Fixed(iconSizePx),
                ),
                TextNode(
                    text = BigText(label),
                    textSizePx = sp(11f),
                    color = Color.DKGRAY,
                    typeface = Typeface.DEFAULT_BOLD,
                    maxLines = 1,
                ),
            )
        )

        return LinearNode(
            orientation = Orientation.HORIZONTAL,
            crossAlign = CrossAlign.CENTER,
            gap = dp(20),
            padding = EdgeInsets.all(dp(16)),
            children = listOf(
                item(
                    label = "Original",
                    image = BigImage(source = baseSource),
                ),
                item(
                    label = "CircleCrop",
                    image = R.drawable.image.toBuilder()
                        .addTransform(CircleCrop)
                        .build(),
                ),
                item(
                    label = "Rounded ${rounded}px",
                    image = R.drawable.img1.toBuilder()
                        .addTransform(RoundedCorners(rounded))
                        .build(),
                ),
            )
        )
    }

    /**
     * **PhoneticChip** — chuyển đổi từ XML ConstraintLayout.
     *
     * ```
     * ┌──────────────────────┐
     * │ /həˈloʊ/  [🔊]      │  ← nền xanh #55BB55
     * └──────────────────────┘
     * ```
     *
     * Cấu trúc node:
     * - [ConstraintNode] wrap_content (root)
     *   - "bg"      → [OutlineNode] fill toàn bộ, màu #55BB55, không có stroke
     *   - "content" → [LinearNode] horizontal + gap 4dp + padding 8/4dp
     *                   ├─ [TextNode]  phonetic text
     *                   └─ [ImageNode] icon 10×24dp
     */
    private fun buildPhoneticChip(phonetic: String): LayoutNode = ConstraintNode(
        children = listOf(
            ConstraintChild(
                id = "bg",
                node = OutlineNode(
                    backgroundColor = Color.parseColor("#55BB55"),
                    strokeWidth = 0f,
                    layoutWidth = LayoutDimension.MatchParent,
                    layoutHeight = LayoutDimension.MatchParent,
                ),
                startToStartOf = "content",
                endToEndOf = "content",
                topToTopOf = "content",
                bottomToBottomOf = "content",
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent,
            ),
            ConstraintChild(
                id = "content",
                node = LinearNode(
                    orientation = Orientation.HORIZONTAL,
                    crossAlign = CrossAlign.CENTER,
                    gap = dp(4), // marginStart của ImageView
                    padding = EdgeInsets.symmetric(h = dp(8), v = dp(4)),
                    children = listOf(
                        TextNode(
                            text = BigText(phonetic),
                            textSizePx = sp(14f),
                            color = Color.WHITE,
                        ),
                        // ic_volume_24dp chưa có trong project → dùng ic_launcher làm placeholder
                        ImageNode(
                            source = BigImage(R.mipmap.ic_launcher),
                            layoutWidth = LayoutDimension.Fixed(dp(10)),
                            layoutHeight = LayoutDimension.Fixed(dp(24)),
                        ),
                    ),
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            ),
        )
    )

    /**
     * **TrySpeakChip** — dựng lại giao diện trong ảnh bằng các node cũ.
     *
     * ```
     * ┌ · · · · · · · · ┐
     * │  (mic)  Thử nói  │
     * └ · · · · · · · · ┘
     * ```
     */
    private fun buildTrySpeakChipFromImage(): LayoutNode {

        val green = 0xFF19D96B.toInt()

        return ConstraintNode(
            children = listOf(
                ConstraintChild(
                    id = "outline",
                    node = OutlineNode(
                        backgroundColor = Color.WHITE,
                        strokeColor = green,
                        strokeWidth = dp(2).toFloat(),
                        cornerRadius = dp(24).toFloat(),
                        dashWidth = dp(5).toFloat(),
                        dashGap = dp(5).toFloat(),
                        layoutWidth = LayoutDimension.MatchParent,
                        layoutHeight = LayoutDimension.MatchParent,
                    ),
                    startToStartOf = "content",
                    endToEndOf = "content",
                    topToTopOf = "content",
                    bottomToBottomOf = "content",
                    width = LayoutDimension.MatchParent,
                    height = LayoutDimension.MatchParent,
                ),
                ConstraintChild(
                    id = "content",
                    node = LinearNode(
                        orientation = Orientation.HORIZONTAL,
                        crossAlign = CrossAlign.CENTER,
                        gap = dp(10),
                        padding = EdgeInsets(left = dp(16), top = dp(10), right = dp(18), bottom = dp(10)),
                        children = listOf(
                            ImageNode(
                                source = BigImage(R.drawable.ic_try_speak_mic),
                                layoutWidth = LayoutDimension.Fixed(dp(22)),
                                layoutHeight = LayoutDimension.Fixed(dp(22)),
                            ),
                            TextNode(
                                text = BigText("Thử nói"),
                                textSizePx = sp(20f),
                                color = green,
                                maxLines = 1,
                            ),
                        )
                    ),
                    startToStartOf = ConstraintNode.PARENT,
                    topToTopOf = ConstraintNode.PARENT,
                )
            )
        )
    }

    /**
     * **DashedLineText** — dựng lại ảnh mẫu bằng LineNode + TextNode.
     */
    private fun buildDashedLineTextFromImage(): LayoutNode = LinearNode(
        orientation = Orientation.VERTICAL,
        crossAlign = CrossAlign.CENTER,
        gap = dp(12),
        padding = EdgeInsets(top = dp(14)),
        layoutWidth = LayoutDimension.MatchParent,
        layoutHeight = LayoutDimension.Fixed(dp(86)),
        children = listOf(
            LineNode(
                color = 0xFFB8B8B8.toInt(),
                strokeWidth = 1.5f * dp,
                dashWidth = dp(6).toFloat(),
                dashGap = dp(6).toFloat(),
                layoutWidth = LayoutDimension.Fixed(dp(240)),
                layoutHeight = LayoutDimension.Fixed(dp(2)),
            ),
            TextNode(
                text = BigText("rất vui được quen biết bạn"),
                textSizePx = sp(20f),
                color = 0xFF202124.toInt(),
                maxLines = 1,
            ),
        )
    )

    /**
     * **OutlineNode** loading card — effect là sibling phủ lên content.
     */
    private fun buildLoadingOutlineCard(): LayoutNode = ConstraintNode(
        layoutWidth = LayoutDimension.MatchParent,
        layoutHeight = LayoutDimension.Fixed(dp(92)),
        children = listOf(
            ConstraintChild(
                id = "outline",
                node = OutlineNode(
                    layoutWidth = LayoutDimension.MatchParent,
                    layoutHeight = LayoutDimension.MatchParent,
                    backgroundColor = 0x11E91E63,
                    strokeColor = 0xFFE91E63.toInt(),
                    strokeWidth = dp(2).toFloat(),
                    cornerRadius = dp(16).toFloat(),
                    dashWidth = dp(10).toFloat(),
                    dashGap = dp(6).toFloat(),
                    loadingSegmentRatio = 0.35f,
                    loadingDurationMs = 10000L,
                    state = OutlineState.LOADING
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
                        TextNode(
                            text = BigText("Outline đang xử lý"),
                            textSizePx = sp(16f),
                            color = Color.BLACK,
                            typeface = Typeface.DEFAULT_BOLD,
                            maxLines = 1
                        ),
                        TextNode(
                            text = BigText("OutlineNode chỉ vẽ effect; content nằm ở sibling khác."),
                            textSizePx = sp(12f),
                            color = Color.DKGRAY,
                            maxLines = 2
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

    /**
     * **ProgressBarNode** card — bar ngang có chiều cao cố định, rộng theo card.
     */
    private fun buildProgressBarCard(
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
                TextNode(
                    text = BigText("$title  $safeProgress/$safeMax"),
                    textSizePx = sp(14f),
                    color = 0xFF202124.toInt(),
                    typeface = Typeface.DEFAULT_BOLD,
                    maxLines = 1
                ),
                ProgressBarNode(
                    progress = safeProgress,
                    max = safeMax,
                    trackColor = 0xFFE8EAED.toInt(),
                    progressColor = progressColor,
                    layoutWidth = LayoutDimension.MatchParent,
                    layoutHeight = LayoutDimension.Fixed(dp(8))
                )
            )
        )
    }

    /**
     * Build DrawSpec cho ScoreGauge. Gọi off-main được (không đụng Context).
     *
     * @param sizePx cạnh vuông của gauge, vd 160.dp.toPx()
     */
    fun buildScoreGaugeSpec(progress: Int, grade: String = "", label: String = "ĐIỂM", sizePx: Int, strokeWidthPx: Float = dp(1).toFloat()): LayoutNode = ConstraintNode(
        layoutWidth = LayoutDimension.Fixed(sizePx),
        layoutHeight = LayoutDimension.Fixed(sizePx),
        children = listOf(
            // Vòng cung — fill toàn bộ frame.
            ConstraintChild(
                id = "arc",
                node = GaugeArcNode(
                    progress = progress,
                    progressColor = Color.BLACK,
                    strokeWidthPx = strokeWidthPx,
                ),
                startToStartOf = ConstraintNode.PARENT,
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
                bottomToBottomOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent,
            ),
            // Text trung tâm — overlay lên cùng frame.
            ConstraintChild(
                id = "score",
                node = GaugeScoreNode(
                    progress = progress,
                    label = label,
                    grade = grade,
                    gradeColor = Color.BLACK,
                ),
                startToStartOf = ConstraintNode.PARENT,
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
                bottomToBottomOf = ConstraintNode.PARENT,
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent,
            ),
        )
    )
}
