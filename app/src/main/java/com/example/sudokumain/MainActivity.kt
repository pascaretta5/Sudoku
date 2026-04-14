package com.example.sudokumain

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sudokumain.util.ActiveGameStorage
import com.example.sudokumain.util.SoundManager

/**
 * Main entry point of the application.
 * Displays the homepage with Play button and other options.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        soundManager = SoundManager
        soundManager.init(this)

        setupButtons()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            soundManager.playButtonClick()
            if (ActiveGameStorage.hasSavedGame(this)) {
                showResumeGameDialog()
            } else {
                startDifficultySelection()
            }
        }

        findViewById<Button>(R.id.btnDailyChallenge).setOnClickListener {
            soundManager.playButtonClick()
            if (ActiveGameStorage.hasSavedGame(this)) {
                showResumeGameDialog()
            } else {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra(GameActivity.EXTRA_DIFFICULTY, "NORMAL")
                intent.putExtra(GameActivity.EXTRA_IS_DAILY_CHALLENGE, true)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnAchievements).setOnClickListener {
            soundManager.playButtonClick()
        }

        findViewById<Button>(R.id.btnLeaderboards).setOnClickListener {
            soundManager.playButtonClick()
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            soundManager.playButtonClick()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showResumeGameDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.resume_saved_game_title)
            .setMessage(R.string.resume_saved_game_message)
            .setPositiveButton(R.string.resume) { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra(GameActivity.EXTRA_RESUME_SAVED_GAME, true)
                startActivity(intent)
            }
            .setNegativeButton(R.string.start_new_game) { dialog, _ ->
                dialog.dismiss()
                ActiveGameStorage.clear(this)
                startDifficultySelection()
            }
            .setNeutralButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startDifficultySelection() {
        val intent = Intent(this, DifficultySelectionActivity::class.java)
        startActivity(intent)
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
