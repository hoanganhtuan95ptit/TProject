# Keep ServiceLoader providers for BigTransformConvert.
-keepnames class com.simple.ui.precompute.image.BigTransformConvert
-keep class * implements com.simple.ui.precompute.image.BigTransformConvert {
    <init>();
}
-keepclassmembers class * implements com.simple.ui.precompute.image.BigTransformConvert {
    <init>();
}
