package com.example.sudokumain.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.sudokumain.R
import com.example.sudokumain.model.Cell
import kotlin.math.min

/**
 * Custom view for rendering the Sudoku board.
 * Handles drawing the grid, cells, and user interactions.
 */
class SudokuBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var boardSize = 9
    private var cellSize = 0f
    private var selectedRow = -1
    private var selectedCol = -1
    private var highlightSameNumbersEnabled = true

    private var onCellSelectedListener: ((Int, Int) -> Unit)? = null

    private var board: Array<Array<Cell>> = Array(9) { row ->
        Array(9) { col ->
            Cell(row, col)
        }
    }

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
        typeface = ResourcesCompat.getFont(context, R.font.roboto_bold)
    }

    private val notesPaint = Paint().apply {
        color = context.getColor(R.color.notes)
        textSize = 12f
        textAlign = Paint.Align.CENTER
    }

    init {
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)

        cellSize = size / boardSize.toFloat()

        givenNumberPaint.textSize = cellSize * 0.5f
        userNumberPaint.textSize = cellSize * 0.5f
        errorNumberPaint.textSize = cellSize * 0.5f
        notesPaint.textSize = cellSize * 0.2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        highlightCells(canvas)
        drawGrid(canvas)
        drawCellContents(canvas)
    }

    private fun highlightCells(canvas: Canvas) {
        if (selectedRow >= 0 && selectedCol >= 0) {
            if (highlightSameNumbersEnabled) {
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

                val boxRow = selectedRow / 3
                val boxCol = selectedCol / 3
                canvas.drawRect(
                    boxCol * 3 * cellSize,
                    boxRow * 3 * cellSize,
                    (boxCol + 1) * 3 * cellSize,
                    (boxRow + 1) * 3 * cellSize,
                    sameNumberHighlightPaint
                )
            }

            canvas.drawRect(
                selectedCol * cellSize,
                selectedRow * cellSize,
                (selectedCol + 1) * cellSize,
                (selectedRow + 1) * cellSize,
                selectedCellPaint
            )

            val selectedValue = board[selectedRow][selectedCol].value
            if (highlightSameNumbersEnabled && selectedValue > 0) {
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
        for (i in 0..boardSize) {
            val paint = if (i % 3 == 0) boldLinePaint else boardPaint

            canvas.drawLine(
                0f,
                i * cellSize,
                width.toFloat(),
                i * cellSize,
                paint
            )

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
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val cell = board[row][col]
                val value = cell.value

                if (value != 0) {
                    val paint = when {
                        cell.isConflict -> errorNumberPaint
                        cell.isGiven -> givenNumberPaint
                        else -> userNumberPaint
                    }

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
                    drawNotes(canvas, row, col, cell.notes)
                }
            }
        }
    }

    private fun drawNotes(canvas: Canvas, row: Int, col: Int, notes: Set<Int>) {
        val thirdCellSize = cellSize / 3

        for (note in notes) {
            if (note !in 1..9) continue

            val noteRow = (note - 1) / 3
            val noteCol = (note - 1) % 3

            val x = col * cellSize + noteCol * thirdCellSize + thirdCellSize / 2
            val y = row * cellSize + noteRow * thirdCellSize + thirdCellSize / 2 + notesPaint.textSize / 3

            canvas.drawText(note.toString(), x, y, notesPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val row = (event.y / cellSize).toInt()
            val col = (event.x / cellSize).toInt()

            if (row in 0 until boardSize && col in 0 until boardSize) {
                selectedRow = row
                selectedCol = col
                onCellSelectedListener?.invoke(row, col)
                invalidate()
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
     * Enables or disables same-number highlighting.
     */
    fun setHighlightSameNumbersEnabled(enabled: Boolean) {
        highlightSameNumbersEnabled = enabled
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
