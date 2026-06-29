# Add project specific ProGuard rules here.

-keepnames class com.simple.ui.precompute.text.BigSpanConvert
-keep class * implements com.simple.ui.precompute.text.BigSpanConvert {
    <init>();
}
-keepclassmembers class * implements com.simple.ui.precompute.text.BigSpanConvert {
    <init>();
}
