package com.example.sudokumain.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * Manages sound effects and background music for the game.
 */
object SoundManager {
    private var appContext: Context? = null
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    private var backgroundPlayer: MediaPlayer? = null
    private var isInitialized = false

    private var soundEffectsEnabled = true
    private var backgroundMusicEnabled = true
    private var masterVolume = 1f

    fun init(context: Context) {
        appContext = context.applicationContext

        if (!isInitialized) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()

            loadSound(context, "click", listOf("boop_click", "click_sound"))
            loadSound(context, "complete", listOf("complete_sound"))
            isInitialized = true
        }

        val settings = GamePreferences.getSettings(context)
        updateSettings(
            soundEffectsEnabled = settings.soundEffectsEnabled,
            backgroundMusicEnabled = settings.backgroundMusicEnabled,
            volume = settings.volume
        )
    }

    fun playSound(name: String, volumeMultiplier: Float = 1f, rate: Float = 1f) {
        if (!isInitialized || soundPool == null || !soundEffectsEnabled) return
        soundMap[name]?.let { soundId ->
            val volume = (masterVolume * volumeMultiplier).coerceIn(0f, 1f)
            soundPool?.play(soundId, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
        }
    }

    fun release() {
        backgroundPlayer?.release()
        backgroundPlayer = null
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        isInitialized = false
        appContext = null
    }

    fun playButtonClick() {
        playSound("click", volumeMultiplier = 0.88f, rate = 1.02f)
    }

    fun updateSettings(soundEffectsEnabled: Boolean, backgroundMusicEnabled: Boolean, volume: Float) {
        this.soundEffectsEnabled = soundEffectsEnabled
        this.backgroundMusicEnabled = backgroundMusicEnabled
        this.masterVolume = volume.coerceIn(0f, 1f)

        ensureBackgroundPlayer()
        backgroundPlayer?.setVolume(masterVolume * 0.55f, masterVolume * 0.55f)

        if (this.backgroundMusicEnabled) {
            resumeBackgroundMusic()
        } else {
            pauseBackgroundMusic()
        }
    }

    fun resumeBackgroundMusic() {
        ensureBackgroundPlayer()
        if (backgroundMusicEnabled && backgroundPlayer != null && backgroundPlayer?.isPlaying == false) {
            backgroundPlayer?.start()
        }
    }

    fun pauseBackgroundMusic() {
        if (backgroundPlayer?.isPlaying == true) {
            backgroundPlayer?.pause()
        }
    }

    fun playError() {
        playSound("click", volumeMultiplier = 0.72f, rate = 0.78f)
    }

    fun playNumberPlaced() {
        playSound("click", volumeMultiplier = 0.92f, rate = 1.08f)
    }

    fun playHint() {
        playSound("click", volumeMultiplier = 0.95f, rate = 1.18f)
    }

    fun playGameComplete() {
        playSound("complete", volumeMultiplier = 1f)
    }

    private fun ensureBackgroundPlayer() {
        val context = appContext ?: return
        if (backgroundPlayer != null) return

        val musicResId = resolveRawResource(context, listOf("silly_background_music", "background_music"))
        if (musicResId == 0) return

        backgroundPlayer = MediaPlayer.create(context, musicResId)?.apply {
            isLooping = true
            setVolume(masterVolume * 0.55f, masterVolume * 0.55f)
        }
    }

    private fun loadSound(context: Context, key: String, resourceNames: List<String>) {
        val resId = resolveRawResource(context, resourceNames)
        if (resId != 0) {
            soundMap[key] = soundPool!!.load(context, resId, 1)
        }
    }

    private fun resolveRawResource(context: Context, candidates: List<String>): Int {
        return candidates.firstNotNullOfOrNull { name ->
            context.resources.getIdentifier(name, "raw", context.packageName)
                .takeIf { it != 0 }
        } ?: 0
    }
}
