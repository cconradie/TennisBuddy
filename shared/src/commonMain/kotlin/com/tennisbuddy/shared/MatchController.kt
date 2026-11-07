package com.tennisbuddy.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Platform-neutral match manager shared by Android and iOS. Holds the current match and the
 * register of past matches, and saves every change through [repository].
 */
class MatchController(
    private val repository: MatchRepository,
    private val nowMillis: () -> Long,
) {
    private val undoStack = ArrayDeque<MatchState>()
    private val _data = MutableStateFlow(repository.load())
    val data: StateFlow<StoreData> = _data.asStateFlow()

    /** Begins a new match. A previous match still in progress is saved to the register as ended early. */
    fun startMatch(
        format: MatchFormat,
        sideA: List<String>,
        sideB: List<String>,
        config: MatchConfig = MatchConfig(),
    ) {
        closeMatch()
        val now = nowMillis()
        val record = MatchRecord(
            id = newId(now),
            format = format,
            sideA = normalizeNames(sideA, format.playersPerSide, "A"),
            sideB = normalizeNames(sideB, format.playersPerSide, "B"),
            startedAtMillis = now,
            state = MatchState(config),
        )
        commit(_data.value.copy(current = record))
    }

    fun pointWonBy(player: Player) {
        val record = _data.value.current ?: return
        if (record.state.isOver) return
        undoStack.addLast(record.state)
        val newState = record.state.pointWonBy(player)
        if (newState.isOver) {
            val finished = record.copy(state = newState, endedAtMillis = nowMillis())
            commit(_data.value.copy(current = finished, history = upsert(_data.value.history, finished)))
        } else {
            commit(_data.value.copy(current = record.copy(state = newState)))
        }
    }

    /** Runs a command received from a watch. Ignored when no match is being played. */
    fun apply(command: WatchCommand) {
        when (command) {
            WatchCommand.POINT_A -> pointWonBy(Player.A)
            WatchCommand.POINT_B -> pointWonBy(Player.B)
            WatchCommand.UNDO -> undo()
        }
    }

    /** Takes back the last point (also reopens a match that was just won). */
    fun undo() {
        val record = _data.value.current ?: return
        val previous = undoStack.removeLastOrNull() ?: return
        val reopened = record.copy(state = previous, endedAtMillis = null)
        commit(_data.value.copy(current = reopened, history = _data.value.history.filterNot { it.id == record.id }))
    }

    /**
     * Leaves the current match. If it was still in progress it is saved to the register as
     * ended early; a match that was already won is in the register already.
     */
    fun closeMatch() {
        val record = _data.value.current ?: return
        undoStack.clear()
        val history = if (record.isFinished) {
            _data.value.history
        } else {
            upsert(_data.value.history, record.copy(endedAtMillis = nowMillis()))
        }
        commit(_data.value.copy(current = null, history = history))
    }

    fun setProfileName(name: String) {
        commit(_data.value.copy(profileName = name.trim()))
    }

    fun deleteRecord(id: String) {
        commit(_data.value.copy(history = _data.value.history.filterNot { it.id == id }))
    }

    /** Unique even if two matches start in the same millisecond. */
    private fun newId(now: Long): String {
        val taken = _data.value.history.map { it.id }.toSet()
        var id = "match-$now"
        var n = 1
        while (id in taken) id = "match-$now-${n++}"
        return id
    }

    private fun upsert(history: List<MatchRecord>, record: MatchRecord): List<MatchRecord> =
        listOf(record) + history.filterNot { it.id == record.id }

    private fun commit(data: StoreData) {
        _data.value = data
        repository.save(data)
    }

    private fun normalizeNames(names: List<String>, count: Int, side: String): List<String> =
        (0 until count).map { i ->
            names.getOrNull(i)?.trim().orEmpty().ifEmpty {
                if (count == 1) "Player $side" else "Player $side${i + 1}"
            }
        }
}
