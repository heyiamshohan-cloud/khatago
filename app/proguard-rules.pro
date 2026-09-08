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
