package com.simple.ui.precompute.text

class BigTextBuilder(val text: String) {

    val bigStyles: ArrayList<BigStyle> = arrayListOf()

    fun add(vararg bigStyle: BigStyle) {
        bigStyles.addAll(bigStyle)
    }

    /**
     * Áp dụng style cho một đoạn text theo vị trí đã biết sẵn.
     *
     * Dùng khi bạn đã có index (ví dụ: từ kết quả search, regex, hay tính toán trước),
     * tránh phải gọi [String.indexOf] tốn thêm O(n) scan.
     *
     * ```kotlin
     * val query = "100.000đ"
     * val start = price.indexOf(query)
     * if (start >= 0) {
     *     price.toBuilder().withRange(start, start + query.length, Bold, ForegroundColor(Color.RED)).build()
     * }
     * ```
     */
    fun withRange(start: Int, end: Int, vararg spans: BigImageSpan): BigTextBuilder {
        require(start >= 0 && end <= text.length && start < end) {
            "withRange: invalid range [$start, $end) for text length ${text.length}"
        }
        bigStyles.add(BigStyle(BigRange(start, end), spans.toList()))
        return this
    }

}