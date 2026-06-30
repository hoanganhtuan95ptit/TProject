package com.simple.ui.precompute

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.BigTransformConverters


fun ImageView.setImage(image: BigImage) {

    val transforms = BigTransformConverters.build(image.transforms).toTypedArray()

    Glide.with(context)
        .load(image.source)
        .transform(*transforms)
        .into(this)
}
