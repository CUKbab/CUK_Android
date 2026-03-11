# Global attributes needed for reflection and generics
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# Keep our data package completely to preserve names and generic signatures
# This includes all repository classes, data models, and the MenuDataTypeToken
-keep class com.cukbab.data.** { *; }
-keep class com.cukbab.data.MenuDataTypeToken { *; }

# Keep the coroutines flow generic signatures
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.flow.Flow

# Gson specific rules to preserve TypeToken's generic information
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Retrofit rules
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# WorkManager and Room (needed for Glance internal state)
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.work.Worker { *; }
-keep class androidx.work.ListenableWorker { *; }

# DataStore and Preferences
-keep class androidx.datastore.** { *; }
-keep class androidx.datastore.preferences.core.Preferences { *; }
-keep class androidx.datastore.preferences.core.MutablePreferences { *; }

# Glance / Widget rules
-keep class com.cukbab.widget.** { *; }
-keep class androidx.glance.** { *; }
-keep interface androidx.glance.appwidget.action.ActionCallback { *; }
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }
-keep class androidx.glance.state.PreferencesGlanceStateDefinition { *; }
-keep class androidx.glance.appwidget.GlanceAppWidgetManager { *; }
-keep class androidx.glance.appwidget.proto.** { *; }

# Compose Runtime (used by Glance for composition)
-keep class androidx.compose.runtime.** { *; }

# Keep generic signatures in com.cukbab package for Gson and Retrofit
-keepclassmembers class com.cukbab.** {
    <methods>;
}

# Line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable

# Google OSS Licenses Plugin
-keep class com.google.android.gms.oss.licenses.** { *; }

# Google Play Services base (needed for oss-licenses internal communication)
-keep class com.google.android.gms.common.** { *; }
-keep interface com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep interface com.google.android.gms.tasks.** { *; }
