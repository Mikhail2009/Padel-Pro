package com.padelpro.app

import com.padelpro.app.domain.engine.EngineReduceResult
import com.padelpro.app.domain.engine.MatchConfig
import com.padelpro.app.domain.engine.MatchEngine
import com.padelpro.app.domain.engine.MatchEngineEvent
import com.padelpro.app.domain.engine.MatchEngineState
import com.padelpro.app.domain.engine.RejectReason
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MatchEngineTest {

    private fun config(
        format: MatchFormat = MatchFormat.DOUBLES,
        deuceMode: DeuceMode = DeuceMode.ADVANTAGE,
        bestOf: Int = 3,
        firstServer: TeamSide = TeamSide.A
    ) = MatchConfig(
        matchId = "m-test",
        format = format,
        deuceMode = deuceMode,
        bestOf = bestOf,
        firstServerTeam = firstServer,
        startedAtMillis = 1_000L,
        teamAPlayerNames = if (format == MatchFormat.SINGLES) listOf("Иван") else listOf("Иван", "Даня"),
        teamBPlayerNames = if (format == MatchFormat.SINGLES) listOf("Марк") else listOf("Марк", "Лев")
    )

    private fun step(state: MatchEngineState, team: TeamSide, ts: Long = 2_000L): MatchEngineState {
        val res = MatchEngine.reduce(state, MatchEngineEvent.PointWon(team, ts))
        assertTrue("Expected Accepted for $team", res is EngineReduceResult.Accepted)
        return res.state
    }

    private fun winGame(state: MatchEngineState, team: TeamSide): MatchEngineState {
        var s = state
        repeat(4) { s = step(s, team) }
        return s
    }

    private fun winSetLove(state: MatchEngineState, team: TeamSide): MatchEngineState {
        var s = state
        repeat(6) { s = winGame(s, team) }
        return s
    }

    private fun reachDeuce(state: MatchEngineState): MatchEngineState {
        var s = step(state, TeamSide.A) // 15-0
        s = step(s, TeamSide.B) // 15-15
        s = step(s, TeamSide.A) // 30-15
        s = step(s, TeamSide.B) // 30-30
        s = step(s, TeamSide.A) // 40-30
        return step(s, TeamSide.B) // 40-40
    }

    private fun reachSixSix(state: MatchEngineState): MatchEngineState {
        var s = state
        repeat(5) {
            s = winGame(s, TeamSide.A)
            s = winGame(s, TeamSide.B)
        }
        s = winGame(s, TeamSide.A) // 6-5
        s = winGame(s, TeamSide.B) // 6-6 -> TB
        return s
    }

    // 1-6: Basic point progression & game win
    @Test fun test01_initialStateIsZeroZeroActive() {
        val s = MatchEngine.initialState(config())
        assertEquals(MatchStatus.ACTIVE, s.status)
        assertEquals("0", s.displayScoreA())
        assertEquals("0", s.displayScoreB())
        assertEquals(0, s.gamesA)
        assertEquals(0, s.gamesB)
        assertEquals(1, s.currentSetIndex)
    }

    @Test fun test02_teamAPointProgression_0_15_30_40() {
        var s = MatchEngine.initialState(config())
        s = step(s, TeamSide.A)
        assertEquals("15", s.displayScoreA())
        assertEquals("0", s.displayScoreB())
        s = step(s, TeamSide.A)
        assertEquals("30", s.displayScoreA())
        s = step(s, TeamSide.A)
        assertEquals("40", s.displayScoreA())
    }

    @Test fun test03_teamBPointProgression_0_15_30_40() {
        var s = MatchEngine.initialState(config())
        s = step(s, TeamSide.B)
        assertEquals("15", s.displayScoreB())
        s = step(s, TeamSide.B)
        assertEquals("30", s.displayScoreB())
        s = step(s, TeamSide.B)
        assertEquals("40", s.displayScoreB())
    }

    @Test fun test04_teamAWinsGameToLove() {
        val s = winGame(MatchEngine.initialState(config()), TeamSide.A)
        assertEquals(1, s.gamesA)
        assertEquals(0, s.gamesB)
        assertEquals("0", s.displayScoreA())
        assertEquals("0", s.displayScoreB())
    }

    @Test fun test05_teamBWinsGameAt40_30() {
        var s = MatchEngine.initialState(config())
        s = step(s, TeamSide.A)
        s = step(s, TeamSide.A)
        repeat(4) { s = step(s, TeamSide.B) }
        assertEquals(0, s.gamesA)
        assertEquals(1, s.gamesB)
    }

    @Test fun test06_totalPointsAreCountedPerTeam() {
        var s = MatchEngine.initialState(config())
        s = step(s, TeamSide.A)
        s = step(s, TeamSide.B)
        s = step(s, TeamSide.B)
        assertEquals(1, s.totalPointsWonA)
        assertEquals(2, s.totalPointsWonB)
    }

    // 7-18: Deuce modes (ADVANTAGE, GOLDEN_POINT, STAR_POINT)
    @Test fun test07_deuceFlagActiveAt40_40() {
        val s = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.ADVANTAGE)))
        assertTrue(s.isDeuce)
        assertNull(s.advantageTeam)
        assertEquals("40", s.displayScoreA())
        assertEquals("40", s.displayScoreB())
    }

    @Test fun test08_advantageMode_setsAdvantageAndDisplaysAD() {
        val d = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.ADVANTAGE)))
        val adA = step(d, TeamSide.A)
        assertFalse(adA.isDeuce)
        assertEquals(TeamSide.A, adA.advantageTeam)
        assertEquals("AD", adA.displayScoreA())
        assertEquals("40", adA.displayScoreB())
    }

    @Test fun test09_advantageMode_twoPointsFromDeuceWinsGame() {
        val d = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.ADVANTAGE)))
        val win = step(step(d, TeamSide.B), TeamSide.B)
        assertEquals(0, win.gamesA)
        assertEquals(1, win.gamesB)
    }

    @Test fun test10_advantageMode_unlimitedDeuceCycles() {
        var s = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.ADVANTAGE)))
        repeat(15) {
            s = step(s, TeamSide.A)
            assertEquals(TeamSide.A, s.advantageTeam)
            s = step(s, TeamSide.B)
            assertTrue(s.isDeuce)
            assertNull(s.advantageTeam)
            assertEquals(0, s.gamesA)
        }
        s = step(step(s, TeamSide.A), TeamSide.A)
        assertEquals(1, s.gamesA)
    }

    @Test fun test11_goldenPoint_activatesAtFirstDeuce() {
        val s = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.GOLDEN_POINT)))
        assertTrue(s.isDeuce)
        assertTrue(s.isGoldenPointActive)
        assertFalse(s.isStarPointDeciderActive)
    }

    @Test fun test12_goldenPoint_nextPointWinsImmediatelyForA() {
        val d = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.GOLDEN_POINT)))
        val s = step(d, TeamSide.A)
        assertEquals(1, s.gamesA)
        assertEquals(0, s.gamesB)
    }

    @Test fun test13_goldenPoint_nextPointWinsImmediatelyForB() {
        val d = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.GOLDEN_POINT)))
        val s = step(d, TeamSide.B)
        assertEquals(0, s.gamesA)
        assertEquals(1, s.gamesB)
    }

    @Test fun test14_starPoint_firstDeuceUsesRegularAdvantage() {
        val d = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.STAR_POINT)))
        assertFalse(d.isStarPointDeciderActive)
        val adA = step(d, TeamSide.A)
        assertEquals(TeamSide.A, adA.advantageTeam)
        assertEquals(0, adA.gamesA)
    }

    @Test fun test15_starPoint_firstAdvantageWinAwardsGame() {
        val d = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.STAR_POINT)))
        val s = step(step(d, TeamSide.A), TeamSide.A)
        assertEquals(1, s.gamesA)
    }

    @Test fun test16_starPoint_secondAdvantageUsesRegularAdvantage() {
        var s = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.STAR_POINT)))
        s = step(step(s, TeamSide.A), TeamSide.B) // Back to 2nd deuce (1 adv lost)
        assertTrue(s.isDeuce)
        assertFalse(s.isStarPointDeciderActive)
        assertEquals(1, s.deuceAdvantagesLost)
        s = step(s, TeamSide.B)
        assertEquals(TeamSide.B, s.advantageTeam)
        s = step(s, TeamSide.B)
        assertEquals(1, s.gamesB)
    }

    @Test fun test17_starPoint_thirdDeuceActivatesStarPointDecider() {
        var s = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.STAR_POINT)))
        s = step(step(s, TeamSide.A), TeamSide.B) // adv 1 lost -> 2nd deuce
        s = step(step(s, TeamSide.B), TeamSide.A) // adv 2 lost -> 3rd deuce
        assertTrue(s.isDeuce)
        assertEquals(2, s.deuceAdvantagesLost)
        assertTrue(s.isStarPointDeciderActive)
    }

    @Test fun test18_starPoint_thirdDeuceDeciderWinsOnSinglePoint_maxFivePointsAfterDeuce() {
        var s = reachDeuce(MatchEngine.initialState(config(deuceMode = DeuceMode.STAR_POINT)))
        s = step(step(s, TeamSide.A), TeamSide.B) // 2 pts
        s = step(step(s, TeamSide.B), TeamSide.A) // 4 pts
        s = step(s, TeamSide.A) // 5th point wins!
        assertEquals(1, s.gamesA)
        assertEquals(0, s.gamesB)
        assertEquals(0, s.deuceAdvantagesLost)
        assertFalse(s.isStarPointDeciderActive)
    }

    // 19-28: Set winning rules & Tie-Break entry
    @Test fun test19_setWonAt6_4() {
        var s = MatchEngine.initialState(config())
        repeat(4) {
            s = winGame(s, TeamSide.A)
            s = winGame(s, TeamSide.B)
        }
        s = winGame(s, TeamSide.A) // 5-4
        s = winGame(s, TeamSide.A) // 6-4 -> Set 1 won
        assertEquals(1, s.completedSets.size)
        assertEquals(6, s.completedSets[0].gamesA)
        assertEquals(4, s.completedSets[0].gamesB)
        assertEquals(2, s.currentSetIndex)
        assertEquals(0, s.gamesA)
    }

    @Test fun test20_6_5DoesNotWinSet() {
        var s = MatchEngine.initialState(config())
        repeat(5) {
            s = winGame(s, TeamSide.A)
            s = winGame(s, TeamSide.B)
        }
        s = winGame(s, TeamSide.A) // 6-5
        assertEquals(0, s.completedSets.size)
        assertEquals(6, s.gamesA)
        assertEquals(5, s.gamesB)
        assertFalse(s.isTieBreak)
    }

    @Test fun test21_7_5WinsSetWithoutTieBreak() {
        var s = MatchEngine.initialState(config())
        repeat(5) {
            s = winGame(s, TeamSide.A)
            s = winGame(s, TeamSide.B)
        }
        s = winGame(s, TeamSide.A) // 6-5
        s = winGame(s, TeamSide.A) // 7-5
        assertEquals(1, s.completedSets.size)
        assertEquals(7, s.completedSets[0].gamesA)
        assertEquals(5, s.completedSets[0].gamesB)
        assertNull(s.completedSets[0].tbA)
        assertNull(s.completedSets[0].tbB)
    }

    @Test fun test22_6_6EntersTieBreak() {
        val s = reachSixSix(MatchEngine.initialState(config()))
        assertTrue(s.isTieBreak)
        assertEquals(6, s.gamesA)
        assertEquals(6, s.gamesB)
        assertEquals("0", s.displayScoreA())
        assertEquals("0", s.displayScoreB())
    }

    @Test fun test23_tieBreakDisplaysNumericScores() {
        var s = reachSixSix(MatchEngine.initialState(config()))
        s = step(s, TeamSide.A)
        s = step(s, TeamSide.A)
        s = step(s, TeamSide.B)
        assertEquals("2", s.displayScoreA())
        assertEquals("1", s.displayScoreB())
    }

    @Test fun test24_tieBreakWonAt7_5() {
        var s = reachSixSix(MatchEngine.initialState(config()))
        repeat(5) {
            s = step(s, TeamSide.A)
            s = step(s, TeamSide.B)
        }
        s = step(s, TeamSide.A) // 6-5
        s = step(s, TeamSide.A) // 7-5
        assertEquals(1, s.completedSets.size)
        val set1 = s.completedSets[0]
        assertEquals(7, set1.gamesA)
        assertEquals(6, set1.gamesB)
        assertEquals(7, set1.tbA)
        assertEquals(5, set1.tbB)
        assertFalse(s.isTieBreak)
        assertEquals(2, s.currentSetIndex)
    }

    @Test fun test25_tieBreakAt6_6Requires2PointLead_7_6DoesNotWin() {
        var s = reachSixSix(MatchEngine.initialState(config()))
        repeat(6) {
            s = step(s, TeamSide.A)
            s = step(s, TeamSide.B)
        }
        s = step(s, TeamSide.A) // 7-6
        assertTrue(s.isTieBreak)
        assertEquals(0, s.completedSets.size)
        assertEquals(7, s.tbPointsA)
        assertEquals(6, s.tbPointsB)
    }

    @Test fun test26_tieBreakExtendedNoCap_12_10() {
        var s = reachSixSix(MatchEngine.initialState(config()))
        repeat(10) {
            s = step(s, TeamSide.A)
            s = step(s, TeamSide.B)
        }
        s = step(s, TeamSide.B) // 10-11
        s = step(s, TeamSide.B) // 10-12
        assertEquals(1, s.completedSets.size)
        assertEquals(6, s.completedSets[0].gamesA)
        assertEquals(7, s.completedSets[0].gamesB)
        assertEquals(10, s.completedSets[0].tbA)
        assertEquals(12, s.completedSets[0].tbB)
    }

    @Test fun test27_deuceModeDoesNotAlterTieBreak2PointLeadRule() {
        var s = reachSixSix(MatchEngine.initialState(config(deuceMode = DeuceMode.GOLDEN_POINT)))
        repeat(6) {
            s = step(s, TeamSide.A)
            s = step(s, TeamSide.B)
        }
        s = step(s, TeamSide.A) // 7-6 in TB must NOT end even in GOLDEN_POINT!
        assertEquals(0, s.completedSets.size)
        assertTrue(s.isTieBreak)
        s = step(s, TeamSide.A) // 8-6 wins
        assertEquals(1, s.completedSets.size)
    }

    @Test fun test28_bestOf3MatchCompletesOn2_0Win() {
        var s = winSetLove(MatchEngine.initialState(config()), TeamSide.A)
        assertEquals(MatchStatus.ACTIVE, s.status)
        s = winSetLove(s, TeamSide.A)
        assertEquals(MatchStatus.COMPLETED, s.status)
        assertEquals(TeamSide.A, s.winnerTeam)
        assertEquals(2, s.setsWonA)
        assertEquals(0, s.setsWonB)
    }

    // 29-38: Best-of-3 comeback, Serving rotation & TB -> next set server
    @Test fun test29_bestOf3MatchCompletesOn2_1Comeback() {
        var s = winSetLove(MatchEngine.initialState(config()), TeamSide.A) // 1-0
        s = winSetLove(s, TeamSide.B) // 1-1
        assertEquals(MatchStatus.ACTIVE, s.status)
        assertEquals(3, s.currentSetIndex)
        s = winSetLove(s, TeamSide.B) // 1-2
        assertEquals(MatchStatus.COMPLETED, s.status)
        assertEquals(TeamSide.B, s.winnerTeam)
    }

    @Test fun test30_singlesServiceAlternatesEveryGame() {
        var s = MatchEngine.initialState(config(format = MatchFormat.SINGLES, firstServer = TeamSide.A))
        assertEquals(TeamSide.A, s.servingTeam)
        assertEquals("Иван", s.servingPlayerName)

        s = winGame(s, TeamSide.A)
        assertEquals(TeamSide.B, s.servingTeam)
        assertEquals("Марк", s.servingPlayerName)

        s = winGame(s, TeamSide.B)
        assertEquals(TeamSide.A, s.servingTeam)
        assertEquals("Иван", s.servingPlayerName)
    }

    @Test fun test31_doublesServiceRotatesAcrossAll4Players() {
        var s = MatchEngine.initialState(config(format = MatchFormat.DOUBLES, firstServer = TeamSide.A))
        assertEquals(TeamSide.A, s.servingTeam)
        assertEquals(0, s.servingSlotInTeam)
        assertEquals("Иван", s.servingPlayerName)

        s = winGame(s, TeamSide.A) // B1 serves
        assertEquals(TeamSide.B, s.servingTeam)
        assertEquals(0, s.servingSlotInTeam)
        assertEquals("Марк", s.servingPlayerName)

        s = winGame(s, TeamSide.A) // A2 serves
        assertEquals(TeamSide.A, s.servingTeam)
        assertEquals(1, s.servingSlotInTeam)
        assertEquals("Даня", s.servingPlayerName)

        s = winGame(s, TeamSide.A) // B2 serves
        assertEquals(TeamSide.B, s.servingTeam)
        assertEquals(1, s.servingSlotInTeam)
        assertEquals("Лев", s.servingPlayerName)

        s = winGame(s, TeamSide.A) // Back to A1
        assertEquals(TeamSide.A, s.servingTeam)
        assertEquals(0, s.servingSlotInTeam)
        assertEquals("Иван", s.servingPlayerName)
    }

    @Test fun test32_tieBreakServeRotation_1Then2Points() {
        var s = reachSixSix(MatchEngine.initialState(config(format = MatchFormat.SINGLES, firstServer = TeamSide.A)))
        // After 12 games (A served 6, B served 6), Team A serves first in TB!
        assertEquals(TeamSide.A, s.tbFirstServerTeam)
        assertEquals(TeamSide.A, s.servingTeam) // pt 0 (1st point)

        s = step(s, TeamSide.A) // pt 1 -> B serves 2 pts
        assertEquals(TeamSide.B, s.servingTeam)
        s = step(s, TeamSide.B) // pt 2 -> still B
        assertEquals(TeamSide.B, s.servingTeam)

        s = step(s, TeamSide.A) // pt 3 -> A serves 2 pts
        assertEquals(TeamSide.A, s.servingTeam)
        s = step(s, TeamSide.B) // pt 4 -> still A
        assertEquals(TeamSide.A, s.servingTeam)

        s = step(s, TeamSide.A) // pt 5 -> B serves 2 pts
        assertEquals(TeamSide.B, s.servingTeam)
    }

    @Test fun test33_tieBreakDoublesPartnerRotationWithinTeam() {
        var s = reachSixSix(MatchEngine.initialState(config(format = MatchFormat.DOUBLES, firstServer = TeamSide.A)))
        // Team A served 6 games -> slot 0 ("Иван") serves 1st TB point
        assertEquals(TeamSide.A, s.servingTeam)
        assertEquals("Иван", s.servingPlayerName)

        s = step(s, TeamSide.A) // k=1 -> Team B serves (6 games -> "Марк")
        assertEquals("Марк", s.servingPlayerName)
        s = step(s, TeamSide.A) // k=2 -> still Team B ("Марк")
        assertEquals("Марк", s.servingPlayerName)

        s = step(s, TeamSide.B) // k=3 -> Team A next partner ("Даня")
        assertEquals("Даня", s.servingPlayerName)
        s = step(s, TeamSide.B) // k=4 -> still "Даня"
        assertEquals("Даня", s.servingPlayerName)

        s = step(s, TeamSide.A) // k=5 -> Team B next partner ("Лев")
        assertEquals("Лев", s.servingPlayerName)
    }

    @Test fun test34_nextSetFirstServerAfterTieBreakIsFirstReceiverOfTieBreak() {
        var s = reachSixSix(MatchEngine.initialState(config(firstServer = TeamSide.A)))
        assertEquals(TeamSide.A, s.tbFirstServerTeam)
        // Team A wins TB 7-0 -> Team B received first in TB -> Team B serves Set 2 Game 1!
        repeat(7) { s = step(s, TeamSide.A) }
        assertEquals(2, s.currentSetIndex)
        assertEquals(TeamSide.B, s.servingTeam)
    }

    @Test fun test35_nextSetFirstServerWhenFirstServerIsTeamB() {
        var s = reachSixSix(MatchEngine.initialState(config(firstServer = TeamSide.B)))
        // 12 games: B started, so TB first server is B, first receiver is A -> Set 2 starts with A serving
        assertEquals(TeamSide.B, s.tbFirstServerTeam)
        repeat(7) { s = step(s, TeamSide.B) }
        assertEquals(2, s.currentSetIndex)
        assertEquals(TeamSide.A, s.servingTeam)
    }

    @Test fun test36_serveSideAlternatesRightLeftEveryPoint() {
        var s = MatchEngine.initialState(config())
        assertTrue(s.serveSideFromRight) // 0-0 -> right
        s = step(s, TeamSide.A)
        assertFalse(s.serveSideFromRight) // 15-0 -> left
        s = step(s, TeamSide.B)
        assertTrue(s.serveSideFromRight) // 15-15 -> right
        s = step(s, TeamSide.A)
        assertFalse(s.serveSideFromRight) // 30-15 -> left
    }

    @Test fun test37_serveSideInTieBreakAlternatesEveryPoint() {
        var s = reachSixSix(MatchEngine.initialState(config()))
        assertTrue(s.serveSideFromRight) // 0-0
        s = step(s, TeamSide.A)
        assertFalse(s.serveSideFromRight) // 1-0
        s = step(s, TeamSide.B)
        assertTrue(s.serveSideFromRight) // 1-1
    }

    @Test fun test38_nextSetServerAfterRegular6_3SetAlternatesCorrectly() {
        var s = MatchEngine.initialState(config(firstServer = TeamSide.A))
        repeat(3) {
            s = winGame(s, TeamSide.A)
            s = winGame(s, TeamSide.B)
        }
        // 3-3 (6 games played -> A serves game 7)
        assertEquals(TeamSide.A, s.servingTeam)
        repeat(3) { s = winGame(s, TeamSide.A) } // 6-3 (9 games played, last server A)
        assertEquals(2, s.currentSetIndex)
        assertEquals(TeamSide.B, s.servingTeam)
    }

    // 39-45: Change ends & Derived flags (SET POINT, MATCH POINT)
    @Test fun test39_changeEndsAtOddGameTotalsOnly() {
        var s = MatchEngine.initialState(config())
        assertFalse(s.shouldChangeEnds) // 0-0
        s = winGame(s, TeamSide.A) // 1-0 -> change ends!
        assertTrue(s.shouldChangeEnds)
        s = step(s, TeamSide.A) // 15-0 -> no longer at start of game
        assertFalse(s.shouldChangeEnds)
        s = step(s, TeamSide.A)
        s = step(s, TeamSide.A)
        s = step(s, TeamSide.A) // 2-0 -> even games -> false
        assertFalse(s.shouldChangeEnds)
        s = winGame(s, TeamSide.B) // 2-1 -> odd -> true
        assertTrue(s.shouldChangeEnds)
    }

    @Test fun test40_changeEndsBetweenSetsDependsOnSetTotalGamesParity() {
        // 6-4 = 10 games (even) -> false
        var s = MatchEngine.initialState(config())
        repeat(4) {
            s = winGame(s, TeamSide.A)
            s = winGame(s, TeamSide.B)
        }
        s = winGame(s, TeamSide.A)
        s = winGame(s, TeamSide.A)
        assertFalse(s.shouldChangeEnds)

        // 6-3 = 9 games (odd) -> true
        var s2 = MatchEngine.initialState(config())
        repeat(3) {
            s2 = winGame(s2, TeamSide.A)
            s2 = winGame(s2, TeamSide.B)
        }
        repeat(3) { s2 = winGame(s2, TeamSide.A) }
        assertTrue(s2.shouldChangeEnds)
    }

    @Test fun test41_changeEndsEvery6PointsInTieBreak() {
        var s = reachSixSix(MatchEngine.initialState(config()))
        assertFalse(s.shouldChangeEnds) // 0 pts
        repeat(5) { s = step(s, TeamSide.A) }
        assertFalse(s.shouldChangeEnds) // 5 pts
        s = step(s, TeamSide.B) // 5-1 = 6 pts
        assertTrue(s.shouldChangeEnds)
        s = step(s, TeamSide.B) // 5-2 = 7 pts
        assertFalse(s.shouldChangeEnds)
    }

    @Test fun test42_setPointFlagsSetInRegularGame() {
        var s = MatchEngine.initialState(config())
        repeat(5) { s = winGame(s, TeamSide.A) } // 5-0
        s = step(s, TeamSide.A)
        s = step(s, TeamSide.A)
        assertFalse(s.isSetPointA)
        s = step(s, TeamSide.A) // 40-0 at 5-0
        assertTrue(s.isSetPointA)
        assertFalse(s.isSetPointB)
        assertFalse(s.isMatchPointA)
    }

    @Test fun test43_setPointInTieBreak() {
        var s = reachSixSix(MatchEngine.initialState(config()))
        repeat(5) { s = step(s, TeamSide.A) }
        s = step(s, TeamSide.B) // 5-1
        assertFalse(s.isSetPointA)
        s = step(s, TeamSide.A) // 6-1 -> set point A
        assertTrue(s.isSetPointA)
        assertFalse(s.isSetPointB)
    }

    @Test fun test44_matchPointWhenLeadingOneSetToZero() {
        var s = winSetLove(MatchEngine.initialState(config()), TeamSide.A)
        repeat(5) { s = winGame(s, TeamSide.A) }
        repeat(3) { s = step(s, TeamSide.A) } // 1-0 sets, 5-0 games, 40-0
        assertTrue(s.isSetPointA)
        assertTrue(s.isMatchPointA)
        assertFalse(s.isMatchPointB)
    }

    @Test fun test45_goldenPointAt5_4MakesSetPointForAOnly() {
        var s = MatchEngine.initialState(config(deuceMode = DeuceMode.GOLDEN_POINT))
        repeat(4) {
            s = winGame(s, TeamSide.A)
            s = winGame(s, TeamSide.B)
        }
        s = winGame(s, TeamSide.A) // 5-4
        s = reachDeuce(s) // 40-40 at 5-4
        assertTrue(s.isGoldenPointActive)
        assertTrue(s.isSetPointA)
        assertFalse(s.isSetPointB)
    }

    // 46-54: Rejected actions, Pause/Resume duration math
    @Test fun test46_pointAfterMatchCompletedIsRejected() {
        var s = winSetLove(MatchEngine.initialState(config()), TeamSide.A)
        s = winSetLove(s, TeamSide.A)
        val res = MatchEngine.reduce(s, MatchEngineEvent.PointWon(TeamSide.B, 9_000L))
        assertTrue(res is EngineReduceResult.Rejected)
        assertEquals(RejectReason.MATCH_ALREADY_COMPLETED, (res as EngineReduceResult.Rejected).reason)
    }

    @Test fun test47_pauseWhenCompletedIsRejected() {
        var s = winSetLove(MatchEngine.initialState(config()), TeamSide.A)
        s = winSetLove(s, TeamSide.A)
        val res = MatchEngine.reduce(s, MatchEngineEvent.Paused(9_000L))
        assertTrue(res is EngineReduceResult.Rejected)
        assertEquals(RejectReason.MATCH_ALREADY_COMPLETED, (res as EngineReduceResult.Rejected).reason)
    }

    @Test fun test48_pointWhilePausedIsRejected() {
        val init = MatchEngine.initialState(config())
        val paused = MatchEngine.reduce(init, MatchEngineEvent.Paused(5_000L)).state
        assertTrue(paused.isPaused)
        val res = MatchEngine.reduce(paused, MatchEngineEvent.PointWon(TeamSide.A, 6_000L))
        assertTrue(res is EngineReduceResult.Rejected)
        assertEquals(RejectReason.MATCH_PAUSED, (res as EngineReduceResult.Rejected).reason)
    }

    @Test fun test49_doublePauseIsRejected() {
        val init = MatchEngine.initialState(config())
        val paused = MatchEngine.reduce(init, MatchEngineEvent.Paused(5_000L)).state
        val res = MatchEngine.reduce(paused, MatchEngineEvent.Paused(6_000L))
        assertTrue(res is EngineReduceResult.Rejected)
        assertEquals(RejectReason.ALREADY_PAUSED, (res as EngineReduceResult.Rejected).reason)
    }

    @Test fun test50_resumeWhenNotPausedIsRejected() {
        val init = MatchEngine.initialState(config())
        val res = MatchEngine.reduce(init, MatchEngineEvent.Resumed(5_000L))
        assertTrue(res is EngineReduceResult.Rejected)
        assertEquals(RejectReason.NOT_PAUSED, (res as EngineReduceResult.Rejected).reason)
    }

    @Test fun test51_elapsedDurationExcludesPausedTime() {
        val cfg = config() // startedAt = 1_000
        val events = listOf(
            MatchEngineEvent.PointWon(TeamSide.A, 5_000L),
            MatchEngineEvent.Paused(11_000L),   // +10s played
            MatchEngineEvent.Resumed(61_000L),  // 50s paused (excluded)
            MatchEngineEvent.Paused(81_000L)    // +20s played
        )
        val s = MatchEngine.replay(cfg, events)
        assertTrue(s.isPaused)
        assertEquals(30_000L, s.elapsedDurationMillis(999_000L))
    }

    @Test fun test52_elapsedDurationContinuesRunningWhenUnpaused() {
        val cfg = config() // startedAt = 1_000
        val events = listOf(
            MatchEngineEvent.Paused(6_000L),    // 5s played
            MatchEngineEvent.Resumed(26_000L)   // resumed at 26s
        )
        val s = MatchEngine.replay(cfg, events)
        assertFalse(s.isPaused)
        assertEquals(15_000L, s.elapsedDurationMillis(36_000L))
    }

    // 53-60: Undo, Replay & Boundaries
    @Test fun test53_undoWithNoPointsReturnsNull() {
        val cfg = config()
        assertNull(MatchEngine.undoLastPoint(cfg, emptyList()))
        assertNull(
            MatchEngine.undoLastPoint(
                cfg,
                listOf(MatchEngineEvent.Paused(2_000L), MatchEngineEvent.Resumed(3_000L))
            )
        )
    }

    @Test fun test54_undoFirstPointReturnsZeroZero() {
        val cfg = config()
        val events = listOf(MatchEngineEvent.PointWon(TeamSide.A, 2_000L))
        val undone = MatchEngine.undoLastPoint(cfg, events)
        assertNotNull(undone)
        assertTrue(undone!!.first.isEmpty())
        assertEquals("0", undone.second.displayScoreA())
    }

    @Test fun test55_undoAcrossGameBoundaryRestoresPreviousGamePoints() {
        val cfg = config(deuceMode = DeuceMode.GOLDEN_POINT)
        val events = listOf(
            MatchEngineEvent.PointWon(TeamSide.A, 2_000L),
            MatchEngineEvent.PointWon(TeamSide.A, 3_000L),
            MatchEngineEvent.PointWon(TeamSide.A, 4_000L),
            MatchEngineEvent.PointWon(TeamSide.A, 5_000L) // wins game 1
        )
        val afterWin = MatchEngine.replay(cfg, events)
        assertEquals(1, afterWin.gamesA)
        assertEquals(TeamSide.B, afterWin.servingTeam)

        val undone = MatchEngine.undoLastPoint(cfg, events)!!
        assertEquals(0, undone.second.gamesA)
        assertEquals("40", undone.second.displayScoreA())
        assertEquals(TeamSide.A, undone.second.servingTeam)
    }

    @Test fun test56_undoAcrossSetBoundaryRestoresCompletedSetAndServer() {
        val cfg = config()
        val events = MutableList(24) { MatchEngineEvent.PointWon(TeamSide.A, 2_000L + it) }
        val set2Start = MatchEngine.replay(cfg, events)
        assertEquals(2, set2Start.currentSetIndex)
        assertEquals(1, set2Start.completedSets.size)

        val undone = MatchEngine.undoLastPoint(cfg, events)!!
        assertEquals(1, undone.second.currentSetIndex)
        assertEquals(0, undone.second.completedSets.size)
        assertEquals(5, undone.second.gamesA)
        assertEquals("40", undone.second.displayScoreA())
    }

    @Test fun test57_undoAcrossTieBreakBoundaryRestoresSetPoints() {
        val cfg = config()
        val events = mutableListOf<MatchEngineEvent>()
        var ts = 2_000L
        repeat(5) {
            repeat(4) { events.add(MatchEngineEvent.PointWon(TeamSide.A, ts++)) }
            repeat(4) { events.add(MatchEngineEvent.PointWon(TeamSide.B, ts++)) }
        }
        repeat(4) { events.add(MatchEngineEvent.PointWon(TeamSide.A, ts++)) } // 6-5
        repeat(4) { events.add(MatchEngineEvent.PointWon(TeamSide.B, ts++)) } // 6-6 -> TB

        val inTb = MatchEngine.replay(cfg, events)
        assertTrue(inTb.isTieBreak)

        val undone = MatchEngine.undoLastPoint(cfg, events)!!
        assertFalse(undone.second.isTieBreak)
        assertEquals(6, undone.second.gamesA)
        assertEquals(5, undone.second.gamesB)
        assertEquals("40", undone.second.displayScoreB())
    }

    @Test fun test58_undoAfterMatchCompletedRevertsToActive() {
        val cfg = config()
        val events = MutableList(48) { MatchEngineEvent.PointWon(TeamSide.A, 2_000L + it) }
        val completed = MatchEngine.replay(cfg, events)
        assertEquals(MatchStatus.COMPLETED, completed.status)
        assertEquals(TeamSide.A, completed.winnerTeam)

        val undone = MatchEngine.undoLastPoint(cfg, events)!!
        assertEquals(MatchStatus.ACTIVE, undone.second.status)
        assertNull(undone.second.winnerTeam)
        assertTrue(undone.second.isMatchPointA)
    }

    @Test fun test59_undoPreservesPauseAndResumeEvents() {
        val cfg = config()
        val events = listOf(
            MatchEngineEvent.PointWon(TeamSide.A, 2_000L),
            MatchEngineEvent.Paused(10_000L),
            MatchEngineEvent.Resumed(20_000L),
            MatchEngineEvent.PointWon(TeamSide.B, 25_000L)
        )
        val undone = MatchEngine.undoLastPoint(cfg, events)!!
        assertEquals(3, undone.first.size)
        assertEquals("15", undone.second.displayScoreA())
        assertEquals("0", undone.second.displayScoreB())
        assertFalse(undone.second.isPaused)
    }

    @Test fun test60_allSetsForDisplayIncludesInProgressSetAndExcludesFinishedPlaceholder() {
        val cfg = config()
        var s = winSetLove(MatchEngine.initialState(cfg), TeamSide.A)
        s = step(s, TeamSide.B)
        val inProgress = s.allSetsForDisplay()
        assertEquals(2, inProgress.size)
        assertEquals(6, inProgress[0].gamesA)
        assertEquals(0, inProgress[1].gamesA)

        // Complete match
        s = winSetLove(s.let { winGame(it, TeamSide.B) }.let { MatchEngine.initialState(cfg) }, TeamSide.A)
        s = winSetLove(s, TeamSide.A)
        assertEquals(MatchStatus.COMPLETED, s.status)
        assertEquals(2, s.allSetsForDisplay().size)
    }

    // 61: Property test across >= 200 random rally seeds
    @Test fun test61_randomizedPropertyTestAcross250Seeds() {
        for (seed in 1..250) {
            val rng = Random(seed)
            val format = if (seed % 2 == 0) MatchFormat.DOUBLES else MatchFormat.SINGLES
            val deuceMode = DeuceMode.entries[seed % DeuceMode.entries.size]
            val firstServer = if (seed % 3 == 0) TeamSide.B else TeamSide.A
            val cfg = config(format = format, deuceMode = deuceMode, firstServer = firstServer)

            var state = MatchEngine.initialState(cfg)
            val history = mutableListOf<MatchEngineEvent>()
            var ts = 2_000L
            var safetySteps = 0

            while (state.status == MatchStatus.ACTIVE && safetySteps < 600) {
                safetySteps++
                ts += 1_000L

                if (rng.nextInt(35) == 0) {
                    val pEvent = MatchEngineEvent.Paused(ts)
                    val pRes = MatchEngine.reduce(state, pEvent)
                    assertTrue(pRes is EngineReduceResult.Accepted)
                    history.add(pEvent)
                    state = pRes.state

                    // Point while paused must be rejected
                    val rej = MatchEngine.reduce(state, MatchEngineEvent.PointWon(TeamSide.A, ts + 100))
                    assertTrue(rej is EngineReduceResult.Rejected)

                    ts += 5_000L
                    val rEvent = MatchEngineEvent.Resumed(ts)
                    val rRes = MatchEngine.reduce(state, rEvent)
                    assertTrue(rRes is EngineReduceResult.Accepted)
                    history.add(rEvent)
                    state = rRes.state
                }

                val winner = if (rng.nextBoolean()) TeamSide.A else TeamSide.B
                val ev = MatchEngineEvent.PointWon(winner, ts)
                val prevBeforePoint = state
                val res = MatchEngine.reduce(state, ev)
                assertTrue(res is EngineReduceResult.Accepted)
                history.add(ev)
                state = res.state

                // Invariant 1: replay == incremental
                val replayed = MatchEngine.replay(cfg, history)
                assertEquals("Seed $seed replay mismatch at step $safetySteps", state, replayed)

                // Invariant 2: legal game and set counts
                assertTrue(state.gamesA in 0..7)
                assertTrue(state.gamesB in 0..7)
                assertTrue(state.setsWonA <= cfg.setsToWin)
                assertTrue(state.setsWonB <= cfg.setsToWin)
                if (state.isTieBreak && state.status == MatchStatus.ACTIVE) {
                    assertEquals(6, state.gamesA)
                    assertEquals(6, state.gamesB)
                }
                if (deuceMode == DeuceMode.STAR_POINT && !state.isTieBreak) {
                    assertTrue(state.deuceAdvantagesLost <= 2)
                }

                // Invariant 3: undo(apply) == identity
                val undone = MatchEngine.undoLastPoint(cfg, history)
                assertNotNull(undone)
                assertEquals("Seed $seed undo mismatch", prevBeforePoint, undone!!.second)
            }

            assertEquals("Seed $seed did not finish within 600 rallies", MatchStatus.COMPLETED, state.status)
            assertNotNull(state.winnerTeam)
            val setsWinnerCount = if (state.winnerTeam == TeamSide.A) state.setsWonA else state.setsWonB
            assertEquals(2, setsWinnerCount)
        }
    }

    @Test fun test62_completedMatchWithoutPausesAccumulatesFullPlayDuration() {
        val cfg = config() // startedAtMillis = 1_000L
        var s = MatchEngine.initialState(cfg)
        var ts = 1_000L
        // 48 points, each 60 seconds apart -> total duration 48 minutes
        repeat(48) {
            ts += 60_000L
            s = step(s, TeamSide.A, ts = ts)
        }
        assertEquals(MatchStatus.COMPLETED, s.status)
        assertNull(s.activeSegmentStartedAtMillis)
        assertEquals(48 * 60_000L, s.accumulatedPlayMs)
        // Elapsed after completion must stay constant and reflect full play time
        assertEquals(48 * 60_000L, s.elapsedDurationMillis(ts))
        assertEquals(48 * 60_000L, s.elapsedDurationMillis(ts + 3_600_000L))
    }

    @Test fun test63_completedMatchWithPauseExcludesPauseFromFinalDuration() {
        val cfg = config() // startedAtMillis = 1_000L
        var s = MatchEngine.initialState(cfg)
        var ts = 1_000L
        // Play 24 points (set 1) at 30s intervals = 12 minutes
        repeat(24) {
            ts += 30_000L
            s = step(s, TeamSide.A, ts = ts)
        }
        assertEquals(1, s.completedSets.size)
        // Pause for 5 minutes (300_000L)
        s = MatchEngine.reduce(s, MatchEngineEvent.Paused(ts)).state
        ts += 300_000L
        s = MatchEngine.reduce(s, MatchEngineEvent.Resumed(ts)).state
        // Play remaining 24 points (set 2) at 30s intervals = 12 minutes
        repeat(24) {
            ts += 30_000L
            s = step(s, TeamSide.A, ts = ts)
        }
        assertEquals(MatchStatus.COMPLETED, s.status)
        assertEquals(24 * 60_000L, s.accumulatedPlayMs)
        assertEquals(24 * 60_000L, s.elapsedDurationMillis(ts + 500_000L))
    }
}
