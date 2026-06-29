package com.simple.ui.precompute.image

import android.graphics.drawable.Drawable
import com.bumptech.glide.load.Transformation

open class RichTransform

interface RichTransformConvert {
    fun getGlideTransform(richTransform: RichTransform): Transformation<Drawable>?
}
