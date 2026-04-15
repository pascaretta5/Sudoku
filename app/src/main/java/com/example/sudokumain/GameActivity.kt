package com.example.sudokumain

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
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
    private lateinit var idleHandler: Handler
    private lateinit var idleHelpRunnable: Runnable
    private lateinit var tvTimer: TextView
    private lateinit var tvDifficulty: TextView
    private lateinit var tvHintLabel: TextView
    private lateinit var btnHint: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnNotes: Button

    private var isNotesMode = false
    private var selectedRow = -1
    private var selectedCol = -1
    private var currentSaveSlotIndex = -1
    private var isTimerTickerRunning = false
    private var isHintSuggested = false
    private var hintPulseAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        soundManager = SoundManager
        soundManager.init(this)

        val savedSession = loadSavedSessionIfRequested()
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
        currentSaveSlotIndex = savedInstanceState?.getInt(STATE_SAVE_SLOT_INDEX)
            ?: savedSession?.slotIndex
            ?: intent.getIntExtra(EXTRA_SAVE_SLOT_INDEX, -1)

        initializeUI(difficulty)
        setupTimer()
        setupIdleHelpTimer()
        setupNumberPad()
        setupControlButtons()
        restoreSelectionIfNeeded()
        updateNotesButtonAppearance()
        updateHintButtonAppearance()
        updateBoardView()
        resetIdleHelpTimer()
    }

    private fun loadSavedSessionIfRequested(): ActiveGameStorage.SavedSession? {
        if (!intent.getBooleanExtra(EXTRA_RESUME_SAVED_GAME, false)) {
            return null
        }

        val requestedSlot = intent.getIntExtra(EXTRA_SAVE_SLOT_INDEX, -1)
        return if (requestedSlot >= 0) {
            ActiveGameStorage.loadGame(this, requestedSlot)
        } else {
            ActiveGameStorage.loadMostRecentGame(this)
        }
    }

    private fun initializeUI(difficulty: Difficulty) {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            soundManager.playButtonClick()
            showExitConfirmationDialog()
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            soundManager.playButtonClick()
            startActivity(Intent(this, SettingsActivity::class.java))
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
        btnUndo = findViewById(R.id.btnUndo)
        tvHintLabel = findViewById(R.id.tvHintLabel)
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

    private fun setupIdleHelpTimer() {
        idleHandler = Handler(Looper.getMainLooper())
        idleHelpRunnable = Runnable {
            if (!gameState.isPaused() && !gameState.isGameOver() && !gameState.board.isSolved()) {
                showHintSuggestionDialog()
            }
        }
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

    private fun resetIdleHelpTimer() {
        if (!::idleHandler.isInitialized) return
        idleHandler.removeCallbacks(idleHelpRunnable)
        if (!gameState.isPaused() && !gameState.isGameOver()) {
            idleHandler.postDelayed(idleHelpRunnable, IDLE_HELP_DELAY_MS)
        }
    }

    private fun stopIdleHelpTimer() {
        if (::idleHandler.isInitialized) {
            idleHandler.removeCallbacks(idleHelpRunnable)
        }
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

        btnUndo.setOnClickListener {
            soundManager.playButtonClick()
            if (gameState.board.undo()) {
                clearHintSuggestion()
                updateBoardView()
                resetIdleHelpTimer()
            }
        }

        btnNotes.setOnClickListener {
            soundManager.playButtonClick()
            isNotesMode = !isNotesMode
            updateNotesButtonAppearance()
            resetIdleHelpTimer()
        }

        findViewById<Button>(R.id.btnErase).setOnClickListener {
            soundManager.playButtonClick()
            if (selectedRow >= 0 && selectedCol >= 0) {
                val cell = gameState.board.getCell(selectedRow, selectedCol)
                if (!cell.isGiven) {
                    gameState.board.setValue(selectedRow, selectedCol, 0)
                    cell.clearNotes()
                    clearHintSuggestion()
                    updateBoardView()
                    resetIdleHelpTimer()
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
            clearHintSuggestion()
            updateBoardView()
            resetIdleHelpTimer()

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

        clearHintSuggestion()
        updateBoardView()
        resetIdleHelpTimer()
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

            clearHintSuggestion()
            updateBoardView()
            updateHintButtonText()
            resetIdleHelpTimer()

            if (gameState.board.isSolved()) {
                gameCompleted()
            }
        }
    }

    private fun showHintSuggestionDialog() {
        if (isHintSuggested) return
        isHintSuggested = true
        updateHintButtonAppearance()

        AlertDialog.Builder(this)
            .setTitle(R.string.help_popup_title)
            .setMessage(R.string.help_popup_message)
            .setPositiveButton(R.string.try_hint) { dialog, _ ->
                dialog.dismiss()
                useHint()
            }
            .setNegativeButton(R.string.keep_solving) { dialog, _ ->
                dialog.dismiss()
                resetIdleHelpTimer()
            }
            .show()
    }

    private fun clearHintSuggestion() {
        if (!isHintSuggested) return
        isHintSuggested = false
        updateHintButtonAppearance()
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
        tvHintLabel.text = getString(R.string.hint_with_count, hintsUsed, maxHints)
    }

    private fun updateHintButtonAppearance() {
        if (isHintSuggested) {
            btnHint.setBackgroundResource(R.drawable.bg_button_hint_highlight)
            startHintPulse()
        } else {
            btnHint.setBackgroundResource(R.drawable.bg_button_control)
            stopHintPulse()
        }
    }

    private fun startHintPulse() {
        if (hintPulseAnimator?.isRunning == true) return
        hintPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            btnHint,
            PropertyValuesHolder.ofFloat(ImageButton.SCALE_X, 1f, 1.06f, 1f),
            PropertyValuesHolder.ofFloat(ImageButton.SCALE_Y, 1f, 1.06f, 1f)
        ).apply {
            duration = 900L
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopHintPulse() {
        hintPulseAnimator?.cancel()
        btnHint.scaleX = 1f
        btnHint.scaleY = 1f
        hintPulseAnimator = null
    }

    private fun updateNotesButtonAppearance() {
        btnNotes.isSelected = isNotesMode
        val textColor = if (isNotesMode) getColor(R.color.white) else getColor(R.color.primary_dark)
        btnNotes.setTextColor(textColor)
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
        stopIdleHelpTimer()

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
        resetIdleHelpTimer()
    }

    private fun persistCurrentGame() {
        if (gameState.isGameOver()) {
            if (currentSaveSlotIndex >= 0) {
                ActiveGameStorage.clear(this, currentSaveSlotIndex)
            }
            return
        }

        currentSaveSlotIndex = ActiveGameStorage.saveGame(
            context = this,
            gameState = gameState,
            isNotesMode = isNotesMode,
            selectedRow = selectedRow,
            selectedCol = selectedCol,
            slotIndex = currentSaveSlotIndex.takeIf { it >= 0 }
        )
    }

    private fun gameCompleted() {
        gameState.endGame()
        stopTimerTicker()
        stopIdleHelpTimer()
        clearHintSuggestion()
        if (currentSaveSlotIndex >= 0) {
            ActiveGameStorage.clear(this, currentSaveSlotIndex)
        }

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
            resetIdleHelpTimer()
        }
        soundManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        gameState.pauseTimer()
        stopTimerTicker()
        stopIdleHelpTimer()
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
        outState.putInt(STATE_SAVE_SLOT_INDEX, currentSaveSlotIndex)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimerTicker()
        stopIdleHelpTimer()
        stopHintPulse()
    }

    companion object {
        const val EXTRA_DIFFICULTY = "DIFFICULTY"
        const val EXTRA_IS_DAILY_CHALLENGE = "IS_DAILY_CHALLENGE"
        const val EXTRA_RESUME_SAVED_GAME = "RESUME_SAVED_GAME"
        const val EXTRA_SAVE_SLOT_INDEX = "SAVE_SLOT_INDEX"
        const val EXTRA_TIME = "TIME"
        const val EXTRA_HINTS_USED = "HINTS_USED"
        const val EXTRA_MAX_HINTS = "MAX_HINTS"
        const val EXTRA_SCORE = "SCORE"

        private const val STATE_SELECTED_ROW = "selected_row"
        private const val STATE_SELECTED_COL = "selected_col"
        private const val STATE_SAVE_SLOT_INDEX = "save_slot_index"
        private const val IDLE_HELP_DELAY_MS = 60_000L
    }
}
