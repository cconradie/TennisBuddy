package com.tennisbuddy.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {
    private var clock = 0L
    private val controller = MatchController(MatchRepository(InMemoryStorage())) { clock }

    private fun play(winner: Player, seconds: Long) {
        controller.startMatch(MatchFormat.SINGLES, listOf("Me"), listOf("Them"))
        repeat(12 * 4) { controller.pointWonBy(winner) }
        clock += seconds * 1000
        controller.closeMatch()
    }

    @Test
    fun emptyRegisterHasZeroStats() {
        val s = computeStats(emptyList())
        assertEquals(0, s.matches)
        assertEquals(0, s.setsWonPercent)
    }

    @Test
    fun countsWinsLossesSetsAndTime() {
        play(Player.A, 0) // 2-0 win
        // Ended early after player B takes 4 points: nothing completed
        controller.startMatch(MatchFormat.SINGLES, listOf("Me"), listOf("Them"))
        repeat(4) { controller.pointWonBy(Player.B) }
        clock += 90_000
        controller.closeMatch()
        val s = computeStats(controller.data.value.history)
        assertEquals(2, s.matches)
        assertEquals(1, s.wins)
        assertEquals(0, s.losses)
        assertEquals(1, s.endedEarly)
        assertEquals(2, s.setsWon)
        assertEquals(2, s.setsPlayed)
        assertEquals(100, s.setsWonPercent)
        assertEquals(90_000L, s.totalMillis)
    }

    @Test
    fun setsWonPercentRounds() {
        assertEquals(67, PlayerStats(1, 0, 0, 0, 2, 3, 0).setsWonPercent)
        assertEquals(82, PlayerStats(1, 0, 0, 0, 9, 11, 0).setsWonPercent)
    }

    @Test
    fun profileNameSurvivesMatchChanges() {
        controller.setProfileName("  Cobus ")
        play(Player.B, 5)
        assertEquals("Cobus", controller.data.value.profileName)
        assertEquals(1, computeStats(controller.data.value.history).losses)
    }
}
