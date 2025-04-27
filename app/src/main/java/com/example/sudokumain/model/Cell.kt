package com.example.sudokumain.model

/**
 * Represents a single cell in the Sudoku grid.
 * 
 * @property row The row index of the cell (0-8)
 * @property col The column index of the cell (0-8)
 * @property value The current value of the cell (1-9, or 0 if empty)
 * @property isGiven Whether this cell was pre-filled (part of the original puzzle)
 * @property notes Set of possible values noted by the user (1-9)
 */
data class Cell(
    val row: Int,
    val col: Int,
    var value: Int = 0,
    val isGiven: Boolean = false,
    val notes: MutableSet<Int> = mutableSetOf()
) {
    /**
     * Checks if the cell is empty (has no value)
     */
    fun isEmpty(): Boolean = value == 0
    
    /**
     * Clears the current value if the cell is not a given cell
     */
    fun clear() {
        if (!isGiven) {
            value = 0
        }
    }
    
    /**
     * Adds a note to the cell
     */
    fun addNote(note: Int) {
        if (note in 1..9) {
            notes.add(note)
        }
    }
    
    /**
     * Removes a note from the cell
     */
    fun removeNote(note: Int) {
        notes.remove(note)
    }
    
    /**
     * Clears all notes from the cell
     */
    fun clearNotes() {
        notes.clear()
    }
    
    /**
     * Checks if the cell has a specific note
     */
    fun hasNote(note: Int): Boolean = notes.contains(note)
}
