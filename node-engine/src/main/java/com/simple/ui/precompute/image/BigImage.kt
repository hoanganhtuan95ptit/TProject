package com.simple.ui.precompute.image

data class BigImage(
    val source: Any,
    val error: Int = 0,
    val placeholder: Int = 0,
    val transforms: List<BigTransform> = emptyList()
)