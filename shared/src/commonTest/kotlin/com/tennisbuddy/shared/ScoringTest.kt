package com.tennisbuddy.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoringTest {
    private fun MatchState.win(p: Player, times: Int): MatchState =
        (1..times).fold(this) { s, _ -> s.pointWonBy(p) }

    private fun MatchState.game(p: Player) = win(p, 4)

    private fun MatchState.games(p: Player, n: Int) = (1..n).fold(this) { s, _ -> s.game(p) }

    @Test
    fun pointsProgress0_15_30_40() {
        var s = MatchState()
        val seen = mutableListOf(s.pointDisplay(Player.A))
        repeat(3) { s = s.pointWonBy(Player.A); seen += s.pointDisplay(Player.A) }
        assertEquals(listOf("0", "15", "30", "40"), seen)
    }

    @Test
    fun gameWonAt40ThenPoint() {
        val s = MatchState().game(Player.A)
        assertEquals(SetScore(1, 0), s.currentSet)
        assertEquals(0, s.pointsA)
    }

    @Test
    fun deuceAndAdvantage() {
        var s = MatchState().win(Player.A, 3).win(Player.B, 3)
        assertEquals("40", s.pointDisplay(Player.A))
        s = s.pointWonBy(Player.A)
        assertEquals("Ad", s.pointDisplay(Player.A))
        assertEquals("40", s.pointDisplay(Player.B))
        s = s.pointWonBy(Player.B) // back to deuce
        assertEquals("40", s.pointDisplay(Player.A))
        s = s.win(Player.B, 2)
        assertEquals(SetScore(0, 1), s.currentSet)
    }

    @Test
    fun setWonSixLove() {
        val s = MatchState().games(Player.A, 6)
        assertEquals(listOf(SetScore(6, 0)), s.completedSets)
        assertEquals(SetScore(), s.currentSet)
        assertEquals(1, s.setsWon(Player.A))
    }

    @Test
    fun mustWinByTwoGames() {
        var s = MatchState().games(Player.A, 5).games(Player.B, 5).games(Player.A, 1)
        assertEquals(SetScore(6, 5), s.currentSet)
        assertTrue(s.completedSets.isEmpty())
        s = s.game(Player.A)
        assertEquals(listOf(SetScore(7, 5)), s.completedSets)
    }

    @Test
    fun tiebreakAtSixAll() {
        var s = MatchState().games(Player.A, 5).games(Player.B, 5).game(Player.A).game(Player.B)
        assertTrue(s.tiebreak)
        s = s.win(Player.A, 6).win(Player.B, 6)
        assertTrue(s.tiebreak)
        s = s.win(Player.A, 2)
        assertEquals(listOf(SetScore(7, 6)), s.completedSets)
        assertTrue(!s.tiebreak)
    }

    @Test
    fun matchWonInStraightSets() {
        val s = MatchState().games(Player.A, 12)
        assertEquals(Player.A, s.winner)
        assertTrue(s.isOver)
        assertEquals(s, s.pointWonBy(Player.B)) // no scoring after match over
    }

    @Test
    fun matchNotOverAfterOneSet() {
        val s = MatchState().games(Player.A, 6)
        assertNull(s.winner)
    }

    @Test
    fun threeSetMatch() {
        val s = MatchState().games(Player.A, 6).games(Player.B, 6).games(Player.A, 6)
        assertEquals(Player.A, s.winner)
        assertEquals(2, s.setsWon(Player.A))
        assertEquals(1, s.setsWon(Player.B))
    }
}
