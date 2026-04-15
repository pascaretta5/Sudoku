package com.example.sudokumain

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sudokumain.util.ActiveGameStorage
import com.example.sudokumain.util.SoundManager
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Main entry point of the application.
 * Displays the homepage with quick actions and a dedicated saved-games area.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var savedGamesContainer: LinearLayout
    private lateinit var tvNoSavedGames: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        soundManager = SoundManager
        soundManager.init(this)

        savedGamesContainer = findViewById(R.id.savedGamesContainer)
        tvNoSavedGames = findViewById(R.id.tvNoSavedGames)

        setupButtons()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            soundManager.playButtonClick()
            startDifficultySelection()
        }

        findViewById<Button>(R.id.btnDailyChallenge).setOnClickListener {
            soundManager.playButtonClick()
            launchDailyChallenge()
        }

        findViewById<Button>(R.id.btnAchievements).setOnClickListener {
            soundManager.playButtonClick()
        }

        findViewById<Button>(R.id.btnLeaderboards).setOnClickListener {
            soundManager.playButtonClick()
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            soundManager.playButtonClick()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun renderSavedGames() {
        val savedGames = ActiveGameStorage.getSavedGameSummaries(this)
        savedGamesContainer.removeAllViews()
        tvNoSavedGames.visibility = if (savedGames.isEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(this)
        savedGames.forEach { savedGame ->
            val itemView = inflater.inflate(R.layout.item_saved_game, savedGamesContainer, false)
            itemView.findViewById<TextView>(R.id.tvSavedTitle).text = getString(
                R.string.saved_game_title_format,
                savedGame.slotIndex + 1,
                savedGame.difficulty.displayName
            )
            itemView.findViewById<TextView>(R.id.tvSavedSubtitle).text = buildSavedGameSubtitle(savedGame)
            itemView.findViewById<Button>(R.id.btnResumeSaved).setOnClickListener {
                soundManager.playButtonClick()
                resumeSavedGame(savedGame.slotIndex)
            }
            savedGamesContainer.addView(itemView)
        }
    }

    private fun buildSavedGameSubtitle(savedGame: ActiveGameStorage.SavedGameSummary): String {
        val minutes = savedGame.elapsedTime / 1000 / 60
        val seconds = savedGame.elapsedTime / 1000 % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        val dateString = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(savedGame.timestamp))
        val challengeLabel = if (savedGame.isDailyChallenge) {
            getString(R.string.saved_game_daily_challenge)
        } else {
            getString(R.string.saved_game_standard)
        }

        return getString(
            R.string.saved_game_summary_format,
            challengeLabel,
            timeString,
            savedGame.hintsUsed,
            dateString
        )
    }

    private fun resumeSavedGame(slotIndex: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_RESUME_SAVED_GAME, true)
            putExtra(GameActivity.EXTRA_SAVE_SLOT_INDEX, slotIndex)
        }
        startActivity(intent)
    }

    private fun launchDailyChallenge() {
        if (ActiveGameStorage.isFull(this)) {
            showOverwriteSlotDialog(isDailyChallenge = true)
            return
        }

        startDailyChallenge(slotIndex = -1)
    }

    private fun showOverwriteSlotDialog(isDailyChallenge: Boolean) {
        val savedGames = ActiveGameStorage.getSavedGameSummaries(this).sortedBy { it.slotIndex }
        val options = savedGames.map {
            getString(R.string.saved_game_title_format, it.slotIndex + 1, it.difficulty.displayName)
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.saved_games_full_title)
            .setMessage(R.string.saved_games_full_message)
            .setItems(options) { dialog, which ->
                val chosenSlot = savedGames[which].slotIndex
                ActiveGameStorage.clear(this, chosenSlot)
                if (isDailyChallenge) {
                    startDailyChallenge(chosenSlot)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startDailyChallenge(slotIndex: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_DIFFICULTY, "NORMAL")
            putExtra(GameActivity.EXTRA_IS_DAILY_CHALLENGE, true)
            if (slotIndex >= 0) {
                putExtra(GameActivity.EXTRA_SAVE_SLOT_INDEX, slotIndex)
            }
        }
        startActivity(intent)
    }

    private fun startDifficultySelection() {
        startActivity(Intent(this, DifficultySelectionActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        renderSavedGames()
        soundManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBackgroundMusic()
    }
}
