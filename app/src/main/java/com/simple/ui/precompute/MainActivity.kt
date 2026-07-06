package com.simple.ui.precompute

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simple.adapter.MultiAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.t.R
import com.simple.ui.precompute.MainActivity.Companion.REPEAT
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.loader.GlideImageLoader
import com.simple.ui.precompute.loader.ImageLoader
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.GroupSpec
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.samples.CardGroup
import com.simple.ui.precompute.samples.ConstraintNodeSamples
import com.simple.ui.precompute.samples.FlexboxNodeSamples
import com.simple.ui.precompute.samples.GradientGroup
import com.simple.ui.precompute.samples.ImageTransformSamples
import com.simple.ui.precompute.samples.IndentedColumn
import com.simple.ui.precompute.samples.LinearNodeSamples
import com.simple.ui.precompute.samples.OutlineNodeSamples
import com.simple.ui.precompute.samples.PhoneticChipSamples
import com.simple.ui.precompute.samples.PhoneticNode
import com.simple.ui.precompute.samples.ProgressBarSamples
import com.simple.ui.precompute.samples.SampleMetrics
import com.simple.ui.precompute.samples.ScoreGaugeSamples
import com.simple.ui.precompute.samples.SectionGroup
import com.simple.ui.precompute.samples.SelfContainedGroup
import com.simple.ui.precompute.samples.SentencePhoneticSamples
import com.simple.ui.precompute.samples.TagRow
import com.simple.ui.precompute.text.BigText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainActivity — Demo bộ LayoutEngine, dùng [RecyclerView] để đo hiệu năng scroll.
 *
 * Trước đây UI là `ScrollView + LinearLayout` (mọi card inflated cùng lúc, không
 * recycle) — mọi [PrecomputedView] đều nằm trên tree ngay cả khi ngoài màn hình,
 * nên draw pass phải walk toàn bộ. Chuyển sang [RecyclerView] cho:
 * - Recycle: chỉ giữ view visible + prefetch → giảm memory + draw time.
 * - Đo hiệu năng có ý nghĩa: [FpsMonitor] ghi lại fps / jank / freeze khi scroll.
 * - Lặp mẫu [REPEAT] lần để list đủ dài để scroll đo được rõ.
 *
 * Toàn bộ measure vẫn chạy trên background thread trước khi bind — RecyclerView
 * chỉ nhận [LayoutResult] đã đo sẵn.
 */
class MainActivity : AppCompatActivity() {

    private val cardWidth by lazy {
        val dp = resources.displayMetrics.density
        resources.displayMetrics.widthPixels - (32 * dp).toInt()
    }

