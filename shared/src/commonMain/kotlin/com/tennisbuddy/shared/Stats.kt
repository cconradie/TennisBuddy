package com.tennisbuddy.shared

/** Statistics for the device owner, who is always side A in every record. */
data class PlayerStats(
    val matches: Int,
    val wins: Int,
    val losses: Int,
    val endedEarly: Int,
    val setsWon: Int,
    val setsPlayed: Int,
    val totalMillis: Long,
) {
    /** Share of completed sets won, rounded to a whole percent. */
    val setsWonPercent: Int
        get() = if (setsPlayed == 0) 0 else (setsWon * 100 + setsPlayed / 2) / setsPlayed
}

fun computeStats(history: List<MatchRecord>): PlayerStats {
    var wins = 0
    var losses = 0
    var early = 0
    var setsWon = 0
    var setsPlayed = 0
    var total = 0L
    for (m in history) {
        when (m.state.winner) {
            Player.A -> wins++
            Player.B -> losses++
            null -> early++
        }
        setsPlayed += m.state.completedSets.size
        setsWon += m.state.completedSets.count { it.a > it.b }
        total += m.elapsedMillis(0)
    }
    return PlayerStats(history.size, wins, losses, early, setsWon, setsPlayed, total)
}
