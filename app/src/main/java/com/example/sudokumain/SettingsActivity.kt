package com.example.sudokumain

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.sudokumain.util.ActiveGameStorage
import com.example.sudokumain.util.GamePreferences
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
    private lateinit var themePreviews: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        soundManager = SoundManager
        soundManager.init(this)

        initializeUI()
        loadSettings()
        setupSwitchListeners()
        setupVolumeSlider()
        setupThemeSelection()
    }

    private fun initializeUI() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            soundManager.playButtonClick()
            finish()
        }

        switchSoundEffects = findViewById(R.id.switchSoundEffects)
        switchBackgroundMusic = findViewById(R.id.switchBackgroundMusic)
        switchVibration = findViewById(R.id.switchVibration)
        switchHighlightSameNumbers = findViewById(R.id.switchHighlightSameNumbers)
        switchAutoCheck = findViewById(R.id.switchAutoCheck)
        seekBarVolume = findViewById(R.id.seekBarVolume)

        themePreviews = listOf(
            findViewById(R.id.themeBlue),
            findViewById(R.id.themeRed),
            findViewById(R.id.themeGreen),
            findViewById(R.id.themeYellow)
        )

        findViewById<Button>(R.id.btnResetProgress).setOnClickListener {
            soundManager.playButtonClick()
            showResetConfirmationDialog()
        }
    }

    private fun loadSettings() {
        val settings = GamePreferences.getSettings(this)
        switchSoundEffects.isChecked = settings.soundEffectsEnabled
        switchBackgroundMusic.isChecked = settings.backgroundMusicEnabled
        switchVibration.isChecked = settings.vibrationEnabled
        switchHighlightSameNumbers.isChecked = settings.highlightSameNumbersEnabled
        switchAutoCheck.isChecked = settings.autoCheckEnabled
        seekBarVolume.progress = (settings.volume * 100).toInt()
        updateThemeSelection(settings.theme)
        updateSoundManagerSettings()
    }

    private fun setupSwitchListeners() {
        switchSoundEffects.setOnCheckedChangeListener { _, isChecked ->
            GamePreferences.updateSoundEffectsEnabled(this, isChecked)
            updateSoundManagerSettings(previewClick = false)
            soundManager.playButtonClick()
        }

        switchBackgroundMusic.setOnCheckedChangeListener { _, isChecked ->
            GamePreferences.updateBackgroundMusicEnabled(this, isChecked)
            updateSoundManagerSettings()
        }

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            GamePreferences.updateVibrationEnabled(this, isChecked)
            soundManager.playButtonClick()
        }

        switchHighlightSameNumbers.setOnCheckedChangeListener { _, isChecked ->
            GamePreferences.updateHighlightSameNumbersEnabled(this, isChecked)
            soundManager.playButtonClick()
        }

        switchAutoCheck.setOnCheckedChangeListener { _, isChecked ->
            GamePreferences.updateAutoCheckEnabled(this, isChecked)
            soundManager.playButtonClick()
        }
    }

    private fun setupVolumeSlider() {
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    GamePreferences.updateVolume(this@SettingsActivity, progress / 100f)
                    updateSoundManagerSettings(previewClick = false)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                soundManager.playButtonClick()
            }
        })
    }

    private fun setupThemeSelection() {
        themePreviews.forEachIndexed { index, preview ->
            preview.setOnClickListener {
                soundManager.playButtonClick()
                GamePreferences.updateTheme(this, index)
                updateThemeSelection(index)
            }
        }
    }

    private fun updateThemeSelection(selectedTheme: Int) {
        themePreviews.forEachIndexed { index, preview ->
            preview.alpha = if (index == selectedTheme) 1.0f else 0.5f
        }
    }

    private fun updateSoundManagerSettings(previewClick: Boolean = true) {
        soundManager.updateSettings(
            soundEffectsEnabled = switchSoundEffects.isChecked,
            backgroundMusicEnabled = switchBackgroundMusic.isChecked,
            volume = seekBarVolume.progress / 100f
        )

        if (previewClick) {
            soundManager.playButtonClick()
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_progress_title)
            .setMessage(R.string.reset_progress_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                resetProgress()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun resetProgress() {
        ActiveGameStorage.clear(this)

        getSharedPreferences("SudokuGameAchievements", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        getSharedPreferences("SudokuGameLeaderboards", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        AlertDialog.Builder(this)
            .setTitle(R.string.reset_progress_title)
            .setMessage(R.string.all_progress_reset)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        soundManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBackgroundMusic()
    }
}
