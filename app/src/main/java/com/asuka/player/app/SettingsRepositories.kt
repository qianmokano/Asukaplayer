package com.asuka.player.app

internal class UiSettingsRepository(
    private val store: AppSettingsStore,
) {
    var themeConfig: ThemeConfig
        get() = store.themeConfig
        set(value) {
            store.themeConfig = value
        }

    var customThemes: List<CustomThemeEntry>
        get() = store.customThemes
        set(value) {
            store.customThemes = value
        }

    var navDurationMs: Int
        get() = store.navDurationMs
        set(value) {
            store.navDurationMs = value
        }

    var hapticFeedbackEnabled: Boolean
        get() = store.hapticFeedbackEnabled
        set(value) {
            store.hapticFeedbackEnabled = value
        }
}

internal class PlayerSettingsRepository(
    private val store: AppSettingsStore,
) {
    var playerSettings: PlayerSettingsConfig
        get() = store.playerSettings
        set(value) {
            store.playerSettings = value
        }
}

internal class PlaybackBehaviorRepository(
    private val store: AppSettingsStore,
) {
    var keepConnectionInBackground: Boolean
        get() = store.keepConnectionInBackground
        set(value) {
            store.keepConnectionInBackground = value
        }
}

