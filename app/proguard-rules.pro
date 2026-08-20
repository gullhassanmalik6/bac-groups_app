-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.cryptopos.pos.data.remote.dto.** { *; }
-keep interface com.cryptopos.pos.data.remote.api.** { *; }
-keep class woyou.aidlservice.jiuiv5.** { *; }
-dontwarn woyou.aidlservice.jiuiv5.**
-keep class androidx.datastore.** { *; }
-keep class androidx.security.crypto.** { *; }
