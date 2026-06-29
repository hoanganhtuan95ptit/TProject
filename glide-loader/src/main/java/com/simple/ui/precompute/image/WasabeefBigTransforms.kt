package com.simple.ui.precompute.image

/** Crop ảnh về hình tròn. */
object CircleCrop : BigTransform()

/** Crop ảnh về hình vuông bằng cạnh lớn hơn của output. */
object CropSquare : BigTransform()

/** Crop ảnh về kích thước chỉ định. `0` nghĩa là dùng kích thước ảnh gốc ở chiều đó. */
data class Crop(
    val widthPx: Int,
    val heightPx: Int,
    val cropType: CropType = CropType.CENTER
) : BigTransform()

enum class CropType {
    TOP,
    CENTER,
    BOTTOM
}

/** Crop tròn kèm viền. */
data class CircleCropWithBorder(
    val borderSizePx: Int,
    val borderColor: Int
) : BigTransform()

/** Bo góc theo bán kính, margin và kiểu góc. */
data class RoundedCorners(
    val radiusPx: Int,
    val marginPx: Int = 0,
    val cornerType: CornerType = CornerType.ALL
) : BigTransform()

enum class CornerType {
    ALL,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    OTHER_TOP_LEFT,
    OTHER_TOP_RIGHT,
    OTHER_BOTTOM_LEFT,
    OTHER_BOTTOM_RIGHT,
    DIAGONAL_FROM_TOP_LEFT,
    DIAGONAL_FROM_TOP_RIGHT
}

/** Blur ảnh. `radius` của Wasabeef hợp lệ trong khoảng 1..25. */
data class Blur(
    val radius: Int = 25,
    val sampling: Int = 1
) : BigTransform()

/** Áp màu lên ảnh bằng PorterDuff SRC_ATOP. */
data class ColorFilter(
    val color: Int
) : BigTransform()

/** Chuyển ảnh sang grayscale. */
object Grayscale : BigTransform()

/** Áp mask drawable resource lên ảnh. */
data class Mask(
    val maskResId: Int
) : BigTransform()
