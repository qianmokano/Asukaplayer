package com.asuka.player.app

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asuka.player.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

internal class MainLibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val appGraph = application.appGraph
    private val uiSettingsRepository = appGraph.uiSettingsRepository
    private val playerSettingsRepository = appGraph.playerSettingsRepository

    // --- Settings state ---
    val themeConfig = MutableStateFlow(uiSettingsRepository.themeConfig)
    val customThemes = MutableStateFlow(uiSettingsRepository.customThemes)
    val navDurationMs = MutableStateFlow(uiSettingsRepository.navDurationMs)
    val hapticFeedbackEnabled = MutableStateFlow(uiSettingsRepository.hapticFeedbackEnabled)
    val playerSettings = MutableStateFlow(playerSettingsRepository.playerSettings)

    // --- Video scanning state ---
    val items = MutableStateFlow(emptyList<LocalVideoItem>())
    val loading = MutableStateFlow(false)
    val hasLoadedOnce = MutableStateFlow(false)
    val permissionGranted = MutableStateFlow(hasVideoPermission(application))
    val userSelectedPermissionGranted = MutableStateFlow(hasUserSelectedVideoPermission(application))

    init {
        // Persist settings changes, dropping the initial emission to avoid write-back on launch.
        viewModelScope.launch {
            themeConfig.drop(1).collect { uiSettingsRepository.themeConfig = it }
        }
        viewModelScope.launch {
            customThemes.drop(1).collect { uiSettingsRepository.customThemes = it }
        }
        viewModelScope.launch {
            navDurationMs.drop(1).collect { uiSettingsRepository.navDurationMs = it }
        }
        viewModelScope.launch {
            hapticFeedbackEnabled.drop(1).collect { uiSettingsRepository.hapticFeedbackEnabled = it }
        }
        viewModelScope.launch {
            playerSettings.drop(1).collect { playerSettingsRepository.playerSettings = it }
        }
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        val app = getApplication<Application>()
        permissionGranted.value = hasVideoPermission(app)
        userSelectedPermissionGranted.value = hasUserSelectedVideoPermission(app) && !permissionGranted.value
    }

    fun refresh() {
        if (!loading.value) {
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
