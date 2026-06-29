package com.simple.launcher.retirement.utils.text

import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService

data class RelativeSize(val proportion: Float) : RichSpan()

@Keep
@AutoService(RichSpanConvert::class)
class RelativeSizeConvert : RichSpanConvert {

    override fun getAndroidSpan(richSpan: RichSpan): CharacterStyle? {
        return if (richSpan is RelativeSize) RelativeSizeSpan(richSpan.proportion) else null
    }
}
