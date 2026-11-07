package com.tennisbuddy.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Platform-neutral match controller. Exposes state as a [StateFlow] so both
 * Compose (Android) and SwiftUI (iOS) can observe it.
 */
class MatchController(config: MatchConfig = MatchConfig()) {
    private val history = ArrayDeque<MatchState>()
    private val _state = MutableStateFlow(MatchState(config))
    val state: StateFlow<MatchState> = _state.asStateFlow()

    fun pointWonBy(player: Player) {
        _state.update { current ->
            history.addLast(current)
            current.pointWonBy(player)
        }
    }

    fun undo() {
        history.removeLastOrNull()?.let { _state.value = it }
    }

    fun reset() {
        history.clear()
        _state.value = MatchState(_state.value.config)
    }
}
