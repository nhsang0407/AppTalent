# ShopLens AI ProGuard rules
# Keep Firestore model classes (reflection-based serialization)
-keepclassmembers class com.shoplens.ai.model.** {
    *;
}
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
