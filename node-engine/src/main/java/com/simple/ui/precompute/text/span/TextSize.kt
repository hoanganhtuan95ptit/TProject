package com.simple.ui.precompute.text.span

import android.text.style.AbsoluteSizeSpan
import android.text.style.CharacterStyle
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.ui.precompute.text.BigImageSpan
import com.simple.ui.precompute.text.BigImageSpanConvert

/**
 * Span đặt kích thước chữ tuyệt đối theo đơn vị dp (device-independent pixels).
 * Dùng để kiểm soát font size của title toolbar, header... từ ViewModel.
 */

fun BigTextSize(sizePx: Float) = BigTextSize(sizePx = sizePx.toInt())

data class BigTextSize(val sizePx: Int) : BigImageSpan

@Keep
@AutoService(BigImageSpanConvert::class)
class BigTextSizeConvert : BigImageSpanConvert {
    override fun convert(bigSpan: BigImageSpan): CharacterStyle? {
        return if (bigSpan is BigTextSize) AbsoluteSizeSpan(bigSpan.sizePx, false) else null
    }
}
