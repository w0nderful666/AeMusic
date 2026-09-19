# Project-specific R8 rules for AeMusic

-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# kotlinx.serialization rules
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    public static ** Companion;
}
-keep class *$$serializer {
    *;
}
-keep class kotlinx.serialization.** { *; }

# Domain models & serialized entities
-keep class com.aemusic.core.data.** { *; }
-keep class com.aemusic.core.model.** { *; }
-keep class com.aemusic.core.database.** { *; }
-keep class com.aemusic.core.backup.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    public static *** Companion;
}
-keepclassmembers class * extends kotlinx.serialization.internal.GeneratedSerializer {
    *;
}

