# FlowReader ProGuard Rules

# Basic Android rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Room entities
-keep class com.flowreader.app.data.local.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembers class * {
    @dagger.hilt.* <fields>;
}

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class * extends kotlinx.coroutines.Job {
    volatile <fields>;
}

# Keep Readium
-keep class org.readium.** { *; }
-dontwarn org.readium.**

# Keep JSoup
-keeppackagenames org.jsoup.nodes
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.** { *; }

# Keep data classes
-keep class com.flowreader.app.domain.model.** { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Keep ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Optimization: Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimization: Remove debug checks
-assumenosideeffects class java.lang.Boolean {
    public static java.lang.Boolean getBoolean(java.lang.String);
}

# Optimization: Remove timestamp calls
-assumenosideeffects class android.os.SystemClock {
    public static long elapsedRealtime();
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove unused code
-optimizationpasses 5
-allowaccessmodification

# Remove inner class references
-dontoptimize
-dontusemixedcaseclassnames
-verbose
