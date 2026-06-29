package com.simple.launcher.retirement.utils.image

/**
 * Marker cho một transform áp vào [RichImage].
 *
 * Engine cố tình **không biết** Glide / Coil / framework load ảnh nào.
 * Caller tự định nghĩa subclass cụ thể (vd `CircleCrop : RichTransform()`)
 * trong module ứng dụng, và `ImageLoader` của họ tự map sang transform
 * tương ứng của framework.
 *
 * Vì vậy không có interface `RichTransformConvert` ở tầng engine; nếu app
 * cần converter, tự khai báo trong module :app.
 */
open class RichTransform
