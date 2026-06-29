package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.RichSpan
import com.simple.ui.precompute.text.RichSpanConvert

import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService

data class ForegroundColor(val color: Int) : RichSpan()

@Keep
@AutoService(RichSpanConvert::class)
class ForegroundColorConvert : RichSpanConvert {

    override fun getAndroidSpan(richSpan: RichSpan): CharacterStyle? {
        return if (richSpan is ForegroundColor) ForegroundColorSpan(richSpan.color) else null
    }
}
