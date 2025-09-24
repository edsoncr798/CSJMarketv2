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
#-keepattributes SourceFile,LineNumberTable

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

-keep class me.relex.circleindicator.** { *; }
-keep class com.airbnb.lottie.** { *; }
-keep class org.imaginativeworld.whynotimagecarousel.** { *; }

-keep class com.visanet.** { *; }
-keep class com.cardinalcommerce.** { *; }
-keep class com.threatmetrix.** { *; }

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
