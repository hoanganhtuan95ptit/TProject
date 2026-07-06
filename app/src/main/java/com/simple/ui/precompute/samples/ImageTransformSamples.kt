package com.simple.ui.precompute.samples

import android.graphics.Color
import android.graphics.Typeface
import com.simple.t.R
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.CircleCrop
import com.simple.ui.precompute.image.RoundedCorners
import com.simple.ui.precompute.image.addTransform
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.toBuilder
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearChild
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.BigText

/**
 * DEMO ⑥ — [ImageNode] + BigTransform (CircleCrop / RoundedCorners).
 *
 * Cùng 1 nguồn ảnh, 3 cách biến đổi khác nhau xếp ngang.
 */
class ImageTransformSamples(m: SampleMetrics) : SampleBuilder(m) {

    fun buildCard(iconSizePx: Int): LayoutNode {
        val baseSource = R.mipmap.ic_launcher
        val rounded = dp(12)

        return LinearNode(
            orientation = Orientation.HORIZONTAL,
            crossAlign = CrossAlign.CENTER,
            gap = dp(20),
            padding = EdgeInsets.all(dp(16)),
            children = listOf(
                LinearChild(node = item("Original", BigImage(source = baseSource), iconSizePx)),
                LinearChild(
                    node = item(
                        "CircleCrop",
                        R.drawable.image.toBuilder().addTransform(CircleCrop).build(),
                        iconSizePx
                    )
                ),
                LinearChild(
                    node = item(
                        "Rounded ${rounded}px",
                        R.drawable.img1.toBuilder().addTransform(RoundedCorners(rounded)).build(),
                        iconSizePx
                    )
                )
            )
        )
    }

    private fun item(label: String, image: BigImage, iconSizePx: Int): LayoutNode = LinearNode(
        orientation = Orientation.VERTICAL,
        crossAlign = CrossAlign.CENTER,
        gap = dp(6),
        children = listOf(
            LinearChild(
                node = ImageNode(
                    source = image,
                    layoutWidth = LayoutDimension.Fixed(iconSizePx),
                    layoutHeight = LayoutDimension.Fixed(iconSizePx)
                )
            ),
            LinearChild(
                node = TextNode(
                    text = BigText(label),
                    textSizePx = sp(11f),
                    color = Color.DKGRAY,
                    typeface = Typeface.DEFAULT_BOLD,
                    maxLines = 1
                )
            )
        )
    )
}
