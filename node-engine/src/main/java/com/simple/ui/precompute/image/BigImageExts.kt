package com.simple.ui.precompute.image

import android.widget.ImageView
import com.bumptech.glide.Glide


fun ImageView.setImage(image: BigImage) {

    Glide.with(context)
        .load(image.source)
        .transform(*image.transforms)
        .into(this)
}


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

fun BigImageBuilder.addTransform(vararg transform: BigImageTransform): BigImageBuilder {

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
    bigTransforms = transforms
)
