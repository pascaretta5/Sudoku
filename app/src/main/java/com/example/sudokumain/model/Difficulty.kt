package com.example.sudokumain.model

/**
 * Enum representing different difficulty levels for the Sudoku game.
 * Each difficulty level has a different number of pre-filled cells.
 *
 * @property displayName The user-friendly name of the difficulty level
 * @property filledCells The approximate number of pre-filled cells for this difficulty
 * @property hintCount The number of hints available for this difficulty
 * @property starRating A rating from 1-4 stars indicating the difficulty
 * @property description A brief description of the difficulty level
 */
enum class Difficulty(
    val displayName: String,
    val filledCells: Int,
    val hintCount: Int,
    val starRating: Int,
    val description: String
) {
    EASY(
        displayName = "Easy",
        filledCells = 30,
        hintCount = 5,
        starRating = 1,
        description = "Perfect for beginners"
    ),
    
    NORMAL(
        displayName = "Normal",
        filledCells = 23,
        hintCount = 3,
        starRating = 2,
        description = "For casual players"
    ),
    
    HARD(
        displayName = "Hard",
        filledCells = 17,
        hintCount = 2,
        starRating = 3,
        description = "For experienced players"
    ),
    
    INSANE(
        displayName = "Insane",
        filledCells = 11,
        hintCount = 1,
        starRating = 4,
        description = "The ultimate challenge"
    );
    
    /**
     * Returns the number of empty cells for this difficulty
     */
    fun emptyCells(): Int = 81 - filledCells
}
