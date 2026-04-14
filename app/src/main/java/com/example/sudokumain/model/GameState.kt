package com.example.sudokumain.model

import com.example.sudokumain.util.SudokuGenerator
import java.util.UUID

/**
 * Manages the state of a Sudoku game session.
 * Handles game initialization, timer, hints, and game completion.
 */
class GameState(
    val difficulty: Difficulty,
    private val sudokuGenerator: SudokuGenerator = SudokuGenerator(),
    savedGame: SaveGame? = null,
    private val isDailyChallenge: Boolean = savedGame?.isDailyChallenge ?: false
) {
    // The Sudoku board for this game
    val board = SudokuBoard()

    // Game statistics
    private var startTime: Long = 0
    private var elapsedTime: Long = savedGame?.elapsedTime ?: 0
    private var isTimerRunning = false
    private var hintsUsed = savedGame?.hintsUsed ?: 0
    private val maxHints = difficulty.hintCount

    // Game status
    private var isGameOver = false
    private var isPaused = false

    // For saving/loading
    private var gameId = savedGame?.id ?: UUID.randomUUID().toString()

    init {
        if (savedGame != null) {
            board.restoreBoard(savedGame.boardState)
        } else {
            val puzzle = sudokuGenerator.generatePuzzle(difficulty)
            board.setupPuzzle(puzzle)
        }

        startTimer()
    }

    /**
     * Starts or resumes the game timer
     */
    fun startTimer() {
        if (!isTimerRunning && !isGameOver) {
            startTime = System.currentTimeMillis() - elapsedTime
            isTimerRunning = true
            isPaused = false
        }
    }

    /**
     * Pauses the game timer
     */
    fun pauseTimer() {
        if (isTimerRunning) {
            elapsedTime = System.currentTimeMillis() - startTime
            isTimerRunning = false
            isPaused = true
        }
    }

    /**
     * Gets the current elapsed time in milliseconds
     */
    fun getElapsedTime(): Long {
        return if (isTimerRunning) {
            System.currentTimeMillis() - startTime
        } else {
            elapsedTime
        }
    }

    /**
     * Gets the formatted time string (MM:SS)
     */
    fun getFormattedTime(): String {
        val timeInSeconds = getElapsedTime() / 1000
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Checks if the game is completed
     */
    fun isGameCompleted(): Boolean {
        return board.isSolved()
    }

    /**
     * Checks if the game is paused
     */
    fun isPaused(): Boolean {
        return isPaused
    }

    /**
     * Checks if the game is over.
     */
    fun isGameOver(): Boolean {
        return isGameOver
    }

    /**
     * Returns whether this session belongs to the daily challenge flow.
     */
    fun isDailyChallenge(): Boolean {
        return isDailyChallenge
    }

    /**
     * Gets a hint for the current board state
     * Returns a triple of (row, column, value) if a hint is available
     * Returns null if no hints are available or the board is invalid
     */
    fun getHint(): Triple<Int, Int, Int>? {
        if (hintsUsed >= maxHints || isGameOver) {
            return null
        }

        // Convert board to 2D array for the generator
        val currentBoard = Array(9) { row ->
            Array(9) { col ->
                board.getCell(row, col).value
            }
        }

        val hint = sudokuGenerator.getHint(currentBoard)
        if (hint != null) {
            hintsUsed++
        }

        return hint
    }

    /**
     * Gets the number of hints used
     */
    fun getHintsUsed(): Int {
        return hintsUsed
    }

    /**
     * Gets the maximum number of hints available
     */
    fun getMaxHints(): Int {
        return maxHints
    }

    /**
     * Calculates the score based on difficulty, time, and hints used
     */
    fun calculateScore(): Int {
        if (!isGameCompleted()) {
            return 0
        }

        // Base score based on difficulty
        val baseScore = when (difficulty) {
            Difficulty.EASY -> 1000
            Difficulty.NORMAL -> 2000
            Difficulty.HARD -> 3000
            Difficulty.INSANE -> 5000
        }

        // Time penalty (lower time is better)
        val timeInSeconds = getElapsedTime() / 1000
        val timeFactor = maxOf(0.5, 1.0 - (timeInSeconds / (15 * 60.0)))

        // Hint penalty
        val hintFactor = maxOf(0.5, 1.0 - (hintsUsed.toDouble() / maxHints))

        return (baseScore * timeFactor * hintFactor).toInt()
    }

    /**
     * Ends the game
     */
    fun endGame() {
        if (!isGameOver) {
            isGameOver = true
            pauseTimer()
        }
    }

    /**
     * Gets the game ID (for saving/loading)
     */
    fun getGameId(): String {
        return gameId
    }

    /**
     * Sets the game ID (for loading saved games)
     */
    fun setGameId(id: String) {
        gameId = id
    }

    /**
     * Creates a save game object for persistence
     */
    fun createSaveGame(): SaveGame {
        val boardState = Array(9) { row ->
            Array(9) { col ->
                val cell = board.getCell(row, col)
                cell.copy(notes = cell.notes.toMutableSet())
            }
        }

        return SaveGame(
            id = gameId,
            difficulty = difficulty,
            boardState = boardState,
            elapsedTime = getElapsedTime(),
            hintsUsed = hintsUsed,
            timestamp = System.currentTimeMillis(),
            isDailyChallenge = isDailyChallenge
        )
    }

    /**
     * Represents a saved game state.
     */
    data class SaveGame(
        val id: String,
        val difficulty: Difficulty,
        val boardState: Array<Array<Cell>>,
        val elapsedTime: Long,
        val hintsUsed: Int,
        val timestamp: Long,
        val isDailyChallenge: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SaveGame

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }
}
