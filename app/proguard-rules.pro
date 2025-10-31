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

-dontwarn com.cardinalcommerce.dependencies.internal.minidev.asm.Accessor
-dontwarn com.cardinalcommerce.dependencies.internal.minidev.asm.BeansAccess
-dontwarn com.cardinalcommerce.dependencies.internal.minidev.asm.ConvertDate
-dontwarn com.cardinalcommerce.dependencies.internal.minidev.asm.FieldFilter
-dontwarn com.squareup.okhttp.Cache
-dontwarn com.squareup.okhttp.CacheControl$Builder
-dontwarn com.squareup.okhttp.CacheControl
-dontwarn com.squareup.okhttp.Call
-dontwarn com.squareup.okhttp.OkHttpClient
-dontwarn com.squareup.okhttp.Request$Builder
-dontwarn com.squareup.okhttp.Request
-dontwarn com.squareup.okhttp.Response
-dontwarn com.squareup.okhttp.ResponseBody

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Google Play In-App Update
-keep class com.google.android.play.** { *; }

# Keep AndroidX Credentials and Google ID libraries
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep Glide (usually covered by consumer rules, but reinforce)
-keep class com.bumptech.glide.** { *; }
-keep class com.bumptech.glide.load.resource.bitmap.** { *; }
-keep class com.bumptech.glide.load.resource.drawable.** { *; }

# Keep Gson models (reflection-based)
-keep class com.csj.csjmarket.modelos.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep retrofit/okhttp interfaces and annotations
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Keep third-party UI libs that may use reflection
-keep class org.imaginativeworld.whynotimagecarousel.** { *; }
-keep class me.relex.circleindicator.** { *; }

# Keep cryptography libs
-keep class com.nimbusds.** { *; }
-keep class org.bouncycastle.** { *; }

# Keep Google Tink classes used by Nimbus JOSE
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.cardinalcommerce.dependencies.internal.bouncycastle.openssl.jcajce.JcaPEMKeyConverter

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

-keep class androidx.multidex.** { *; }

-dontwarn com.android.volley.toolbox.**
-keep class com.android.volley.** { *; }
-dontwarn com.android.volley.**
-keep class me.relex.circleindicator.** { *; }
-keep class com.airbnb.lottie.** { *; }
-keep class org.imaginativeworld.whynotimagecarousel.** { *; }

-dontwarn com.visanet.**
-dontwarn lib.visanet.**
-dontwarn com.cardinalcommerce.**
-dontwarn com.threatmetrix.**
-dontwarn com.lexisnexisrisk.threatmetrix.**

# Mantener clases usadas con Gson
-keep class com.google.gson.stream.** { *; }
-keepattributes *Annotation*

# Mantener tus modelos
-keep class com.csj.csjmarket.modelos.** { *; }

# Gson uses generic type information stored in a class file when working with
# fields. Proguard removes such information by default, keep it.
-keepattributes Signature

# This is also needed for R8 in compat mode since multiple
# optimizations will remove the generic signature such as class
# merging and argument removal. See:
# https://r8.googlesource.com/r8/+/refs/heads/main/compatibility-faq.md#troubleshooting-gson-gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Optional. For using GSON @Expose annotation
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations
-keep class com.google.gson.reflect.TypeToken { <fields>; }
-keepclassmembers class **$TypeAdapterFactory { <fields>; }

# Suprimir logs en release: eliminar llamadas a android.util.Log
-assumenosideeffects class android.util.Log {
    public static int v(java.lang.String, java.lang.String);
    public static int v(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int d(java.lang.String, java.lang.String);
    public static int d(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int i(java.lang.String, java.lang.String);
    public static int i(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int w(java.lang.String, java.lang.String);
    public static int w(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int e(java.lang.String, java.lang.String);
    public static int e(java.lang.String, java.lang.String, java.lang.Throwable);
}

# Reglas faltantes para Niubiz SDK 2.2.1 (R8 missing classes)
-dontwarn com.mastercard.sonic.controller.SonicController
-dontwarn com.mastercard.sonic.controller.SonicEnvironment
-dontwarn com.mastercard.sonic.controller.SonicType
-dontwarn com.mastercard.sonic.listeners.OnPrepareListener
-dontwarn com.mastercard.sonic.model.SonicMerchant$Builder
-dontwarn com.mastercard.sonic.model.SonicMerchant
-dontwarn com.mastercard.sonic.widget.SonicBackground
-dontwarn com.mastercard.sonic.widget.SonicView
-dontwarn com.visa.CheckmarkMode
-dontwarn com.visa.CheckmarkTextOption
-dontwarn com.visa.SensoryBrandingView
