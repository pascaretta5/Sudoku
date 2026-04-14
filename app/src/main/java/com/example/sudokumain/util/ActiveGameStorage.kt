package com.example.sudokumain.util

import android.content.Context
import com.example.sudokumain.model.Cell
import com.example.sudokumain.model.Difficulty
import com.example.sudokumain.model.GameState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the currently active Sudoku game so it can be resumed later.
 */
object ActiveGameStorage {
    private const val PREFS_NAME = "SudokuGameSaves"
    private const val KEY_ACTIVE_GAME = "active_game"

    data class SavedSession(
        val saveGame: GameState.SaveGame,
        val isNotesMode: Boolean,
        val selectedRow: Int,
        val selectedCol: Int
    )

    fun hasSavedGame(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(KEY_ACTIVE_GAME)
    }

    fun saveGame(
        context: Context,
        gameState: GameState,
        isNotesMode: Boolean,
        selectedRow: Int,
        selectedCol: Int
    ) {
        val saveGame = gameState.createSaveGame()
        val json = JSONObject().apply {
            put("id", saveGame.id)
            put("difficulty", saveGame.difficulty.name)
            put("elapsed_time", saveGame.elapsedTime)
            put("hints_used", saveGame.hintsUsed)
            put("timestamp", saveGame.timestamp)
            put("is_daily_challenge", saveGame.isDailyChallenge)
            put("is_notes_mode", isNotesMode)
            put("selected_row", selectedRow)
            put("selected_col", selectedCol)
            put("board", boardToJson(saveGame.boardState))
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_GAME, json.toString())
            .apply()
    }

    fun loadGame(context: Context): SavedSession? {
        val jsonString = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_GAME, null) ?: return null

        return runCatching {
            val json = JSONObject(jsonString)
            val saveGame = GameState.SaveGame(
                id = json.getString("id"),
                difficulty = Difficulty.valueOf(json.getString("difficulty")),
                boardState = jsonToBoard(json.getJSONArray("board")),
                elapsedTime = json.getLong("elapsed_time"),
                hintsUsed = json.getInt("hints_used"),
                timestamp = json.getLong("timestamp"),
                isDailyChallenge = json.optBoolean("is_daily_challenge", false)
            )

            SavedSession(
                saveGame = saveGame,
                isNotesMode = json.optBoolean("is_notes_mode", false),
                selectedRow = json.optInt("selected_row", -1),
                selectedCol = json.optInt("selected_col", -1)
            )
        }.getOrNull()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE_GAME)
            .apply()
    }

    private fun boardToJson(board: Array<Array<Cell>>): JSONArray {
        return JSONArray().apply {
            board.forEach { row ->
                put(JSONArray().apply {
                    row.forEach { cell ->
                        put(JSONObject().apply {
                            put("value", cell.value)
                            put("is_given", cell.isGiven)
                            put("notes", JSONArray(cell.notes.sorted()))
                        })
                    }
                })
            }
        }
    }

    private fun jsonToBoard(boardJson: JSONArray): Array<Array<Cell>> {
        return Array(9) { row ->
            Array(9) { col ->
                val cellJson = boardJson.getJSONArray(row).getJSONObject(col)
                val notesJson = cellJson.optJSONArray("notes") ?: JSONArray()
                val notes = mutableSetOf<Int>()
                for (index in 0 until notesJson.length()) {
                    notes.add(notesJson.getInt(index))
                }

                Cell(
                    row = row,
                    col = col,
                    value = cellJson.optInt("value", 0),
                    isGiven = cellJson.optBoolean("is_given", false),
                    notes = notes
                )
            }
        }
    }
}
