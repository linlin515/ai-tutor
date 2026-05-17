# ============================================
# Ai Tutor — ProGuard / R8 混淆保留规则
# ============================================
# 生成时间: 2026-05-16
# 覆盖: Retrofit, OkHttp, Gson, Room, Hilt/Dagger,
#       Kotlin Coroutines, Compose, CameraX, ML Kit,
#       Coil, DataStore, Navigation Compose
# ============================================

# ====================
# 通用 — 调试符号
# ====================
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes Exceptions

# ====================
# Release 日志清理 — 移除 Log.d / Log.v 调用
# ====================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static java.lang.String getStackTraceString(java.lang.Throwable);
}

# ====================
# CrashHandler — 崩溃监控保留规则
# ====================
-keep class com.aitutor.app.data.local.CrashHandler { *; }
-keepclassmembers class com.aitutor.app.data.local.CrashHandler$Companion {
    *;
}
-keepclassmembers class com.aitutor.app.ui.settings.CrashLogViewModel { *; }

# ====================
# 应用主包 — 保留所有类（按需细化）
# ====================
-keep class com.aitutor.app.** { *; }

# ====================
# Gson — 序列化保留规则
# ====================
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}
# Prevent Gson from stripping TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# ====================
# Retrofit — API 接口保留
# ====================
-keep,allowobfuscation,allowshrinking interface com.aitutor.app.data.remote.api.*
-keepclassmembers,allowshrinking,allowobfuscation interface com.aitutor.app.data.remote.api.* {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# Retrofit converter
-keep class retrofit2.converter.gson.** { *; }

# ====================
# OkHttp — 保留核心类
# ====================
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep class okhttp3.sse.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okio.**

# ====================
# Room — 实体、DAO、数据库
# ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# ====================
# Hilt / Dagger — DI 框架
# ====================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keepclassmembers class * {
    @dagger.hilt.android.qualifiers.* <fields>;
}
-keepclassmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <init>(...);
}
-keepclassmembers class * {
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
    @dagger.Module <fields>;
}
-dontwarn dagger.hilt.**
-dontwarn hilt.**
# 保留 Hilt 生成代码
-keep class * {
    @dagger.hilt.* <fields>;
    @dagger.hilt.* <methods>;
}
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.hiltcomponents.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }

# ====================
# Kotlin Coroutines
# ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ====================
# Kotlin 标准库 / 反射
# ====================
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.**

# ====================
# Kotlin Serialization (如有)
# ====================
-keepclassmembers class kotlinx.serialization.json.** { *; }
-dontwarn kotlinx.serialization.**

# ====================
# Jetpack Compose
# ====================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
# 保留 Composable 函数签名
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
# Compose Navigation
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ====================
# CameraX
# ====================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class * extends androidx.camera.core.ImageAnalysis.Analyzer { *; }

# ====================
# ML Kit (文字识别)
# ====================
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ====================
# Coil (图片加载)
# ====================
-keep class coil.** { *; }
-dontwarn coil.**

# ====================
# DataStore
# ====================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ====================
# Security Crypto
# ====================
-keep class androidx.security.** { *; }
-dontwarn androidx.security.**

# ====================
# AndroidX / Android 核心库
# ====================
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**
-keep class androidx.activity.** { *; }
-dontwarn androidx.activity.**

# ====================
# 枚举类 — 保留 values() / valueOf()
# ====================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ====================
# Parcelable / Serializable
# ====================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ====================
# 资源 & R 文件
# ====================
-keep class **.R$* { *; }
-keepclassmembers class **.R$* { *; }

# ====================
# 第三方库常见警告忽略
# ====================
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ====================
# WebSocket / SSE (OkHttp SSE)
# ====================
-keep class okhttp3.internal.sse.** { *; }

# ====================
# Tink / Security Crypto — 缺失类忽略
# ====================
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant

# ====================
# Gson 类型适配器 — 保留自定义的 TypeAdapter / JsonDeserializer
# ====================
-keep class * implements com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }

# ====================
# 应用自定义数据模型 — 安全网
# ====================
# DTO 数据类（ApiResponse, PaginatedData, AuthDtos, ChatDtos 等）
-keep class com.aitutor.app.data.remote.dto.** { *; }
# 媒体模型
-keep class com.aitutor.app.data.media.** { *; }
# 领域模型
-keep class com.aitutor.app.domain.model.** { *; }
# 本地实体
-keep class com.aitutor.app.data.local.entity.** { *; }

# ====================
# R8 全模式 — 避免过度混淆
# ====================
# 保持所有 View/Composable/Activity/Fragment 的类名
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }
-keep class * extends android.view.View { *; }
