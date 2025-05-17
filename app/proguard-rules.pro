# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.kilagee.onelove.**$$serializer { *; }
-keepclassmembers class com.kilagee.onelove.** {
    *** Companion;
}
-keepclasseswithmembers class com.kilagee.onelove.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ViewModel and LiveData
-keep class androidx.lifecycle.** { *; }
-keep class androidx.arch.core.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager.ViewWithFragmentComponentBuilderEntryPoint { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# Retrofit and OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# Stripe
-keep class com.stripe.android.** { *; }
-dontwarn com.stripe.android.**

# Accompanist
-keep class com.google.accompanist.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn io.coil-kt.**

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }

# Threetenbp
-keep class org.threeten.bp.** { *; }
-dontwarn org.threeten.bp.**

# Shimmer
-keep class com.valentinilk.shimmer.** { *; }

# Keep model classes
-keep class com.kilagee.onelove.data.model.** { *; }