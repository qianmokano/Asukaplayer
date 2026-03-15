package com.asuka.player.runtime

import android.app.Application
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.data.DataStoreAppSettingsStore
import kotlinx.coroutines.CoroutineScope

data class SettingsRuntimeFeature(
    val uiSettingsRepository: UiSettingsRepository,
    val playerSettingsRepository: PlayerSettingsRepository,
    val playbackBehaviorRepository: PlaybackBehaviorRepository,
    val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource,
) {
    companion object {
        fun create(
            application: Application,
            scope: CoroutineScope,
        ): SettingsRuntimeFeature {
            val settingsStore = DataStoreAppSettingsStore(
                context = application,
                scope = scope,
            )
            val uiSettingsRepository = UiSettingsRepository(settingsStore, scope)
            val playerSettingsRepository = PlayerSettingsRepository(settingsStore, scope)
            val playbackBehaviorRepository = PlaybackBehaviorRepository(settingsStore, scope)
            val playbackRuntimeSettingsSource = AppPlaybackRuntimeSettingsSource(
                playerSettingsRepository = playerSettingsRepository,
                playbackBehaviorRepository = playbackBehaviorRepository,
                scope = scope,
            )
            return SettingsRuntimeFeature(
                uiSettingsRepository = uiSettingsRepository,
                playerSettingsRepository = playerSettingsRepository,
                playbackBehaviorRepository = playbackBehaviorRepository,
                playbackRuntimeSettingsSource = playbackRuntimeSettingsSource,
            )
        }
    }
}
