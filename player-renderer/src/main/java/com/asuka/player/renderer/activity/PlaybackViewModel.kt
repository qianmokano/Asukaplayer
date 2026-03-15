package com.asuka.player.renderer.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.ui.state.PlayerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PlaybackViewModel(
    application: Application,
    private val dependencies: PlaybackActivityDependencies,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        PlaybackHostState(
            runtimeSettings = dependencies.playbackRuntimeSettingsSource.current(),
        ),
    )
    val state: StateFlow<PlaybackHostState> = _state.asStateFlow()
    val uiState: StateFlow<PlayerUiState> get() = sessionHost.uiState

    val activityBehavior = PlaybackActivityBehavior()

    val sessionHost = PlaybackSessionHost(
        contentResolver = application.contentResolver,
        cacheDir = application.cacheDir,
        scope = viewModelScope,
        dependencies = dependencies,
        controllerContext = application,
        sharedState = _state,
    )

    init {
        viewModelScope.launch {
            dependencies.playbackRuntimeSettingsSource.settings.collect(::applyRuntimeSettings)
        }
        viewModelScope.launch {
            dependencies.persistenceDegraded.collect { degraded ->
                _state.update { it.copy(isPersistenceDegraded = degraded) }
            }
        }
    }

    fun applyRuntimeSettings(settings: PlayerSettings) {
        _state.update { current -> current.copy(runtimeSettings = settings) }
        activityBehavior.onRuntimeSettingsChanged(settings)
    }

    fun updatePictureInPicture(isInPip: Boolean) {
        _state.update { it.copy(isInPictureInPicture = isInPip) }
    }

    override fun onCleared() {
        sessionHost.releaseAll()
    }

    class Factory(
        private val application: Application,
        private val dependencies: PlaybackActivityDependencies,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PlaybackViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            @Suppress("UNCHECKED_CAST")
            return PlaybackViewModel(application, dependencies) as T
        }
    }
}
