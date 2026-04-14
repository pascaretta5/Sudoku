package com.example.sudokumain

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.sudokumain.model.Difficulty
import com.example.sudokumain.util.ActiveGameStorage
import com.example.sudokumain.util.SoundManager

/**
 * Activity for selecting game difficulty level.
 */
class DifficultySelectionActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_difficulty_selection)

        soundManager = SoundManager
        soundManager.init(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            soundManager.playButtonClick()
            finish()
        }

        setupDifficultyCards()
    }

    private fun setupDifficultyCards() {
        findViewById<CardView>(R.id.cardEasy).setOnClickListener {
            soundManager.playButtonClick()
            startGame(Difficulty.EASY)
        }

        findViewById<CardView>(R.id.cardNormal).setOnClickListener {
            soundManager.playButtonClick()
            startGame(Difficulty.NORMAL)
        }

        findViewById<CardView>(R.id.cardHard).setOnClickListener {
            soundManager.playButtonClick()
            startGame(Difficulty.HARD)
        }

        findViewById<CardView>(R.id.cardInsane).setOnClickListener {
            soundManager.playButtonClick()
            startGame(Difficulty.INSANE)
        }
    }

    private fun startGame(difficulty: Difficulty) {
        if (ActiveGameStorage.isFull(this)) {
            showOverwriteSlotDialog(difficulty)
            return
        }

        launchGame(difficulty, slotIndex = -1)
    }

    private fun showOverwriteSlotDialog(difficulty: Difficulty) {
        val saves = ActiveGameStorage.getSavedGameSummaries(this).sortedBy { it.slotIndex }
        val items = saves.map {
            getString(R.string.saved_game_title_format, it.slotIndex + 1, it.difficulty.displayName)
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.saved_games_full_title)
            .setMessage(R.string.saved_games_full_message)
            .setItems(items) { dialog, which ->
                val chosenSlot = saves[which].slotIndex
                ActiveGameStorage.clear(this, chosenSlot)
                launchGame(difficulty, chosenSlot)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun launchGame(difficulty: Difficulty, slotIndex: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.name)
            if (slotIndex >= 0) {
                putExtra(GameActivity.EXTRA_SAVE_SLOT_INDEX, slotIndex)
            }
        }
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
