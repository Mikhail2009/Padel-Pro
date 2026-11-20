package com.padelpro.app.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.padelpro.app.core.designsystem.components.BottomTab
import com.padelpro.app.core.designsystem.components.PpBottomBar
import com.padelpro.app.core.designsystem.theme.PpBackground
import com.padelpro.app.feature.academy.AcademyScreen
import com.padelpro.app.feature.academy.AcademyViewModel
import com.padelpro.app.feature.academy.CompletionScreen
import com.padelpro.app.feature.academy.CompletionViewModel
import com.padelpro.app.feature.academy.CourseDetailScreen
import com.padelpro.app.feature.academy.CourseDetailViewModel
import com.padelpro.app.feature.academy.LessonScreen
import com.padelpro.app.feature.academy.LessonViewModel
import com.padelpro.app.feature.academy.QuizScreen
import com.padelpro.app.feature.academy.QuizViewModel
import com.padelpro.app.feature.academy.RulebookScreen
import com.padelpro.app.feature.history.HistoryViewModel
import com.padelpro.app.feature.history.MatchDetailsScreen
import com.padelpro.app.feature.history.MatchHistoryScreen
import com.padelpro.app.feature.home.HomeScreen
import com.padelpro.app.feature.home.HomeViewModel
import com.padelpro.app.feature.main.AppEntryState
import com.padelpro.app.feature.main.AppEntryViewModel
import com.padelpro.app.feature.match.LiveMatchScreen
import com.padelpro.app.feature.match.LiveMatchViewModel
import com.padelpro.app.feature.match.MatchCenterScreen
import com.padelpro.app.feature.match.MatchResultsScreen
import com.padelpro.app.feature.match.MatchResultsViewModel
import com.padelpro.app.feature.match.MatchSetupScreen
import com.padelpro.app.feature.match.MatchViewModel
import com.padelpro.app.feature.onboarding.OnboardingScreen
import com.padelpro.app.feature.onboarding.OnboardingViewModel
import com.padelpro.app.feature.profile.ProfileScreen
import com.padelpro.app.feature.profile.ProfileViewModel
import com.padelpro.app.feature.statistics.PerformanceScreen
import com.padelpro.app.feature.statistics.PerformanceViewModel
import kotlinx.coroutines.launch

@Composable
fun PadelProNavHost(
    viewModelFactory: ViewModelProvider.Factory
) {
    val entryViewModel: AppEntryViewModel = viewModel(factory = viewModelFactory)
    val entryState by entryViewModel.entryState.collectAsStateWithLifecycle()

    var resolvedStartDestination by remember { mutableStateOf<AppRoute?>(null) }
    if (resolvedStartDestination == null && entryState != AppEntryState.Loading) {
        resolvedStartDestination = if (entryState == AppEntryState.Ready) {
            AppRoute.Home
        } else {
            AppRoute.Onboarding
        }
    }

    val startDestination = resolvedStartDestination
    if (startDestination == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PpBackground)
        )
    } else {
        PadelProNavGraph(
            startDestination = startDestination,
            viewModelFactory = viewModelFactory
        )
    }
}

