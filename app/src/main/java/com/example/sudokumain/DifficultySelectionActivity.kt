package com.example.sudokumain

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.sudokumain.model.Difficulty
import com.example.sudokumain.util.SoundManager

/**
 * Activity for selecting game difficulty level.
 */
class DifficultySelectionActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_difficulty_selection)

        // Initialize sound manager
        soundManager = SoundManager
        soundManager.init(this)


        // Set up back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            soundManager.playButtonClick()
            finish()
        }

        // Set up difficulty cards
        setupDifficultyCards()
    }

    private fun setupDifficultyCards() {
        // Easy difficulty
        findViewById<CardView>(R.id.cardEasy).setOnClickListener {
            soundManager.playButtonClick()
            startGame(Difficulty.EASY)
        }

        // Normal difficulty
        findViewById<CardView>(R.id.cardNormal).setOnClickListener {
            soundManager.playButtonClick()
            startGame(Difficulty.NORMAL)
        }

        // Hard difficulty
        findViewById<CardView>(R.id.cardHard).setOnClickListener {
            soundManager.playButtonClick()
            startGame(Difficulty.HARD)
        }

        // Insane difficulty
        findViewById<CardView>(R.id.cardInsane).setOnClickListener {
            soundManager.playButtonClick()
            startGame(Difficulty.INSANE)
        }
    }

    private fun startGame(difficulty: Difficulty) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("DIFFICULTY", difficulty.name)
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
