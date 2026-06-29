package com.simple.ui.precompute.text

import android.widget.TextView
import com.simple.ui.precompute.text.span.TextSize

fun TextView.setText(text: BigText?) {

    setText(text?.textChar)
}


private val EMPTY by lazy {
    BigText("")
}

fun emptyText() = EMPTY


fun String.toBuilder(): BigTextBuilder {

    return BigTextBuilder(this)
}

fun String.with(vararg spannable: BigSpan): BigTextBuilder {

    return toBuilder().with(*spannable)
}

fun String.withFirst(bold: String, vararg spannable: BigSpan): BigTextBuilder {

    return toBuilder().withFirst(bold, *spannable)
}

fun String.withAll(textUpdate: String, vararg spannable: BigSpan): BigTextBuilder {

    return toBuilder().withAll(textUpdate, *spannable)
}

fun BigTextBuilder.with(vararg spans: BigSpan): BigTextBuilder {

    return withFirst(text, *spans)
}

fun BigTextBuilder.withFirst(text: String, vararg spans: BigSpan): BigTextBuilder {

    if (text.isEmpty()) return this
    val start = this.text.indexOf(text)
    if (start == -1) return this

    add(BigStyle(BigRange(start, start + text.length), spans.toList()))

    return this
}

fun BigTextBuilder.withAll(text: String, vararg spans: BigSpan): BigTextBuilder {

    if (text.isEmpty()) return this
    var index = this.text.indexOf(text)
    if (index == -1) return this

    val styleList = spans.toList()
    val length = text.length
    while (index != -1) {
        add(BigStyle(BigRange(index, index + length), styleList))
        index = this.text.indexOf(text, index + length)
    }

    return this
}

fun BigTextBuilder.build(): BigText = BigText(
    text = text,
    spans = bigStyles
)


fun String.withStyleDisplayLarge(): BigTextBuilder {
    return toBuilder().with(TextSize(57))
}

fun String.withStyleDisplayMedium(): BigTextBuilder {
    return toBuilder().with(TextSize(45))
}

fun String.withStyleDisplaySmall(): BigTextBuilder {
    return toBuilder().with(TextSize(36))
}

fun String.withStyleHeadlineLarge(): BigTextBuilder {
    return toBuilder().with(TextSize(32))
}

fun String.withStyleHeadlineMedium(): BigTextBuilder {
    return toBuilder().with(TextSize(28))
}

fun String.withStyleHeadlineSmall(): BigTextBuilder {
    return toBuilder().with(TextSize(24))
}

fun String.withStyleTitleLarge(): BigTextBuilder {
    return toBuilder().with(TextSize(22))
}

fun String.withStyleTitleMedium(): BigTextBuilder {
    return toBuilder().with(TextSize(16))
}

fun String.withStyleTitleSmall(): BigTextBuilder {
    return toBuilder().with(TextSize(14))
}

fun String.withStyleBodyLarge(): BigTextBuilder {
    return toBuilder().with(TextSize(16))
}

fun String.withStyleBodyMedium(): BigTextBuilder {
    return toBuilder().with(TextSize(14))
}

fun String.withStyleBodySmall(): BigTextBuilder {
    return toBuilder().with(TextSize(12))
}

fun String.withStyleLabelLarge(): BigTextBuilder {
    return toBuilder().with(TextSize(14))
}

fun String.withStyleLabelMedium(): BigTextBuilder {
    return toBuilder().with(TextSize(12))
}

fun String.withStyleLabelSmall(): BigTextBuilder {
    return toBuilder().with(TextSize(11))
}