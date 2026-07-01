package com.simple.ui.precompute.text

import android.text.style.CharacterStyle
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

interface BigImageSpan

interface BigImageSpanConvert {

    fun convert(bigSpan: BigImageSpan): CharacterStyle?

    companion object {

        private val CONVERTS by lazy {

            try {

                ServiceLoader.load(BigImageSpanConvert::class.java).toList()
            } catch (_: ServiceConfigurationError) {

                emptyList()
            }
        }

        private val CACHES = ConcurrentHashMap<KClass<out BigImageSpan>, BigImageSpanConvert>()


        fun List<BigImageSpan>.convert() = mapNotNull { transform ->

            transform.convert()
        }

        fun BigImageSpan.convert() = this::class.let { type ->

            CACHES[type]?.convert(this) ?: CONVERTS.firstNotNullOfOrNull { converter ->

                converter.convert(this)?.also {
                    CACHES[type] = converter
                }
            }
        }
    }
}