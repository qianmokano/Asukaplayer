package com.asuka.player.contract

data class PersistedTrackSelection(
    val stableId: String,
) {
    val isDisabledSubtitle: Boolean
        get() = stableId == DISABLED_SUBTITLE_ID

    companion object {
        const val DISABLED_SUBTITLE_ID = "track-selection:v1:subtitle-disabled"

        fun disabledSubtitle(): PersistedTrackSelection = PersistedTrackSelection(DISABLED_SUBTITLE_ID)
    }
}
