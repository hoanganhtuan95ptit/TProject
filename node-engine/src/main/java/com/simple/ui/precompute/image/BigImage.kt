package com.simple.ui.precompute.image

import com.simple.ui.precompute.image.BigImageTransformConvert.Companion.convert

data class BigImage(
    val source: Any,
    val error: Int = 0,
    val placeholder: Int = 0,
    val bigTransforms: List<BigImageTransform> = emptyList()
) {

    internal val transforms = bigTransforms.convert()
}