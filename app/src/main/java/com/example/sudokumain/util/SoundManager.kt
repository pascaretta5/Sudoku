package com.example.sudokumain.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.sudokumain.R

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
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            soundMap["click"] = soundPool!!.load(context, R.raw.click_sound, 1)
            soundMap["complete"] = soundPool!!.load(context, R.raw.complete_sound, 1)
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
        playSound("click", volumeMultiplier = 0.8f)
    }

    fun updateSettings(soundEffectsEnabled: Boolean, backgroundMusicEnabled: Boolean, volume: Float) {
        this.soundEffectsEnabled = soundEffectsEnabled
        this.backgroundMusicEnabled = backgroundMusicEnabled
        this.masterVolume = volume.coerceIn(0f, 1f)

        ensureBackgroundPlayer()
        backgroundPlayer?.setVolume(masterVolume, masterVolume)

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
        playSound("click", volumeMultiplier = 0.7f, rate = 0.8f)
    }

    fun playNumberPlaced() {
        playSound("click", volumeMultiplier = 0.9f, rate = 1.05f)
    }

    fun playHint() {
        playSound("click", volumeMultiplier = 0.85f, rate = 1.15f)
    }

    fun playGameComplete() {
        playSound("complete", volumeMultiplier = 1f)
    }

    private fun ensureBackgroundPlayer() {
        val context = appContext ?: return
        if (backgroundPlayer != null) return

        val musicResId = context.resources.getIdentifier("background_music", "raw", context.packageName)
        if (musicResId == 0) return

        backgroundPlayer = MediaPlayer.create(context, musicResId)?.apply {
            isLooping = true
            setVolume(masterVolume, masterVolume)
        }
    }
}
