# Kotlin
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlin.Metadata { *; }

# Kotlin Serialization
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** { *; }
-if @kotlinx.serialization.Serializable class ** { static **$$serializer INSTANCE; }
-keep class <1>$$serializer { *; }
-keep class com.workout.shared.backup.** { *; }

# Koin
-keep class org.koin.** { *; }
-keepnames class * extends org.koin.core.module.Module

# SQLDelight
-keep class com.squareup.sqldelight.** { *; }
-keep class app.cash.sqldelight.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# App models
-keep class com.workout.core.model.** { *; }
-keep class com.workout.core.database.** { *; }
-keep class com.workout.core.repository.** { *; }
-keep class com.workout.shared.feature.**.** { *; }
-keep class com.workout.shared.onboarding.** { *; }

# Navigation routes (Compose Navigation + kotlinx.serialization)
-keep class com.workout.shared.ui.navigation.** { *; }
-keep class com.workout.shared.ui.navigation.**$$serializer { *; }

# Room (needed by WorkManager which is pulled in via Glance)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** INSTANCE;
    public static ** Companion;
}

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# androidx.startup
-keep class androidx.startup.** { *; }
-keep class * implements androidx.startup.Initializer { *; }
