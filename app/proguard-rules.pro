# ── Parcelable ──────────────────────────────────────────────────────────────
# Manual Parcelable implementation in PlayerRuntimeSettings uses a named
# companion object CREATOR — keep the full class to prevent R8 from renaming
# the CREATOR field or the companion class it references.
-keep class com.asuka.player.ui.PlayerRuntimeSettings { *; }

# ── Enums in Parcels ─────────────────────────────────────────────────────────
# DoubleTapAction is packed by ordinal; keep entries[] and name() intact.
-keepclassmembers enum com.asuka.player.ui.PlayerRuntimeSettings$DoubleTapAction { *; }

# ── Kotlin object singletons ─────────────────────────────────────────────────
# Cross-module access via INSTANCE field. PlaybackStoreProvider.activityClass
# is a @Volatile var — keep it so PlaybackService can read it.
-keepclassmembers class com.asuka.player.core.PlaybackStoreProvider {
    public static final com.asuka.player.core.PlaybackStoreProvider INSTANCE;
}
-keepclassmembers class com.asuka.player.core.service.PlaybackService {
    public static volatile java.lang.Class activityClass;
}
