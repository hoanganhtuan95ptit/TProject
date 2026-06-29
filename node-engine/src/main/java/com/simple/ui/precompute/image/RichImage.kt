package com.simple.ui.precompute.image

data class RichImage(
    val source: Any,
    val error: Int = 0,
    val placeholder: Int = 0,
    val transforms: List<RichTransform> = emptyList()
)