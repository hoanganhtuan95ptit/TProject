package com.simple.ui.precompute.text

import android.text.style.CharacterStyle

open class BigSpan

interface BigSpanConvert {
    fun getAndroidSpan(bigSpan: BigSpan): CharacterStyle?
}