package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.BigSpan
import com.simple.ui.precompute.text.BigSpanConvert

import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService

data class ForegroundColor(val color: Int) : BigSpan()

@Keep
@AutoService(BigSpanConvert::class)
class ForegroundColorConvert : BigSpanConvert {

    override fun getAndroidSpan(bigSpan: BigSpan): CharacterStyle? {
        return if (bigSpan is ForegroundColor) ForegroundColorSpan(bigSpan.color) else null
    }
}
