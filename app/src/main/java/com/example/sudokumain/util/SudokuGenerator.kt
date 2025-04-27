package com.example.sudokumain.util

import com.example.sudokumain.model.Difficulty
import java.util.*
import kotlin.math.sqrt

/**
 * Utility class for generating valid Sudoku puzzles of different difficulty levels.
 */
class SudokuGenerator {
    private val random = Random()
    
    /**
     * Generates a Sudoku puzzle with the specified difficulty level.
     * Returns a 9x9 array of integers (0 for empty cells).
     */
    fun generatePuzzle(difficulty: Difficulty): Array<Array<Int>> {
        // Start with a solved board
        val solvedBoard = generateSolvedBoard()
        
        // Create a copy that we'll remove numbers from
        val puzzle = Array(9) { row ->
            Array(9) { col ->
                solvedBoard[row][col]
            }
        }
        
        // Calculate how many cells to remove based on difficulty
        val cellsToRemove = difficulty.emptyCells()
        
        // Create a list of all positions and shuffle it
        val positions = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                positions.add(Pair(row, col))
            }
        }
        positions.shuffle()
        
        // Remove numbers one by one, ensuring the puzzle remains solvable
        var removed = 0
        for (pos in positions) {
            val (row, col) = pos
            val temp = puzzle[row][col]
            puzzle[row][col] = 0
            
            // Check if the puzzle still has a unique solution
            if (hasUniqueSolution(puzzle)) {
                removed++
                if (removed >= cellsToRemove) {
                    break
                }
            } else {
                // If removing this number creates multiple solutions, put it back
                puzzle[row][col] = temp
            }
        }
        
        return puzzle
    }
    
    /**
     * Generates a completely solved Sudoku board.
     */
    private fun generateSolvedBoard(): Array<Array<Int>> {
        val board = Array(9) { Array(9) { 0 } }
        solveSudoku(board)
        return board
    }
    
    /**
     * Solves the given Sudoku board using backtracking.
     * Returns true if a solution was found.
     */
    private fun solveSudoku(board: Array<Array<Int>>): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col] == 0) {
                    // Try different numbers for this cell
                    val numbers = (1..9).toMutableList()
                    numbers.shuffle(random) // Try numbers in random order
                    
                    for (num in numbers) {
                        if (isValid(board, row, col, num)) {
                            board[row][col] = num
                            
                            if (solveSudoku(board)) {
                                return true
                            }
                            
                            board[row][col] = 0 // Backtrack
                        }
                    }
                    
                    return false // No valid number found
                }
            }
        }
        
        return true // All cells filled
    }
    
    /**
     * Checks if placing a number at the specified position is valid.
     */
    private fun isValid(board: Array<Array<Int>>, row: Int, col: Int, num: Int): Boolean {
        // Check row
        for (c in 0 until 9) {
            if (board[row][c] == num) {
                return false
            }
        }
        
        // Check column
        for (r in 0 until 9) {
            if (board[r][col] == num) {
                return false
            }
        }
        
        // Check 3x3 box
        val boxRow = row - row % 3
        val boxCol = col - col % 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (board[r][c] == num) {
                    return false
                }
            }
        }
        
        return true
    }
    
    /**
     * Checks if the given puzzle has a unique solution.
     * Returns true if there is exactly one solution.
     */
    private fun hasUniqueSolution(puzzle: Array<Array<Int>>): Boolean {
        val copy = Array(9) { row -> Array(9) { col -> puzzle[row][col] } }
        val solutions = countSolutions(copy, 2)
        return solutions == 1
    }


    /**
     * Counts the number of solutions for the given puzzle up to maxCount.
     */
    private fun countSolutions(board: Array<Array<Int>>, maxCount: Int): Int {
        var row = -1
        var col = -1
        var isEmpty = true
        
        // Find an empty cell
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (board[r][c] == 0) {
                    row = r
                    col = c
                    isEmpty = false
                    break
                }
            }
            if (!isEmpty) {
                break
            }
        }
        
        // No empty cells means we found a solution
        if (isEmpty) {
            return 1
        }
        
        var count = 0
        
        // Try each number
        for (num in 1..9) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num
                count += countSolutions(board, maxCount - count)
                
                // If we've reached the maximum count, stop searching
                if (count >= maxCount) {
                    break
                }
                
                board[row][col] = 0 // Backtrack
            }
        }
        
        return count
    }
    
    /**
     * Provides a hint for the current puzzle state.
     * Returns a triple of (row, column, value) for a cell that can be filled.
     */
    fun getHint(board: Array<Array<Int>>): Triple<Int, Int, Int>? {
        val copy = Array(9) { row -> Array(9) { col -> board[row][col] } }
        
        // Find all empty cells
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (copy[row][col] == 0) {
                    emptyCells.add(Pair(row, col))
                }
            }
        }
        
        if (emptyCells.isEmpty()) {
            return null
        }
        
        // Shuffle to give a random hint
        emptyCells.shuffle(random)
        
        // Solve the puzzle to find the correct value
        if (solveSudoku(copy)) {
            val (row, col) = emptyCells.first()
            return Triple(row, col, copy[row][col])
        }
        
        return null
    }
}
