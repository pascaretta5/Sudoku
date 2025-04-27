package com.example.sudokumain.model

/**
 * Represents the entire Sudoku board with a 9x9 grid of cells.
 * Handles operations on the board like setting values, checking validity, etc.
 */
class SudokuBoard {
    // 9x9 grid of cells
    private val board: Array<Array<Cell>> = Array(9) { row ->
        Array(9) { col ->
            Cell(row, col)
        }
    }
    
    // Track moves for undo/redo functionality
    private val moveHistory = mutableListOf<Move>()
    private val redoStack = mutableListOf<Move>()
    
    /**
     * Gets the cell at the specified position
     */
    fun getCell(row: Int, col: Int): Cell {
        return board[row][col]
    }
    
    /**
     * Sets the value of a cell at the specified position
     * Returns true if the value was set successfully
     */
    fun setValue(row: Int, col: Int, value: Int): Boolean {
        val cell = board[row][col]
        
        // Can't modify given cells
        if (cell.isGiven) {
            return false
        }
        
        // Record the move for undo functionality
        val oldValue = cell.value
        moveHistory.add(Move(row, col, oldValue, value))
        
        // Clear redo stack when a new move is made
        redoStack.clear()
        
        // Set the new value
        cell.value = value
        
        // Clear notes when setting a value
        if (value > 0) {
            cell.clearNotes()
        }
        
        return true
    }
    
    /**
     * Checks if the current board state is valid
     * (no duplicate numbers in rows, columns, or 3x3 boxes)
     */
    fun isValid(): Boolean {
        // Check rows
        for (row in 0 until 9) {
            if (!isRowValid(row)) return false
        }
        
        // Check columns
        for (col in 0 until 9) {
            if (!isColumnValid(col)) return false
        }
        
        // Check 3x3 boxes
        for (boxRow in 0 until 3) {
            for (boxCol in 0 until 3) {
                if (!isBoxValid(boxRow, boxCol)) return false
            }
        }
        
        return true
    }
    
    /**
     * Checks if a specific row is valid (no duplicate numbers)
     */
    fun isRowValid(row: Int): Boolean {
        val seen = mutableSetOf<Int>()
        for (col in 0 until 9) {
            val value = board[row][col].value
            if (value != 0) {
                if (value in seen) {
                    return false
                }
                seen.add(value)
            }
        }
        return true
    }
    
    /**
     * Checks if a specific column is valid (no duplicate numbers)
     */
    fun isColumnValid(col: Int): Boolean {
        val seen = mutableSetOf<Int>()
        for (row in 0 until 9) {
            val value = board[row][col].value
            if (value != 0) {
                if (value in seen) {
                    return false
                }
                seen.add(value)
            }
        }
        return true
    }
    
    /**
     * Checks if a specific 3x3 box is valid (no duplicate numbers)
     */
    fun isBoxValid(boxRow: Int, boxCol: Int): Boolean {
        val seen = mutableSetOf<Int>()
        val startRow = boxRow * 3
        val startCol = boxCol * 3
        
        for (row in startRow until startRow + 3) {
            for (col in startCol until startCol + 3) {
                val value = board[row][col].value
                if (value != 0) {
                    if (value in seen) {
                        return false
                    }
                    seen.add(value)
                }
            }
        }
        return true
    }
    
    /**
     * Checks if the board is completely filled
     */
    fun isFilled(): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col].value == 0) {
                    return false
                }
            }
        }
        return true
    }
    
    /**
     * Checks if the board is solved (filled and valid)
     */
    fun isSolved(): Boolean {
        return isFilled() && isValid()
    }
    
    /**
     * Undoes the last move
     * Returns true if a move was undone
     */
    fun undo(): Boolean {
        if (moveHistory.isEmpty()) {
            return false
        }
        
        val lastMove = moveHistory.removeAt(moveHistory.lastIndex)
        redoStack.add(lastMove)
        
        // Restore the previous value
        board[lastMove.row][lastMove.col].value = lastMove.oldValue
        
        return true
    }
    
    /**
     * Redoes the last undone move
     * Returns true if a move was redone
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) {
            return false
        }
        
        val redoMove = redoStack.removeAt(redoStack.lastIndex)
        moveHistory.add(redoMove)
        
        // Apply the redone move
        board[redoMove.row][redoMove.col].value = redoMove.newValue
        
        return true
    }
    
    /**
     * Sets up the board with a given puzzle
     * @param puzzle 9x9 array of integers (0 for empty cells)
     */
    fun setupPuzzle(puzzle: Array<Array<Int>>) {
        // Clear history
        moveHistory.clear()
        redoStack.clear()
        
        // Set up the board
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val value = puzzle[row][col]
                board[row][col] = Cell(
                    row = row,
                    col = col,
                    value = value,
                    isGiven = value > 0
                )
            }
        }
    }
    
    /**
     * Gets all cells with the specified value
     */
    fun getCellsWithValue(value: Int): List<Cell> {
        val cells = mutableListOf<Cell>()
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col].value == value) {
                    cells.add(board[row][col])
                }
            }
        }
        return cells
    }
    
    /**
     * Represents a move in the Sudoku game for undo/redo functionality
     */
    data class Move(
        val row: Int,
        val col: Int,
        val oldValue: Int,
        val newValue: Int
    )
}
