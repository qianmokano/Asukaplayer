package com.asuka.player.ui.controller

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.asuka.player.platform.TrackInfoReader
import com.asuka.player.platform.TrackSelectionStateReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlaybackTrackUiStateHolder(private val player: Player) : Player.Listener {
    private val trackInfoReader = TrackInfoReader(player)
    private val trackSelectionStateReader = TrackSelectionStateReader(player)
    private val _state = MutableStateFlow(readState())

    val state: StateFlow<PlaybackTrackUiState> = _state

    fun attach() {
        player.addListener(this)
        _state.value = readState()
    }

    fun detach() {
        player.removeListener(this)
    }

    override fun onTracksChanged(tracks: Tracks) {
        _state.value = readState()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        _state.value = readState()
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        _state.value = _state.value.copy(
            currentMediaId = player.currentMediaItem?.mediaId,
            currentSpeed = playbackParameters.speed,
        )
    }

    private fun readState(): PlaybackTrackUiState {
        val tracks = trackInfoReader.listTracks()
        val selected = trackSelectionStateReader.read()
        val audio = selected.firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
        val text = selected.firstOrNull { it.type == C.TRACK_TYPE_TEXT }
        val subtitlesDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        val hasTextTracks = player.currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT }

        return PlaybackTrackUiState(
            currentMediaId = player.currentMediaItem?.mediaId,
            currentSpeed = player.playbackParameters.speed,
            audioTracks = tracks.filter { it.type == C.TRACK_TYPE_AUDIO }.map(TrackInfoReader.TrackInfo::toTrackOption),
            subtitleTracks = tracks.filter { it.type == C.TRACK_TYPE_TEXT }.map(TrackInfoReader.TrackInfo::toTrackOption),
            selectedAudio = SelectionStateResolver.audioSelection(audio),
            selectedSubtitle = SelectionStateResolver.subtitleSelection(
                selected = text,
                subtitlesDisabled = subtitlesDisabled,
                hasTextTracks = hasTextTracks,
            ),
        )
    }
}

private fun TrackInfoReader.TrackInfo.toTrackOption(): TrackOption {
    return TrackOption(
        groupIndex = groupIndex,
        trackIndex = trackIndex,
        label = label,
        language = language,
        selectionId = selectionId,
    )
}
