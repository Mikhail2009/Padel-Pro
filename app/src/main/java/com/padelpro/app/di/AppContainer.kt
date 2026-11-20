package com.padelpro.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.padelpro.app.core.common.SystemTimeProvider
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.data.repository.AcademyRepositoryImpl
import com.padelpro.app.data.repository.MatchRepositoryImpl
import com.padelpro.app.data.repository.PlayerRepositoryImpl
import com.padelpro.app.data.repository.StatsRepositoryImpl
import com.padelpro.app.domain.repository.AcademyRepository
import com.padelpro.app.domain.repository.MatchRepository
import com.padelpro.app.domain.repository.PlayerRepository
import com.padelpro.app.domain.repository.StatsRepository
import com.padelpro.app.feature.academy.AcademyViewModel
import com.padelpro.app.feature.history.HistoryViewModel
import com.padelpro.app.feature.home.HomeViewModel
import com.padelpro.app.feature.main.AppEntryViewModel
import com.padelpro.app.feature.match.LiveMatchViewModel
import com.padelpro.app.feature.match.MatchResultsViewModel
import com.padelpro.app.feature.match.MatchViewModel
import com.padelpro.app.feature.onboarding.OnboardingViewModel
import com.padelpro.app.feature.profile.ProfileViewModel
import com.padelpro.app.feature.statistics.PerformanceViewModel

class AppContainer(context: Context) {

    val timeProvider: TimeProvider = SystemTimeProvider()

    val database: PadelProDatabase = Room.databaseBuilder(
        context.applicationContext,
        PadelProDatabase::class.java,
        "padel_pro.db"
    ).build()

    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)

    val playerRepository: PlayerRepository = PlayerRepositoryImpl(
        database = database,
        settingsRepository = settingsRepository,
        timeProvider = timeProvider
    )

    val academyRepository: AcademyRepository = AcademyRepositoryImpl(
        academyDao = database.academyDao()
    )

    val matchRepository: MatchRepository = MatchRepositoryImpl(
        database = database,
        timeProvider = timeProvider
    )

    val statsRepository: StatsRepository = StatsRepositoryImpl(
        matchRepository = matchRepository
    )

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when {
                modelClass.isAssignableFrom(AppEntryViewModel::class.java) ->
                    AppEntryViewModel(settingsRepository, playerRepository) as T
                modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
                    OnboardingViewModel(playerRepository) as T
                modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                    HomeViewModel(
                        playerRepository = playerRepository,
                        academyRepository = academyRepository,
                        matchRepository = matchRepository,
                        statsRepository = statsRepository,
                        timeProvider = timeProvider
                    ) as T
                modelClass.isAssignableFrom(AcademyViewModel::class.java) ->
                    AcademyViewModel(
                        playerRepository = playerRepository,
                        academyRepository = academyRepository
                    ) as T
                modelClass.isAssignableFrom(MatchViewModel::class.java) ->
                    MatchViewModel(
                        playerRepository = playerRepository,
                        matchRepository = matchRepository,
                        settingsRepository = settingsRepository,
                        timeProvider = timeProvider
                    ) as T
                modelClass.isAssignableFrom(LiveMatchViewModel::class.java) ->
                    LiveMatchViewModel(
                        matchRepository = matchRepository,
                        settingsRepository = settingsRepository,
                        timeProvider = timeProvider
                    ) as T
                modelClass.isAssignableFrom(MatchResultsViewModel::class.java) ->
                    MatchResultsViewModel(
                        matchRepository = matchRepository,
                        timeProvider = timeProvider
                    ) as T
                modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                    HistoryViewModel(
                        matchRepository = matchRepository,
                        timeProvider = timeProvider
                    ) as T
                modelClass.isAssignableFrom(PerformanceViewModel::class.java) ->
                    PerformanceViewModel(statsRepository = statsRepository) as T
                modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                    ProfileViewModel(
                        playerRepository = playerRepository,
                        academyRepository = academyRepository,
                        statsRepository = statsRepository,
                        settingsRepository = settingsRepository,
                        timeProvider = timeProvider
                    ) as T
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
