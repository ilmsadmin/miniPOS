# Add project specific ProGuard rules here.
-keep class com.minipos.data.database.entity.** { *; }
-keep class com.minipos.domain.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Google API Client
-keep class com.google.api.** { *; }
