# ── Parcelable ──────────────────────────────────────────────────────────────
# Manual Parcelable implementation in PlaybackRuntimeSettings uses a named
# companion object CREATOR — keep the full class to prevent R8 from renaming
# the CREATOR field or the companion class it references.
-keep class com.asuka.player.core.PlaybackRuntimeSettings { *; }

# ── Enums in Parcels ─────────────────────────────────────────────────────────
# DoubleTapAction is packed by ordinal; keep entries[] and name() intact.
-keepclassmembers enum com.asuka.player.core.PlaybackRuntimeSettings$DoubleTapAction { *; }
