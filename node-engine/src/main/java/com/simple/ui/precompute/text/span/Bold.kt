package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.BigImageSpan
import com.simple.ui.precompute.text.BigImageSpanConvert

import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService

object BigBold : BigImageSpan

@Keep
@AutoService(BigImageSpanConvert::class)
class BigBoldConvert : BigImageSpanConvert {

    override fun convert(bigSpan: BigImageSpan): CharacterStyle? {
        return if (bigSpan is BigBold) StyleSpan(Typeface.BOLD) else null
    }
}
