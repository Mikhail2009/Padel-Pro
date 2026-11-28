package com.padelpro.app

import android.content.Context
import android.os.Looper
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.core.designsystem.theme.PadelProTheme
import com.padelpro.app.data.content.AssetAcademyContentLoader
import com.padelpro.app.data.repository.AcademyRepositoryImpl
import com.padelpro.app.data.repository.MatchRepositoryImpl
import com.padelpro.app.data.repository.PlayerRepositoryImpl
import com.padelpro.app.data.repository.StatsRepositoryImpl
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.LiveMatchSnapshot
import com.padelpro.app.domain.model.ManualFinishResult
import com.padelpro.app.domain.model.MatchCompletionResult
import com.padelpro.app.domain.model.MatchDetailsData
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.MomentumPoint
import com.padelpro.app.domain.model.StartMatchParticipant
import com.padelpro.app.domain.model.StartMatchRequest
import com.padelpro.app.domain.model.StartMatchResult
import com.padelpro.app.domain.model.TeamSide
import com.padelpro.app.domain.repository.MatchRepository
import com.padelpro.app.feature.history.MatchDetailsScreen
import com.padelpro.app.feature.history.MatchDetailsViewModel
import com.padelpro.app.feature.home.HomeScreen
import com.padelpro.app.feature.home.HomeViewModel
import com.padelpro.app.feature.insights.MatchInsightsScreen
import com.padelpro.app.feature.insights.MatchInsightsViewModel
import com.padelpro.app.feature.insights.extractMomentumSetBoundaries
import com.padelpro.app.feature.insights.findNearestMomentumSample
import com.padelpro.app.feature.insights.prepareMomentumSamples
import com.padelpro.app.feature.match.MatchCenterScreen
import com.padelpro.app.feature.match.MatchResultsScreen
import com.padelpro.app.feature.match.MatchResultsViewModel
import com.padelpro.app.feature.match.MatchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatchInsightsRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class TestTimeProvider(var currentNow: Long = 100_000L) : TimeProvider {
        override fun nowMillis(): Long = currentNow
        override fun currentHourOfDay(): Int = 18
        override fun currentLocalDate(): LocalDate = LocalDate.of(2026, 11, 28)
        override fun formatMonthYear(epochMillis: Long): String = "нояб 2026"
        override fun formatShortDate(epochMillis: Long): String = "28 нояб 2026"
        override fun formatShortDateTime(epochMillis: Long): String = "28 нояб 2026, 18:00"
    }

    private class SingleMatchReadRepo(
        private val details: MatchDetailsData?,
        private val activeMatch: MatchSummary? = null
    ) : MatchRepository {
        override fun observeActiveMatch(): Flow<MatchSummary?> = flowOf(activeMatch)
        override fun observeRecentMatches(limit: Int): Flow<List<MatchSummary>> = flowOf(emptyList())
        override fun observeCompletedMatches(): Flow<List<MatchSummary>> = flowOf(emptyList())
        override fun observeMatchById(matchId: String): Flow<MatchSummary?> = flowOf(details?.summary)
        override fun observeActiveMatchLive(): Flow<LiveMatchSnapshot?> = flowOf(null)
        override suspend fun startMatch(request: StartMatchRequest, cancelExisting: Boolean): StartMatchResult =
            StartMatchResult.Started("test")
        override suspend fun addPoint(team: TeamSide): MatchCompletionResult? = null
        override suspend fun undoLastPoint(): Boolean = false
        override suspend fun pauseActiveMatch(): Boolean = false
        override suspend fun resumeActiveMatch(): Boolean = false
        override suspend fun cancelActiveMatch(): Boolean = false
        override suspend fun finishActiveMatchManually(): ManualFinishResult = ManualFinishResult.NoActiveMatch
        override suspend fun completeMatchIfFinished(matchId: String): MatchCompletionResult? = null
        override fun observeMatchDetails(matchId: String): Flow<MatchDetailsData?> = flowOf(details)
        override suspend fun deleteMatch(matchId: String): Boolean = false
        override fun dismissRestoredBanner(matchId: String) = Unit
    }

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var db: PadelProDatabase
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var playerRepo: PlayerRepositoryImpl
    private lateinit var matchRepo: MatchRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.cacheDir, "test_padel_insights_${System.nanoTime()}.db")
        db = Room.databaseBuilder(context, PadelProDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        timeProvider = TestTimeProvider()
        settingsRepo = SettingsRepository(context)
        playerRepo = PlayerRepositoryImpl(db, settingsRepo, timeProvider)
        matchRepo = MatchRepositoryImpl(db, timeProvider)
    }

    @After
    fun teardown() {
        if (db.isOpen) db.close()
        dbFile.delete()
    }

    private suspend fun startSinglesMatch(): String {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.SINGLES)
        val opp = playerRepo.createFriend("Марк", 1)
        return (
            matchRepo.startMatch(
                StartMatchRequest(
                    format = MatchFormat.SINGLES,
                    deuceMode = DeuceMode.GOLDEN_POINT,
                    firstServerTeam = TeamSide.A,
                    participants = listOf(
                        StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                        StartMatchParticipant(opp.id, TeamSide.B, 0, isUser = false)
                    )
                )
            ) as StartMatchResult.Started
            ).matchId
    }

    private suspend fun startDoublesMatch(): String {
        val user = playerRepo.createLocalProfile("Алекс", 0, MatchFormat.DOUBLES)
        val partner = playerRepo.createFriend("Иван", 1)
        val opp1 = playerRepo.createFriend("Марк", 2)
        val opp2 = playerRepo.createFriend("Лев", 3)
        return (
            matchRepo.startMatch(
                StartMatchRequest(
                    format = MatchFormat.DOUBLES,
                    deuceMode = DeuceMode.GOLDEN_POINT,
                    firstServerTeam = TeamSide.A,
                    participants = listOf(
                        StartMatchParticipant(user.id, TeamSide.A, 0, isUser = true),
                        StartMatchParticipant(partner.id, TeamSide.A, 1, isUser = false),
                        StartMatchParticipant(opp1.id, TeamSide.B, 0, isUser = false),
                        StartMatchParticipant(opp2.id, TeamSide.B, 1, isUser = false)
                    )
                )
            ) as StartMatchResult.Started
            ).matchId
    }

    private suspend fun addPoints(team: TeamSide, count: Int, stepMs: Long = 15_000L) {
        repeat(count) {
            timeProvider.currentNow += stepMs
            matchRepo.addPoint(team)
        }
    }

    private suspend fun awaitMainCondition(timeoutMs: Long = 5_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate()) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (System.currentTimeMillis() >= deadline) break
            delay(20L)
        }
        assertTrue(predicate())
    }

    @Test
    fun `momentum downsampling preserves extrema set boundaries and chronological order`() {
        assertTrue(prepareMomentumSamples(emptyList()).isEmpty())

        val single = listOf(
            MomentumPoint(
                rallyIndex = 1,
                winner = TeamSide.A,
                pointDifference = 1,
                gamesA = 0,
                gamesB = 0,
                setIndex = 1,
                timestampMillis = 1000L
            )
        )
        val singlePrepared = prepareMomentumSamples(single)
        assertEquals(1, singlePrepared.size)
        assertEquals(0.5f, singlePrepared.first().xFraction, 0.0001f)

        var diff = 0
        val longPoints = (1..240).map { rally ->
            val setIdx = when {
                rally <= 80 -> 1
                rally <= 160 -> 2
                else -> 3
            }
            val winner = when {
                rally in 1..30 -> TeamSide.A
                rally in 31..90 -> TeamSide.B
                rally in 91..180 -> TeamSide.A
                else -> TeamSide.B
            }
            diff += if (winner == TeamSide.A) 1 else -1
            MomentumPoint(
                rallyIndex = rally,
                winner = winner,
                pointDifference = diff,
                gamesA = rally / 10,
                gamesB = rally / 12,
                setIndex = setIdx,
                timestampMillis = rally * 10_000L
            )
        }

        val samples = prepareMomentumSamples(longPoints, maxSamples = 64)
        assertTrue("Long match should be compacted", samples.size <= 72)
        assertEquals(1, samples.first().point.rallyIndex)
        assertEquals(240, samples.last().point.rallyIndex)
        assertTrue(samples.zipWithNext().all { (a, b) -> a.point.rallyIndex < b.point.rallyIndex })
        assertEquals(
            longPoints.maxOf { it.pointDifference },
            samples.maxOf { it.point.pointDifference }
        )
        assertEquals(
            longPoints.minOf { it.pointDifference },
            samples.minOf { it.point.pointDifference }
        )
        val sampledRallies = samples.map { it.point.rallyIndex }.toSet()
        assertTrue(81 in sampledRallies)
        assertTrue(161 in sampledRallies)

        val boundaries = extractMomentumSetBoundaries(longPoints)
        assertEquals(listOf(2, 3), boundaries.map { it.setIndex })

        val nearest = findNearestMomentumSample(samples, 0.5f)
        assertNotNull(nearest)
        assertTrue(nearest!!.point.rallyIndex in 100..145)
    }

    @Test
    fun `viewmodel reads MatchDetailsData with analytics directly from Room repository`() = runTest {
        val matchId = startDoublesMatch()
        addPoints(TeamSide.A, 24, stepMs = 15_000L) // Сет 1: 6-0
        addPoints(TeamSide.B, 4, stepMs = 15_000L)  // Сет 2: 0-1
        addPoints(TeamSide.A, 1, stepMs = 15_000L)  // 15-0

        val store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MatchInsightsViewModel(matchRepo, timeProvider) as T
            }
        )[MatchInsightsViewModel::class.java]

        val job = launch { vm.uiState.collect { } }
        try {
            vm.loadMatch(matchId)
            awaitMainCondition { !vm.uiState.value.isLoading }

            val details = requireNotNull(vm.uiState.value.details)
            val analytics = requireNotNull(details.analytics)
            assertFalse(analytics.overview.isCompleted)
            assertEquals(1, analytics.overview.sets.size)
            assertEquals(2, analytics.overview.incompleteSet?.setIndex)
            assertEquals(29, analytics.pointStats.totalPoints)
            assertEquals("28 нояб 2026, 18:00", vm.formatDateTime(100_000L))
        } finally {
            job.cancel()
            store.clear()
        }

        val missingStore = ViewModelStore()
        val missingVm = ViewModelProvider(
            missingStore,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MatchInsightsViewModel(matchRepo, timeProvider) as T
            }
        )[MatchInsightsViewModel::class.java]

        val missingJob = launch { missingVm.uiState.collect { } }
        try {
            missingVm.loadMatch("missing-id")
            awaitMainCondition { !missingVm.uiState.value.isLoading }
            assertNull(missingVm.uiState.value.details)
        } finally {
            missingJob.cancel()
            missingStore.clear()
        }
    }

    @Test
    fun `completed doubles match renders all six insights sections and interactive momentum`() {
        val (matchId, details) = runBlocking {
            val id = startDoublesMatch()
            addPoints(TeamSide.A, 24, stepMs = 10_000L) // Сет 1: 6-0
            addPoints(TeamSide.B, 8, stepMs = 10_000L)  // Сет 2: 0-2
            addPoints(TeamSide.A, 24, stepMs = 10_000L) // Сет 2: 6-2
            id to matchRepo.observeMatchDetails(id).first()!!
        }

        val vm = MatchInsightsViewModel(SingleMatchReadRepo(details), timeProvider)
        composeRule.setContent {
            PadelProTheme {
                MatchInsightsScreen(
                    matchId = matchId,
                    viewModel = vm,
                    onBack = {}
                )
            }
        }
        composeRule.waitUntil(5_000) { !vm.uiState.value.isLoading }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("insights_overview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("insights_live_badge").assertDoesNotExist()
        composeRule.onNodeWithTag("insights_manual_badge").assertDoesNotExist()
        composeRule.onNodeWithText("Парный (2×2)").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("insights_points_section"))
        composeRule.onNodeWithTag("insights_points_section").assertIsDisplayed()
        composeRule.onNodeWithText("56").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("+40").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("insights_games_section"))
        composeRule.onNodeWithTag("insights_games_section").assertIsDisplayed()
        composeRule.onNodeWithText("12 / 2").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("insights_sets_section"))
        composeRule.onNodeWithTag("insights_sets_section").assertIsDisplayed()
        composeRule.onNodeWithTag("insights_set_card_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("insights_set_card_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Самый долгий сет").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("insights_momentum_section"))
        composeRule.onNodeWithTag("insights_momentum_canvas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("insights_momentum_selected_box").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Розыгрыш #56 · Сет 2").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("insights_momentum_canvas").performScrollTo().performTouchInput {
            click(Offset(x = width * 0.2f, y = height * 0.5f))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Розыгрыш #56 · Сет 2").assertDoesNotExist()

        composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("insights_highlights_section"))
        composeRule.onNodeWithTag("insights_highlights_section").assertIsDisplayed()
        composeRule.onAllNodesWithText("Лучшая серия очков").onFirst().performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Максимальный отрыв по очкам").onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `completed singles tiebreak match renders singles format tiebreak and set comparisons`() {
        val (matchId, details) = runBlocking {
            val id = startSinglesMatch()
            // Сет 1: 6-6, затем тай-брейк 7-4 -> 7-6(4)
            repeat(6) {
                addPoints(TeamSide.A, 4, stepMs = 15_000L)
                addPoints(TeamSide.B, 4, stepMs = 15_000L)
            }
            addPoints(TeamSide.A, 4, stepMs = 15_000L)
            addPoints(TeamSide.B, 4, stepMs = 15_000L)
            addPoints(TeamSide.A, 3, stepMs = 15_000L)

            // Сет 2: 6-2 (короче первого сета)
            addPoints(TeamSide.B, 8, stepMs = 10_000L)
            addPoints(TeamSide.A, 24, stepMs = 10_000L)

            id to matchRepo.observeMatchDetails(id).first()!!
        }

        val analytics = requireNotNull(details.analytics)
        assertEquals(MatchFormat.SINGLES, analytics.overview.format)
        assertEquals(7, analytics.overview.sets[0].gamesA)
        assertEquals(6, analytics.overview.sets[0].gamesB)
        assertEquals(7, analytics.overview.sets[0].tbA)
        assertEquals(4, analytics.overview.sets[0].tbB)

        val vm = MatchInsightsViewModel(SingleMatchReadRepo(details), timeProvider)
        composeRule.setContent {
            PadelProTheme {
                MatchInsightsScreen(
                    matchId = matchId,
                    viewModel = vm,
                    onBack = {}
                )
            }
        }
        composeRule.waitUntil(5_000) { !vm.uiState.value.isLoading }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Одиночный (1×1)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("insights_list").performScrollToNode(hasTestTag("insights_sets_section"))
        composeRule.onNodeWithText("Тай-брейк 7:4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Самый короткий сет").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Самый упорный сет").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `active match shows LIVE badge and honest empty state when zero points`() {
        val (matchId, zeroDetails) = runBlocking {
            val id = startDoublesMatch()
            id to matchRepo.observeMatchDetails(id).first()!!
        }

        val vm = MatchInsightsViewModel(SingleMatchReadRepo(zeroDetails), timeProvider)
        composeRule.setContent {
            PadelProTheme {
                MatchInsightsScreen(
                    matchId = matchId,
                    viewModel = vm,
                    onBack = {}
                )
            }
        }
        composeRule.waitUntil(5_000) { !vm.uiState.value.isLoading }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("insights_live_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasText("Розыгрыши ещё не зафиксированы"))
        composeRule.onNodeWithText("Розыгрыши ещё не зафиксированы").assertIsDisplayed()

        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasText("Завершённых геймов пока нет"))
        composeRule.onNodeWithText("Завершённых геймов пока нет").assertIsDisplayed()

        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasText("Нет сохранённых розыгрышей для графика динамики"))
        composeRule.onNodeWithText("Нет сохранённых розыгрышей для графика динамики").assertIsDisplayed()
    }

    @Test
    fun `active match with completed and incomplete sets separates sets cleanly`() {
        val (matchId, liveDetails) = runBlocking {
            val id = startDoublesMatch()
            addPoints(TeamSide.A, 24, stepMs = 15_000L) // Сет 1: 6-0
            addPoints(TeamSide.B, 8, stepMs = 15_000L)  // Сет 2: 0-2
            addPoints(TeamSide.A, 1, stepMs = 15_000L)  // 15:0
            id to matchRepo.observeMatchDetails(id).first()!!
        }

        val vm = MatchInsightsViewModel(SingleMatchReadRepo(liveDetails), timeProvider)
        composeRule.setContent {
            PadelProTheme {
                MatchInsightsScreen(
                    matchId = matchId,
                    viewModel = vm,
                    onBack = {}
                )
            }
        }
        composeRule.waitUntil(5_000) { !vm.uiState.value.isLoading }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("insights_live_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasTestTag("insights_overview_section"))
        composeRule.onNodeWithTag("insights_overview_incomplete_set").assertIsDisplayed()

        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasTestTag("insights_sets_section"))
        composeRule.onNodeWithTag("insights_set_card_1").assertIsDisplayed()
        composeRule.onNodeWithTag("insights_incomplete_set_card").assertIsDisplayed()

        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasTestTag("insights_momentum_section"))
        composeRule.onNodeWithTag("insights_momentum_canvas").assertIsDisplayed()
    }

    @Test
    fun `manually completed match displays manual badge draw status and incomplete set without winner`() {
        val (matchId, manualDetails) = runBlocking {
            val id = startDoublesMatch()
            addPoints(TeamSide.A, 24, stepMs = 20_000L) // Сет 1: 6-0 (8 мин)
            addPoints(TeamSide.B, 8, stepMs = 20_000L)  // Сет 2: 0-2 (+2m 40s)
            timeProvider.currentNow += 60_000L          // > 10 мин -> сохранение вручную
            matchRepo.finishActiveMatchManually()
            id to matchRepo.observeMatchDetails(id).first()!!
        }

        val analytics = manualDetails.analytics!!
        assertTrue(analytics.overview.isManuallyCompleted)
        assertNull(analytics.overview.winnerTeam)

        val vm = MatchInsightsViewModel(SingleMatchReadRepo(manualDetails), timeProvider)
        composeRule.setContent {
            PadelProTheme {
                MatchInsightsScreen(
                    matchId = matchId,
                    viewModel = vm,
                    onBack = {}
                )
            }
        }
        composeRule.waitUntil(5_000) { !vm.uiState.value.isLoading }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("insights_manual_badge").assertIsDisplayed()
        composeRule.onNodeWithText("Ничья · завершён вручную").assertIsDisplayed()
        composeRule.onNodeWithTag("insights_list")
            .performScrollToNode(hasTestTag("insights_sets_section"))
        composeRule.onNodeWithTag("insights_incomplete_set_card").assertIsDisplayed()
    }

    @Test
    fun `details screen entry point triggers onOpenInsights with matchId`() {
        val (matchId, details) = runBlocking {
            val id = startDoublesMatch()
            addPoints(TeamSide.A, 48, stepMs = 5_000L)
            id to matchRepo.observeMatchDetails(id).first()!!
        }

        val detailsVm = MatchDetailsViewModel(SingleMatchReadRepo(details), timeProvider)
        var openedFromDetails: String? = null

        composeRule.setContent {
            PadelProTheme {
                MatchDetailsScreen(
                    matchId = matchId,
                    viewModel = detailsVm,
                    onBack = {},
                    onOpenInsights = { openedFromDetails = it }
                )
            }
        }
        composeRule.waitUntil(5_000) { !detailsVm.uiState.value.isLoading }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("details_list")
            .performScrollToNode(hasTestTag("details_insights_button"))
        composeRule.onNodeWithTag("details_insights_button").performClick()
        assertEquals(matchId, openedFromDetails)
    }

    @Test
    fun `results screen entry point triggers onOpenInsights with matchId`() {
        val (matchId, details) = runBlocking {
            val id = startDoublesMatch()
            addPoints(TeamSide.A, 48, stepMs = 5_000L)
            id to matchRepo.observeMatchDetails(id).first()!!
        }

        val resultsVm = MatchResultsViewModel(SingleMatchReadRepo(details), timeProvider)
        var openedFromResults: String? = null

        composeRule.setContent {
            PadelProTheme {
                MatchResultsScreen(
                    matchId = matchId,
                    viewModel = resultsVm,
                    onViewStatistics = {},
                    onOpenDetails = {},
                    onReturnHome = {},
                    onOpenInsights = { openedFromResults = it }
                )
            }
        }
        composeRule.waitUntil(5_000) { !resultsVm.uiState.value.isLoading }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("results_insights_button").performScrollTo().performClick()
        assertEquals(matchId, openedFromResults)
    }

    @Test
    fun `home active match card exposes Match Insights entry point`() {
        val (matchId, liveDetails) = runBlocking {
            val id = startDoublesMatch()
            addPoints(TeamSide.A, 4, stepMs = 15_000L)
            id to matchRepo.observeMatchDetails(id).first()!!
        }

        val readRepo = SingleMatchReadRepo(liveDetails, activeMatch = liveDetails.summary)
        val contentPack = AssetAcademyContentLoader(context).load()
        val academyRepo = AcademyRepositoryImpl(db, contentPack, timeProvider)
        val statsRepo = StatsRepositoryImpl(readRepo, timeProvider, ZoneId.of("UTC"))
        val homeVm = HomeViewModel(playerRepo, academyRepo, readRepo, statsRepo, timeProvider)

        var openedFromHome: String? = null
        composeRule.setContent {
            PadelProTheme {
                val homeListState = rememberLazyListState()
                HomeScreen(
                    viewModel = homeVm,
                    listState = homeListState,
                    onOpenProfile = {},
                    onStartMatch = {},
                    onContinueLearning = {},
                    onOpenMatchDetails = {},
                    onResumeMatch = {},
                    onOpenPerformance = {},
                    onOpenMatchInsights = { openedFromHome = it }
                )
            }
        }
        composeRule.waitUntil(5_000) { homeVm.uiState.value.activeMatch?.id == matchId }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_active_match_insights_button").performClick()
        assertEquals(matchId, openedFromHome)
    }

    @Test
    fun `match center active match card exposes Match Insights entry point`() {
        val (matchId, liveDetails) = runBlocking {
            val id = startDoublesMatch()
            addPoints(TeamSide.A, 4, stepMs = 15_000L)
            id to matchRepo.observeMatchDetails(id).first()!!
        }

        val readRepo = SingleMatchReadRepo(liveDetails, activeMatch = liveDetails.summary)
        val matchVm = MatchViewModel(playerRepo, readRepo, settingsRepo, timeProvider)

        var openedFromMatchCenter: String? = null
        composeRule.setContent {
            PadelProTheme {
                val playListState = rememberLazyListState()
                MatchCenterScreen(
                    viewModel = matchVm,
                    listState = playListState,
                    onOpenSetup = {},
                    onOpenHistory = {},
                    onOpenMatchDetails = {},
                    onResumeMatch = {},
                    onOpenMatchInsights = { openedFromMatchCenter = it }
                )
            }
        }
        composeRule.waitUntil(5_000) { matchVm.uiState.value.activeMatch?.id == matchId }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("match_center_active_insights_button").performClick()
        assertEquals(matchId, openedFromMatchCenter)
    }

    @Test
    fun `unknown matchId shows empty state cleanly`() {
        val vm = MatchInsightsViewModel(SingleMatchReadRepo(null), timeProvider)
        composeRule.setContent {
            PadelProTheme {
                MatchInsightsScreen(
                    matchId = "non-existent-id",
                    viewModel = vm,
                    onBack = {}
                )
            }
        }
        composeRule.waitUntil(5_000) { !vm.uiState.value.isLoading }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("insights_empty_state").assertIsDisplayed()
    }
}