@Composable
private fun PadelProNavGraph(
    startDestination: AppRoute,
    viewModelFactory: ViewModelProvider.Factory
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val scope = rememberCoroutineScope()

    val homeListState = rememberLazyListState()
    val academyListState = rememberLazyListState()
    val playListState = rememberLazyListState()
    val statsListState = rememberLazyListState()

    val activeTab: BottomTab? = when {
        currentDestination?.hasRoute<AppRoute.Home>() == true -> BottomTab.HOME
        currentDestination?.hasRoute<AppRoute.Academy>() == true -> BottomTab.ACADEMY
        currentDestination?.hasRoute<AppRoute.MatchCenter>() == true -> BottomTab.PLAY
        currentDestination?.hasRoute<AppRoute.Performance>() == true -> BottomTab.STATS
        else -> null
    }

    fun navigateToTab(tab: BottomTab) {
        if (tab == activeTab) {
            scope.launch {
                when (tab) {
                    BottomTab.HOME -> homeListState.animateScrollToItem(0)
                    BottomTab.ACADEMY -> academyListState.animateScrollToItem(0)
                    BottomTab.PLAY -> playListState.animateScrollToItem(0)
                    BottomTab.STATS -> statsListState.animateScrollToItem(0)
                }
            }
            return
        }
        val route: AppRoute = when (tab) {
            BottomTab.HOME -> AppRoute.Home
            BottomTab.ACADEMY -> AppRoute.Academy
            BottomTab.PLAY -> AppRoute.MatchCenter
            BottomTab.STATS -> AppRoute.Performance
        }
        navController.navigate(route) {
            popUpTo<AppRoute.Home> {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = PpBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (activeTab != null) {
                PpBottomBar(
                    selectedTab = activeTab,
                    onSelectTab = ::navigateToTab
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(220)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 01 Onboarding
            composable<AppRoute.Onboarding> {
                val vm: OnboardingViewModel = viewModel(factory = viewModelFactory)
                OnboardingScreen(
                    viewModel = vm,
                    onOnboardingComplete = {
                        navController.navigate(AppRoute.Home) {
                            popUpTo<AppRoute.Onboarding> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 02 Home Dashboard
            composable<AppRoute.Home> {
                val vm: HomeViewModel = viewModel(factory = viewModelFactory)
                HomeScreen(
                    viewModel = vm,
                    listState = homeListState,
                    onOpenProfile = { navController.navigate(AppRoute.Profile) },
                    onStartMatch = { navigateToTab(BottomTab.PLAY) },
                    onContinueLearning = { nextLessonId ->
                        if (nextLessonId != null) {
                            navController.navigate(AppRoute.Lesson(nextLessonId))
                        } else {
                            navigateToTab(BottomTab.ACADEMY)
                        }
                    },
                    onOpenMatchDetails = { id -> navController.navigate(AppRoute.MatchDetails(id)) },
                    onResumeMatch = { navController.navigate(AppRoute.LiveMatch) },
                    onOpenPerformance = { navigateToTab(BottomTab.STATS) }
                )
            }

            // 03 Padel Academy
            composable<AppRoute.Academy> {
                val vm: AcademyViewModel = viewModel(factory = viewModelFactory)
                AcademyScreen(
                    viewModel = vm,
                    listState = academyListState,
                    onOpenCourse = { courseId -> navController.navigate(AppRoute.CourseDetail(courseId)) },
                    onOpenRulebook = { navController.navigate(AppRoute.Rulebook) }
                )
            }

            // 04 Course Detail
            composable<AppRoute.CourseDetail> { entry ->
                val args = entry.toRoute<AppRoute.CourseDetail>()
                val vm: CourseDetailViewModel = viewModel(factory = viewModelFactory)
                CourseDetailScreen(
                    courseId = args.courseId,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenLesson = { lessonId -> navController.navigate(AppRoute.Lesson(lessonId)) }
                )
            }

            // 05 Interactive Lesson
            composable<AppRoute.Lesson> { entry ->
                val args = entry.toRoute<AppRoute.Lesson>()
                val vm: LessonViewModel = viewModel(factory = viewModelFactory)
                LessonScreen(
                    lessonId = args.lessonId,
                    viewModel = vm,
                    onExit = { navController.popBackStack() },
                    onStartQuiz = { lessonId -> navController.navigate(AppRoute.Quiz(lessonId)) }
                )
            }

            // 06 Knowledge Quiz
            composable<AppRoute.Quiz> { entry ->
                val args = entry.toRoute<AppRoute.Quiz>()
                val vm: QuizViewModel = viewModel(factory = viewModelFactory)
                QuizScreen(
                    lessonId = args.lessonId,
                    viewModel = vm,
                    onExit = { navController.popBackStack() },
                    onCompleteQuiz = { lessonId ->
                        navController.navigate(AppRoute.Completion(lessonId)) {
                            popUpTo<AppRoute.Lesson> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 07 Lesson Completion
            composable<AppRoute.Completion> { entry ->
                val args = entry.toRoute<AppRoute.Completion>()
                val vm: CompletionViewModel = viewModel(factory = viewModelFactory)
                CompletionScreen(
                    lessonId = args.lessonId,
                    viewModel = vm,
                    onContinueNext = { nextLessonId ->
                        navController.navigate(AppRoute.Lesson(nextLessonId)) {
                            popUpTo<AppRoute.Completion> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onReturnToAcademy = {
                        val popped = navController.popBackStack(AppRoute.Academy, inclusive = false)
                        if (!popped) {
                            navigateToTab(BottomTab.ACADEMY)
                        }
                    }
                )
            }

            // 08 Padel Rulebook
            composable<AppRoute.Rulebook> {
                val vm: AcademyViewModel = viewModel(factory = viewModelFactory)
                RulebookScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            // 09 Match Center
            composable<AppRoute.MatchCenter> {
                val vm: MatchViewModel = viewModel(factory = viewModelFactory)
                MatchCenterScreen(
                    viewModel = vm,
                    listState = playListState,
                    onOpenSetup = { format -> navController.navigate(AppRoute.MatchSetup(format.name)) },
                    onOpenHistory = { navController.navigate(AppRoute.MatchHistory) },
                    onOpenMatchDetails = { id -> navController.navigate(AppRoute.MatchDetails(id)) },
                    onResumeMatch = { navController.navigate(AppRoute.LiveMatch) }
                )
            }

            // 10 Match Setup
            composable<AppRoute.MatchSetup> { entry ->
                val args = entry.toRoute<AppRoute.MatchSetup>()
                val vm: MatchViewModel = viewModel(factory = viewModelFactory)
                MatchSetupScreen(
                    initialFormat = args.format,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onStartMatch = {
                        navController.navigate(AppRoute.LiveMatch) {
                            popUpTo<AppRoute.MatchSetup> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onResumeActiveMatch = {
                        navController.navigate(AppRoute.LiveMatch) {
                            popUpTo<AppRoute.MatchSetup> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 11 Live Match Scoreboard
            composable<AppRoute.LiveMatch> {
                val vm: LiveMatchViewModel = viewModel(factory = viewModelFactory)
                LiveMatchScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onMatchFinished = { matchId ->
                        navController.navigate(AppRoute.MatchResults(matchId)) {
                            popUpTo<AppRoute.LiveMatch> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 12 Match Results
            composable<AppRoute.MatchResults> { entry ->
                val args = entry.toRoute<AppRoute.MatchResults>()
                val vm: MatchResultsViewModel = viewModel(factory = viewModelFactory)
                MatchResultsScreen(
                    matchId = args.matchId,
                    viewModel = vm,
                    onViewStatistics = { navigateToTab(BottomTab.STATS) },
                    onOpenDetails = { id -> navController.navigate(AppRoute.MatchDetails(id)) },
                    onReturnHome = { navigateToTab(BottomTab.HOME) }
                )
            }

            // 13 Match History
            composable<AppRoute.MatchHistory> {
                val vm: HistoryViewModel = viewModel(factory = viewModelFactory)
                MatchHistoryScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onStartMatch = { navigateToTab(BottomTab.PLAY) },
                    onOpenMatchDetails = { id -> navController.navigate(AppRoute.MatchDetails(id)) }
                )
            }

            // 14 Match Details
            composable<AppRoute.MatchDetails> { entry ->
                val args = entry.toRoute<AppRoute.MatchDetails>()
                MatchDetailsScreen(
                    matchId = args.matchId,
                    onBack = { navController.popBackStack() }
                )
            }

            // 15 Performance Analytics
            composable<AppRoute.Performance> {
                val vm: PerformanceViewModel = viewModel(factory = viewModelFactory)
                PerformanceScreen(
                    viewModel = vm,
                    listState = statsListState,
                    onOpenMatchHistory = { navController.navigate(AppRoute.MatchHistory) },
                    onStartMatch = { navigateToTab(BottomTab.PLAY) }
                )
            }

            // 16 Player Profile
            composable<AppRoute.Profile> {
                val vm: ProfileViewModel = viewModel(factory = viewModelFactory)
                ProfileScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onResetComplete = {
                        navController.navigate(AppRoute.Onboarding) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
