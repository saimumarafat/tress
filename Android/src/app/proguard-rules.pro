# Preserve Jetpack Compose runtime & AndroidLiquidGlass backdrop modifiers
-keep class androidx.compose.** { *; }
-keep class com.kyant.backdrop.** { *; }

# Keep @Keep and native/JNI entry points (LiteRT-LM, MediaPipe, Icing, SQLite vector store, PDF)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    native <methods>;
}
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class com.google.mediapipe.** { *; }
-keep class com.google.android.libraries.** { *; }
-keep class com.google.mlkit.** { *; }

# Gson / Moshi / Kotlinx Serialization models
-keep class com.google.ai.edge.gallery.data.** { *; }
-keep class com.google.ai.edge.gallery.proto.** { *; }
-keep class com.google.ai.edge.gallery.tools.** { *; }
-keep class com.google.ai.edge.gallery.intents.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass <fields>;
}
-keepclassmembers class **$Companion {
    *** serializer(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Protobuf Lite
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Ktor / CIO / Coroutines / MCP
-dontwarn io.ktor.**
-dontwarn io.modelcontextprotocol.**
-dontwarn kotlinx.atomicfu.**
-dontwarn org.slf4j.**
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.mediapipe.proto.CalculatorProfileProto$CalculatorProfile
-dontwarn com.google.mediapipe.proto.GraphTemplateProto$CalculatorGraphTemplate
-dontwarn com.google.crypto.tink.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Strip verbose debug logs in R8 release builds for smaller DEX and smoother runtime
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
