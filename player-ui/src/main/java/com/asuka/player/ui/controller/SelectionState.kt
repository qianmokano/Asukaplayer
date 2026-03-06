package com.asuka.player.ui.controller

import androidx.media3.common.C
import androidx.media3.common.Player
import com.asuka.player.core.TrackIndexCodec
import com.asuka.player.core.TrackSelectionStateReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SelectionState(private val player: Player) : Player.Listener {
    private val _selectedAudio = MutableStateFlow<Int?>(null)
    private val _selectedSubtitle = MutableStateFlow<Int?>(null)

    val selectedAudio: StateFlow<Int?> = _selectedAudio
    val selectedSubtitle: StateFlow<Int?> = _selectedSubtitle

    fun attach() {
        player.addListener(this)
        refresh()
    }

    fun detach() {
        player.removeListener(this)
    }

    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
        refresh()
    }

    private fun refresh() {
        val reader = TrackSelectionStateReader(player)
        val selected = reader.read()
        val audio = selected.firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
        val text = selected.firstOrNull { it.type == C.TRACK_TYPE_TEXT }
        val subtitlesDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        val hasTextTracks = player.currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT }

        _selectedAudio.value = SelectionStateResolver.audioSelection(audio)
        _selectedSubtitle.value = SelectionStateResolver.subtitleSelection(
            selected = text,
            subtitlesDisabled = subtitlesDisabled,
            hasTextTracks = hasTextTracks,
        )
    }
}
