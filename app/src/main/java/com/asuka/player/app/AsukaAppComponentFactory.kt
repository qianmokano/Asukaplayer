package com.asuka.player.app

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Intent
import androidx.core.app.CoreComponentFactory
import com.asuka.player.core.PlaybackDependencyRegistry
import com.asuka.player.core.service.PlaybackService
import com.asuka.player.ui.activity.PlaybackActivity

class AsukaAppComponentFactory : CoreComponentFactory() {
    override fun instantiateActivity(
        cl: ClassLoader,
        className: String,
        intent: Intent?,
    ): Activity {
        return when (className) {
            MainActivity::class.java.name ->
                MainActivity(MainActivityDependencyRegistry.require())
            PlaybackActivity::class.java.name ->
                PlaybackActivity(PlaybackDependencyRegistry.requireActivityDependencies())
            else -> super.instantiateActivity(cl, className, intent)
        }
    }

    override fun instantiateApplication(
        cl: ClassLoader,
        className: String,
    ): Application {
        return super.instantiateApplication(cl, className)
    }

    override fun instantiateService(
        cl: ClassLoader,
        className: String,
        intent: Intent?,
    ): Service {
        return when (className) {
            PlaybackService::class.java.name ->
                PlaybackService(PlaybackDependencyRegistry.requireServiceDependencies())
            else -> super.instantiateService(cl, className, intent)
        }
    }
}
