package com.asuka.player.app

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.R
import com.asuka.player.runtime.CustomThemeEntry
import com.asuka.player.runtime.PlayerSettingsRepository
import com.asuka.player.runtime.ThemeConfig
import com.asuka.player.runtime.UiSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface MainLibraryEvent {
    data class ShowToast(val message: String) : MainLibraryEvent
}

internal data class MainLibraryViewModelDependencies(
    val appContext: Context,
    val uiSettingsRepository: UiSettingsRepository,
    val playerSettingsRepository: PlayerSettingsRepository,
    val resolveVideoAccessUseCase: ResolveVideoAccessUseCase,
    val refreshMediaLibraryUseCase: RefreshMediaLibraryUseCase,
    val loadRecentMediaIdsUseCase: LoadRecentMediaIdsUseCase,
)

internal class MainLibraryViewModel(
    private val dependencies: MainLibraryViewModelDependencies,
) : ViewModel() {
    private val appContext = dependencies.appContext.applicationContext
    private val uiSettingsRepository = dependencies.uiSettingsRepository
    private val playerSettingsRepository = dependencies.playerSettingsRepository
    private val resolveVideoAccessUseCase = dependencies.resolveVideoAccessUseCase
    private val refreshMediaLibraryUseCase = dependencies.refreshMediaLibraryUseCase
    private val loadRecentMediaIdsUseCase = dependencies.loadRecentMediaIdsUseCase

    val uiSettings = uiSettingsRepository.settings
    val playerSettings = playerSettingsRepository.settings

    private val _mediaLibraryState = MutableStateFlow(MediaLibraryRefreshState())
    val mediaLibraryState = _mediaLibraryState.asStateFlow()

    private val _recentMediaIds = MutableStateFlow(emptyList<String>())
    val recentMediaIds = _recentMediaIds.asStateFlow()

    private val initialVideoAccessState = resolveVideoAccessUseCase()

    private val _permissionGranted = MutableStateFlow(initialVideoAccessState.permissionGranted)
    val permissionGranted = _permissionGranted.asStateFlow()

    private val _userSelectedPermissionGranted =
        MutableStateFlow(initialVideoAccessState.userSelectedPermissionGranted)
    val userSelectedPermissionGranted = _userSelectedPermissionGranted.asStateFlow()

    private val _events = MutableSharedFlow<MainLibraryEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun setThemeConfig(value: ThemeConfig) {
        if (uiSettings.value.themeConfig == value) return
        uiSettingsRepository.themeConfig = value
    }

    fun setCustomThemes(value: List<CustomThemeEntry>) {
        if (uiSettings.value.customThemes == value) return
        uiSettingsRepository.customThemes = value
    }

    fun setNavDurationMs(value: Int) {
        if (uiSettings.value.navDurationMs == value) return
        uiSettingsRepository.navDurationMs = value
    }

    fun setHapticFeedbackEnabled(value: Boolean) {
        if (uiSettings.value.hapticFeedbackEnabled == value) return
        uiSettingsRepository.hapticFeedbackEnabled = value
    }

    fun setPlayerSettings(value: PlayerSettings) {
        if (playerSettings.value == value) return
        playerSettingsRepository.playerSettings = value
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPermissionResult(result: Map<String, Boolean>) {
        syncVideoAccessState()
    }

    fun refresh() {
        if (!mediaLibraryState.value.isLoading) {
            scanVideos()
        }
    }

    fun refreshRecentMediaIds() {
        viewModelScope.launch(Dispatchers.IO) {
            _recentMediaIds.value = loadRecentMediaIdsUseCase(limit = 100)
        }
    }

    fun validateNetworkStreamUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
        if (trimmed.isBlank() || parsed?.scheme.isNullOrBlank()) {
            _events.tryEmit(
                MainLibraryEvent.ShowToast(
                    appContext.getString(R.string.open_network_stream_invalid),
                ),
            )
            return null
        }
        return trimmed
    }

    fun scanVideos() {
        if (!_permissionGranted.value && !_userSelectedPermissionGranted.value) return
        viewModelScope.launch {
            val currentState = mediaLibraryState.value
            val isUserRefresh = currentState.hasLoadedOnce
            _mediaLibraryState.value = currentState.copy(
                status = MediaLibraryRefreshStatus.Loading,
                errorMessage = null,
            )

            when (val refreshResult = refreshMediaLibraryUseCase(hasLoadedOnce = isUserRefresh)) {
                is MediaLibraryRefreshOutcome.Success -> {
                    _mediaLibraryState.value = reduceMediaLibraryRefreshState(
                        currentState = mediaLibraryState.value,
                        result = refreshResult,
                    )
                    if (refreshResult.warmupVideos.isNotEmpty()) {
                        viewModelScope.launch(Dispatchers.IO) {
                            runCatching {
                                refreshMediaLibraryUseCase.warmupInitialThumbnails(refreshResult.warmupVideos)
                            }
                        }
                    }
                    if (isUserRefresh) {
                        _events.tryEmit(
                            MainLibraryEvent.ShowToast(
                                appContext.getString(R.string.refresh_done, refreshResult.items.size),
                            ),
                        )
                    }
                }
                is MediaLibraryRefreshOutcome.Failure -> {
                    if (refreshResult.reason == MediaLibraryRefreshFailure.PermissionDenied) {
                        syncVideoAccessState()
                        if (!_permissionGranted.value && !_userSelectedPermissionGranted.value) {
                            return@launch
                        }
                    }
                    _mediaLibraryState.value = reduceMediaLibraryRefreshState(
                        currentState = mediaLibraryState.value,
                        result = refreshResult,
                        errorMessage = refreshResult.reason.toMessage(appContext),
                    )
                }
            }
        }
    }

    private fun syncVideoAccessState() {
        val accessState = resolveVideoAccessUseCase()
        _permissionGranted.value = accessState.permissionGranted
        _userSelectedPermissionGranted.value = accessState.userSelectedPermissionGranted
        if (!accessState.permissionGranted && !accessState.userSelectedPermissionGranted) {
            _mediaLibraryState.value = MediaLibraryRefreshState()
        }
    }

    internal class Factory(
        private val dependencies: MainLibraryViewModelDependencies,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainLibraryViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            @Suppress("UNCHECKED_CAST")
            return MainLibraryViewModel(dependencies) as T
        }
    }
}

internal fun reduceMediaLibraryRefreshState(
    currentState: MediaLibraryRefreshState,
    result: MediaLibraryRefreshOutcome,
    errorMessage: String? = null,
): MediaLibraryRefreshState {
    return when (result) {
        is MediaLibraryRefreshOutcome.Success -> currentState.copy(
            items = result.items,
            status = MediaLibraryRefreshStatus.Idle,
            hasLoadedOnce = true,
            errorMessage = null,
        )
        is MediaLibraryRefreshOutcome.Failure -> currentState.copy(
            status = MediaLibraryRefreshStatus.Idle,
            errorMessage = errorMessage,
        )
    }
}

private fun MediaLibraryRefreshFailure.toMessage(context: Context): String {
    return when (this) {
        MediaLibraryRefreshFailure.PermissionDenied ->
            context.getString(R.string.media_library_refresh_error_permission)
        MediaLibraryRefreshFailure.ProviderUnavailable ->
            context.getString(R.string.media_library_refresh_error_provider)
        MediaLibraryRefreshFailure.Unknown ->
            context.getString(R.string.media_library_refresh_error_unknown)
    }
}
