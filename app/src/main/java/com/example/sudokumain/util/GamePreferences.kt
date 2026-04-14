package com.example.sudokumain.util

import android.content.Context

/**
 * Centralized access to persisted game preferences.
 */
object GamePreferences {
    private const val PREFS_NAME = "SudokuGamePrefs"

    private const val KEY_SOUND_EFFECTS_ENABLED = "sound_effects_enabled"
    private const val KEY_BACKGROUND_MUSIC_ENABLED = "background_music_enabled"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_HIGHLIGHT_SAME_NUMBERS = "highlight_same_numbers"
    private const val KEY_AUTO_CHECK = "auto_check"
    private const val KEY_VOLUME = "volume"
    private const val KEY_THEME = "theme"

    data class Settings(
        val soundEffectsEnabled: Boolean,
        val backgroundMusicEnabled: Boolean,
        val vibrationEnabled: Boolean,
        val highlightSameNumbersEnabled: Boolean,
        val autoCheckEnabled: Boolean,
        val volume: Float,
        val theme: Int
    )

    fun getSettings(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Settings(
            soundEffectsEnabled = prefs.getBoolean(KEY_SOUND_EFFECTS_ENABLED, true),
            backgroundMusicEnabled = prefs.getBoolean(KEY_BACKGROUND_MUSIC_ENABLED, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true),
            highlightSameNumbersEnabled = prefs.getBoolean(KEY_HIGHLIGHT_SAME_NUMBERS, true),
            autoCheckEnabled = prefs.getBoolean(KEY_AUTO_CHECK, true),
            volume = prefs.getFloat(KEY_VOLUME, 1.0f),
            theme = prefs.getInt(KEY_THEME, 0)
        )
    }

    fun updateSoundEffectsEnabled(context: Context, enabled: Boolean) {
        edit(context, KEY_SOUND_EFFECTS_ENABLED, enabled)
    }

    fun updateBackgroundMusicEnabled(context: Context, enabled: Boolean) {
        edit(context, KEY_BACKGROUND_MUSIC_ENABLED, enabled)
    }

    fun updateVibrationEnabled(context: Context, enabled: Boolean) {
        edit(context, KEY_VIBRATION_ENABLED, enabled)
    }

    fun updateHighlightSameNumbersEnabled(context: Context, enabled: Boolean) {
        edit(context, KEY_HIGHLIGHT_SAME_NUMBERS, enabled)
    }

    fun updateAutoCheckEnabled(context: Context, enabled: Boolean) {
        edit(context, KEY_AUTO_CHECK, enabled)
    }

    fun updateVolume(context: Context, volume: Float) {
        edit(context, KEY_VOLUME, volume)
    }

    fun updateTheme(context: Context, theme: Int) {
        edit(context, KEY_THEME, theme)
    }

    private fun edit(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    private fun edit(context: Context, key: String, value: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(key, value)
            .apply()
    }

    private fun edit(context: Context, key: String, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(key, value)
            .apply()
    }
}
