package com.simple.ui.precompute.text

import android.text.Spannable
import android.text.SpannableString
import android.text.style.CharacterStyle
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val bigSpanConvertList by lazy {

    try {

        ServiceLoader.load(BigSpanConvert::class.java).toList()
    } catch (_: ServiceConfigurationError) {

        emptyList()
    }
}

// Cache KClass → BigSpanConvert: lần đầu O(n) scan, từ lần 2 trở đi O(1).
// BigText có thể được build từ bất kỳ thread nào (kể cả Dispatchers.Default
// trước khi gắn vào view), nên dùng ConcurrentHashMap để an toàn.
private val bigSpanConvertCache =
    ConcurrentHashMap<KClass<out BigSpan>, BigSpanConvert>()

data class BigText(
    val text: String,
    val spans: ArrayList<BigStyle> = arrayListOf()
) {

    // Lazy: nếu không ai đọc textChar (vd BigText chỉ tạm để equals/copy)
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

    fun refresh(): BigText {

        synchronized(this) {

            _textChar = buildTextChar()
        }

        return this
    }

    private fun buildTextChar(): CharSequence {

        if (spans.isEmpty()) return text

        val spannable = SpannableString(text)
        spans.forEach { style ->

            style.applyTo(spannable)
        }

        return spannable
    }

    private fun BigStyle.applyTo(spannable: Spannable) {

        styles.forEach { styleData ->

            applyStyle(spannable, styleData)
        }
    }

    private fun BigStyle.applyStyle(spannable: Spannable, styleData: BigSpan) {

        spannable.setSpan(
            styleData.toAndroidSpan(),
            range.start,
            range.end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun BigSpan.toAndroidSpan(): CharacterStyle {

        val klass = this::class
        findCachedAndroidSpan(klass)?.let { return it }

        return findServiceAndroidSpan(klass)
            ?: error("No BigSpanConvert found for ${klass.simpleName}")
    }

    private fun BigSpan.findCachedAndroidSpan(klass: KClass<out BigSpan>): CharacterStyle? {

        val cached = bigSpanConvertCache[klass] ?: return null
        val span = cached.getAndroidSpan(this)
        if (span != null) return span

        bigSpanConvertCache.remove(klass, cached)
        return null
    }

    private fun BigSpan.findServiceAndroidSpan(klass: KClass<out BigSpan>): CharacterStyle? {

        bigSpanConvertList.forEach { converter ->

            val span = converter.getAndroidSpan(this)
            if (span != null) {

                bigSpanConvertCache.putIfAbsent(klass, converter)
                return span
            }
        }

        return null
    }

    companion object {

        fun Builder(text: String) = BigTextBuilder(text)
    }
}

data class BigStyle(
    val range: BigRange,
    val styles: List<BigSpan> = arrayListOf()
)

data class BigRange(
    val start: Int,
    val end: Int
)
