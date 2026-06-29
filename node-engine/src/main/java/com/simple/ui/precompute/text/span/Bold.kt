package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.RichSpan
import com.simple.ui.precompute.text.RichSpanConvert

import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService

object Bold : RichSpan()

@Keep
@AutoService(RichSpanConvert::class)
class BoldConvert : RichSpanConvert {

    override fun getAndroidSpan(richSpan: RichSpan): CharacterStyle? {
        return if (richSpan is Bold) StyleSpan(Typeface.BOLD) else null
    }
}
