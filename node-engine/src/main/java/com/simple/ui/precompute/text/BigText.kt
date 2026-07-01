package com.simple.ui.precompute.text

import android.text.Spannable
import android.text.SpannableString
import com.simple.ui.precompute.text.BigImageSpanConvert.Companion.convert

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
