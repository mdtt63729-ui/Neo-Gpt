# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Kotlin Serialization
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.altrex.mobile.**$$serializer { *; }
-keepclassmembers class com.altrex.mobile.** {
    *** Companion;
}
-keepclasseswithmembers class com.altrex.mobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
