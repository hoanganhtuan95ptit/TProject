package com.simple.ui.precompute.image

private val EMPTY by lazy {
    BigImage("")
}

fun emptyImage() = EMPTY


fun Int.toBuilder(): BigImageBuilder {

    return BigImageBuilder(this)
}

fun String.toBuilder(): BigImageBuilder {

    return BigImageBuilder(this)
}

fun BigImageBuilder.addTransform(vararg transform: BigTransform): BigImageBuilder {

    add(*transform)
    return this
}

fun BigImageBuilder.setError(error: Int): BigImageBuilder {

    this.error = error
    return this
}

fun BigImageBuilder.setPlaceholder(placeholder: Int): BigImageBuilder {

    this.placeholder = placeholder
    return this
}

fun BigImageBuilder.build() = BigImage(
    source = source,
    error = error,
    placeholder = placeholder,
    transforms = transforms
)
