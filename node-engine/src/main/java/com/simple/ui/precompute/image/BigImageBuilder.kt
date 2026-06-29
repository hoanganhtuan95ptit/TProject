package com.simple.ui.precompute.image

class BigImageBuilder(val source: Any) {

    var error: Int = 0
    var placeholder: Int = 0

    val transforms: ArrayList<BigTransform> = arrayListOf()

    fun add(vararg bigTransforms: BigTransform) {
        transforms.addAll(bigTransforms)
    }
}
