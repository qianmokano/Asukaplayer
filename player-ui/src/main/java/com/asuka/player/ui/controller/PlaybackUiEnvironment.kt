package com.asuka.player.ui.controller

data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val selectionId: String,
)

data class PlaybackTrackUiState(
    val currentMediaId: String? = null,
    val currentSpeed: Float = 1.0f,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val selectedAudio: Int? = null,
    val selectedSubtitle: Int? = null,
)

interface PlaybackTrackSelectionController {
    fun setAudioTrack(groupIndex: Int, trackIndex: Int)
    fun setSubtitleTrack(groupIndex: Int, trackIndex: Int)
    fun disableSubtitles()
}
