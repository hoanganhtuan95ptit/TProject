package com.simple.ui.precompute.image

class RichImageBuilder(val source: Any) {

    var error: Int = 0
    var placeholder: Int = 0

    val transforms: ArrayList<RichTransform> = arrayListOf()

    fun add(vararg richTransforms: RichTransform) {
        transforms.addAll(richTransforms)
    }
}
