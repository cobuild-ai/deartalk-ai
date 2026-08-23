# ============================================================================
# DearTalk AI: Proguard / R8 Optimization & Keep Rules
# ============================================================================

# ----------------------------------------------------------------------------
# 1. Google LiteRT-LM & MediaPipe GenAI (Native C++ / JNI Interfaces)
# ----------------------------------------------------------------------------
-keep class com.google.ai.edge.litertlm.** { *; }
-keep interface com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

-keep class com.google.mediapipe.tasks.genai.** { *; }
-keep interface com.google.mediapipe.tasks.genai.** { *; }
-dontwarn com.google.mediapipe.tasks.genai.**

# Preserve native JNI methods across the application
-keepclasseswithmembernames class * {
    native <methods>;
}

# ----------------------------------------------------------------------------
# 2. Jetpack Compose & Kotlin Coroutines
# ----------------------------------------------------------------------------
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn kotlinx.coroutines.**

# ----------------------------------------------------------------------------
# 3. DearTalk Core Models & Data Entities
# ----------------------------------------------------------------------------
-keep class ai.deartalk.android.data.pref.** { *; }
-keep class ai.deartalk.android.data.repository.** { *; }
-keep class ai.deartalk.android.ime.HangulComposer { *; }

# Keep Enum values and valueOf methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ----------------------------------------------------------------------------
# 4. AndroidX Lifecycle & Architecture Components
# ----------------------------------------------------------------------------
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ----------------------------------------------------------------------------
# 5. Annotation Processors & Metadata dontwarn rules
# ----------------------------------------------------------------------------
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**
-dontwarn kotlin.Metadata

