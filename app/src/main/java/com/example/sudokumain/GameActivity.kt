package com.example.sudokumain

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sudokumain.adapter.NumberPadAdapter
import com.example.sudokumain.model.Cell
import com.example.sudokumain.model.Difficulty
import com.example.sudokumain.model.GameState
import com.example.sudokumain.util.SoundManager
import com.example.sudokumain.view.SudokuBoardView

/**
 * Activity for the main Sudoku game screen.
 * Handles game state, user interactions, and UI updates.
 */
class GameActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var gameState: GameState
    private lateinit var sudokuBoardView: SudokuBoardView
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable
    private lateinit var tvTimer: TextView
    private lateinit var tvDifficulty: TextView
    private lateinit var btnHint: Button
    private lateinit var btnNotes: Button
    
    private var isNotesMode = false
    private var selectedRow = -1
    private var selectedCol = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Initialize sound manager
        soundManager = SoundManager
        soundManager.init(this)


        // Get difficulty from intent
        val difficultyString = intent.getStringExtra("DIFFICULTY") ?: "NORMAL"
        val difficulty = Difficulty.valueOf(difficultyString)
        
        // Initialize game state
        gameState = GameState(difficulty)
        
        // Initialize UI components
        initializeUI(difficulty)
        
        // Set up timer
        setupTimer()
        
        // Set up number pad
        setupNumberPad()
        
        // Set up control buttons
        setupControlButtons()
    }

    private fun initializeUI(difficulty: Difficulty) {
        // Set up back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            soundManager.playButtonClick()
            showExitConfirmationDialog()
        }
        
        // Set up settings button
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            soundManager.playButtonClick()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // Set up difficulty text
        tvDifficulty = findViewById(R.id.tvDifficulty)
        tvDifficulty.text = difficulty.displayName
        
        // Set up timer text
        tvTimer = findViewById(R.id.tvTimer)
        
        // Set up Sudoku board view
        sudokuBoardView = findViewById(R.id.sudokuBoard)
        updateBoardView()
        
        // Set up cell selection listener
        sudokuBoardView.setOnCellSelectedListener { row, col ->
            selectedRow = row
            selectedCol = col
        }
        
        // Set up hint button
        btnHint = findViewById(R.id.btnHint)
        updateHintButtonText()
        
        // Set up notes button
        btnNotes = findViewById(R.id.btnNotes)
    }

    private fun setupTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                tvTimer.text = gameState.getFormattedTime()
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }

    private fun setupNumberPad() {
        val numberPad = findViewById<RecyclerView>(R.id.numberPad)
        val numbers = (1..9).toList()
        
        val adapter = NumberPadAdapter(numbers) { number ->
            if (selectedRow >= 0 && selectedCol >= 0) {
                if (isNotesMode) {
                    toggleNote(selectedRow, selectedCol, number)
                } else {
                    placeNumber(selectedRow, selectedCol, number)
                }
            }
        }
        
        numberPad.layoutManager = GridLayoutManager(this, 9)
        numberPad.adapter = adapter
    }

    private fun setupControlButtons() {
        // Hint button
        btnHint.setOnClickListener {
            soundManager.playButtonClick()
            useHint()
        }
        
        // Undo button
        findViewById<Button>(R.id.btnUndo).setOnClickListener {
            soundManager.playButtonClick()
            if (gameState.board.undo()) {
                updateBoardView()
            }
        }
        
        // Notes button
        btnNotes.setOnClickListener {
            soundManager.playButtonClick()
            isNotesMode = !isNotesMode
            updateNotesButtonAppearance()
        }
        
        // Erase button
        findViewById<Button>(R.id.btnErase).setOnClickListener {
            soundManager.playButtonClick()
            if (selectedRow >= 0 && selectedCol >= 0) {
                val cell = gameState.board.getCell(selectedRow, selectedCol)
                if (!cell.isGiven) {
                    gameState.board.setValue(selectedRow, selectedCol, 0)
                    cell.clearNotes()
                    updateBoardView()
                }
            }
        }
        
        // Pause button
        findViewById<Button>(R.id.btnPause).setOnClickListener {
            soundManager.playButtonClick()
            pauseGame()
        }
    }

    private fun placeNumber(row: Int, col: Int, number: Int) {
        val cell = gameState.board.getCell(row, col)
        
        // Can't modify given cells
        if (cell.isGiven) {
            soundManager.playError()
            return
        }
        
        // Set the value
        if (gameState.board.setValue(row, col, number)) {
            soundManager.playNumberPlaced()
            updateBoardView()
            
            // Check if game is completed
            if (gameState.board.isSolved()) {
                gameCompleted()
            }
        }
    }

    private fun toggleNote(row: Int, col: Int, number: Int) {
        val cell = gameState.board.getCell(row, col)
        
        // Can't modify given cells or cells with values
        if (cell.isGiven || cell.value > 0) {
            soundManager.playError()
            return
        }
        
        // Toggle the note
        if (cell.hasNote(number)) {
            cell.removeNote(number)
        } else {
            cell.addNote(number)
        }
        
        updateBoardView()
    }

    private fun useHint() {
        if (gameState.getHintsUsed() >= gameState.getMaxHints()) {
            // No hints left
            showNoHintsLeftDialog()
            return
        }
        
        val hint = gameState.getHint()
        if (hint != null) {
            val (row, col, value) = hint
            
            // Select the cell
            sudokuBoardView.selectCell(row, col)
            selectedRow = row
            selectedCol = col
            
            // Place the number
            gameState.board.setValue(row, col, value)
            soundManager.playHint()
            
            // Update UI
            updateBoardView()
            updateHintButtonText()
            
            // Check if game is completed
            if (gameState.board.isSolved()) {
                gameCompleted()
            }
        }
    }

    private fun updateBoardView() {
        // Create a 2D array of cells for the view
        val boardCells = Array(9) { row ->
            Array(9) { col ->
                gameState.board.getCell(row, col)
            }
        }
        
        // Update the board view
        sudokuBoardView.updateBoard(boardCells)
    }

    private fun updateHintButtonText() {
        val hintsUsed = gameState.getHintsUsed()
        val maxHints = gameState.getMaxHints()
        btnHint.text = getString(R.string.hint) + " ($hintsUsed/$maxHints)"
    }

    private fun updateNotesButtonAppearance() {
        if (isNotesMode) {
            btnNotes.setBackgroundColor(getColor(R.color.primary))
            btnNotes.setTextColor(getColor(R.color.white))
        } else {
            btnNotes.setBackgroundColor(getColor(R.color.white))
            btnNotes.setTextColor(getColor(R.color.primary))
        }
    }

    private fun pauseGame() {
        gameState.pauseTimer()
        timerHandler.removeCallbacks(timerRunnable)
        
        // Show pause dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.pause)
            .setMessage("Game paused")
            .setCancelable(false)
            .setPositiveButton(R.string.resume) { dialog, _ ->
                dialog.dismiss()
                resumeGame()
            }
            .setNegativeButton(R.string.exit_game_title) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
        
        builder.create().show()
    }

    private fun resumeGame() {
        gameState.startTimer()
        timerHandler.post(timerRunnable)
    }

    private fun gameCompleted() {
        // Stop the timer
        gameState.endGame()
        timerHandler.removeCallbacks(timerRunnable)
        
        // Play completion sound
        soundManager.playGameComplete()
        
        // Navigate to game complete screen
        val intent = Intent(this, GameCompleteActivity::class.java)
        intent.putExtra("DIFFICULTY", gameState.difficulty.name)
        intent.putExtra("TIME", gameState.getElapsedTime())
        intent.putExtra("HINTS_USED", gameState.getHintsUsed())
        intent.putExtra("MAX_HINTS", gameState.getMaxHints())
        intent.putExtra("SCORE", gameState.calculateScore())
        startActivity(intent)
        finish()
    }

    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.exit_game_title)
            .setMessage(R.string.exit_game_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        
        builder.create().show()
    }

    private fun showNoHintsLeftDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.hint)
            .setMessage(R.string.no_hints_left)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
            }
        
        builder.create().show()
    }

    override fun onResume() {
        super.onResume()
        if (!gameState.isPaused()) {
            resumeGame()
        }
        soundManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        gameState.pauseTimer()
        timerHandler.removeCallbacks(timerRunnable)
        soundManager.pauseBackgroundMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }
}
