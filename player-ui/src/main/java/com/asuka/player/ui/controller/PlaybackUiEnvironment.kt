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

interface PlaybackDeviceController {
    fun currentVolumePercent(): Int
    fun setVolumePercent(percent: Int)
    fun currentBrightnessPercent(): Int
    fun setBrightnessPercent(percent: Int)
}

interface PlaybackTrackSelectionController {
    fun setAudioTrack(groupIndex: Int, trackIndex: Int)
    fun setSubtitleTrack(groupIndex: Int, trackIndex: Int)
    fun disableSubtitles()
}

interface PlaybackUiPersistence {
    fun readZoom(mediaId: String): Float?
    fun savePlaybackSpeed(mediaId: String, speed: Float)
    fun saveAudioTrack(mediaId: String, trackId: String)
    fun saveSubtitleTrack(mediaId: String, trackId: String)
    fun disableSubtitles(mediaId: String)
    fun saveZoom(mediaId: String, zoom: Float)
}
