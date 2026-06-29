package com.simple.launcher.retirement.utils.text

import android.text.style.AbsoluteSizeSpan
import android.text.style.CharacterStyle
import androidx.annotation.Keep
import com.google.auto.service.AutoService

/**
 * Span đặt kích thước chữ tuyệt đối theo đơn vị dp (device-independent pixels).
 * Dùng để kiểm soát font size của title toolbar, header... từ ViewModel.
 */
data class TextSize(val sizeDip: Int) : RichSpan()

@Keep
@AutoService(RichSpanConvert::class)
class TextSizeConvert : RichSpanConvert {
    override fun getAndroidSpan(richSpan: RichSpan): CharacterStyle? {
        return if (richSpan is TextSize) AbsoluteSizeSpan(richSpan.sizeDip, true) else null
    }
}
