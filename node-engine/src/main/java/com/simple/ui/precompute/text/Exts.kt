package com.simple.launcher.retirement.utils.text

import android.widget.TextView

fun TextView.setText(text: RichText?) {

    setText(text?.textChar)
}


private val EMPTY by lazy {
    RichText("")
}

fun emptyText() = EMPTY


fun String.toBuilder(): RichTextBuilder {

    return RichTextBuilder(this)
}

fun String.with(vararg spannable: RichSpan): RichTextBuilder {

    return toBuilder().with(*spannable)
}

fun String.withFirst(bold: String, vararg spannable: RichSpan): RichTextBuilder {

    return toBuilder().withFirst(bold, *spannable)
}

fun String.withAll(textUpdate: String, vararg spannable: RichSpan): RichTextBuilder {

    return toBuilder().withAll(textUpdate, *spannable)
}

fun RichTextBuilder.with(vararg spans: RichSpan): RichTextBuilder {

    return withFirst(text, *spans)
}

fun RichTextBuilder.withFirst(text: String, vararg spans: RichSpan): RichTextBuilder {

    if (text.isEmpty()) return this
    val start = this.text.indexOf(text)
    if (start == -1) return this

    add(RichStyle(RichRange(start, start + text.length), spans.toList()))

    return this
}

fun RichTextBuilder.withAll(text: String, vararg spans: RichSpan): RichTextBuilder {

    if (text.isEmpty()) return this
    var index = this.text.indexOf(text)
    if (index == -1) return this

    val styleList = spans.toList()
    val length = text.length
    while (index != -1) {
        add(RichStyle(RichRange(index, index + length), styleList))
        index = this.text.indexOf(text, index + length)
    }

    return this
}

fun RichTextBuilder.build(): RichText = RichText(
    text = text,
    spans = richStyles
)


fun String.withStyleDisplayLarge(): RichTextBuilder {
    return toBuilder().with(TextSize(57))
}

fun String.withStyleDisplayMedium(): RichTextBuilder {
    return toBuilder().with(TextSize(45))
}

fun String.withStyleDisplaySmall(): RichTextBuilder {
    return toBuilder().with(TextSize(36))
}

fun String.withStyleHeadlineLarge(): RichTextBuilder {
    return toBuilder().with(TextSize(32))
}

fun String.withStyleHeadlineMedium(): RichTextBuilder {
    return toBuilder().with(TextSize(28))
}

fun String.withStyleHeadlineSmall(): RichTextBuilder {
    return toBuilder().with(TextSize(24))
}

fun String.withStyleTitleLarge(): RichTextBuilder {
    return toBuilder().with(TextSize(22))
}

fun String.withStyleTitleMedium(): RichTextBuilder {
    return toBuilder().with(TextSize(16))
}

fun String.withStyleTitleSmall(): RichTextBuilder {
    return toBuilder().with(TextSize(14))
}

fun String.withStyleBodyLarge(): RichTextBuilder {
    return toBuilder().with(TextSize(16))
}

fun String.withStyleBodyMedium(): RichTextBuilder {
    return toBuilder().with(TextSize(14))
}

fun String.withStyleBodySmall(): RichTextBuilder {
    return toBuilder().with(TextSize(12))
}

fun String.withStyleLabelLarge(): RichTextBuilder {
    return toBuilder().with(TextSize(14))
}

fun String.withStyleLabelMedium(): RichTextBuilder {
    return toBuilder().with(TextSize(12))
}

fun String.withStyleLabelSmall(): RichTextBuilder {
    return toBuilder().with(TextSize(11))
}