    private val fpsMonitor by lazy { FpsMonitor(findViewById(R.id.fps_label)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ImageLoader.install(GlideImageLoader(this))

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.setHasFixedSize(false)
        // RecycledViewPool mặc định đủ dùng; tăng cache offscreen 1 chút để scroll
        // fast bớt bind rebuild — vẫn recycle được, khác với ScrollView cũ.
//        recycler.setItemViewCacheSize(8)

        val adapter = MultiAdapter(SectionAdapter(), CardAdapter(), FooterAdapter())
        recycler.adapter = adapter

        val m = SampleMetrics(resources.displayMetrics)
        val linear = LinearNodeSamples(m)
        val constraint = ConstraintNodeSamples(m)
        val outline = OutlineNodeSamples(m)
        val transform = ImageTransformSamples(m)
        val phonetic = PhoneticChipSamples(m)
        val gauge = ScoreGaugeSamples(m)
        val progress = ProgressBarSamples(m)
        val flexbox = FlexboxNodeSamples(m)
        val sentence = SentencePhoneticSamples(m)

        val items = listOf(
            Triple("Hello World — a very long word to test text wrapping behavior", "/həˈloʊ wɜːrld/", "Xin chào thế giới"),
            Triple("Google nói Advertising ID là mã định danh do Google Play services cung cấp, người dùng có thể reset hoặc xóa. Khi bị xóa, app đọc ID có thể nhận chuỗi toàn số 0", "/ˈændrɔɪd/", "Hệ điều hành"),
            Triple("Kotlin", "/ˈkɒtlɪn/", "Ngôn ngữ lập trình"),
            Triple("Precompute", "/priːkəmˈpjuːt/", "Tính trước"),
        )
        val iconSource = BigImage(R.mipmap.ic_launcher)
        val iconSizePx = m.dp(48)

        lifecycleScope.launch {
            val allItems = withContext(Dispatchers.Default) {
                val list = ArrayList<ViewItem>(256)
                var cardIndex = 0

                // Lặp REPEAT lần để list đủ dài đo scroll — mọi measure vẫn ở
                // background nên block tính toán không dội lên main.
                repeat(REPEAT) { rep ->
                    val prefix = if (REPEAT > 1) "[#${rep + 1}] " else ""

                    list += SectionViewItem("${prefix}① LinearNode  —  Row / Column")
                    items.forEach { (w, i, msg) ->
                        list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = linear.buildCard(w, i, msg, iconSource, iconSizePx), constraints = Constraints(cardWidth)))
                    }

                    list += SectionViewItem("${prefix}② ConstraintNode  —  tương tự ConstraintLayout")
                    items.forEach { (w, i, msg) ->
                        list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = constraint.buildCard(w, i, msg, iconSource, iconSizePx), constraints = Constraints(cardWidth)))
                    }

                    list += SectionViewItem("${prefix}③ ConstraintNode  —  view con leo nhau (view-to-view)")
                    val profiles = listOf(
                        Triple("Alice Nguyen", "#Android", "Senior Engineer  ·  Google"),
                        Triple("Bob Tran", "#Kotlin", "Staff Engineer  ·  JetBrains"),
                        Triple("Carol Le", "#Compose", "UI Engineer  ·  Meta"),
                    )
                    profiles.forEach { (n, t, r) ->
                        list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = constraint.buildProfileCard(n, t, r, iconSource, iconSizePx), constraints = Constraints(cardWidth)))
                    }

                    list += SectionViewItem("${prefix}④ ConstraintNode  —  WrapContent")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = constraint.buildWrapContentTagsCard(), constraints = Constraints(cardWidth)))
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = constraint.buildWrapContentCenterCard(), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑤ OutlineNode  —  viền loading bo góc")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = outline.buildLoadingCard(), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑥ ImageNode  —  Glide transform (Circle / Rounded)")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = transform.buildCard(iconSizePx), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑦ PhoneticChip  —  từ XML → Node")
                    items.forEach { (w, i, _) ->
                        list += CardViewItem(
                            "card_${cardIndex++}",
                            LayoutEngine.measure(
                                node = phonetic.buildChip("$w\n$i"), constraints = Constraints(cardWidth)
                            ).apply {
                                Log.d("tuanha", "Draw Hierarchy:\n${printDrawSpecs(this.draws)}")
                            }
                        )
                    }

                    list += SectionViewItem("${prefix}⑦b PhoneticNode  —  Spec tự xử lý click")
                    list += CardViewItem(
                        "card_${cardIndex++}",
                        LayoutEngine.measure(
                            node = PhoneticNode(
                                id = "phonetic_demo_$rep",
                                text = "android",
                                contentColor = 0xFF19D96B.toInt(),
                                strokeShow = true,
                                onlyReading = true,
                                textDisplay = BigText("android"),
                                phoneticDisplay = BigText("/ˈændrɔɪd/"),
                                textSizePx = m.sp(20f),
                                phoneticTextSizePx = m.sp(14f),
                                gapPx = m.dp(6),
                                strokeWidthPx = m.dp(2).toFloat(),
                                cornerRadiusPx = m.dp(16).toFloat(),
                                dashWidthPx = m.dp(5).toFloat(),
                                dashGapPx = m.dp(5).toFloat(),
                                contentPadding = EdgeInsets.symmetric(h = m.dp(14), v = m.dp(8))
                            ),
                            constraints = Constraints(cardWidth)
                        )
                    )

                    list += SectionViewItem("${prefix}⑧ ScoreGauge  —  GaugeArcNode + GaugeScoreNode")
                    items.forEach { _ ->
                        list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = gauge.buildGauge(progress = 90, sizePx = m.dp(160)), constraints = Constraints(cardWidth)))
                    }

                    list += SectionViewItem("${prefix}⑨ ProgressBarNode  —  thanh tiến độ ngang")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = progress.buildCard("Listening accuracy", 68, 100, 0xFF1B998B.toInt()), constraints = Constraints(cardWidth)))
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = progress.buildCard("Speaking fluency", 42, 100, 0xFFE76F51.toInt()), constraints = Constraints(cardWidth)))
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = progress.buildCard("Daily goal", 9, 12, 0xFF5B7CFA.toInt()), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑩ XML NoteRow  —  LinearLayout → Node")
                    val notes = listOf(
                        "Meet" to "Meet",
                        "Design sync" to "Review node spacing from the XML version",
                        "Long title wraps inside" to "The text column uses MatchParent after icon width."
                    )
                    notes.forEach { (t, n) ->
                        list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = linear.buildNoteRow(t, n, iconSource), constraints = Constraints(cardWidth)))
                    }

                    list += SectionViewItem("${prefix}⑪ TrySpeakChip  —  ảnh mẫu → node")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = constraint.buildTrySpeakChip(), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑫ DashedLineText  —  LineNode + TextNode")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = linear.buildDashedLineText(), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑮ WordPhonetic  —  Theo yêu cầu")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = linear.buildWordPhonetic("android", "/ˈændrɔɪd/"), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑬ FlexboxNode  —  wrap tags giống FlexboxLayout")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = flexbox.buildTagsCard(), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑭ GroupNode kế thừa  —  custom patterns")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = TagRow(listOf("Kotlin", "Android", "Precompute", "Node"), m.sp(14f), 0xFF6200EE.toInt(), m.dp(12), EdgeInsets.all(m.dp(16))), constraints = Constraints(cardWidth)))
                    list += CardViewItem(
                        "card_${cardIndex++}",
                        LayoutEngine.measure(
                            node = SelfContainedGroup(
                                "SelfContainedGroup",
                                "Caller chỉ truyền data/config, node tự tạo TextNode bên trong",
                                "Không cần truyền childNode",
                                m.sp(16f),
                                m.sp(13f),
                                m.sp(12f),
                                0xFF263238.toInt(),
                                0xFF03DAC5.toInt(),
                                m.dp(16).toFloat(),
                                EdgeInsets.all(m.dp(16))
                            ), constraints = Constraints(cardWidth)
                        )
                    )
                    list += CardViewItem(
                        "card_${cardIndex++}",
                        LayoutEngine.measure(
                            node = IndentedColumn(listOf(TextNode(BigText("Item 1"), m.sp(14f), Color.BLACK)), m.dp(20).let { EdgeInsets(it, m.dp(6), m.dp(12), m.dp(6)) }, m.dp(4), EdgeInsets.all(m.dp(12))),
                            constraints = Constraints(cardWidth)
                        )
                    )
                    list += CardViewItem(
                        "card_${cardIndex++}",
                        LayoutEngine.measure(
                            node = SectionGroup("Header", listOf(TextNode(BigText("Item A"), m.sp(14f), Color.DKGRAY)), m.sp(16f), 0xFF6200EE.toInt(), m.dp(8), m.dp(4), EdgeInsets.all(m.dp(12))),
                            constraints = Constraints(cardWidth)
                        )
                    )
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = CardGroup(listOf(TextNode(BigText("CardGroup"), m.sp(16f), Color.WHITE)), 0xFF6200EE.toInt(), m.dp(16).toFloat(), EdgeInsets.all(m.dp(16))), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑯ GradientGroup  —  Custom Spec drawing")
                    list += CardViewItem(
                        "card_${cardIndex++}",
                        LayoutEngine.measure(
                            node = GradientGroup(
                                initialChildren = listOf(
                                    TextNode(BigText("Nền Gradient tùy biến"), m.sp(16f), Color.WHITE, typeface = Typeface.DEFAULT_BOLD),
                                    TextNode(BigText("Được vẽ trực tiếp trong drawBackground"), m.sp(12f), 0xCCFFFFFF.toInt())
                                ),
                                startColor = 0xFF6200EE.toInt(),
                                endColor = 0xFF03DAC5.toInt(),
                                cornerRadius = m.dp(12).toFloat(),
                                padding = EdgeInsets.all(m.dp(20)),
                                onClick = {
                                    Toast.makeText(this@MainActivity, "Gradient Group Clicked!", Toast.LENGTH_SHORT).show()
                                }
                            ),
                            constraints = Constraints(cardWidth)
                        ))

                    list += SectionViewItem("${prefix}⑰ Sentence Phonetic Layout — Theo ảnh mẫu")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = sentence.buildOriginalSample(), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑱ Sentence Stress Test — Nhiều từ dài")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = sentence.buildLongStressTest(), constraints = Constraints(cardWidth)))

                    list += SectionViewItem("${prefix}⑲ Sentence Stress Test — 150+ chips (Wrap)")
                    list += CardViewItem("card_${cardIndex++}", LayoutEngine.measure(node = sentence.buildWrappingStressTest(), constraints = Constraints(cardWidth)))
                }

                list += FooterViewItem
                list
            }
            adapter.submitList(allItems)
        }
    }

    override fun onResume() {
        super.onResume()
        fpsMonitor.attach()
    }

    override fun onPause() {
        fpsMonitor.detach()
        super.onPause()
    }

    fun printDrawSpecs(specs: List<DrawSpec>, indent: String = ""): String {
        return specs.joinToString("\n") { spec ->
            val idString = spec.node?.id?.let { " [id=$it]" } ?: ""
            var line = "$indent- ${spec.javaClass.simpleName}$idString (${spec.width}x${spec.height}) @(${spec.left},${spec.top})"

            val children = when (spec) {
                is GroupSpec -> spec.children
                is CachedDrawSpec -> spec.children
                else -> emptyList()
            }

            if (children.isNotEmpty()) {
                line += "\n" + printDrawSpecs(children, "$indent  ")
            }
            line
        }
    }

    // ─── Item model + Adapter ──────────────────────────────────────────────

    companion object {
        /** Số lần lặp toàn bộ demo — tăng để list dài hơn khi đo scroll. */
        private const val REPEAT = 10
    }
}
