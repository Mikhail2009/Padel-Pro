package com.padelpro.app

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.padelpro.app.core.common.SystemTimeProvider
import com.padelpro.app.data.repository.StatsRepositoryImpl
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.ManualFinishResult
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.SetScore
import com.padelpro.app.domain.model.TeamSide
import com.padelpro.app.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizationTest {

    @Test
    fun `system time provider formats dates in Russian regardless of system default locale`() {
        val prev = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val tp = SystemTimeProvider()
            val epochMillis = LocalDate.of(2026, 11, 17)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val shortDate = tp.formatShortDate(epochMillis)
            val monthYear = tp.formatMonthYear(epochMillis)

            assertTrue("Expected Russian short date like '17 нояб. 2026', got '$shortDate'", shortDate.startsWith("17 нояб"))
            assertTrue("Expected year 2026 in '$shortDate'", shortDate.contains("2026"))
            assertTrue("Expected Russian month/year in '$monthYear'", monthYear.contains("нояб") && monthYear.contains("2026"))
        } finally {
            Locale.setDefault(prev)
        }
    }

    @Test
    fun `russian plurals resolve one few many forms properly`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val ruConfig = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ru"))
        }
        val res = baseContext.createConfigurationContext(ruConfig).resources

        assertEquals("1 матч", res.getQuantityString(R.plurals.history_count_format, 1, 1))
        assertEquals("2 матча", res.getQuantityString(R.plurals.history_count_format, 2, 2))
        assertEquals("5 матчей", res.getQuantityString(R.plurals.history_count_format, 5, 5))

        assertEquals("Серия 1 день", res.getQuantityString(R.plurals.streak_days_format, 1, 1))
        assertEquals("Серия 3 дня", res.getQuantityString(R.plurals.streak_days_format, 3, 3))
        assertEquals("Серия 7 дней", res.getQuantityString(R.plurals.streak_days_format, 7, 7))

        assertEquals("1 очко", res.getQuantityString(R.plurals.points_count_format, 1, 1))
        assertEquals("4 очка", res.getQuantityString(R.plurals.points_count_format, 4, 4))
        assertEquals("10 очков", res.getQuantityString(R.plurals.points_count_format, 10, 10))
    }

    @Test
    fun `stats repository formats streaks and month chart labels in Russian`() = runTest {
        val sampleMatches = listOf(
            MatchSummary(
                id = "m2",
                format = MatchFormat.DOUBLES,
                deuceMode = DeuceMode.GOLDEN_POINT,
                status = MatchStatus.COMPLETED,
                startedAt = 2_000L,
                endedAt = 3_000L,
                durationMs = 1_000L,
                winnerTeam = TeamSide.A,
                userWon = true,
                teamANames = listOf("Иван"),
                teamBNames = listOf("Олег"),
                sets = listOf(SetScore(1, 6, 4))
            ),
            MatchSummary(
                id = "m1",
                format = MatchFormat.SINGLES,
                deuceMode = DeuceMode.ADVANTAGE,
                status = MatchStatus.COMPLETED,
                startedAt = 1_000L,
                endedAt = 1_500L,
                durationMs = 500L,
                winnerTeam = TeamSide.A,
                userWon = true,
                teamANames = listOf("Иван"),
                teamBNames = listOf("Пётр"),
                sets = listOf(SetScore(1, 6, 3))
            )
        )
        val repo = object : MatchRepository {
            override fun observeActiveMatch(): Flow<MatchSummary?> = flowOf(null)
            override fun observeRecentMatches(limit: Int): Flow<List<MatchSummary>> = flowOf(sampleMatches.take(limit))
            override fun observeCompletedMatches(): Flow<List<MatchSummary>> = flowOf(sampleMatches)
            override fun observeMatchById(matchId: String): Flow<MatchSummary?> = flowOf(null)
            override fun observeActiveMatchLive(): Flow<com.padelpro.app.domain.model.LiveMatchSnapshot?> = flowOf(null)
            override suspend fun startMatch(
                request: com.padelpro.app.domain.model.StartMatchRequest,
                cancelExisting: Boolean
            ): com.padelpro.app.domain.model.StartMatchResult =
                com.padelpro.app.domain.model.StartMatchResult.Started("test")

            override suspend fun addPoint(team: TeamSide) = null
            override suspend fun undoLastPoint(): Boolean = false
            override suspend fun pauseActiveMatch(): Boolean = false
            override suspend fun resumeActiveMatch(): Boolean = false
            override suspend fun cancelActiveMatch(): Boolean = false
            override suspend fun finishActiveMatchManually(): ManualFinishResult =
                ManualFinishResult.NoActiveMatch
            override suspend fun completeMatchIfFinished(matchId: String) = null
            override fun observeMatchDetails(matchId: String): Flow<com.padelpro.app.domain.model.MatchDetailsData?> = flowOf(null)
            override suspend fun deleteMatch(matchId: String): Boolean = false
            override fun dismissRestoredBanner(matchId: String) = Unit
        }
        val statsRepo = StatsRepositoryImpl(repo)
        val summary = statsRepo.observePerformanceSummary().first()

        assertEquals("В2", summary.currentStreakLabel)
        assertEquals(2, summary.currentStreakCount)
        assertEquals(true, summary.isCurrentStreakWin)
        assertEquals(6, summary.monthlyActivity.size)
        assertTrue(
            "Expected Russian 3-char lowercase month labels, got ${summary.monthlyActivity.map { it.label }}",
            summary.monthlyActivity.all { item ->
                item.label.length == 3 && item.label.all { it in 'а'..'я' }
            }
        )
    }
}
