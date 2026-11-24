package com.padelpro.app.data.repository

import com.padelpro.app.core.common.SystemTimeProvider
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.domain.model.MatchFormOutcome
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.MonthlyMatchCount
import com.padelpro.app.domain.model.PerformanceSummary
import com.padelpro.app.domain.repository.MatchRepository
import com.padelpro.app.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class StatsRepositoryImpl(
    private val matchRepository: MatchRepository,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : StatsRepository {

    override fun observePerformanceSummary(): Flow<PerformanceSummary> =
        matchRepository.observeCompletedMatches().map { matches ->
            val userMatches = matches
                .filter { it.isUserParticipant }
                .sortedByDescending { it.endedAt ?: it.startedAt }

            val total = userMatches.size
            if (total == 0) {
                return@map PerformanceSummary(
                    monthlyActivity = buildSixMonths(emptyList())
                )
            }

            val wins = userMatches.count { it.userWon == true }
            val losses = userMatches.count { it.userWon == false }
            val draws = userMatches.count { it.isManuallyCompleted }
            val decided = wins + losses
            val winRate = if (decided == 0) null else {
                ((wins.toDouble() / decided.toDouble()) * 100.0).roundToInt()
            }

            val latestOutcome = userMatches.first().userWon
            val currentStreakCount: Int
            val isCurrentStreakWin: Boolean?
            val currentStreakLabel: String?
            if (latestOutcome == null) {
                currentStreakCount = 0
                isCurrentStreakWin = null
                currentStreakLabel = null
            } else {
                var streak = 0
                for (m in userMatches) {
                    if (m.userWon == latestOutcome) streak++ else break
                }
                currentStreakCount = streak
                isCurrentStreakWin = latestOutcome
                currentStreakLabel = if (latestOutcome) "В$streak" else "П$streak"
            }

            var bestWinStreak = 0
            var runningWins = 0
            for (m in userMatches.reversed()) {
                if (m.userWon == true) {
                    runningWins++
                    if (runningWins > bestWinStreak) bestWinStreak = runningWins
                } else {
                    runningWins = 0
                }
            }

            val singles = userMatches.filter { it.format == MatchFormat.SINGLES }
            val doubles = userMatches.filter { it.format == MatchFormat.DOUBLES }
            val singlesDecided = singles.count { it.userWon != null }
            val doublesDecided = doubles.count { it.userWon != null }
            val singlesWinRate = if (singlesDecided == 0) null else {
                ((singles.count { it.userWon == true }.toDouble() / singlesDecided) * 100.0).roundToInt()
            }
            val doublesWinRate = if (doublesDecided == 0) null else {
                ((doubles.count { it.userWon == true }.toDouble() / doublesDecided) * 100.0).roundToInt()
            }

            PerformanceSummary(
                totalMatches = total,
                wins = wins,
                losses = losses,
                draws = draws,
                winRatePercent = winRate,
                currentStreakLabel = currentStreakLabel,
                currentStreakCount = currentStreakCount,
                isCurrentStreakWin = isCurrentStreakWin,
                bestWinStreak = bestWinStreak,
                singlesMatches = singles.size,
                singlesWinRatePercent = singlesWinRate,
                doublesMatches = doubles.size,
                doublesWinRatePercent = doublesWinRate,
                recentForm = userMatches.take(5).map { match ->
                    when (match.userWon) {
                        true -> MatchFormOutcome.WIN
                        false -> MatchFormOutcome.LOSS
                        null -> MatchFormOutcome.DRAW
                    }
                },
                monthlyActivity = buildSixMonths(userMatches)
            )
        }

    private fun buildSixMonths(userMatches: List<MatchSummary>): List<MonthlyMatchCount> {
        val current = YearMonth.from(timeProvider.currentLocalDate())
        val ruLocale = Locale.forLanguageTag("ru")
        val fmt = DateTimeFormatter.ofPattern("LLL", ruLocale)
        val countsByMonth = userMatches
            .groupingBy { match ->
                val ts = match.endedAt ?: match.startedAt
                YearMonth.from(Instant.ofEpochMilli(ts).atZone(zoneId))
            }
            .eachCount()

        return (5 downTo 0).map { offset ->
            val ym = current.minusMonths(offset.toLong())
            MonthlyMatchCount(
                label = ym.format(fmt).trimEnd('.').take(3).lowercase(ruLocale),
                count = countsByMonth[ym] ?: 0,
                isCurrentMonth = offset == 0
            )
        }
    }
}
