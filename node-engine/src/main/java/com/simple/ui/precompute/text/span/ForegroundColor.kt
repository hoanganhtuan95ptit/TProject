package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.BigImageSpan
import com.simple.ui.precompute.text.BigImageSpanConvert

import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService

data class BigForegroundColor(val color: Int) : BigImageSpan

@Keep
@AutoService(BigImageSpanConvert::class)
class BigForegroundColorConvert : BigImageSpanConvert {

    override fun convert(bigSpan: BigImageSpan): CharacterStyle? {
        return if (bigSpan is BigForegroundColor) ForegroundColorSpan(bigSpan.color) else null
    }
}
