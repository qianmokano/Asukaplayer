package com.asuka.player.ui.controller

class BackgroundPlaybackPolicy(
    retainControllerConnection: Boolean = true,
    autoBackgroundPlaybackEnabled: Boolean = false,
) {
    private var retainControllerConnection: Boolean = retainControllerConnection
    private var autoBackgroundPlaybackEnabled: Boolean = autoBackgroundPlaybackEnabled
    private var inPictureInPicture: Boolean = false
    private var manualBackgroundPlaybackRequested: Boolean = false

    fun update(
        retainControllerConnection: Boolean,
        autoBackgroundPlaybackEnabled: Boolean,
    ) {
        this.retainControllerConnection = retainControllerConnection
        this.autoBackgroundPlaybackEnabled = autoBackgroundPlaybackEnabled
    }

    fun setPictureInPicture(active: Boolean) {
        inPictureInPicture = active
    }

    fun requestBackgroundPlayback() {
        manualBackgroundPlaybackRequested = true
    }

    fun clearManualBackgroundPlaybackRequest() {
        manualBackgroundPlaybackRequested = false
    }

    fun shouldRetainSession(): Boolean {
        return retainControllerConnection ||
            autoBackgroundPlaybackEnabled ||
            inPictureInPicture ||
            manualBackgroundPlaybackRequested
    }
}
