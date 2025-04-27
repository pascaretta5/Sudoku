package com.example.sudokumain

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.sudokumain.util.SoundManager

/**
 * Activity for managing game settings.
 * Handles sound, music, vibration, and other game preferences.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var switchSoundEffects: SwitchCompat
    private lateinit var switchBackgroundMusic: SwitchCompat
    private lateinit var switchVibration: SwitchCompat
    private lateinit var switchHighlightSameNumbers: SwitchCompat
    private lateinit var switchAutoCheck: SwitchCompat
    private lateinit var seekBarVolume: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize sound manager
        soundManager = SoundManager
        soundManager.init(this)


        // Initialize UI components
        initializeUI()

        // Load saved settings
        loadSettings()
    }

    private fun initializeUI() {
        // Set up back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            soundManager.playButtonClick()
            finish()
        }

        // Initialize switches
        switchSoundEffects = findViewById(R.id.switchSoundEffects)
        switchBackgroundMusic = findViewById(R.id.switchBackgroundMusic)
        switchVibration = findViewById(R.id.switchVibration)
        switchHighlightSameNumbers = findViewById(R.id.switchHighlightSameNumbers)
        switchAutoCheck = findViewById(R.id.switchAutoCheck)

        // Initialize volume slider
        seekBarVolume = findViewById(R.id.seekBarVolume)

        // Set up theme selection
        setupThemeSelection()

        // Set up reset progress button
        findViewById<Button>(R.id.btnResetProgress).setOnClickListener {
            soundManager.playButtonClick()
            showResetConfirmationDialog()
        }

        // Set up switch listeners
        setupSwitchListeners()

        // Set up volume slider listener
        setupVolumeSlider()
    }

    private fun setupSwitchListeners() {
        switchSoundEffects.setOnCheckedChangeListener { _, isChecked ->
            soundManager.playButtonClick()
            saveSoundEffectsEnabled(isChecked)
            updateSoundManagerSettings()
        }

        switchBackgroundMusic.setOnCheckedChangeListener { _, isChecked ->
            soundManager.playButtonClick()
            saveBackgroundMusicEnabled(isChecked)
            updateSoundManagerSettings()
        }

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            soundManager.playButtonClick()
            saveVibrationEnabled(isChecked)
        }

        switchHighlightSameNumbers.setOnCheckedChangeListener { _, isChecked ->
            soundManager.playButtonClick()
            saveHighlightSameNumbersEnabled(isChecked)
        }

        switchAutoCheck.setOnCheckedChangeListener { _, isChecked ->
            soundManager.playButtonClick()
            saveAutoCheckEnabled(isChecked)
        }
    }

    private fun setupVolumeSlider() {
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volume = progress / 100f
                    saveVolume(volume)
                    updateSoundManagerSettings()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                soundManager.playButtonClick()
            }
        })
    }

    private fun setupThemeSelection() {
        val themes = listOf(
            findViewById<ImageView>(R.id.themeBlue),
            findViewById<ImageView>(R.id.themeRed),
            findViewById<ImageView>(R.id.themeGreen),
            findViewById<ImageView>(R.id.themeYellow)
        )

        val currentTheme = getSharedPreferences("SudokuGamePrefs", MODE_PRIVATE)
            .getInt("theme", 0)

        // Highlight current theme
        themes[currentTheme].alpha = 1.0f
        for (i in themes.indices) {
            if (i != currentTheme) {
                themes[i].alpha = 0.5f
            }

            themes[i].setOnClickListener {
                soundManager.playButtonClick()
                
                // Update theme selection
                for (j in themes.indices) {
                    themes[j].alpha = if (j == i) 1.0f else 0.5f
                }
                
                // Save theme selection
                saveTheme(i)
            }
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("SudokuGamePrefs", MODE_PRIVATE)
        
        // Load switch states
        switchSoundEffects.isChecked = prefs.getBoolean("sound_effects_enabled", true)
        switchBackgroundMusic.isChecked = prefs.getBoolean("background_music_enabled", true)
        switchVibration.isChecked = prefs.getBoolean("vibration_enabled", true)
        switchHighlightSameNumbers.isChecked = prefs.getBoolean("highlight_same_numbers", true)
        switchAutoCheck.isChecked = prefs.getBoolean("auto_check", true)
        
        // Load volume
        val volume = prefs.getFloat("volume", 1.0f)
        seekBarVolume.progress = (volume * 100).toInt()
    }

    private fun saveSoundEffectsEnabled(enabled: Boolean) {
        getSharedPreferences("SudokuGamePrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("sound_effects_enabled", enabled)
            .apply()
    }

    private fun saveBackgroundMusicEnabled(enabled: Boolean) {
        getSharedPreferences("SudokuGamePrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("background_music_enabled", enabled)
            .apply()
    }

    private fun saveVibrationEnabled(enabled: Boolean) {
        getSharedPreferences("SudokuGamePrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("vibration_enabled", enabled)
            .apply()
    }

    private fun saveHighlightSameNumbersEnabled(enabled: Boolean) {
        getSharedPreferences("SudokuGamePrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("highlight_same_numbers", enabled)
            .apply()
    }

    private fun saveAutoCheckEnabled(enabled: Boolean) {
        getSharedPreferences("SudokuGamePrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("auto_check", enabled)
            .apply()
    }

    private fun saveVolume(volume: Float) {
        getSharedPreferences("SudokuGamePrefs", MODE_PRIVATE)
            .edit()
            .putFloat("volume", volume)
            .apply()
    }

    private fun saveTheme(theme: Int) {
        getSharedPreferences("SudokuGamePrefs", MODE_PRIVATE)
            .edit()
            .putInt("theme", theme)
            .apply()
    }

    private fun updateSoundManagerSettings() {
        val soundEffectsEnabled = switchSoundEffects.isChecked
        val backgroundMusicEnabled = switchBackgroundMusic.isChecked
        val volume = seekBarVolume.progress / 100f
        
        soundManager.updateSettings(soundEffectsEnabled, backgroundMusicEnabled, volume)
    }

    private fun showResetConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.reset_progress_title)
            .setMessage(R.string.reset_progress_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                resetProgress()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        
        builder.create().show()
    }

    private fun resetProgress() {
        // Clear saved games, achievements, and leaderboards
        getSharedPreferences("SudokuGameSaves", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        
        getSharedPreferences("SudokuGameAchievements", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        
        getSharedPreferences("SudokuGameLeaderboards", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        
        // Show confirmation
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.reset_progress_title)
            .setMessage("All progress has been reset.")
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
            }
        
        builder.create().show()
    }

    override fun onResume() {
        super.onResume()
        soundManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBackgroundMusic()
    }
}
