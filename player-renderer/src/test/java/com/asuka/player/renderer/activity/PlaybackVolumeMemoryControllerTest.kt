package com.asuka.player.renderer.activity

import com.asuka.player.contract.PlaybackDeviceController
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.PlayerSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class PlaybackVolumeMemoryControllerTest {

    @Test
    fun restoreIfNeeded_waitsUntilPlaybackIsReady() = runBlocking {
        val persistence = FakePlaybackUiPersistence(rememberedVolumePercent = 72)
        val deviceController = FakePlaybackDeviceController(currentVolumePercent = 20)
        val controller = PlaybackVolumeMemoryController(persistence, deviceController)

        controller.restoreIfNeeded(
            settings = PlayerSettings(rememberVolume = true),
            isPlaybackReady = false,
        )

        assertNull(deviceController.setVolumePercent)
        assertEquals(0, persistence.volumeReadCount)
    }

    @Test
    fun restoreIfNeeded_restoresClampedVolumeOnlyOnce() = runBlocking {
        val persistence = FakePlaybackUiPersistence(rememberedVolumePercent = 180)
        val deviceController = FakePlaybackDeviceController(currentVolumePercent = 20)
        val controller = PlaybackVolumeMemoryController(persistence, deviceController)
        val settings = PlayerSettings(rememberVolume = true)

        controller.restoreIfNeeded(settings, isPlaybackReady = true)
        controller.restoreIfNeeded(settings, isPlaybackReady = true)

        assertEquals(100, deviceController.setVolumePercent)
        assertEquals(1, persistence.volumeReadCount)
    }

    @Test
    fun restoreIfNeeded_keepsCurrentVolumeWhenPersistenceIsMissing() = runBlocking {
        val persistence = FakePlaybackUiPersistence(rememberedVolumePercent = null)
        val deviceController = FakePlaybackDeviceController(currentVolumePercent = 35)
        val controller = PlaybackVolumeMemoryController(persistence, deviceController)

        controller.restoreIfNeeded(
            settings = PlayerSettings(rememberVolume = true),
            isPlaybackReady = true,
        )

        assertNull(deviceController.setVolumePercent)
    }

    @Test
    fun saveIfNeeded_savesClampedCurrentVolumeOnlyWhenEnabledAndReady() = runBlocking {
        val persistence = FakePlaybackUiPersistence()
        val deviceController = FakePlaybackDeviceController(currentVolumePercent = -20)
        val controller = PlaybackVolumeMemoryController(persistence, deviceController)

        val disabledSettings = PlayerSettings(rememberVolume = false)
        controller.restoreIfNeeded(disabledSettings, isPlaybackReady = true)
        controller.saveIfNeeded(disabledSettings)
        assertNull(persistence.savedVolumePercent)

        val enabledSettings = PlayerSettings(rememberVolume = true)
        controller.saveIfNeeded(enabledSettings)
        assertEquals(0, persistence.savedVolumePercent)
    }

    @Test
    fun saveIfNeeded_doesNotOverwriteVolumeWhenPlaybackNeverBecameReady() {
        val persistence = FakePlaybackUiPersistence()
        val controller = PlaybackVolumeMemoryController(
            playbackUiPersistence = persistence,
            playbackDeviceController = FakePlaybackDeviceController(currentVolumePercent = 80),
        )

        controller.saveIfNeeded(PlayerSettings(rememberVolume = true))

        assertNull(persistence.savedVolumePercent)
    }

    @Test
    fun restoreAndSave_ignoreUnavailableCollaborators() = runBlocking {
        val persistence = FakePlaybackUiPersistence(
            rememberedVolumePercent = 50,
            failVolumeRead = true,
            failVolumeSave = true,
        )
        val deviceController = FakePlaybackDeviceController(
            currentVolumePercent = 50,
            failVolumeRead = true,
            failVolumeWrite = true,
        )
        val controller = PlaybackVolumeMemoryController(persistence, deviceController)
        val settings = PlayerSettings(rememberVolume = true)

        controller.restoreIfNeeded(settings, isPlaybackReady = true)
        controller.saveIfNeeded(settings)

        assertNull(deviceController.setVolumePercent)
        assertNull(persistence.savedVolumePercent)
    }
}

private class FakePlaybackUiPersistence(
    private val rememberedVolumePercent: Int? = null,
    private val failVolumeRead: Boolean = false,
    private val failVolumeSave: Boolean = false,
) : PlaybackUiPersistence {
    var volumeReadCount = 0
        private set
    var savedVolumePercent: Int? = null
        private set

    override suspend fun readZoom(mediaId: String): Float? = null

    override suspend fun saveZoom(mediaId: String, zoom: Float) = Unit

    override fun readRememberedBrightness(): Float? = null

    override fun saveRememberedBrightness(brightness: Float) = Unit

    override suspend fun readRememberedVolumePercent(): Int? {
        volumeReadCount += 1
        if (failVolumeRead) error("volume persistence unavailable")
        return rememberedVolumePercent
    }

    override fun saveRememberedVolumePercent(volumePercent: Int) {
        if (failVolumeSave) error("volume persistence unavailable")
        savedVolumePercent = volumePercent
    }
}

private class FakePlaybackDeviceController(
    private val currentVolumePercent: Int,
    private val failVolumeRead: Boolean = false,
    private val failVolumeWrite: Boolean = false,
) : PlaybackDeviceController {
    var setVolumePercent: Int? = null
        private set

    override fun currentVolumePercent(): Int {
        if (failVolumeRead) error("audio service unavailable")
        return currentVolumePercent
    }

    override fun setVolumePercent(percent: Int) {
        if (failVolumeWrite) error("audio service unavailable")
        setVolumePercent = percent
    }

    override fun currentBrightnessPercent(): Int = 50

    override fun setBrightnessPercent(percent: Int) = Unit
}
