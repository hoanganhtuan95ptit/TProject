package com.simple.ui.precompute.image

import android.graphics.Bitmap
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop as GlideCircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners as GlideRoundedCorners
import com.simple.launcher.retirement.utils.image.RichImage
import com.simple.launcher.retirement.utils.image.RichTransform

/**
 * Map từ [RichTransform] (engine, không biết Glide) sang [Transformation]
 * của Glide. Tầng :app sở hữu interface này — engine không phụ thuộc.
 *
 * Cách mở rộng: tạo class kế thừa [RichTransform] + một converter tương ứng
 * (hoặc gom vào 1 converter chung) rồi thêm vào [RichTransformConverters.ALL].
 */
interface RichTransformConvert {
    fun convert(transform: RichTransform): Transformation<Bitmap>?
}

// ── Built-in transforms ─────────────────────────────────────────────────────

/** Crop tròn. */
object CircleCrop : RichTransform()

/** Bo góc theo bán kính (px). */
data class RoundedCorners(val radiusPx: Int) : RichTransform()

// ── Converters ──────────────────────────────────────────────────────────────

private object BuiltInConvert : RichTransformConvert {
    private val circleCrop by lazy { GlideCircleCrop() }

    override fun convert(transform: RichTransform): Transformation<Bitmap>? = when (transform) {
        is CircleCrop -> circleCrop
        is RoundedCorners -> GlideRoundedCorners(transform.radiusPx.coerceAtLeast(0))
        else -> null
    }
}

/**
 * Registry. Engine giữ [RichImage.transforms] dạng list marker;
 * loader gọi [build] để biến thành một transform Glide thật.
 */
object RichTransformConverters {

    /**
     * Cho phép app thêm converter của riêng họ (vd custom blur, vignette).
     * Thread-safe — chỉ append trước khi load ảnh đầu tiên là an toàn.
     */
    private val extra = mutableListOf<RichTransformConvert>()

    fun register(convert: RichTransformConvert) {
        synchronized(extra) { extra.add(convert) }
    }

    /**
     * Build [Transformation] tổng hợp từ [RichImage.transforms]; trả về `null`
     * nếu list rỗng hoặc không có converter nào match.
     */
    fun build(transforms: List<RichTransform>): Transformation<Bitmap>? {
        if (transforms.isEmpty()) return null
        val mapped = transforms.mapNotNull { t ->
            BuiltInConvert.convert(t)
                ?: synchronized(extra) { extra.firstNotNullOfOrNull { it.convert(t) } }
        }
        return when (mapped.size) {
            0 -> null
            1 -> mapped[0]
            else -> MultiTransformation(*mapped.toTypedArray())
        }
    }
}
