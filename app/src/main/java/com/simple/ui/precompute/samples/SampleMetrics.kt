package com.simple.ui.precompute.samples

import android.util.DisplayMetrics
import android.util.TypedValue

/**
 * Helper mật độ / kích thước dùng chung cho toàn bộ sample builders.
 *
 * Truyền từ [android.content.res.Resources.getDisplayMetrics] một lần khi khởi tạo
 * Activity, chia sẻ cho mọi builder — tránh mỗi sample tự lấy `Resources`.
 */
class SampleMetrics(val displayMetrics: DisplayMetrics) {

    val density: Float get() = displayMetrics.density

    /** dp → px (Int). */
    fun dp(value: Int): Int = (value * density).toInt()

    /** sp → px (Float). */
    fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, displayMetrics)
}

/**
 * Base cho mỗi sample class — chỉ giữ ref tới [SampleMetrics] và expose
 * shortcut `dp` / `sp` để nội dung builder gọn.
 */
abstract class SampleBuilder(protected val m: SampleMetrics) {

    protected fun dp(value: Int): Int = m.dp(value)

    protected fun sp(value: Float): Float = m.sp(value)
}
