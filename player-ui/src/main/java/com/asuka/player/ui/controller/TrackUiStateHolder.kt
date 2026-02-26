package com.asuka.player.ui.controller

import androidx.media3.common.Player
import com.asuka.player.core.TrackInfoReader
import androidx.media3.common.C
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TrackUiStateHolder(private val player: Player) : Player.Listener {
    private val reader = TrackInfoReader(player)
    private val _audioTracks = MutableStateFlow<List<TrackInfoReader.TrackInfo>>(emptyList())
    private val _subtitleTracks = MutableStateFlow<List<TrackInfoReader.TrackInfo>>(emptyList())

    val audioTracks: StateFlow<List<TrackInfoReader.TrackInfo>> = _audioTracks
    val subtitleTracks: StateFlow<List<TrackInfoReader.TrackInfo>> = _subtitleTracks

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
        val all = reader.listTracks()
        _audioTracks.value = all.filter { it.type == C.TRACK_TYPE_AUDIO }
        _subtitleTracks.value = all.filter { it.type == C.TRACK_TYPE_TEXT }
    }
}
