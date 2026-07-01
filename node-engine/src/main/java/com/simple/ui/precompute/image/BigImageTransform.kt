package com.simple.ui.precompute.image

import android.graphics.Bitmap
import com.bumptech.glide.load.Transformation
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

interface BigImageTransform

interface BigImageTransformConvert {

    fun convert(transform: BigImageTransform): Transformation<Bitmap>?

    companion object {

        private val CONVERTS by lazy {

            try {

                ServiceLoader.load(BigImageTransformConvert::class.java).toList()
            } catch (_: ServiceConfigurationError) {

                emptyList()
            }
        }

        private val CACHES = ConcurrentHashMap<KClass<out BigImageTransform>, BigImageTransformConvert>()


        fun List<BigImageTransform>.convert() = mapNotNull { transform ->

            transform.convert()
        }.toTypedArray()

        fun BigImageTransform.convert() = this::class.let { type ->

            CACHES[type]?.convert(this) ?: CONVERTS.firstNotNullOfOrNull { converter ->

                converter.convert(this)?.also {
                    CACHES[type] = converter
                }
            }
        }
    }
}
