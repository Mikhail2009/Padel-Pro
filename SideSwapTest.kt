package com.padelpro.app

import com.padelpro.app.domain.engine.MatchConfig
import com.padelpro.app.domain.engine.MatchEngine
import com.padelpro.app.domain.engine.MatchEngineEvent
import com.padelpro.app.domain.engine.SideSwap
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SideSwapTest {

    private fun config() = MatchConfig(
        matchId = "m", format = MatchFormat.DOUBLES, deuceMode = DeuceMode.GOLDEN_POINT,
        firstServerTeam = TeamSide.A, startedAtMillis = 0L,
        teamAPlayerNames = listOf("A1", "A2"), teamBPlayerNames = listOf("B1", "B2")
    )

    private fun play(points: List<TeamSide>): com.padelpro.app.domain.engine.MatchEngineState {
        var t = 0L
        val events = points.map { t += 60_000; MatchEngineEvent.PointWon(it, t) }
        return MatchEngine.replay(config(), events)
    }

    @Test fun startNoSwap() {
        assertFalse(SideSwap.isSwapped(play(emptyList()), autoEnabled = true, manualTaps = 0))
    }

    @Test fun swapAfterFirstGame() {
        val s = play(List(4) { TeamSide.A })          // 1-0
        assertEquals(1, SideSwap.autoChanges(s))
        assertTrue(SideSwap.isSwapped(s, true, 0))
    }

    @Test fun noExtraSwapAfterSecondGame() {
        val s = play(List(4) { TeamSide.A } + List(4) { TeamSide.B })  // 1-1
        assertEquals(1, SideSwap.autoChanges(s))
    }

    @Test fun swapBackAfterThirdGame() {
        val s = play(List(4) { TeamSide.A } + List(4) { TeamSide.B } + List(4) { TeamSide.A }) // 2-1
        assertEquals(2, SideSwap.autoChanges(s))
        assertFalse(SideSwap.isSwapped(s, true, 0))
    }

    @Test fun manualTapInvertsWhenAutoDisabled() {
        val s = play(List(4) { TeamSide.A })
        assertFalse(SideSwap.isSwapped(s, autoEnabled = false, manualTaps = 0))
        assertTrue(SideSwap.isSwapped(s, autoEnabled = false, manualTaps = 1))
        assertFalse(SideSwap.isSwapped(s, autoEnabled = false, manualTaps = 2))
    }

    @Test fun undoRollsBackSides() {
        val full = List(4) { TeamSide.A }
        assertTrue(SideSwap.isSwapped(play(full), true, 0))
        assertFalse(SideSwap.isSwapped(play(full.dropLast(1)), true, 0))
    }

    @Test fun tieBreakSwapsEverySixPoints() {
        val games = MutableList(24) { if (it / 4 % 2 == 0) TeamSide.A else TeamSide.B } // 6-6
        val tb = List(6) { TeamSide.A }
        val s = play(games + tb)
        val base = SideSwap.autoChanges(play(games))
        assertEquals(base + 1, SideSwap.autoChanges(s))
    }
}