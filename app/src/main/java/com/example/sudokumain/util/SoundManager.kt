package com.example.sudokumain.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.sudokumain.R

/**
 * Manages sound effects and background music for the game.
 */
object SoundManager {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        soundMap["click"] = soundPool!!.load(context, R.raw.click_sound, 1)
        soundMap["complete"] = soundPool!!.load(context, R.raw.complete_sound, 1)
        isInitialized = true
    }

    fun playSound(name: String) {
        if (!isInitialized || soundPool == null) return
        soundMap[name]?.let { soundId ->
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
//needs to check
    fun playButtonClick() {
        playSound("click")
    }
//needs to check
    fun updateSettings(soundEffectsEnabled: Boolean, backgroundMusicEnabled: Boolean, volume: Float) {

    }
//needs to check
    fun resumeBackgroundMusic() {
        //NEEDS TO DO
    }
//needs to check
    fun pauseBackgroundMusic() {
         //NEEDS TO DO
    }

    fun playError() {
        //NEEDS TO DO
    }

    fun playNumberPlaced() {
        //NEEDS TO DO
    }

    fun playHint() {
        //NEEDS TO DO
    }

    fun playGameComplete() {
        //NEEDS TO DO
    }
}
