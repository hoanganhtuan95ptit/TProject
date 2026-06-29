package com.simple.launcher.retirement.utils.image

data class RichImage(
    val source: Any,
    val error: Int = 0,
    val placeholder: Int = 0,
    val transforms: List<RichTransform> = emptyList()
)