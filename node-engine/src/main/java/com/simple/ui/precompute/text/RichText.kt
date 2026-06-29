package com.simple.launcher.retirement.utils.text

import android.text.Spannable
import android.text.SpannableString
import android.text.style.CharacterStyle
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

private val builtInRichSpanConvertList by lazy {
    listOf(
        BoldConvert(),
        CustomFontConvert(),
        ForegroundColorConvert(),
        RelativeSizeConvert(),
        RoundedOutlineSpanConvert(),
        TextSizeConvert()
    )
}

private val richSpanConvertList by lazy {
    (loadServiceRichSpanConverters() + builtInRichSpanConvertList)
        .distinctBy { it.javaClass.name }
}

private fun loadServiceRichSpanConverters(): List<RichSpanConvert> {
    return try {
        ServiceLoader.load(RichSpanConvert::class.java).toList()
    } catch (_: ServiceConfigurationError) {
        emptyList()
    }
}

// Cache KClass → RichSpanConvert: lần đầu O(n) scan, từ lần 2 trở đi O(1).
// RichText có thể được build từ bất kỳ thread nào (kể cả Dispatchers.Default
// trước khi gắn vào view), nên dùng ConcurrentHashMap để an toàn.
private val richSpanConvertCache =
    ConcurrentHashMap<kotlin.reflect.KClass<out RichSpan>, RichSpanConvert>()

data class RichText(
    val text: String,
    val spans: ArrayList<RichStyle> = arrayListOf()
) {

    // Lazy: nếu không ai đọc textChar (vd RichText chỉ tạm để equals/copy)
    // thì khỏi đo spannable. Khi đo trên bg thread, lần truy cập đầu tiên sẽ
    // chạy refresh() — vẫn off main thread. @Volatile để publish an toàn
    // qua thread boundary.
    @Volatile
    private var _textChar: CharSequence? = null

    var textChar: CharSequence
        get() = _textChar ?: synchronized(this) {
            _textChar ?: buildTextChar().also { _textChar = it }
        }
        set(value) {
            _textChar = value
        }

    fun refresh(): RichText {
        synchronized(this) { _textChar = buildTextChar() }
        return this
    }

    private fun buildTextChar(): CharSequence {
        if (spans.isEmpty()) return text
        val spannable = SpannableString(text)
        spans.forEach { span ->
            span.styles.forEach { styleData ->
                val style = styleData.toAndroidSpan()
                spannable.setSpan(style, span.range.start, span.range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return spannable
    }

    private fun RichSpan.toAndroidSpan(): CharacterStyle {
        val klass = this::class
        val cached = richSpanConvertCache[klass]
        if (cached != null) {
            cached.getAndroidSpan(this)?.let { return it }
            richSpanConvertCache.remove(klass, cached)
        }

        for (converter in richSpanConvertList) {
            val span = converter.getAndroidSpan(this)
            if (span != null) {
                richSpanConvertCache.putIfAbsent(klass, converter)
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