package com.asuka.player.app

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asuka.player.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

internal class MainLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val appSettingsStore = AppSettingsStore(application)

    // --- Settings state ---
    val themeConfig = MutableStateFlow(appSettingsStore.themeConfig)
    val customThemes = MutableStateFlow(appSettingsStore.customThemes)
    val navDurationMs = MutableStateFlow(appSettingsStore.navDurationMs)
    val keepConnectionInBackground = MutableStateFlow(appSettingsStore.keepConnectionInBackground)
    val hapticFeedbackEnabled = MutableStateFlow(appSettingsStore.hapticFeedbackEnabled)
    val experimentalFeaturesEnabled = MutableStateFlow(appSettingsStore.experimentalFeaturesEnabled)
    val playerSettings = MutableStateFlow(appSettingsStore.playerSettings)

    // --- Video scanning state ---
    val items = MutableStateFlow(emptyList<LocalVideoItem>())
    val loading = MutableStateFlow(false)
    val hasLoadedOnce = MutableStateFlow(false)
    val permissionGranted = MutableStateFlow(hasVideoPermission(application))
    val userSelectedPermissionGranted = MutableStateFlow(hasUserSelectedVideoPermission(application))

    private val _refreshTick = MutableStateFlow(0)

    init {
        // Persist settings changes, dropping the initial emission to avoid write-back on launch.
        viewModelScope.launch {
            themeConfig.drop(1).collect { appSettingsStore.themeConfig = it }
        }
        viewModelScope.launch {
            customThemes.drop(1).collect { appSettingsStore.customThemes = it }
        }
        viewModelScope.launch {
            navDurationMs.drop(1).collect { appSettingsStore.navDurationMs = it }
        }
        viewModelScope.launch {
            keepConnectionInBackground.drop(1).collect { appSettingsStore.keepConnectionInBackground = it }
        }
        viewModelScope.launch {
            hapticFeedbackEnabled.drop(1).collect { appSettingsStore.hapticFeedbackEnabled = it }
        }
        viewModelScope.launch {
            experimentalFeaturesEnabled.drop(1).collect { appSettingsStore.experimentalFeaturesEnabled = it }
        }
        viewModelScope.launch {
            playerSettings.drop(1).collect { appSettingsStore.playerSettings = it }
        }
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        val app = getApplication<Application>()
        permissionGranted.value = hasVideoPermission(app)
        userSelectedPermissionGranted.value = hasUserSelectedVideoPermission(app) && !permissionGranted.value
    }

    fun refresh() {
        if (!loading.value) {
            _refreshTick.value++
            scanVideos()
        }
    }

    fun scanVideos() {
        if (!permissionGranted.value && !userSelectedPermissionGranted.value) return
        viewModelScope.launch {
            val startedAtMs = System.currentTimeMillis()
            val isUserRefresh = hasLoadedOnce.value
            loading.value = true
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
            items.value = latestItems
            if (!isUserRefresh) {
                viewModelScope.launch(Dispatchers.IO) {
                    warmupInitialThumbnails(
                        context = getApplication(),
                        videos = latestItems,
                        limit = INITIAL_THUMB_WARMUP_LIMIT,
                    )
                }
            }
            loading.value = false
            hasLoadedOnce.value = true
            if (isUserRefresh) {
                Toast.makeText(
                    getApplication(),
                    getApplication<Application>().getString(R.string.refresh_done, latestItems.size),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
}
