package com.simple.ui.precompute.samples

import android.graphics.Color
import android.graphics.Typeface
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
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.LoadingNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.toBuilder

/**
 * DEMO — Giao diện Sentence với các Phonetic Chips giống trong ảnh mẫu.
 */
class SentencePhoneticSamples(m: SampleMetrics) : SampleBuilder(m) {

    fun buildSentenceLayout(words: List<WordData>, showMic: Boolean = true): LayoutNode {
        val children = mutableListOf<FlexChild>()

        // Add "Thử nói" chip if requested
        if (showMic) {
            children.add(
                FlexChild(
                    node = buildTrySpeakChip()
                )
            )
        }

        // Add word chips
        words.forEach { word ->
            children.add(
                FlexChild(
                    node = buildWordChip(word)
                )
            )
        }

        return FlexboxNode(
            flexDirection = FlexDirection.ROW,
            flexWrap = FlexWrap.WRAP,
            justifyContent = FlexJustifyContent.FLEX_START,
            alignItems = FlexAlignItems.FLEX_START,
            alignContent = FlexAlignContent.FLEX_START,
            gap = dp(8),
            padding = EdgeInsets.all(dp(16)),
            layoutWidth = LayoutDimension.MatchParent,
            children = children
        )
    }

    /**
     * Stress test 1: Một câu rất dài với nhiều từ phức tạp.
     */
    fun buildLongStressTest(): LayoutNode {
        val words = mutableListOf<WordData>()
        repeat(5) {
            words.addAll(listOf(
                WordData("extraordinary", "/ɪkˈstrɔːr.dn.er.i/", hasArrow = true),
                WordData("comprehension", "/ˌkɑːm.prəˈhen.ʃən/", hasArrow = true),
                WordData("sophisticated", "/səˈfɪs.tə.keɪ.t̬ɪd/", hasArrow = true),
                WordData("implementation", "/ˌɪm.plə.menˈteɪ.ʃən/", hasArrow = true),
                WordData("infrastructure", "/ˈɪn.frəˌstrʌk.tʃər/", hasArrow = true)
            ))
        }
        return buildSentenceLayout(words)
    }

    /**
     * Stress test 2: Nhiều từ ngắn, gây ra cực nhiều Flexbox wrapping.
     */
    fun buildWrappingStressTest(): LayoutNode {
        val words = mutableListOf<WordData>()
        repeat(50) {
            words.add(WordData("a", "/ə/", hasArrow = false))
            words.add(WordData("is", "/ɪz/", hasArrow = true))
            words.add(WordData("the", "/ðə/", hasArrow = false))
        }
        return buildSentenceLayout(words, showMic = false)
    }

    /**
     * Mẫu câu gốc từ ảnh.
     */
    fun buildOriginalSample(): LayoutNode {
        val words = listOf(
            WordData("android", "/ˈæn.drɔɪd/", hasArrow = true),
            WordData("code", "/koʊd/", hasArrow = true),
            WordData("is", "/ɪz/", hasArrow = true),
            WordData("the", "/ðiː/ - /ðə/ - /ði/", hasArrow = true),
            WordData("foundation", "/faʊnˈdeɪ.ʃən/", hasArrow = true),
            WordData("of", "/əv/", hasArrow = true),
            WordData("every", "/ˈev.ri/ - /ˈev.ə.ri/", hasArrow = true),
            WordData("android", "/ˈæn.drɔɪd/", hasArrow = true),
            WordData("application", "/ˌæp.lɪˈkeɪ.ʃən/", hasArrow = true),
            WordData("and", "/ænd/ - /ənd/", hasArrow = true),
            WordData("it", "/ɪt/ - /ɪt/", hasArrow = true),
            WordData("plays", "/pleɪz/", hasArrow = true),
            WordData("an", "/æn/ - /ən/", hasArrow = true),
            WordData("important", "/ɪmˈpɔːr.tənt/", hasArrow = true),
            WordData("role", "/roʊl/", hasArrow = true),
            WordData("in", "/ɪn/ - /ɪn/", hasArrow = true),
            WordData("how", "/haʊ/", hasArrow = true),
            WordData("an", "/æn/ - /ən/", hasArrow = true),
            WordData("app", "/æp/", hasArrow = true),
            WordData("looks", "/lʊks/", hasArrow = true),
            WordData("behaves", "/bɪˈheɪvz/", hasArrow = true),
            WordData("and", "/ænd/ - /ənd/", hasArrow = true),
            WordData("responds", "/rɪˈspɑːndz/ - /rɪˈspɑːndz/", hasArrow = true),
            WordData("to", "/tuː/ - /tə/ - /tʊ/", hasArrow = true),
            WordData("users", "/ˈjuː.zərz/", hasArrow = true)
        )
        return buildSentenceLayout(words)
    }

