package com.asuka.player.app

import android.app.Application
import com.asuka.player.core.PlaybackStoreProvider
import com.asuka.player.core.service.PlaybackService
import com.asuka.player.ui.activity.PlaybackActivity

class AsuraPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlaybackStoreProvider.init(this)
        PlaybackService.activityClass = PlaybackActivity::class.java
    }
}
