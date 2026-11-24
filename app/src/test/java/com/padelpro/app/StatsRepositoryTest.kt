package com.padelpro.app

import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.data.repository.StatsRepositoryImpl
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.LiveMatchSnapshot
import com.padelpro.app.domain.model.ManualFinishResult
import com.padelpro.app.domain.model.MatchDetailsData
import com.padelpro.app.domain.model.MatchFormOutcome
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.SetScore
import com.padelpro.app.domain.model.StartMatchRequest
import com.padelpro.app.domain.model.StartMatchResult
import com.padelpro.app.domain.model.TeamSide
import com.padelpro.app.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsRepositoryTest {

    private class FakeMatchesRepo(initial: List<MatchSummary> = emptyList()) : MatchRepository {
        val completedFlow = MutableStateFlow(initial)
        override fun observeActiveMatch(): Flow<MatchSummary?> = flowOf(null)
        override fun observeRecentMatches(limit: Int): Flow<List<MatchSummary>> = flowOf(completedFlow.value.take(limit))
        override fun observeCompletedMatches(): Flow<List<MatchSummary>> = completedFlow
        override fun observeMatchById(matchId: String): Flow<MatchSummary?> = flowOf(null)
        override fun observeMatchDetails(matchId: String): Flow<MatchDetailsData?> = flowOf(null)
        override fun observeActiveMatchLive(): Flow<LiveMatchSnapshot?> = flowOf(null)
        override suspend fun startMatch(request: StartMatchRequest, cancelExisting: Boolean): StartMatchResult =
            StartMatchResult.Started("test")

        override suspend fun addPoint(team: TeamSide) = null
        override suspend fun undoLastPoint(): Boolean = false
        override suspend fun pauseActiveMatch(): Boolean = false
        override suspend fun resumeActiveMatch(): Boolean = false
        override suspend fun cancelActiveMatch(): Boolean = false
        override suspend fun finishActiveMatchManually(): ManualFinishResult =
            ManualFinishResult.NoActiveMatch
        override suspend fun completeMatchIfFinished(matchId: String) = null
        override suspend fun deleteMatch(matchId: String): Boolean {
            val before = completedFlow.value.size
            completedFlow.value = completedFlow.value.filterNot { it.id == matchId }
            return completedFlow.value.size < before
        }

        override fun dismissRestoredBanner(matchId: String) = Unit
    }

    private val fixedZone = ZoneId.of("UTC")
    private val fixedTimeProvider = object : TimeProvider {
        override fun nowMillis(): Long = epoch(2026, 11, 20)
        override fun currentHourOfDay(): Int = 14
        override fun currentLocalDate(): LocalDate = LocalDate.of(2026, 11, 20)
        override fun formatMonthYear(epochMillis: Long): String = "нояб 2026"
        override fun formatShortDate(epochMillis: Long): String = "20 нояб 2026"
    }

    private fun epoch(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        LocalDate.of(year, month, day).atTime(hour, 0).atZone(fixedZone).toInstant().toEpochMilli()

    private fun completedMatch(
        id: String,
        format: MatchFormat = MatchFormat.DOUBLES,
        userWon: Boolean?,
        endedAt: Long,
        winnerTeam: TeamSide? = if (userWon == true) TeamSide.A else TeamSide.B,
        isUserParticipant: Boolean = userWon != null
    ) = MatchSummary(
        id = id,
        format = format,
        deuceMode = DeuceMode.GOLDEN_POINT,
        status = MatchStatus.COMPLETED,
        startedAt = endedAt - 1_800_000L,
        endedAt = endedAt,
        durationMs = 1_800_000L,
        winnerTeam = winnerTeam,
        userWon = userWon,
        teamANames = listOf("Алекс"),
        teamBNames = listOf("Марк"),
        sets = listOf(SetScore(1, 6, 4)),
        isUserParticipant = isUserParticipant
    )

    @Test
    fun `empty match history returns honest empty summary with zero division safety`() = runTest {
        val statsRepo = StatsRepositoryImpl(FakeMatchesRepo(), fixedTimeProvider, fixedZone)
        val summary = statsRepo.observePerformanceSummary().first()

        assertEquals(0, summary.totalMatches)
        assertEquals(0, summary.wins)
        assertEquals(0, summary.losses)
        assertNull(summary.winRatePercent)
        assertNull(summary.currentStreakLabel)
        assertEquals(0, summary.bestWinStreak)
        assertEquals(0, summary.singlesMatches)
        assertNull(summary.singlesWinRatePercent)
        assertEquals(0, summary.doublesMatches)
        assertNull(summary.doublesWinRatePercent)
        assertTrue(summary.recentForm.isEmpty())
        assertEquals(6, summary.monthlyActivity.size)
        assertTrue(summary.monthlyActivity.all { it.count == 0 })
    }

    @Test
    fun `single match win computes 100 percent and streak B1`() = runTest {
        val repo = FakeMatchesRepo(
            listOf(completedMatch("m1", MatchFormat.SINGLES, userWon = true, endedAt = epoch(2026, 11, 10)))
        )
        val summary = StatsRepositoryImpl(repo, fixedTimeProvider, fixedZone).observePerformanceSummary().first()

        assertEquals(1, summary.totalMatches)
        assertEquals(1, summary.wins)
        assertEquals(0, summary.losses)
        assertEquals(100, summary.winRatePercent)
        assertEquals("В1", summary.currentStreakLabel)
        assertEquals(1, summary.currentStreakCount)
        assertEquals(true, summary.isCurrentStreakWin)
        assertEquals(1, summary.bestWinStreak)
        assertEquals(1, summary.singlesMatches)
        assertEquals(100, summary.singlesWinRatePercent)
        assertEquals(0, summary.doublesMatches)
        assertNull(summary.doublesWinRatePercent)
    }

    @Test
    fun `mixed W-L computes rounded winRate, current loss streak, historical best win streak, and format split`() = runTest {
        // Chronological: W, W, W (best=3), L, W, L, L (newest -> current P2)
        val matches = listOf(
            completedMatch("m7", MatchFormat.DOUBLES, userWon = false, endedAt = epoch(2026, 11, 18)),
            completedMatch("m6", MatchFormat.SINGLES, userWon = false, endedAt = epoch(2026, 11, 17)),
            completedMatch("m5", MatchFormat.DOUBLES, userWon = true, endedAt = epoch(2026, 11, 15)),
            completedMatch("m4", MatchFormat.DOUBLES, userWon = false, endedAt = epoch(2026, 10, 20)),
            completedMatch("m3", MatchFormat.SINGLES, userWon = true, endedAt = epoch(2026, 10, 10)),
            completedMatch("m2", MatchFormat.SINGLES, userWon = true, endedAt = epoch(2026, 9, 5)),
            completedMatch("m1", MatchFormat.DOUBLES, userWon = true, endedAt = epoch(2026, 8, 2))
        )
        val repo = FakeMatchesRepo(matches)
        val summary = StatsRepositoryImpl(repo, fixedTimeProvider, fixedZone).observePerformanceSummary().first()

        // 4 wins / 7 total -> 57.14% -> 57
        assertEquals(7, summary.totalMatches)
        assertEquals(4, summary.wins)
        assertEquals(3, summary.losses)
        assertEquals(57, summary.winRatePercent)
        assertEquals("П2", summary.currentStreakLabel)
        assertEquals(2, summary.currentStreakCount)
        assertEquals(false, summary.isCurrentStreakWin)
        assertEquals(3, summary.bestWinStreak)

        // Singles: 2W / 3 -> 67%; Doubles: 2W / 4 -> 50%
        assertEquals(3, summary.singlesMatches)
        assertEquals(67, summary.singlesWinRatePercent)
        assertEquals(4, summary.doublesMatches)
        assertEquals(50, summary.doublesWinRatePercent)

        // Recent form (last 5 newest-first): LOSS, LOSS, WIN, LOSS, WIN
        assertEquals(
            listOf(
                MatchFormOutcome.LOSS,
                MatchFormOutcome.LOSS,
                MatchFormOutcome.WIN,
                MatchFormOutcome.LOSS,
                MatchFormOutcome.WIN
            ),
            summary.recentForm
        )
    }

    @Test
    fun `six-month buckets group matches accurately and exclude older than 6 months and non-user matches`() = runTest {
        // Current month: Nov 2026 -> 6-month window: Jun, Jul, Aug, Sep, Oct, Nov 2026
        val matches = listOf(
            completedMatch("nov1", userWon = true, endedAt = epoch(2026, 11, 19)),
            completedMatch("nov2", userWon = false, endedAt = epoch(2026, 11, 3)),
            completedMatch("non_user", userWon = null, endedAt = epoch(2026, 11, 1)), // excluded
            completedMatch("sep1", userWon = true, endedAt = epoch(2026, 9, 12)),
            completedMatch("jun1", userWon = true, endedAt = epoch(2026, 6, 4)),
            completedMatch("may_old", userWon = false, endedAt = epoch(2026, 5, 29)) // outside 6m window, still counted in totals
        )
        val repo = FakeMatchesRepo(matches)
        val summary = StatsRepositoryImpl(repo, fixedTimeProvider, fixedZone).observePerformanceSummary().first()

        assertEquals(5, summary.totalMatches)
        assertEquals(listOf(1, 0, 0, 1, 0, 2), summary.monthlyActivity.map { it.count })
        assertTrue(summary.monthlyActivity.last().isCurrentMonth)
        assertTrue(summary.monthlyActivity.dropLast(1).none { it.isCurrentMonth })
    }

    @Test
    fun `deleting a match dynamically updates statistics`() = runTest {
        val repo = FakeMatchesRepo(
            listOf(
                completedMatch("m2", userWon = false, endedAt = epoch(2026, 11, 18)),
                completedMatch("m1", userWon = true, endedAt = epoch(2026, 11, 15))
            )
        )
        val statsRepo = StatsRepositoryImpl(repo, fixedTimeProvider, fixedZone)

        val before = statsRepo.observePerformanceSummary().first()
        assertEquals(2, before.totalMatches)
        assertEquals(50, before.winRatePercent)
        assertEquals("П1", before.currentStreakLabel)

        assertTrue(repo.deleteMatch("m2"))

        val after = statsRepo.observePerformanceSummary().first()
        assertEquals(1, after.totalMatches)
        assertEquals(100, after.winRatePercent)
        assertEquals("В1", after.currentStreakLabel)
        assertEquals(1, after.monthlyActivity.last().count)
    }

    @Test
    fun `manual draws count in totals, format counts, and monthly activity but exclude from winRate denominator and interrupt streaks`() = runTest {
        // Chronological: W, W, DRAW (interrupts current & best streak), W, L, DRAW (newest)
        val matches = listOf(
            completedMatch(
                id = "d_new",
                format = MatchFormat.SINGLES,
                userWon = null,
                endedAt = epoch(2026, 11, 19),
                winnerTeam = null,
                isUserParticipant = true
            ),
            completedMatch("l1", MatchFormat.DOUBLES, userWon = false, endedAt = epoch(2026, 11, 18)),
            completedMatch("w3", MatchFormat.SINGLES, userWon = true, endedAt = epoch(2026, 11, 15)),
            completedMatch(
                id = "d_mid",
                format = MatchFormat.DOUBLES,
                userWon = null,
                endedAt = epoch(2026, 11, 12),
                winnerTeam = null,
                isUserParticipant = true
            ),
            completedMatch("w2", MatchFormat.SINGLES, userWon = true, endedAt = epoch(2026, 11, 10)),
            completedMatch("w1", MatchFormat.DOUBLES, userWon = true, endedAt = epoch(2026, 10, 20))
        )
        val repo = FakeMatchesRepo(matches)
        val summary = StatsRepositoryImpl(repo, fixedTimeProvider, fixedZone).observePerformanceSummary().first()

        // 3 wins, 1 loss, 2 draws = 6 total matches
        assertEquals(6, summary.totalMatches)
        assertEquals(3, summary.wins)
        assertEquals(1, summary.losses)
        assertEquals(2, summary.draws)
        // Win rate: 3 / (3 + 1) = 75% (draws excluded from denominator)
        assertEquals(75, summary.winRatePercent)
        // Latest match is draw -> current streak interrupted
        assertNull(summary.currentStreakLabel)
        assertEquals(0, summary.currentStreakCount)
        assertNull(summary.isCurrentStreakWin)
        // Best win streak: 2 (w1, w2 before d_mid reset)
        assertEquals(2, summary.bestWinStreak)
        // Format counts include draws; winRates exclude draws
        assertEquals(3, summary.singlesMatches) // w2, w3, d_new
        assertEquals(100, summary.singlesWinRatePercent) // 2W / 2 decided
        assertEquals(3, summary.doublesMatches) // w1, d_mid, l1
        assertEquals(50, summary.doublesWinRatePercent) // 1W / 2 decided
        // Monthly counts include draws: Oct=1, Nov=5
        assertEquals(listOf(0, 0, 0, 0, 1, 5), summary.monthlyActivity.map { it.count })
        // Recent 5 newest-first: DRAW, LOSS, WIN, DRAW, WIN
        assertEquals(
            listOf(
                MatchFormOutcome.DRAW,
                MatchFormOutcome.LOSS,
                MatchFormOutcome.WIN,
                MatchFormOutcome.DRAW,
                MatchFormOutcome.WIN
            ),
            summary.recentForm
        )
    }

    @Test
    fun `only manual draws returns null winRatePercent while counting total matches`() = runTest {
        val repo = FakeMatchesRepo(
            listOf(
                completedMatch(
                    id = "d1",
                    format = MatchFormat.DOUBLES,
                    userWon = null,
                    endedAt = epoch(2026, 11, 19),
                    winnerTeam = null,
                    isUserParticipant = true
                )
            )
        )
        val summary = StatsRepositoryImpl(repo, fixedTimeProvider, fixedZone).observePerformanceSummary().first()

        assertEquals(1, summary.totalMatches)
        assertEquals(0, summary.wins)
        assertEquals(0, summary.losses)
        assertEquals(1, summary.draws)
        assertNull(summary.winRatePercent)
        assertNull(summary.doublesWinRatePercent)
        assertEquals(1, summary.doublesMatches)
        assertNull(summary.currentStreakLabel)
        assertEquals(listOf(MatchFormOutcome.DRAW), summary.recentForm)
    }
}
