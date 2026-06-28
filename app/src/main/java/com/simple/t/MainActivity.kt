package com.simple.phonetics.ui.precompute

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
        val iconSource = ImageSource.ResSource(R.mipmap.ic_launcher)

        BitmapLoader.install(GlideBitmapLoader(this))

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
            //     title ← startToEndOf(icon), endToStartOf(badge), MatchConstraint
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

            // Footer
            container.addView(TextView(this@MainActivity).apply {
                text = "${items.size * 2 + profiles.size} cards — LinearNode + ConstraintNode, measured on bg thread"
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
        iconSource: ImageSource,
        iconSizePx: Int,
    ): LayoutNode = LinearNode(
        orientation = Orientation.HORIZONTAL,
        crossAlign = CrossAlign.CENTER,
        gap = dp(12),
        padding = EdgeInsets.all(dp(12)),
        children = listOf(
            ImageNode(source = iconSource, width = iconSizePx, height = iconSizePx),
            LinearNode(
                orientation = Orientation.VERTICAL,
                gap = dp(4),
                children = listOf(
                    TextNode(word,    sp(16f), Color.BLACK,         typeface = Typeface.DEFAULT_BOLD, maxLines = 2),
                    TextNode(ipa,     sp(14f), 0xFF6200EE.toInt(),  maxLines = 1),
                    TextNode(meaning, sp(12f), Color.GRAY,          maxLines = 2),
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
     * │ [icon]  title (MatchConstraint) [EN]│
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
        iconSource: ImageSource,
        iconSizePx: Int,
    ): LayoutNode = ConstraintNode(
        padding = EdgeInsets.all(dp(12)),
        children = listOf(

            // ① Icon — anchor start-top vào PARENT
            ConstraintChild(
                id = "icon",
                node = ImageNode(source = iconSource, width = iconSizePx, height = iconSizePx),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf     = ConstraintNode.PARENT,
            ),

            // ② Badge "EN" — anchor end-top vào PARENT
            ConstraintChild(
                id = "badge",
                node = TextNode(
                    text       = "EN",
                    textSizePx = sp(10f),
                    color      = 0xFF6200EE.toInt(),
                    typeface   = Typeface.DEFAULT_BOLD,
                    padding    = EdgeInsets.symmetric(h = dp(6), v = dp(3)),
                ),
                endToEndOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            ),

            // ③ Title — nằm giữa icon (start) và badge (end), fill hết chiều rộng
            ConstraintChild(
                id   = "title",
                node = TextNode(word, sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 2),
                startToEndOf = "icon",  marginStart = dp(12),
                endToStartOf = "badge", marginEnd   = dp(8),
                topToTopOf   = ConstraintNode.PARENT,
                width        = ConstraintDim.MatchConstraint,
            ),

            // ④ IPA — ngay bên dưới title, căn trái/phải theo title
            ConstraintChild(
                id   = "ipa",
                node = TextNode(ipa, sp(14f), 0xFF6200EE.toInt(), maxLines = 1),
                startToStartOf = "title",
                endToEndOf     = "title",
                topToBottomOf  = "title", marginTop = dp(4),
                width          = ConstraintDim.MatchConstraint,
            ),

            // ⑤ Meaning — ngay bên dưới ipa, căn trái/phải theo ipa
            ConstraintChild(
                id   = "meaning",
                node = TextNode(meaning, sp(12f), Color.GRAY, maxLines = 2),
                startToStartOf = "ipa",
                endToEndOf     = "ipa",
                topToBottomOf  = "ipa", marginTop = dp(4),
                width          = ConstraintDim.MatchConstraint,
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
        avatarSource: ImageSource,
        avatarSizePx: Int,
    ): LayoutNode = ConstraintNode(
        padding = EdgeInsets(left = dp(12), top = dp(12), right = dp(12), bottom = dp(12)),
        children = listOf(

            // ① Avatar — góc trên-trái của PARENT
            ConstraintChild(
                id = "avatar",
                node = ImageNode(source = avatarSource, width = avatarSizePx, height = avatarSizePx),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf     = ConstraintNode.PARENT,
            ),

            // ② Name — start leo vào END của avatar, top leo vào TOP của PARENT
            ConstraintChild(
                id   = "name",
                node = TextNode(name, sp(16f), Color.BLACK, typeface = Typeface.DEFAULT_BOLD, maxLines = 1),
                startToEndOf = "avatar",  marginStart = dp(12),
                endToEndOf   = ConstraintNode.PARENT,
                topToTopOf   = ConstraintNode.PARENT,
                width        = ConstraintDim.MatchConstraint,
            ),

            // ③ Tag badge — start leo vào END của avatar, top leo vào BOTTOM của name
            //   → phụ thuộc cả avatar lẫn name: resolve ở pass 2
            ConstraintChild(
                id   = "tag",
                node = TextNode(
                    text       = tag,
                    textSizePx = sp(11f),
                    color      = 0xFFFFFFFF.toInt(),
                    typeface   = Typeface.DEFAULT_BOLD,
                    padding    = EdgeInsets.symmetric(h = dp(6), v = dp(3)),
                ),
                startToEndOf  = "avatar",  marginStart = dp(12),
                topToBottomOf = "name",    marginTop   = dp(4),
            ),

            // ④ Role — start leo vào END của tag (chaining ngang)
            //   → phụ thuộc tag: resolve ở pass 3
            ConstraintChild(
                id   = "role",
                node = TextNode(role, sp(11f), Color.DKGRAY, maxLines = 1),
                startToEndOf  = "tag",             marginStart = dp(6),
                endToEndOf    = ConstraintNode.PARENT,
                topToTopOf    = "tag",
                width         = ConstraintDim.MatchConstraint,
            ),

            // ⑤ Bio (dòng sub-text) — top leo vào BOTTOM của role (chaining dọc)
            //   → phụ thuộc role: resolve ở pass 4
            ConstraintChild(
                id   = "bio",
                node = TextNode("Tapped: view ③ leo ④ (ngang) → ⑤ leo ④ (dọc)", sp(10f), 0xFF9E9E9E.toInt(), maxLines = 2),
                startToEndOf  = "avatar",  marginStart = dp(12),
                endToEndOf    = ConstraintNode.PARENT,
                topToBottomOf = "role",    marginTop   = dp(4),
                width         = ConstraintDim.MatchConstraint,
            ),

            // ⑥ Button Like — anchor start-bottom vào PARENT
            ConstraintChild(
                id   = "btn_like",
                node = TextNode(
                    text       = "♥  Like",
                    textSizePx = sp(12f),
                    color      = 0xFFFFFFFF.toInt(),
                    typeface   = Typeface.DEFAULT_BOLD,
                    padding    = EdgeInsets.symmetric(h = dp(14), v = dp(7)),
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToBottomOf  = "bio", marginTop = dp(10),
            ),

            // ⑦ Button Share — start leo vào END của btn_like (chaining ngang)
            //   → phụ thuộc btn_like: resolve sau btn_like
            ConstraintChild(
                id   = "btn_share",
                node = TextNode(
                    text       = "↗  Share",
                    textSizePx = sp(12f),
                    color      = 0xFF6200EE.toInt(),
                    typeface   = Typeface.DEFAULT_BOLD,
                    padding    = EdgeInsets.symmetric(h = dp(14), v = dp(7)),
                ),
                startToEndOf = "btn_like", marginStart = dp(8),
                topToTopOf   = "btn_like",
            ),
        )
    )

    // ── Dimension helpers ─────────────────────────────────────────────────────

    private fun dp(value: Int): Int = (value * dp).toInt()

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
