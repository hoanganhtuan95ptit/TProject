package com.simple.ui.precompute.text.span

import com.simple.ui.precompute.text.BigImageSpan
import com.simple.ui.precompute.text.BigImageSpanConvert

import android.text.style.AbsoluteSizeSpan
import android.text.style.CharacterStyle
import androidx.annotation.Keep
import com.google.auto.service.AutoService

/**
 * Span đặt kích thước chữ tuyệt đối theo đơn vị dp (device-independent pixels).
 * Dùng để kiểm soát font size của title toolbar, header... từ ViewModel.
 */
data class BigTextSize(val sizePx: Int) : BigImageSpan

@Keep
@AutoService(BigImageSpanConvert::class)
class BigTextSizeConvert : BigImageSpanConvert {
    override fun convert(bigSpan: BigImageSpan): CharacterStyle? {
        return if (bigSpan is BigTextSize) AbsoluteSizeSpan(bigSpan.sizePx, false) else null
    }
}
