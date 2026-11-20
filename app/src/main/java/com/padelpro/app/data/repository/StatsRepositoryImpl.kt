package com.padelpro.app.data.repository

import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MonthlyMatchCount
import com.padelpro.app.domain.model.PerformanceSummary
import com.padelpro.app.domain.repository.MatchRepository
import com.padelpro.app.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class StatsRepositoryImpl(
    private val matchRepository: MatchRepository
) : StatsRepository {

    override fun observePerformanceSummary(): Flow<PerformanceSummary> =
        matchRepository.observeCompletedMatches().map { matches ->
            val userMatches = matches.filter { it.userWon != null }
            val total = userMatches.size
            if (total == 0) {
                return@map PerformanceSummary(
                    monthlyActivity = emptySixMonths()
                )
            }

            val wins = userMatches.count { it.userWon == true }
            val losses = total - wins
            val winRate = ((wins.toDouble() / total.toDouble()) * 100.0).roundToInt()

            val latestResult = userMatches.first().userWon == true
            var currentStreakCount = 0
            for (m in userMatches) {
                if (m.userWon == latestResult) currentStreakCount++ else break
            }
            val currentStreakLabel = if (latestResult) "В$currentStreakCount" else "П$currentStreakCount"

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
            val singlesWinRate = if (singles.isEmpty()) null else {
                ((singles.count { it.userWon == true }.toDouble() / singles.size) * 100.0).roundToInt()
            }
            val doublesWinRate = if (doubles.isEmpty()) null else {
                ((doubles.count { it.userWon == true }.toDouble() / doubles.size) * 100.0).roundToInt()
            }

            PerformanceSummary(
                totalMatches = total,
                wins = wins,
                losses = losses,
                winRatePercent = winRate,
                currentStreakLabel = currentStreakLabel,
                currentStreakCount = currentStreakCount,
                isCurrentStreakWin = latestResult,
                bestWinStreak = bestWinStreak,
                singlesMatches = singles.size,
                singlesWinRatePercent = singlesWinRate,
                doublesMatches = doubles.size,
                doublesWinRatePercent = doublesWinRate,
                recentForm = userMatches.take(5).map { it.userWon == true },
                monthlyActivity = emptySixMonths()
            )
        }

    private fun emptySixMonths(): List<MonthlyMatchCount> {
        val current = YearMonth.now()
        val ruLocale = Locale.forLanguageTag("ru")
        val fmt = DateTimeFormatter.ofPattern("LLL", ruLocale)
        return (5 downTo 0).map { offset ->
            val ym = current.minusMonths(offset.toLong())
            MonthlyMatchCount(
                label = ym.format(fmt).trimEnd('.').take(3).lowercase(ruLocale),
                count = 0,
                isCurrentMonth = offset == 0
            )
        }
    }
}
