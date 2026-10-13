# ProGuard rules for LEGO Dimensions NFC
-keep class com.ldnfc.app.data.model.** { *; }
-keep class com.ldnfc.app.crypto.** { *; }
-keep class com.ldnfc.app.nfc.** { *; }
-keep class com.ldnfc.app.data.repository.** { *; }

-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

-keepattributes *Annotation*
-dontwarn java.lang.management.**
