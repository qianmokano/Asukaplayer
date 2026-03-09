package com.asuka.player.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asuka.player.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface MainLibraryEvent {
    data class ShowToast(val message: String) : MainLibraryEvent
}

internal class MainLibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val appGraph = application.appGraph
    private val uiSettingsRepository = appGraph.uiSettingsRepository
    private val playerSettingsRepository = appGraph.playerSettingsRepository
    private val playbackStateRepository = appGraph.playbackStateRepository
    private val queueHistoryRepository = appGraph.queueHistoryRepository

    val uiSettings = uiSettingsRepository.settings
    val playerSettings = playerSettingsRepository.settings

    private val _items = MutableStateFlow(emptyList<LocalVideoItem>())
    val items = _items.asStateFlow()

    private val _recentMediaIds = MutableStateFlow(emptyList<String>())
    val recentMediaIds = _recentMediaIds.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _hasLoadedOnce = MutableStateFlow(false)
    val hasLoadedOnce = _hasLoadedOnce.asStateFlow()

    private val _permissionGranted = MutableStateFlow(hasVideoPermission(application))
    val permissionGranted = _permissionGranted.asStateFlow()

    private val _userSelectedPermissionGranted = MutableStateFlow(hasUserSelectedVideoPermission(application))
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

    fun setPlayerSettings(value: PlayerSettingsConfig) {
        if (playerSettings.value == value) return
        playerSettingsRepository.playerSettings = value
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        val app = getApplication<Application>()
        _permissionGranted.value = hasVideoPermission(app)
        _userSelectedPermissionGranted.value = hasUserSelectedVideoPermission(app) && !_permissionGranted.value
    }

    fun refresh() {
        if (!_loading.value) {
            scanVideos()
        }
    }

    fun refreshRecentMediaIds() {
        viewModelScope.launch(Dispatchers.IO) {
            _recentMediaIds.value = resolveRecentMediaIds(
                historyUris = queueHistoryRepository.items(),
                fallbackMediaIds = playbackStateRepository.recentMediaIds(limit = 100),
            )
        }
    }

    fun validateNetworkStreamUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
        if (trimmed.isBlank() || parsed?.scheme.isNullOrBlank()) {
            _events.tryEmit(
                MainLibraryEvent.ShowToast(
                    getApplication<Application>().getString(R.string.open_network_stream_invalid),
                ),
            )
            return null
        }
        return trimmed
    }

    fun scanVideos() {
        if (!_permissionGranted.value && !_userSelectedPermissionGranted.value) return
        viewModelScope.launch {
            val startedAtMs = System.currentTimeMillis()
            val isUserRefresh = _hasLoadedOnce.value
            _loading.value = true
            val latestItems = withContext(Dispatchers.IO) {
                queryLocalVideos(getApplication())
            }
            if (isUserRefresh) {
                val elapsed = System.currentTimeMillis() - startedAtMs
                val minRefreshAnimMs = 500L
                if (elapsed < minRefreshAnimMs) {
                    delay(minRefreshAnimMs - elapsed)
                }
            }
            _items.value = latestItems
            if (!isUserRefresh) {
                viewModelScope.launch(Dispatchers.IO) {
                    warmupInitialThumbnails(
                        context = getApplication(),
                        videos = latestItems,
                        limit = INITIAL_THUMB_WARMUP_LIMIT,
                    )
                }
            }
            _loading.value = false
            _hasLoadedOnce.value = true
            if (isUserRefresh) {
                _events.tryEmit(
                    MainLibraryEvent.ShowToast(
                        getApplication<Application>().getString(R.string.refresh_done, latestItems.size),
                    ),
                )
            }
        }
    }
}

internal fun resolveRecentMediaIds(
    historyUris: List<Uri>,
    fallbackMediaIds: List<String>,
): List<String> {
    val historyIds = historyUris
        .asReversed()
        .map(Uri::toString)
        .distinct()
    return historyIds.ifEmpty { fallbackMediaIds }
}
