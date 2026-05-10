package com.asuka.player.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.runtime.CustomThemeEntry
import com.asuka.player.runtime.PlayerSettingsRepository
import com.asuka.player.runtime.ThemeConfig
import com.asuka.player.runtime.UiSettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal sealed interface MainLibraryEvent {
    data class ShowMessage(val message: MainLibraryText) : MainLibraryEvent
}

internal data class MainLibraryViewModelDependencies(
    val uiSettingsRepository: UiSettingsRepository,
    val playerSettingsRepository: PlayerSettingsRepository,
    val resolveVideoAccessUseCase: ResolveVideoAccessUseCase,
    val loadFolderPageUseCase: LoadFolderPageUseCase,
    val loadVideoPageUseCase: LoadVideoPageUseCase,
    val loadRecentMediaIdsUseCase: LoadRecentMediaIdsUseCase,
    val resolveRecentMediaItemsUseCase: ResolveRecentMediaItemsUseCase,
    val observeMediaLibraryChangesUseCase: ObserveMediaLibraryChangesUseCase,
)

internal class MainLibraryViewModel(
    dependencies: MainLibraryViewModelDependencies,
) : ViewModel() {
    private val uiSettingsRepository = dependencies.uiSettingsRepository
    private val playerSettingsRepository = dependencies.playerSettingsRepository
    private val mediaLibraryRepository = dependencies.resolveVideoAccessUseCase.mediaLibraryRepository
    private val _events = MutableSharedFlow<MainLibraryEvent>(extraBufferCapacity = 1)

    private val catalog = MainLibraryCatalogStore(
        resolveVideoAccessUseCase = dependencies.resolveVideoAccessUseCase,
        loadFolderPageUseCase = dependencies.loadFolderPageUseCase,
        loadVideoPageUseCase = dependencies.loadVideoPageUseCase,
        loadRecentMediaIdsUseCase = dependencies.loadRecentMediaIdsUseCase,
        resolveRecentMediaItemsUseCase = dependencies.resolveRecentMediaItemsUseCase,
        observeMediaLibraryChangesUseCase = dependencies.observeMediaLibraryChangesUseCase,
        scope = viewModelScope,
        publishMessage = { _events.tryEmit(MainLibraryEvent.ShowMessage(it)) },
    )

    val uiSettings = uiSettingsRepository.settings
    val playerSettings = playerSettingsRepository.settings
    val permissionGranted = catalog.permissionGranted
    val userSelectedPermissionGranted = catalog.userSelectedPermissionGranted
    val foldersState = catalog.foldersState
    val allVideosState = catalog.allVideosState
    val currentFolderId = catalog.currentFolderId
    val currentFolderVideosState = catalog.currentFolderVideosState
    val recentMediaIds = catalog.recentMediaIds
    val recentKnownVideos = catalog.recentKnownVideos
    val events = _events.asSharedFlow()

    fun setThemeConfig(value: ThemeConfig) {
        if (uiSettings.value.themeConfig == value) return
        viewModelScope.launch { uiSettingsRepository.setThemeConfig(value) }
    }

    fun setCustomThemes(value: List<CustomThemeEntry>) {
        if (uiSettings.value.customThemes == value) return
        viewModelScope.launch { uiSettingsRepository.setCustomThemes(value) }
    }

    fun setNavDurationMs(value: Int) {
        if (uiSettings.value.navDurationMs == value) return
        viewModelScope.launch { uiSettingsRepository.setNavDurationMs(value) }
    }

    fun setHapticFeedbackEnabled(value: Boolean) {
        if (uiSettings.value.hapticFeedbackEnabled == value) return
        viewModelScope.launch { uiSettingsRepository.setHapticFeedbackEnabled(value) }
    }

    fun setPlayerSettings(value: PlayerSettings) {
        if (playerSettings.value == value) return
        viewModelScope.launch { playerSettingsRepository.setPlayerSettings(value) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPermissionResult(result: Map<String, Boolean>) {
        catalog.onPermissionResult()
    }

    fun ensureFoldersLoaded() = catalog.ensureFoldersLoaded()

    fun refreshFolders() = catalog.refreshFolders()

    fun loadMoreFolders() = catalog.loadMoreFolders()

    fun ensureAllVideosLoaded() = catalog.ensureAllVideosLoaded()

    fun refreshAllVideos() = catalog.refreshAllVideos()

    fun loadMoreAllVideos() = catalog.loadMoreAllVideos()

    fun ensureFolderLoaded(folderId: Long) = catalog.ensureFolderLoaded(folderId)

    fun refreshFolder(folderId: Long) = catalog.refreshFolder(folderId)

    fun loadMoreFolder(folderId: Long) = catalog.loadMoreFolder(folderId)

    fun refreshRecentMediaIds() = catalog.refreshRecentMediaIds()

    fun validateNetworkStreamUrl(rawUrl: String): String? = catalog.validateNetworkStreamUrl(rawUrl)

    override fun onCleared() {
        mediaLibraryRepository.close()
        super.onCleared()
    }

    internal class Factory(
        private val createDependencies: () -> MainLibraryViewModelDependencies,
    ) : ViewModelProvider.Factory {
        constructor(dependencies: MainLibraryViewModelDependencies) : this({ dependencies })

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainLibraryViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            @Suppress("UNCHECKED_CAST")
            return MainLibraryViewModel(createDependencies()) as T
        }
    }
}
