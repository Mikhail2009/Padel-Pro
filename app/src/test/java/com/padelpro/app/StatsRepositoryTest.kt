package com.padelpro.app

import com.padelpro.app.data.repository.StatsRepositoryImpl
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsRepositoryTest {

    @Test
    fun `empty match history returns honest empty summary with zero division safety`() = runTest {
        val emptyRepo = object : MatchRepository {
            override fun observeActiveMatch(): Flow<MatchSummary?> = flowOf(null)
            override fun observeRecentMatches(limit: Int): Flow<List<MatchSummary>> = flowOf(emptyList())
            override fun observeCompletedMatches(): Flow<List<MatchSummary>> = flowOf(emptyList())
            override fun observeMatchById(matchId: String): Flow<MatchSummary?> = flowOf(null)
            override fun observeActiveMatchLive(): Flow<com.padelpro.app.domain.model.LiveMatchSnapshot?> = flowOf(null)
            override suspend fun startMatch(
                request: com.padelpro.app.domain.model.StartMatchRequest,
                cancelExisting: Boolean
            ): com.padelpro.app.domain.model.StartMatchResult =
                com.padelpro.app.domain.model.StartMatchResult.Started("test")

            override suspend fun addPoint(team: com.padelpro.app.domain.model.TeamSide) = null
            override suspend fun undoLastPoint(): Boolean = false
            override suspend fun pauseActiveMatch(): Boolean = false
            override suspend fun resumeActiveMatch(): Boolean = false
            override suspend fun cancelActiveMatch(): Boolean = false
            override suspend fun completeMatchIfFinished(matchId: String) = null
            override fun dismissRestoredBanner(matchId: String) = Unit
        }
        val statsRepo = StatsRepositoryImpl(emptyRepo)
        val summary = statsRepo.observePerformanceSummary().first()

        assertEquals(0, summary.totalMatches)
        assertEquals(0, summary.wins)
        assertEquals(0, summary.losses)
        assertNull(summary.winRatePercent)
        assertNull(summary.currentStreakLabel)
        assertEquals(0, summary.bestWinStreak)
        assertEquals(6, summary.monthlyActivity.size)
        assertTrue(summary.monthlyActivity.all { it.count == 0 })
    }
}
