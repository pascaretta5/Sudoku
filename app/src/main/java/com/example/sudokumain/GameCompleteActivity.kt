package com.example.sudokumain

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sudokumain.model.Difficulty
import com.example.sudokumain.util.SoundManager
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Activity displayed when a game is completed.
 * Shows game statistics and options to play again or return to main menu.
 */
class GameCompleteActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_complete)

        // Initialize sound manager
        soundManager = SoundManager
        soundManager.init(this)


        // Get data from intent
        val difficultyString = intent.getStringExtra("DIFFICULTY") ?: "NORMAL"
        val difficulty = Difficulty.valueOf(difficultyString)
        val timeInMillis = intent.getLongExtra("TIME", 0)
        val hintsUsed = intent.getIntExtra("HINTS_USED", 0)
        val maxHints = intent.getIntExtra("MAX_HINTS", 3)
        val score = intent.getIntExtra("SCORE", 0)
        
        // Set up UI with game data
        setupUI(difficulty, timeInMillis, hintsUsed, maxHints, score)
        
        // Set up buttons
        setupButtons(difficulty)
    }

    private fun setupUI(difficulty: Difficulty, timeInMillis: Long, hintsUsed: Int, maxHints: Int, score: Int) {
        // Set difficulty text
        findViewById<TextView>(R.id.tvDifficulty).text = difficulty.displayName
        
        // Format time (MM:SS)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) - TimeUnit.MINUTES.toSeconds(minutes)
        val timeText = String.format("Time: %02d:%02d", minutes, seconds)
        findViewById<TextView>(R.id.tvTime).text = timeText
        
        // Set hints used
        val hintsText = "Hints Used: $hintsUsed/$maxHints"
        findViewById<TextView>(R.id.tvHintsUsed).text = hintsText
        
        // Format score with commas
        val formattedScore = NumberFormat.getNumberInstance(Locale.US).format(score)
        val scoreText = "Score: $formattedScore"
        findViewById<TextView>(R.id.tvScore).text = scoreText
        
        // Check if it's a new record (this would be implemented with a leaderboard system)
        // For now, just show it randomly for demo purposes
        if (score > 5000) {
            findViewById<TextView>(R.id.tvNewRecord).visibility = View.VISIBLE
        }
    }

    private fun setupButtons(difficulty: Difficulty) {
        // Play Again button
        findViewById<Button>(R.id.btnPlayAgain).setOnClickListener {
            soundManager.playButtonClick()
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("DIFFICULTY", difficulty.name)
            startActivity(intent)
            finish()
        }
        
        // Share Result button
        findViewById<Button>(R.id.btnShareResult).setOnClickListener {
            soundManager.playButtonClick()
            shareResult(difficulty)
        }
        
        // Main Menu button
        findViewById<Button>(R.id.btnMainMenu).setOnClickListener {
            soundManager.playButtonClick()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun shareResult(difficulty: Difficulty) {
        val scoreText = findViewById<TextView>(R.id.tvScore).text
        val timeText = findViewById<TextView>(R.id.tvTime).text
        
        val shareText = "I just completed a ${difficulty.displayName} Sudoku puzzle in $timeText with $scoreText! Can you beat that? #SudokuGame"
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share your result"))
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
