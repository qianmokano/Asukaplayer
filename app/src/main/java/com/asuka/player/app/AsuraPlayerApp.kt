package com.asuka.player.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.asuka.player.R
import com.asuka.player.core.PlaybackStoreProvider
import com.asuka.player.core.service.PlaybackService
import com.asuka.player.ui.activity.PlaybackActivity

class AsuraPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel_id",
                getString(R.string.notification_channel_playback_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        PlaybackStoreProvider.init(this)
        PlaybackService.activityClass = PlaybackActivity::class.java
    }
}
