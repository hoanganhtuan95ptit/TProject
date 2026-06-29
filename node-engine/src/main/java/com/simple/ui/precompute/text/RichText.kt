package com.simple.launcher.retirement.utils.text

import android.text.Spannable
import android.text.SpannableString
import android.text.style.CharacterStyle
import java.util.ServiceLoader

private val richSpanConvertList by lazy {
    ServiceLoader.load(RichSpanConvert::class.java).toList()
}

// Cache KClass → RichSpanConvert: lần đầu O(n) scan, từ lần 2 trở đi O(1).
// Chạy trên main thread nên HashMap thường là đủ.
private val richSpanConvertCache = HashMap<kotlin.reflect.KClass<out RichSpan>, RichSpanConvert>()

data class RichText(
    val text: String,
    val spans: ArrayList<RichStyle> = arrayListOf()
) {

    var textChar: CharSequence = text

    init {
        if (spans.isNotEmpty()) refresh()
    }

    fun refresh(): RichText {

        val spannable = SpannableString(text)
        spans.forEach { span ->
            span.styles.forEach { styleData ->
                val style = styleData.toAndroidSpan()
                spannable.setSpan(style, span.range.start, span.range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        textChar = spannable

        return this
    }

    private fun RichSpan.toAndroidSpan(): CharacterStyle {
        val klass = this::class
        val cached = richSpanConvertCache[klass]
        if (cached != null) return cached.getAndroidSpan(this)!!

        for (converter in richSpanConvertList) {
            val span = converter.getAndroidSpan(this)
            if (span != null) {
                richSpanConvertCache[klass] = converter
                return span
            }
        }
        error("No RichSpanConvert found for ${klass.simpleName}")
    }

    companion object {

        fun Builder(text: String) = RichTextBuilder(text)
    }
}

data class RichStyle(
    val range: RichRange,
    val styles: List<RichSpan> = arrayListOf()
)

data class RichRange(
    val start: Int,
    val end: Int
)