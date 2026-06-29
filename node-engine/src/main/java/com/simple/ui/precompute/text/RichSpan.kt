package com.simple.ui.precompute.text

import android.text.style.CharacterStyle

open class RichSpan

interface RichSpanConvert {
    fun getAndroidSpan(richSpan: RichSpan): CharacterStyle?
}