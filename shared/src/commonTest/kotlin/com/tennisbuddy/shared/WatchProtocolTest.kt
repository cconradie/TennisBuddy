package com.tennisbuddy.shared

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchProtocolTest {
    private var clock = 1_000L
    private val c = MatchController(MatchRepository(InMemoryStorage())) { clock }

    private fun snapshot() = watchSnapshot(c.data.value.current)

    @Test
    fun noMatchGivesIdleSnapshot() {
        assertFalse(snapshot().active)
    }

    @Test
    fun snapshotShowsScoreAndNames() {
        c.startMatch(MatchFormat.DOUBLES, listOf("Ann", "Bea"), listOf("Cy", "Di"))
        repeat(2) { c.apply(WatchCommand.POINT_A) }
        c.apply(WatchCommand.POINT_B)
        val s = snapshot()
        assertTrue(s.active)
        assertEquals("Ann / Bea", s.nameA)
        assertEquals("Cy / Di", s.nameB)
        assertEquals("30", s.pointsA)
        assertEquals("15", s.pointsB)
        assertEquals(1_000L, s.startedAtMillis)
        assertFalse(s.over)
    }

    @Test
    fun commandsScoreAndUndo() {
        c.startMatch(MatchFormat.SINGLES, listOf("A"), listOf("B"))
        c.apply(WatchCommand.POINT_B)
        assertEquals("15", snapshot().pointsB)
        c.apply(WatchCommand.UNDO)
        assertEquals("0", snapshot().pointsB)
    }

    @Test
    fun commandsAreIgnoredWithoutAMatch() {
        c.apply(WatchCommand.POINT_A)
        c.apply(WatchCommand.UNDO)
        assertNull(c.data.value.current)
    }

    @Test
    fun snapshotMarksWinner() {
        c.startMatch(MatchFormat.SINGLES, listOf("A"), listOf("B"))
        repeat(12 * 4) { c.apply(WatchCommand.POINT_A) }
        val s = snapshot()
        assertTrue(s.over)
        assertEquals("A", s.winner)
        assertEquals(2, s.setsA)
    }

    @Test
    fun decodeCommandIsStrict() {
        assertEquals(WatchCommand.POINT_A, WatchProtocol.decodeCommand("POINT_A"))
        assertEquals(WatchCommand.UNDO, WatchProtocol.decodeCommand(" UNDO\n"))
        assertNull(WatchProtocol.decodeCommand("point_a"))
        assertNull(WatchProtocol.decodeCommand("DROP TABLE"))
        assertNull(WatchProtocol.decodeCommand(""))
    }

    @Test
    fun encodedSnapshotRoundTripsAndIsSmall() {
        c.startMatch(MatchFormat.SINGLES, listOf("Roger"), listOf("Rafa"))
        val text = WatchProtocol.encode(snapshot())
        assertEquals(snapshot(), Json.decodeFromString(WatchSnapshot.serializer(), text))
        assertTrue(text.length < 500, "payload should stay small for a watch link, was ${text.length}")
    }
}
