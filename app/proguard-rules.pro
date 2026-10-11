# ProGuard rules for LEGO Dimensions NFC
-keep class com.ldnfc.app.data.model.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
