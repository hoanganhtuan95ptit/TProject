package com.simple.ui.precompute.text

import android.text.Spannable
import android.text.SpannableString
import android.text.style.CharacterStyle
import com.simple.ui.precompute.text.BigImageSpanConvert.Companion.convert
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val bigSpanConvertList by lazy {

    try {

        ServiceLoader.load(BigImageSpanConvert::class.java).toList()
    } catch (_: ServiceConfigurationError) {

        emptyList()
    }
}

// Cache KClass → BigSpanConvert: lần đầu O(n) scan, từ lần 2 trở đi O(1).
// BigText có thể được build từ bất kỳ thread nào (kể cả Dispatchers.Default
// trước khi gắn vào view), nên dùng ConcurrentHashMap để an toàn.
private val bigSpanConvertCache =
    ConcurrentHashMap<KClass<out BigImageSpan>, BigImageSpanConvert>()

data class BigText(
    val text: String,
    val spans: ArrayList<BigStyle> = arrayListOf()
) {

    var textChar: CharSequence =  SpannableString(text).apply {

        spans.forEach { style ->

            style.applyTo(this)
        }
    }

    private fun BigStyle.applyTo(spannable: Spannable) {

        styles.forEach { styleData ->

            applyStyle(spannable, styleData)
        }
    }

    private fun BigStyle.applyStyle(spannable: Spannable, styleData: BigImageSpan) {

        spannable.setSpan(
            styleData.convert(),
            range.start,
            range.end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    companion object {

        fun Builder(text: String) = BigTextBuilder(text)
    }
}

data class BigStyle(
    val range: BigRange,
    val styles: List<BigImageSpan> = arrayListOf()
)

data class BigRange(
    val start: Int,
    val end: Int
)
