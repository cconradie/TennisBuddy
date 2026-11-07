package com.tennisbuddy.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchControllerTest {
    private var clock = 1_000L
    private val storage = InMemoryStorage()

    private fun controller() = MatchController(MatchRepository(storage)) { clock }

    private fun MatchController.winGames(p: Player, games: Int) = repeat(games * 4) { pointWonBy(p) }

    @Test
    fun singlesNamesAreTrimmedAndDefaulted() {
        val c = controller()
        c.startMatch(MatchFormat.SINGLES, listOf("  Roger "), listOf(""))
        val m = c.data.value.current!!
        assertEquals(listOf("Roger"), m.sideA)
        assertEquals(listOf("Player B"), m.sideB)
    }

    @Test
    fun doublesHasTwoNamesPerSide() {
        val c = controller()
        c.startMatch(MatchFormat.DOUBLES, listOf("Ann", "Bea"), listOf("Cy"))
        val m = c.data.value.current!!
        assertEquals("Ann / Bea", m.names(Player.A))
        assertEquals("Cy / Player B2", m.names(Player.B))
    }

    @Test
    fun winningArchivesMatchWithDuration() {
        val c = controller()
        c.startMatch(MatchFormat.SINGLES, listOf("A"), listOf("B"))
        clock += 65_000
        c.winGames(Player.A, 12)
        val d = c.data.value
        val m = assertNotNull(d.current)
        assertTrue(m.isFinished)
        assertEquals(Player.A, m.state.winner)
        assertEquals(listOf(m), d.history)
        assertEquals("01:05", formatDuration(m.elapsedMillis(clock + 999_999)))
        assertEquals("6-0 6-0", m.scoreSummary())
    }

    @Test
    fun undoAfterWinRemovesFromRegister() {
        val c = controller()
        c.startMatch(MatchFormat.SINGLES, listOf("A"), listOf("B"))
        c.winGames(Player.A, 12)
        c.undo()
        assertTrue(c.data.value.history.isEmpty())
        assertTrue(!c.data.value.current!!.isFinished)
    }

    @Test
    fun closingAnInProgressMatchSavesItAsEndedEarly() {
        val c = controller()
        c.startMatch(MatchFormat.SINGLES, listOf("A"), listOf("B"))
        c.winGames(Player.B, 2)
        clock += 10_000
        c.closeMatch()
        assertNull(c.data.value.current)
        val saved = c.data.value.history.single()
        assertEquals("0-2", saved.scoreSummary())
        assertNull(saved.state.winner)
        assertTrue(saved.isFinished)
    }

    @Test
    fun startingANewMatchArchivesTheOldOne() {
        val c = controller()
        c.startMatch(MatchFormat.SINGLES, listOf("A"), listOf("B"))
        c.pointWonBy(Player.A)
        clock += 5
        c.startMatch(MatchFormat.DOUBLES, listOf("1", "2"), listOf("3", "4"))
        assertEquals(1, c.data.value.history.size)
        assertEquals(MatchFormat.DOUBLES, c.data.value.current!!.format)
    }

    @Test
    fun registerSurvivesRestart() {
        val c = controller()
        c.startMatch(MatchFormat.SINGLES, listOf("A"), listOf("B"))
        c.winGames(Player.A, 12)
        c.startMatch(MatchFormat.DOUBLES, listOf("1", "2"), listOf("3", "4"))
        c.pointWonBy(Player.B)

        val reloaded = controller().data.value
        assertEquals(c.data.value, reloaded)
        assertEquals(1, reloaded.history.size)
        assertEquals("15", reloaded.current!!.state.pointDisplay(Player.B))
    }

    @Test
    fun deleteRemovesFromRegister() {
        val c = controller()
        c.startMatch(MatchFormat.SINGLES, listOf("A"), listOf("B"))
        c.winGames(Player.A, 12)
        c.closeMatch()
        c.deleteRecord(c.data.value.history.single().id)
        assertTrue(c.data.value.history.isEmpty())
    }

    @Test
    fun corruptStorageGivesEmptyStore() {
        storage.write("not json")
        assertEquals(StoreData(), controller().data.value)
    }

    @Test
    fun durationFormatting() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("09:07", formatDuration(547_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
    }
}
