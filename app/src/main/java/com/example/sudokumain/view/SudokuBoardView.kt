package com.example.sudokumain.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.sudokumain.R
import com.example.sudokumain.model.Cell
import kotlin.math.min
import androidx.core.content.res.ResourcesCompat

/**
 * Custom view for rendering the Sudoku board.
 * Handles drawing the grid, cells, and user interactions.
 */
class SudokuBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Board dimensions
    private var boardSize = 9
    private var cellSize = 0f
    private var selectedRow = -1
    private var selectedCol = -1

    // Callback for cell selection
    private var onCellSelectedListener: ((Int, Int) -> Unit)? = null

    // Board data
    private var board: Array<Array<Cell>> = Array(9) { row ->
        Array(9) { col ->
            Cell(row, col)
        }
    }

    // Paints for drawing
    private val boardPaint = Paint().apply {
        color = context.getColor(R.color.grid_lines)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val boldLinePaint = Paint().apply {
        color = context.getColor(R.color.grid_lines_bold)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val selectedCellPaint = Paint().apply {
        color = context.getColor(R.color.selected_cell)
        style = Paint.Style.FILL
    }

    private val sameNumberHighlightPaint = Paint().apply {
        color = context.getColor(R.color.selected_cell)
        alpha = 100
        style = Paint.Style.FILL
    }

    private val givenNumberPaint = Paint().apply {
        color = context.getColor(R.color.text_primary)
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.roboto_bold)
    }

    private val userNumberPaint = Paint().apply {
        color = context.getColor(R.color.primary)
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)

    }

    private val errorNumberPaint = Paint().apply {
        color = context.getColor(R.color.error)
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val hintNumberPaint = Paint().apply {
        color = context.getColor(R.color.hint)
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val notesPaint = Paint().apply {
        color = context.getColor(R.color.notes)
        textSize = 12f
        textAlign = Paint.Align.CENTER
    }

    init {
        // Make view clickable
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        // Make the board square
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
        
        // Calculate cell size
        cellSize = size / boardSize.toFloat()
        
        // Adjust text sizes based on cell size
        givenNumberPaint.textSize = cellSize * 0.5f
        userNumberPaint.textSize = cellSize * 0.5f
        errorNumberPaint.textSize = cellSize * 0.5f
        hintNumberPaint.textSize = cellSize * 0.5f
        notesPaint.textSize = cellSize * 0.2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        board?.let {
            // Draw board background
            canvas.drawColor(Color.WHITE)
            // Highlight selected cell and same numbers
            highlightCells(canvas)
            // Draw grid lines
            drawGrid(canvas)
            // Draw cell contents (numbers and notes)
            drawCellContents(canvas)
        }

    }

    private fun highlightCells(canvas: Canvas) {
        // Highlight selected cell
        if (selectedRow >= 0 && selectedCol >= 0) {
            canvas.drawRect(
                selectedCol * cellSize,
                selectedRow * cellSize,
                (selectedCol + 1) * cellSize,
                (selectedRow + 1) * cellSize,
                selectedCellPaint
            )
            
            // Highlight same row and column
            canvas.drawRect(
                0f,
                selectedRow * cellSize,
                width.toFloat(),
                (selectedRow + 1) * cellSize,
                sameNumberHighlightPaint
            )
            
            canvas.drawRect(
                selectedCol * cellSize,
                0f,
                (selectedCol + 1) * cellSize,
                height.toFloat(),
                sameNumberHighlightPaint
            )
            
            // Highlight 3x3 box
            val boxRow = selectedRow / 3
            val boxCol = selectedCol / 3
            canvas.drawRect(
                boxCol * 3 * cellSize,
                boxRow * 3 * cellSize,
                (boxCol + 1) * 3 * cellSize,
                (boxRow + 1) * 3 * cellSize,
                sameNumberHighlightPaint
            )
            
            // Highlight same numbers
            val selectedValue = board[selectedRow][selectedCol].value
            if (selectedValue > 0) {
                for (row in 0 until boardSize) {
                    for (col in 0 until boardSize) {
                        if (board[row][col].value == selectedValue && (row != selectedRow || col != selectedCol)) {
                            canvas.drawRect(
                                col * cellSize,
                                row * cellSize,
                                (col + 1) * cellSize,
                                (row + 1) * cellSize,
                                sameNumberHighlightPaint
                            )
                        }
                    }
                }
            }
        }
    }

    private fun drawGrid(canvas: Canvas) {
        // Draw the grid lines
        for (i in 0..boardSize) {
            val paint = if (i % 3 == 0) boldLinePaint else boardPaint
            
            // Draw horizontal lines
            canvas.drawLine(
                0f,
                i * cellSize,
                width.toFloat(),
                i * cellSize,
                paint
            )
            
            // Draw vertical lines
            canvas.drawLine(
                i * cellSize,
                0f,
                i * cellSize,
                height.toFloat(),
                paint
            )
        }
    }

    private fun drawCellContents(canvas: Canvas) {
        // Draw numbers and notes
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val cell = board[row][col]
                val value = cell.value
                
                if (value != 0) {
                    // Determine which paint to use
                    val paint = when {
                        cell.isGiven -> givenNumberPaint
                        // Add more conditions for error and hint states if needed
                        else -> userNumberPaint
                    }
                    
                    // Draw the number
                    val textBounds = Rect()
                    val valueString = value.toString()
                    paint.getTextBounds(valueString, 0, valueString.length, textBounds)
                    
                    canvas.drawText(
                        valueString,
                        (col * cellSize) + cellSize / 2,
                        (row * cellSize) + cellSize / 2 + (textBounds.height() / 2),
                        paint
                    )
                } else if (cell.notes.isNotEmpty()) {
                    // Draw notes
                    drawNotes(canvas, row, col, cell.notes)
                }
            }
        }
    }

    private fun drawNotes(canvas: Canvas, row: Int, col: Int, notes: Set<Int>) {
        val thirdCellSize = cellSize / 3
        
        for (note in notes) {
            if (note < 1 || note > 9) continue
            
            // Calculate position within the cell (3x3 grid)
            val noteRow = (note - 1) / 3
            val noteCol = (note - 1) % 3
            
            val x = col * cellSize + noteCol * thirdCellSize + thirdCellSize / 2
            val y = row * cellSize + noteRow * thirdCellSize + thirdCellSize / 2 + notesPaint.textSize / 3
            
            canvas.drawText(note.toString(), x, y, notesPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            // Calculate which cell was touched
            val row = (event.y / cellSize).toInt()
            val col = (event.x / cellSize).toInt()

            // Ensure valid cell
            if (row in 0 until boardSize && col in 0 until boardSize) {
                selectedRow = row
                selectedCol = col
                onCellSelectedListener?.invoke(row, col)
                invalidate() // Redraw the view
                return true
            }
        }
        return false
    }

    /**
     * Updates the board data and redraws the view
     */
    fun updateBoard(newBoard: Array<Array<Cell>>) {
        board = newBoard
        invalidate()
    }

    /**
     * Sets the listener for cell selection events
     */
    fun setOnCellSelectedListener(listener: (Int, Int) -> Unit) {
        onCellSelectedListener = listener
    }

    /**
     * Selects a specific cell programmatically
     */
    fun selectCell(row: Int, col: Int) {
        if (row in 0 until boardSize && col in 0 until boardSize) {
            selectedRow = row
            selectedCol = col
            invalidate()
        }
    }

    /**
     * Clears the current selection
     */
    fun clearSelection() {
        selectedRow = -1
        selectedCol = -1
        invalidate()
    }

    /**
     * Gets the currently selected cell position
     */
    fun getSelectedCell(): Pair<Int, Int>? {
        return if (selectedRow >= 0 && selectedCol >= 0) {
            Pair(selectedRow, selectedCol)
        } else {
            null
        }
    }
}
