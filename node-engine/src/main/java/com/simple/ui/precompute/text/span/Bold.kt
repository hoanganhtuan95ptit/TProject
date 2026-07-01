package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.BigSpan
import com.simple.ui.precompute.text.BigSpanConvert

import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService

object BigBold : BigSpan()

@Keep
@AutoService(BigSpanConvert::class)
class BigBoldConvert : BigSpanConvert {

    override fun getAndroidSpan(bigSpan: BigSpan): CharacterStyle? {
        return if (bigSpan is BigBold) StyleSpan(Typeface.BOLD) else null
    }
}
