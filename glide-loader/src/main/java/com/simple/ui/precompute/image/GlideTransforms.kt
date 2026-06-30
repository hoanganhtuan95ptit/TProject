package com.simple.ui.precompute.image

import android.graphics.Bitmap
import com.bumptech.glide.load.Transformation
import com.simple.ui.precompute.image.BigTransformConverters.build
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

/**
 * Map từ [BigTransform] (engine, không biết Glide) sang [Transformation]
 * của Glide. Module Glide loader sở hữu interface này — engine không phụ thuộc.
 *
 * Cách mở rộng: tạo class kế thừa [BigTransform] + một converter tương ứng
 * (hoặc gom vào 1 converter chung), annotate bằng AutoService.
 */
interface BigTransformConvert {
    fun convert(transform: BigTransform): Transformation<Bitmap>?
}

private val bigTransformConvertList by lazy {
    try {
        ServiceLoader.load(BigTransformConvert::class.java).toList()
    } catch (_: ServiceConfigurationError) {
        emptyList()
    }
}

private val bigTransformConvertCache =
    ConcurrentHashMap<kotlin.reflect.KClass<out BigTransform>, BigTransformConvert>()

/**
 * Registry. Engine giữ [BigImage.transforms] dạng list marker;
 * loader gọi [build] để biến thành một transform Glide thật.
 */
object BigTransformConverters {

    /**
     * Build [Transformation] tổng hợp từ [BigImage.transforms]; trả về `null`
     * nếu list rỗng hoặc không có converter nào match.
     */
    fun build(transforms: List<BigTransform>): List<Transformation<Bitmap>> {
        if (transforms.isEmpty()) return emptyList()
        return transforms.mapNotNull { it.toGlideTransformation() }
    }

    private fun BigTransform.toGlideTransformation(): Transformation<Bitmap>? {
        val klass = this::class
        val cached = bigTransformConvertCache[klass]
        if (cached != null) {
            cached.convert(this)?.let { return it }
            bigTransformConvertCache.remove(klass, cached)
        }

        for (converter in bigTransformConvertList) {
            val transform = converter.convert(this)
            if (transform != null) {
                bigTransformConvertCache.putIfAbsent(klass, converter)
                return transform
            }
        }
        return null
    }
}
