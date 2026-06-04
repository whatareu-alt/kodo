# Proguard rules for Sunnyside Weather App

# Keep kotlin serialization attributes and companion objects
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    *** Companion;
}

# Keep kotlinx serialization structures
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializer *;
}
