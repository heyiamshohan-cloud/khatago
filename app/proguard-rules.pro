# KhataGo release shrinker configuration.
# Keep Room generated code, Kotlin serialization models and backup models intact.
-keep class com.shohan.khatago.data.local.db.entities.** { *; }
-keep class com.shohan.khatago.data.backup.** { *; }
-keepclassmembers class * {
    @com.shohan.khatago.core.money.* <methods>;
}
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepclasseswithmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn org.bouncycastle.**
-dontnote androidx.room.paging.**

# kotlinx.serialization: generated serializers are looked up through the
# companion object, so keep companions and serializer() factories for the app's
# own models (backup files are decoded through them).
-keepclassmembers class com.shohan.khatago.** {
    *** Companion;
}
-keepclasseswithmembers class com.shohan.khatago.** {
    kotlinx.serialization.KSerializer serializer(...);
}
