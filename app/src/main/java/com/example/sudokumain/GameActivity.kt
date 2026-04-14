package com.example.sudokumain

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sudokumain.adapter.NumberPadAdapter
import com.example.sudokumain.model.Difficulty
import com.example.sudokumain.model.GameState
import com.example.sudokumain.util.ActiveGameStorage
import com.example.sudokumain.util.GamePreferences
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
    private var isTimerTickerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        soundManager = SoundManager
        soundManager.init(this)

        val savedSession = if (intent.getBooleanExtra(EXTRA_RESUME_SAVED_GAME, false)) {
            ActiveGameStorage.loadGame(this)
        } else {
            null
        }

        val difficulty = savedSession?.saveGame?.difficulty
            ?: Difficulty.valueOf(intent.getStringExtra(EXTRA_DIFFICULTY) ?: Difficulty.NORMAL.name)

        gameState = if (savedSession != null) {
            GameState(
                difficulty = savedSession.saveGame.difficulty,
                savedGame = savedSession.saveGame
            )
        } else {
            GameState(
                difficulty = difficulty,
                isDailyChallenge = intent.getBooleanExtra(EXTRA_IS_DAILY_CHALLENGE, false)
            )
        }

        isNotesMode = savedSession?.isNotesMode ?: false
        selectedRow = savedInstanceState?.getInt(STATE_SELECTED_ROW)
            ?: savedSession?.selectedRow
            ?: -1
        selectedCol = savedInstanceState?.getInt(STATE_SELECTED_COL)
            ?: savedSession?.selectedCol
            ?: -1

        initializeUI(difficulty)
        setupTimer()
        setupNumberPad()
        setupControlButtons()
        restoreSelectionIfNeeded()
        updateNotesButtonAppearance()
        updateBoardView()
    }

    private fun initializeUI(difficulty: Difficulty) {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            soundManager.playButtonClick()
            showExitConfirmationDialog()
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            soundManager.playButtonClick()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        tvDifficulty = findViewById(R.id.tvDifficulty)
        tvDifficulty.text = difficulty.displayName

        tvTimer = findViewById(R.id.tvTimer)
        tvTimer.text = gameState.getFormattedTime()

        sudokuBoardView = findViewById(R.id.sudokuBoard)
        sudokuBoardView.setOnCellSelectedListener { row, col ->
            selectedRow = row
            selectedCol = col
        }

        btnHint = findViewById(R.id.btnHint)
        updateHintButtonText()

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
        startTimerTicker()
    }

    private fun startTimerTicker() {
        if (isTimerTickerRunning) return
        timerHandler.post(timerRunnable)
        isTimerTickerRunning = true
    }

    private fun stopTimerTicker() {
        if (!::timerHandler.isInitialized) return
        timerHandler.removeCallbacks(timerRunnable)
        isTimerTickerRunning = false
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
        btnHint.setOnClickListener {
            soundManager.playButtonClick()
            useHint()
        }

        findViewById<Button>(R.id.btnUndo).setOnClickListener {
            soundManager.playButtonClick()
            if (gameState.board.undo()) {
                updateBoardView()
            }
        }

        btnNotes.setOnClickListener {
            soundManager.playButtonClick()
            isNotesMode = !isNotesMode
            updateNotesButtonAppearance()
        }

        findViewById<Button>(R.id.btnErase).setOnClickListener {
            soundManager.playButtonClick()
            if (selectedRow >= 0 && selectedCol >= 0) {
                val cell = gameState.board.getCell(selectedRow, selectedCol)
                if (!cell.isGiven) {
                    gameState.board.setValue(selectedRow, selectedCol, 0)
                    cell.clearNotes()
                    updateBoardView()
                } else {
                    soundManager.playError()
                    triggerHapticFeedback(HapticFeedbackConstants.REJECT)
                }
            }
        }

        findViewById<Button>(R.id.btnPause).setOnClickListener {
            soundManager.playButtonClick()
            pauseGame()
        }
    }

    private fun placeNumber(row: Int, col: Int, number: Int) {
        val cell = gameState.board.getCell(row, col)

        if (cell.isGiven) {
            soundManager.playError()
            triggerHapticFeedback(HapticFeedbackConstants.REJECT)
            return
        }

        if (gameState.board.setValue(row, col, number)) {
            soundManager.playNumberPlaced()
            updateBoardView()

            if (gameState.board.isSolved()) {
                gameCompleted()
            }
        }
    }

    private fun toggleNote(row: Int, col: Int, number: Int) {
        val cell = gameState.board.getCell(row, col)

        if (cell.isGiven || cell.value > 0) {
            soundManager.playError()
            triggerHapticFeedback(HapticFeedbackConstants.REJECT)
            return
        }

        if (cell.hasNote(number)) {
            cell.removeNote(number)
        } else {
            cell.addNote(number)
        }

        updateBoardView()
    }

    private fun useHint() {
        if (gameState.getHintsUsed() >= gameState.getMaxHints()) {
            showNoHintsLeftDialog()
            triggerHapticFeedback(HapticFeedbackConstants.REJECT)
            return
        }

        val hint = gameState.getHint()
        if (hint != null) {
            val (row, col, value) = hint

            sudokuBoardView.selectCell(row, col)
            selectedRow = row
            selectedCol = col

            gameState.board.setValue(row, col, value)
            soundManager.playHint()

            updateBoardView()
            updateHintButtonText()

            if (gameState.board.isSolved()) {
                gameCompleted()
            }
        }
    }

    private fun updateBoardView() {
        val settings = GamePreferences.getSettings(this)
        sudokuBoardView.setHighlightSameNumbersEnabled(settings.highlightSameNumbersEnabled)

        if (settings.autoCheckEnabled) {
            updateConflictStates()
        } else {
            clearConflictStates()
        }

        val boardCells = Array(9) { row ->
            Array(9) { col ->
                gameState.board.getCell(row, col)
            }
        }

        sudokuBoardView.updateBoard(boardCells)
    }

    private fun updateConflictStates() {
        clearConflictStates()

        for (row in 0..8) {
            for (col in 0..8) {
                val cell = gameState.board.getCell(row, col)
                val value = cell.value
                if (value != 0 && hasConflict(row, col, value)) {
                    cell.isConflict = true
                }
            }
        }
    }

    private fun clearConflictStates() {
        for (row in 0..8) {
            for (col in 0..8) {
                gameState.board.getCell(row, col).isConflict = false
            }
        }
    }

    private fun hasConflict(targetRow: Int, targetCol: Int, value: Int): Boolean {
        for (col in 0..8) {
            if (col != targetCol && gameState.board.getCell(targetRow, col).value == value) {
                return true
            }
        }

        for (row in 0..8) {
            if (row != targetRow && gameState.board.getCell(row, targetCol).value == value) {
                return true
            }
        }

        val startRow = (targetRow / 3) * 3
        val startCol = (targetCol / 3) * 3
        for (row in startRow until startRow + 3) {
            for (col in startCol until startCol + 3) {
                if ((row != targetRow || col != targetCol) && gameState.board.getCell(row, col).value == value) {
                    return true
                }
            }
        }

        return false
    }

    private fun updateHintButtonText() {
        val hintsUsed = gameState.getHintsUsed()
        val maxHints = gameState.getMaxHints()
        btnHint.text = getString(R.string.hint_with_count, hintsUsed, maxHints)
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

    private fun restoreSelectionIfNeeded() {
        if (selectedRow in 0..8 && selectedCol in 0..8) {
            sudokuBoardView.selectCell(selectedRow, selectedCol)
        }
    }

    private fun triggerHapticFeedback(feedbackConstant: Int) {
        if (GamePreferences.getSettings(this).vibrationEnabled) {
            window.decorView.performHapticFeedback(feedbackConstant)
        }
    }

    private fun pauseGame() {
        gameState.pauseTimer()
        stopTimerTicker()

        AlertDialog.Builder(this)
            .setTitle(R.string.pause)
            .setMessage(R.string.game_paused)
            .setCancelable(false)
            .setPositiveButton(R.string.resume) { dialog, _ ->
                dialog.dismiss()
                resumeGame()
            }
            .setNegativeButton(R.string.exit_game_title) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun resumeGame() {
        gameState.startTimer()
        startTimerTicker()
    }

    private fun persistCurrentGame() {
        if (gameState.isGameOver()) {
            ActiveGameStorage.clear(this)
            return
        }

        ActiveGameStorage.saveGame(
            context = this,
            gameState = gameState,
            isNotesMode = isNotesMode,
            selectedRow = selectedRow,
            selectedCol = selectedCol
        )
    }

    private fun gameCompleted() {
        gameState.endGame()
        stopTimerTicker()
        ActiveGameStorage.clear(this)

        soundManager.playGameComplete()
        triggerHapticFeedback(HapticFeedbackConstants.CONFIRM)

        val intent = Intent(this, GameCompleteActivity::class.java)
        intent.putExtra(EXTRA_DIFFICULTY, gameState.difficulty.name)
        intent.putExtra(EXTRA_TIME, gameState.getElapsedTime())
        intent.putExtra(EXTRA_HINTS_USED, gameState.getHintsUsed())
        intent.putExtra(EXTRA_MAX_HINTS, gameState.getMaxHints())
        intent.putExtra(EXTRA_SCORE, gameState.calculateScore())
        startActivity(intent)
        finish()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.exit_game_title)
            .setMessage(R.string.exit_game_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showNoHintsLeftDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.hint)
            .setMessage(R.string.no_hints_left)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateBoardView()
        if (!gameState.isPaused()) {
            startTimerTicker()
        }
        soundManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        gameState.pauseTimer()
        stopTimerTicker()
        soundManager.pauseBackgroundMusic()
    }

    override fun onStop() {
        super.onStop()
        persistCurrentGame()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SELECTED_ROW, selectedRow)
        outState.putInt(STATE_SELECTED_COL, selectedCol)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimerTicker()
    }

    companion object {
        const val EXTRA_DIFFICULTY = "DIFFICULTY"
        const val EXTRA_IS_DAILY_CHALLENGE = "IS_DAILY_CHALLENGE"
        const val EXTRA_RESUME_SAVED_GAME = "RESUME_SAVED_GAME"
        const val EXTRA_TIME = "TIME"
        const val EXTRA_HINTS_USED = "HINTS_USED"
        const val EXTRA_MAX_HINTS = "MAX_HINTS"
        const val EXTRA_SCORE = "SCORE"

        private const val STATE_SELECTED_ROW = "selected_row"
        private const val STATE_SELECTED_COL = "selected_col"
    }
}