    private fun buildTrySpeakChip(): LayoutNode {
        return ConstraintNode(
            children = listOf(
                ConstraintChild(
                    id = "bg",
                    node = LoadingNode(
                        strokeColor = Color.parseColor("#4CAF50"),
                        strokeWidth = dp(1).toFloat(),
                        cornerRadius = dp(16).toFloat(),
                        dashWidth = dp(4).toFloat(),
                        dashGap = dp(2).toFloat(),
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
                        padding = EdgeInsets.symmetric(h = dp(12), v = dp(8)),
                        children = listOf(
                            TextNode(
                                text = BigText("\uD83C\uDF99\uFE0F"), // Mic emoji
                                textSizePx = sp(14f)
                            ).linearChild(),
                            TextNode(
                                text = BigText("Thử nói"),
                                textSizePx = sp(14f),
                                color = Color.parseColor("#4CAF50")
                            ).linearChild()
                        )
                    ),
                    startToStartOf = ConstraintNode.PARENT,
                    topToTopOf = ConstraintNode.PARENT
                )
            )
        )
    }

    private fun buildWordChip(word: WordData): LayoutNode {
        val wordText = if (word.hasArrow) "${word.word} ▾" else word.word
        
        return ConstraintNode(
            children = listOf(
                ConstraintChild(
                    id = "bg",
                    node = LoadingNode(
                        strokeColor = Color.parseColor("#4CAF50"),
                        strokeWidth = dp(1).toFloat(),
                        cornerRadius = dp(12).toFloat(),
                        dashWidth = dp(4).toFloat(),
                        dashGap = dp(2).toFloat(),
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
                        orientation = Orientation.VERTICAL,
                        crossAlign = CrossAlign.CENTER,
                        padding = EdgeInsets.symmetric(h = dp(10), v = dp(6)),
                        children = listOf(
                            TextNode(
                                text = BigText(wordText),
                                textSizePx = sp(14f),
                                typeface = Typeface.DEFAULT_BOLD,
                                color = Color.BLACK
                            ).linearChild(),
                            TextNode(
                                text = buildPhoneticText(word.phonetic),
                                textSizePx = sp(12f),
                                color = Color.GRAY
                            ).linearChild()
                        )
                    ),
                    startToStartOf = ConstraintNode.PARENT,
                    topToTopOf = ConstraintNode.PARENT
                )
            )
        )
    }

    private fun buildPhoneticText(phonetic: String): BigText {
        val builder = phonetic.toBuilder()
        
        // Simple logic to color vowels red
        val vowels = listOf("æ", "ʊ", "ɔ", "ɪ", "ə", "e", "u", "a")
        
        vowels.forEach { vowel ->
            var index = phonetic.indexOf(vowel)
            while (index != -1) {
                builder.withRange(index, index + vowel.length, BigForegroundColor(Color.RED))
                index = phonetic.indexOf(vowel, index + vowel.length)
            }
        }

        return builder.build()
    }

    data class WordData(
        val word: String,
        val phonetic: String,
        val hasArrow: Boolean = false
    )
}
