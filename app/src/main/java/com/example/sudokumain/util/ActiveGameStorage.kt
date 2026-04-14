package com.example.sudokumain.util

import android.content.Context
import com.example.sudokumain.model.Cell
import com.example.sudokumain.model.Difficulty
import com.example.sudokumain.model.GameState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists up to three saved Sudoku sessions so players can switch between puzzles.
 */
object ActiveGameStorage {
    private const val PREFS_NAME = "SudokuGameSaves"
    private const val KEY_SAVED_GAMES = "saved_games"
    const val MAX_SAVED_GAMES = 3

    data class SavedSession(
        val slotIndex: Int,
        val saveGame: GameState.SaveGame,
        val isNotesMode: Boolean,
        val selectedRow: Int,
        val selectedCol: Int
    )

    data class SavedGameSummary(
        val slotIndex: Int,
        val id: String,
        val difficulty: Difficulty,
        val elapsedTime: Long,
        val hintsUsed: Int,
        val timestamp: Long,
        val isDailyChallenge: Boolean
    )

    fun hasSavedGame(context: Context): Boolean = getSavedGames(context).isNotEmpty()

    fun getSavedGames(context: Context): List<SavedSession> {
        val jsonString = getPreferences(context).getString(KEY_SAVED_GAMES, null) ?: return emptyList()

        return runCatching {
            val jsonArray = JSONArray(jsonString)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(index)
                    add(jsonToSavedSession(json))
                }
            }.sortedByDescending { it.saveGame.timestamp }
        }.getOrElse { emptyList() }
    }

    fun getSavedGameSummaries(context: Context): List<SavedGameSummary> {
        return getSavedGames(context).map { session ->
            SavedGameSummary(
                slotIndex = session.slotIndex,
                id = session.saveGame.id,
                difficulty = session.saveGame.difficulty,
                elapsedTime = session.saveGame.elapsedTime,
                hintsUsed = session.saveGame.hintsUsed,
                timestamp = session.saveGame.timestamp,
                isDailyChallenge = session.saveGame.isDailyChallenge
            )
        }
    }

    fun loadGame(context: Context, slotIndex: Int): SavedSession? {
        return getSavedGames(context).firstOrNull { it.slotIndex == slotIndex }
    }

    fun loadMostRecentGame(context: Context): SavedSession? {
        return getSavedGames(context).maxByOrNull { it.saveGame.timestamp }
    }

    fun saveGame(
        context: Context,
        gameState: GameState,
        isNotesMode: Boolean,
        selectedRow: Int,
        selectedCol: Int,
        slotIndex: Int? = null
    ): Int {
        val saves = getSavedGames(context).toMutableList()
        val existingIndex = saves.indexOfFirst { it.saveGame.id == gameState.getGameId() }
        val targetSlot = when {
            existingIndex >= 0 -> saves[existingIndex].slotIndex
            slotIndex != null -> slotIndex.coerceIn(0, MAX_SAVED_GAMES - 1)
            saves.size < MAX_SAVED_GAMES -> firstAvailableSlot(saves)
            else -> saves.minByOrNull { it.saveGame.timestamp }?.slotIndex ?: 0
        }

        val updatedSession = SavedSession(
            slotIndex = targetSlot,
            saveGame = gameState.createSaveGame(),
            isNotesMode = isNotesMode,
            selectedRow = selectedRow,
            selectedCol = selectedCol
        )

        val filtered = saves.filterNot {
            it.slotIndex == targetSlot || it.saveGame.id == updatedSession.saveGame.id
        }.toMutableList()
        filtered.add(updatedSession)
        persistSessions(context, filtered)
        return targetSlot
    }

    fun clear(context: Context, slotIndex: Int? = null) {
        if (slotIndex == null) {
            getPreferences(context).edit().remove(KEY_SAVED_GAMES).apply()
            return
        }

        val remaining = getSavedGames(context).filterNot { it.slotIndex == slotIndex }
        persistSessions(context, remaining)
    }

    fun findSlotForGameId(context: Context, gameId: String): Int? {
        return getSavedGames(context).firstOrNull { it.saveGame.id == gameId }?.slotIndex
    }

    fun isFull(context: Context): Boolean = getSavedGames(context).size >= MAX_SAVED_GAMES

    private fun persistSessions(context: Context, sessions: List<SavedSession>) {
        val jsonArray = JSONArray().apply {
            sessions.sortedBy { it.slotIndex }.forEach { session ->
                put(savedSessionToJson(session))
            }
        }

        getPreferences(context).edit()
            .putString(KEY_SAVED_GAMES, jsonArray.toString())
            .apply()
    }

    private fun savedSessionToJson(session: SavedSession): JSONObject {
        val saveGame = session.saveGame
        return JSONObject().apply {
            put("slot_index", session.slotIndex)
            put("id", saveGame.id)
            put("difficulty", saveGame.difficulty.name)
            put("elapsed_time", saveGame.elapsedTime)
            put("hints_used", saveGame.hintsUsed)
            put("timestamp", saveGame.timestamp)
            put("is_daily_challenge", saveGame.isDailyChallenge)
            put("is_notes_mode", session.isNotesMode)
            put("selected_row", session.selectedRow)
            put("selected_col", session.selectedCol)
            put("board", boardToJson(saveGame.boardState))
        }
    }

    private fun jsonToSavedSession(json: JSONObject): SavedSession {
        val saveGame = GameState.SaveGame(
            id = json.getString("id"),
            difficulty = Difficulty.valueOf(json.getString("difficulty")),
            boardState = jsonToBoard(json.getJSONArray("board")),
            elapsedTime = json.getLong("elapsed_time"),
            hintsUsed = json.getInt("hints_used"),
            timestamp = json.getLong("timestamp"),
            isDailyChallenge = json.optBoolean("is_daily_challenge", false)
        )

        return SavedSession(
            slotIndex = json.optInt("slot_index", 0),
            saveGame = saveGame,
            isNotesMode = json.optBoolean("is_notes_mode", false),
            selectedRow = json.optInt("selected_row", -1),
            selectedCol = json.optInt("selected_col", -1)
        )
    }

    private fun firstAvailableSlot(saves: List<SavedSession>): Int {
        val usedSlots = saves.map { it.slotIndex }.toSet()
        return (0 until MAX_SAVED_GAMES).firstOrNull { it !in usedSlots } ?: 0
    }

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
