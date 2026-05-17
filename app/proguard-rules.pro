# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK's proguard-android-optimize.txt file.

# Keep Hilt entry points
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.InstallIn class *

# Keep Room entities
-keep class com.metashield.app.data.db.entity.** { *; }

# Keep metadata models
-keep class com.metashield.app.data.model.** { *; }

# JAudioTagger
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Apache Commons Imaging
-keep class org.apache.commons.imaging.** { *; }
-dontwarn org.apache.commons.imaging.**

# MP4Parser
-keep class org.mp4parser.** { *; }
-dontwarn org.mp4parser.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep enum names for Room and DataStore
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }

-dontwarn java.awt.**
-dontwarn javax.imageio.**

# PDFBox-Android
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn com.sun.jna.**

# Gson rules to preserve generic signatures for TypeToken and prevent R8 crashes
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class com.metashield.app.data.model.MetadataField { *; }
-keep class * extends com.google.gson.reflect.TypeToken

