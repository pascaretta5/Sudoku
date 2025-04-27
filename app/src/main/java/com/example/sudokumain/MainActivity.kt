package com.example.sudokumain

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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

        // Initialize sound manager
        soundManager = SoundManager
        soundManager.init(this)


        // Set up button click listeners
        setupButtons()
    }

    private fun setupButtons() {
        // Play button
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            soundManager.playButtonClick()
            val intent = Intent(this, DifficultySelectionActivity::class.java)
            startActivity(intent)
        }

        // Daily Challenge button
        findViewById<Button>(R.id.btnDailyChallenge).setOnClickListener {
            soundManager.playButtonClick()
            // TODO: Implement daily challenge
            // For now, just start a random Normal difficulty game
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("DIFFICULTY", "NORMAL")
            intent.putExtra("IS_DAILY_CHALLENGE", true)
            startActivity(intent)
        }
        //ToDo
        // Achievements button
        findViewById<Button>(R.id.btnAchievements).setOnClickListener {
            soundManager.playButtonClick()
           // val intent = Intent(this, AchievementsActivity::class.java)
          //  startActivity(intent)
        }
        //TODO
        // Leaderboards button
        findViewById<Button>(R.id.btnLeaderboards).setOnClickListener {
            soundManager.playButtonClick()
           // val intent = Intent(this, LeaderboardActivity::class.java)
            //startActivity(intent)
        }

        // Settings button
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            soundManager.playButtonClick()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
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
