package com.simple.launcher.retirement.utils.text

import android.text.style.CharacterStyle

open class RichSpan

interface RichSpanConvert {
    fun getAndroidSpan(richSpan: RichSpan): CharacterStyle?
}