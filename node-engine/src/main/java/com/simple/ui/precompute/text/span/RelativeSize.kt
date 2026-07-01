package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.BigImageSpan
import com.simple.ui.precompute.text.BigImageSpanConvert

import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService

data class BigRelativeSize(val proportion: Float) : BigImageSpan

@Keep
@AutoService(BigImageSpanConvert::class)
class BigRelativeSizeConvert : BigImageSpanConvert {

    override fun convert(bigSpan: BigImageSpan): CharacterStyle? {
        return if (bigSpan is BigRelativeSize) RelativeSizeSpan(bigSpan.proportion) else null
    }
}
