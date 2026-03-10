# ── Parcelable ──────────────────────────────────────────────────────────────
# PlaybackRuntimeSettings remains parcelable and is referenced across app/runtime
# boundaries; keep the full class to avoid CREATOR/name-related parcel issues.
-keep class com.asuka.player.core.PlaybackRuntimeSettings { *; }

# ── Enums in Parcels ─────────────────────────────────────────────────────────
# DoubleTapAction is packed by name through parcelization on PlayerSettings.
-keepclassmembers enum com.asuka.player.core.PlayerSettings$DoubleTapAction { *; }
