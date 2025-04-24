# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}


-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static d(...);
    public static w(...);
    public static v(...);
    public static i(...);
    public static e(...);
}

# Preserve generic type information for Gson
-keepattributes Signature

# Keep TypeToken class
#-keep class com.google.gson.reflect.TypeToken {
#    *;
#}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep Gson related classes and methods
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }


-keep class androidx.room.RoomDatabase { *; }
-keep class androidx.room.Room { *; }
-keep class android.arch.** { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

-keep class **.R
-keep class **.R$* {*;}
-keep class **.BuildConfig {*;}
-keep class **.Manifest {*;}

-keep class androidx.constraintlayout.motion.widget.KeyAttributes { *; }


# Keep Dependency Injection Framework related classes and methods
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class javax.annotation.** { *; }

-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.EarlyEntryPoint class *


# Keep Parcelable Classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keep class org.bouncycastle.** {*;}
-keep class org.conscrypt.** {*;}
-keep class org.openjsse.** {*;}

-keep,includedescriptorclasses class ezvcard.** { *; }
-keep enum ezvcard.VCardVersion { *; }
-dontwarn ezvcard.io.json.**
-dontwarn freemarker.**
-keep,includedescriptorclasses class ezvcard.property.** { *; }

-keepclasseswithmembernames class * { native <methods>; }

-keep class com.google.mlkit.common.** { *; }
-keep class com.google.mlkit.vision.** { *; }

-keepnames class com.google.android.gms.** {*;}

# Preserve Firebase Protobuf well-known types
-keep class com.google.firebaseprotolite.** { *; }
-keep class com.google.protobuf.** { *; }

# Preserve generated Protobuf classes and avoid obfuscation
-keep class com.google.protobuf.GeneratedMessageLite { *; }
-keep class com.google.protobuf.GeneratedMessageLite$Builder { *; }

# Prevent stripping of Firebase internal classes
-keep class com.google.firebase.** { *; }


-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }
-keep interface com.airbnb.lottie.** { *; }
-keep enum com.airbnb.lottie.** { *; }

-keep class androidx.appcompat.app.AppCompatDelegate { *; }
-keep class com.messages.data.pref.AppPreferences { *; }
