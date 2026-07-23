# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools proguard-defaults.txt.

# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Firebase model classes
-keep class com.google.firebase.** { *; }

# Keep Whis core contracts
-keep class com.whis.app.core.** { *; }
