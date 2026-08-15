##############################################################################
# NS Vault — R8 / ProGuard configuration
#
# Most libraries (Hilt, Room, Media3, DataStore, Coroutines, Compose)
# ship their own consumer rules, so this file only covers what is specific
# to this app: reflection-driven kotlinx.serialization, readable crash
# traces, and a few defensive keeps.
##############################################################################

# --- Crash readability -------------------------------------------------------
# Keep source file + line numbers so Play Console stack traces deobfuscate
# against the mapping file. (SourceFile is renamed to hide original names.)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Runtime-visible annotations are used by serialization and DI.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature,Exceptions

# --- kotlinx.serialization ---------------------------------------------------
# We only serialize our own @Serializable types (type-safe nav Routes,
# VaultFileMetadata, the PIN record). Keep their generated serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.nsvault.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.nsvault.app.**
-keepclassmembers class com.nsvault.app.**$$serializer {
    *** INSTANCE;
}
-dontnote kotlinx.serialization.**

# --- Enums persisted by name (Room converter, DataStore) ---------------------
# valueOf()/name are called directly, but keep values() defensively so R8
# never strips constants referenced only through persisted strings.
-keepclassmembers enum com.nsvault.app.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Media3 custom DataSource ------------------------------------------------
# Instantiated directly (no reflection); nothing extra needed, but keep the
# UnstableApi surface stable against aggressive optimization of overrides.
-keep class com.nsvault.app.data.playback.VaultDataSource { *; }

# --- Coroutines --------------------------------------------------------------
-dontwarn kotlinx.coroutines.**

# --- General safety ----------------------------------------------------------
# Do not touch classes referenced only from the (offline) manifest/resources.
-keep class com.nsvault.app.NSVaultApplication
-keep class com.nsvault.app.MainActivity
-keep class com.nsvault.app.data.audio.RecordingService
