# Keep classes for Retrofit JSON serialization
-keep class com.sentinel.core.network.** { *; }

# Keep Room database entities
-keep class com.sentinel.core.memory.entity.** { *; }

# AndroidX Navigation, Compose, and DataStore standard limits
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
