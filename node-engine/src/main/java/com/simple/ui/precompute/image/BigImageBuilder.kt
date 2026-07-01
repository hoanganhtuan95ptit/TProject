package com.simple.ui.precompute.image

class BigImageBuilder(val source: Any) {

    var error: Int = 0
    var placeholder: Int = 0

    val transforms: ArrayList<BigImageTransform> = arrayListOf()

    internal fun add(vararg bigTransforms: BigImageTransform) {
        transforms.addAll(bigTransforms)
    }
}
