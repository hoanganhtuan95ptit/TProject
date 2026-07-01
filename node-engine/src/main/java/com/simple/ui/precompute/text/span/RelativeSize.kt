package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.BigSpan
import com.simple.ui.precompute.text.BigSpanConvert

import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService

data class BigRelativeSize(val proportion: Float) : BigSpan()

@Keep
@AutoService(BigSpanConvert::class)
class BigRelativeSizeConvert : BigSpanConvert {

    override fun getAndroidSpan(bigSpan: BigSpan): CharacterStyle? {
        return if (bigSpan is BigRelativeSize) RelativeSizeSpan(bigSpan.proportion) else null
    }
}
