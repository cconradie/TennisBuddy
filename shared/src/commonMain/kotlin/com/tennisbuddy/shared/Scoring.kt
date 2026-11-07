package com.tennisbuddy.shared

enum class Player {
    A, B;

    val other: Player get() = if (this == A) B else A
}

/** Point score within a game, as displayed on the scoreboard. */
fun pointLabel(points: Int, opponentPoints: Int): String = when {
    points >= 3 && opponentPoints >= 3 -> when {
        points == opponentPoints -> "40"
        points > opponentPoints -> "Ad"
        else -> "40"
    }
    else -> when (points) {
        0 -> "0"
        1 -> "15"
        2 -> "30"
        else -> "40"
    }
}

data class MatchConfig(
    val bestOfSets: Int = 3,
    val gamesPerSet: Int = 6,
    val tiebreakPoints: Int = 7,
) {
    init {
        require(bestOfSets % 2 == 1) { "bestOfSets must be odd" }
    }

    val setsToWin: Int get() = bestOfSets / 2 + 1
}

data class SetScore(val a: Int = 0, val b: Int = 0) {
    fun games(p: Player) = if (p == Player.A) a else b
    fun withGame(p: Player) = if (p == Player.A) copy(a = a + 1) else copy(b = b + 1)
}

/** Immutable match state. Call [pointWonBy] to get the next state. */
data class MatchState(
    val config: MatchConfig = MatchConfig(),
    val completedSets: List<SetScore> = emptyList(),
    val currentSet: SetScore = SetScore(),
    val pointsA: Int = 0,
    val pointsB: Int = 0,
    val tiebreak: Boolean = false,
    val winner: Player? = null,
) {
    val isOver: Boolean get() = winner != null

    fun points(p: Player) = if (p == Player.A) pointsA else pointsB

    fun setsWon(p: Player): Int = completedSets.count { it.games(p) > it.games(p.other) }

    /** Scoreboard text for the current game, e.g. "30" or "Ad" (or raw numbers in a tiebreak). */
    fun pointDisplay(p: Player): String =
        if (tiebreak) points(p).toString() else pointLabel(points(p), points(p.other))

    fun pointWonBy(p: Player): MatchState {
        if (isOver) return this
        val mine = points(p) + 1
        val theirs = points(p.other)
        val gameWon = if (tiebreak) {
            mine >= config.tiebreakPoints && mine - theirs >= 2
        } else {
            mine >= 4 && mine - theirs >= 2
        }
        if (!gameWon) return withPoints(p, mine)
        return gameWonBy(p)
    }

    private fun withPoints(p: Player, value: Int) =
        if (p == Player.A) copy(pointsA = value) else copy(pointsB = value)

    private fun gameWonBy(p: Player): MatchState {
        val set = currentSet.withGame(p)
        val mine = set.games(p)
        val theirs = set.games(p.other)
        val n = config.gamesPerSet
        val setWon = when {
            tiebreak -> true
            mine >= n && mine - theirs >= 2 -> true
            mine == n + 1 && theirs == n -> true // e.g. 7-5
            else -> false
        }
        val reset = copy(pointsA = 0, pointsB = 0, tiebreak = false)
        if (setWon) {
            val sets = completedSets + set
            val won = sets.count { it.games(p) > it.games(p.other) }
            return reset.copy(
                completedSets = sets,
                currentSet = SetScore(),
                winner = if (won >= config.setsToWin) p else null,
            )
        }
        val startTiebreak = mine == n && theirs == n
        return reset.copy(currentSet = set, tiebreak = startTiebreak)
    }
}
