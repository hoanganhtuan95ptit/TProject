package com.simple.t

import android.graphics.Color
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simple.ui.precompute.text.span.ForegroundColor
import com.simple.ui.precompute.text.span.TextSize
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.with
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BigTextTest {

    @Test
    fun bigTextAppliesForegroundColorAndTextSize() {
        val bigText = "Kotlin".with(ForegroundColor(Color.GREEN), TextSize(20)).build()
        val spanned = bigText.textChar as Spanned

        val colorSpans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
        val sizeSpans = spanned.getSpans(0, spanned.length, AbsoluteSizeSpan::class.java)

        assertEquals(1, colorSpans.size)
        assertEquals(Color.GREEN, colorSpans.single().foregroundColor)
        assertEquals(1, sizeSpans.size)
        assertEquals(20, sizeSpans.single().size)
    }
}
