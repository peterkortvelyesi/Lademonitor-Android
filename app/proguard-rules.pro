# Moshi / Retrofit reflection-based adapters
-keep class com.dominiqueherbrigpersonalteam.lademonitor.data.model.** { *; }
-keepclassmembers class ** {
    @com.squareup.moshi.Json <fields>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn okhttp3.**
-dontwarn okio.**
