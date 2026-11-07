package com.tennisbuddy.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the phone sends to a watch: a small, self-contained picture of the current match,
 * already formatted for display so the watch app needs no scoring logic.
 */
@Serializable
data class WatchSnapshot(
    /** False when no match is being played; the watch should show "Start a match on your phone". */
    val active: Boolean,
    val format: String = "",
    val nameA: String = "",
    val nameB: String = "",
    /** Current game points as shown on a scoreboard: "0", "15", "30", "40", "Ad" (or raw numbers in a tiebreak). */
    val pointsA: String = "",
    val pointsB: String = "",
    val gamesA: Int = 0,
    val gamesB: Int = 0,
    val setsA: Int = 0,
    val setsB: Int = 0,
    val tiebreak: Boolean = false,
    /** True once the match has been won; the watch should disable the point buttons. */
    val over: Boolean = false,
    /** "A", "B" or empty. */
    val winner: String = "",
    /** Epoch millis; the watch can run its own clock from this. */
    val startedAtMillis: Long = 0,
)

/** What a watch can ask the phone to do. On the wire these are the plain strings POINT_A, POINT_B and UNDO. */
@Serializable
enum class WatchCommand { POINT_A, POINT_B, UNDO }

fun watchSnapshot(record: MatchRecord?): WatchSnapshot {
    if (record == null) return WatchSnapshot(active = false)
    val s = record.state
    return WatchSnapshot(
        active = true,
        format = record.format.name,
        nameA = record.names(Player.A),
        nameB = record.names(Player.B),
        pointsA = s.pointDisplay(Player.A),
        pointsB = s.pointDisplay(Player.B),
        gamesA = s.currentSet.a,
        gamesB = s.currentSet.b,
        setsA = s.setsWon(Player.A),
        setsB = s.setsWon(Player.B),
        tiebreak = s.tiebreak,
        over = s.isOver,
        winner = s.winner?.name.orEmpty(),
        startedAtMillis = record.startedAtMillis,
    )
}

object WatchProtocol {
    private val json = Json { encodeDefaults = true }

    fun encode(snapshot: WatchSnapshot): String = json.encodeToString(WatchSnapshot.serializer(), snapshot)

    /** Returns null for anything that is not a known command, so a garbled message is ignored. */
    fun decodeCommand(text: String): WatchCommand? =
        WatchCommand.entries.firstOrNull { it.name == text.trim() }
}
