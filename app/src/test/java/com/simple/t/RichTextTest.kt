package com.simple.t

import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import com.simple.ui.precompute.RichSpanConvert
import com.simple.ui.precompute.TextSize
import com.simple.ui.precompute.with
import java.util.ServiceLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {

    @Test
    fun textSizeSpanIsLoadedThroughAutoService() {
        val converters = ServiceLoader.load(RichSpanConvert::class.java).toList()

        assertTrue(converters.any { it.javaClass.name == "com.simple.ui.precompute.TextSizeConvert" })
    }

    @Test
    fun richTextAppliesTextSizeSpan() {
        val richText = "Outline đang xử lý".with(TextSize(20))
        val spanned = richText.textChar as Spanned

        val spans = spanned.getSpans(0, spanned.length, AbsoluteSizeSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals(20, spans.single().size)
    }
}
