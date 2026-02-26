package com.asuka.player.ui.controller

/**
 * Simplified policy for background play. Extend with settings/prefs.
 */
class BackgroundPlaybackPolicy {
    var allowBackground: Boolean = false
        private set

    fun enableBackground() {
        allowBackground = true
    }

    fun disableBackground() {
        allowBackground = false
    }
}
