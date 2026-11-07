package com.tennisbuddy.shared

import kotlinx.serialization.Serializable

@Serializable
enum class MatchFormat {
    SINGLES, DOUBLES;

    val playersPerSide: Int get() = if (this == SINGLES) 1 else 2
}

/** A match with its players, timing and score. Side A / side B are one player (singles) or a pair (doubles). */
@Serializable
data class MatchRecord(
    val id: String,
    val format: MatchFormat,
    val sideA: List<String>,
    val sideB: List<String>,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val state: MatchState,
) {
    val isFinished: Boolean get() = endedAtMillis != null

    fun names(side: Player): String = (if (side == Player.A) sideA else sideB).joinToString(" / ")

    /** Time played so far, or the final duration once the match has ended. */
    fun elapsedMillis(nowMillis: Long): Long = ((endedAtMillis ?: nowMillis) - startedAtMillis).coerceAtLeast(0)

    /** e.g. "6-4 3-6 2-1": completed sets, then the set in progress if it has any games. */
    fun scoreSummary(): String {
        val sets = state.completedSets.map { "${it.a}-${it.b}" }.toMutableList()
        if (state.currentSet != SetScore()) sets += "${state.currentSet.a}-${state.currentSet.b}"
        return if (sets.isEmpty()) "0-0" else sets.joinToString(" ")
    }
}

/** "mm:ss", or "h:mm:ss" from one hour. */
fun formatDuration(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    fun two(n: Long) = n.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(s)}" else "${two(m)}:${two(s)}"
}
