package com.simple.ui.precompute.image

import com.simple.launcher.retirement.utils.image.RichImage

private val EMPTY by lazy {
    RichImage("")
}

fun emptyImage() = EMPTY


fun String.toBuilder(): RichImageBuilder {

    return RichImageBuilder(this)
}

fun RichImageBuilder.addTransform(vararg transform: RichTransform): RichImageBuilder {

    add(*transform)
    return this
}

fun RichImageBuilder.setError(error: Int): RichImageBuilder {

    this.error =  error
    return this
}

fun RichImageBuilder.setPlaceholder(placeholder: Int): RichImageBuilder {

    this.placeholder =  placeholder
    return this
}

fun RichImageBuilder.build() = RichImage(
    source = source,
    error = error,
    placeholder = placeholder,
    transforms = transforms
)

