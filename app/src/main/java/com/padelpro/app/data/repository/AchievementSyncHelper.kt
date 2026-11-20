package com.padelpro.app.data.repository

import com.padelpro.app.core.common.LevelUtils
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.database.entity.AchievementUnlockedEntity
import com.padelpro.app.domain.academy.AchievementEngine
import com.padelpro.app.domain.academy.AchievementEvaluationState
import com.padelpro.app.domain.academy.ContentPack
import com.padelpro.app.domain.model.AchievementItem
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat

internal object AchievementSyncHelper {

    suspend fun evaluateAndUnlock(
        database: PadelProDatabase,
        contentPack: ContentPack,
        unlockedAtMillis: Long
    ): List<AchievementItem> {
        val academyDao = database.academyDao()
        val matchDao = database.matchDao()
        val userProgressDao = database.userProgressDao()

        val completedLessonIds = academyDao.getAllLessonProgressOnce()
            .filter { it.status == "COMPLETED" }
            .map { it.lessonId }
            .toSet()

        val completedCourses = contentPack.courses.count { course ->
            course.lessons.isNotEmpty() && course.lessons.all { it.id in completedLessonIds }
        }
        val hasPerfectQuiz = academyDao.hasPerfectQuizAttemptOnce()
        val userProgress = userProgressDao.getUserProgressOnce()
        val learningStreak = userProgress?.streakCount ?: 0
        val currentLevel = LevelUtils.calculateLevel(userProgress?.totalXp ?: 0)

        val completedMatches = matchDao.getCompletedMatchesOnce()
        val userTeamByMatch = matchDao.getAllUserParticipationsOnce()
            .associate { it.matchId to it.team }

        var matchesPlayed = 0
        var matchWins = 0
        var bestWinStreak = 0
        var runningWins = 0
        var singlesWins = 0
        var doublesWins = 0
        var hasGoldenPointWin = false

        for (match in completedMatches) {
            val userTeam = userTeamByMatch[match.id] ?: continue
            matchesPlayed++
            val userWon = match.winnerTeam != null && match.winnerTeam == userTeam
            if (userWon) {
                matchWins++
                runningWins++
                if (runningWins > bestWinStreak) {
                    bestWinStreak = runningWins
                }
                when (match.format) {
                    MatchFormat.SINGLES.name -> singlesWins++
                    MatchFormat.DOUBLES.name -> doublesWins++
                }
                if (match.deuceMode == DeuceMode.GOLDEN_POINT.name) {
                    hasGoldenPointWin = true
                }
            } else {
                runningWins = 0
            }
        }

        val state = AchievementEvaluationState(
            completedLessons = completedLessonIds.size,
            completedCourses = completedCourses,
            hasPerfectQuiz = hasPerfectQuiz,
            learningStreak = learningStreak,
            matchesPlayed = matchesPlayed,
            matchWins = matchWins,
            bestWinStreak = bestWinStreak,
            singlesWins = singlesWins,
            doublesWins = doublesWins,
            currentLevel = currentLevel,
            hasGoldenPointWin = hasGoldenPointWin
        )

        val alreadyUnlocked = academyDao.getUnlockedAchievementsOnce()
            .map { it.achievementId }
            .toSet()

        val newlyUnlocked = AchievementEngine.evaluateNewlyUnlocked(
            definitions = contentPack.achievements,
            alreadyUnlockedIds = alreadyUnlocked,
            state = state
        )

        for (definition in newlyUnlocked) {
            academyDao.insertUnlockedAchievement(
                AchievementUnlockedEntity(
                    achievementId = definition.id,
                    unlockedAt = unlockedAtMillis
                )
            )
        }

        return newlyUnlocked.map { def ->
            AchievementItem(
                id = def.id,
                isUnlocked = true,
                unlockedAt = unlockedAtMillis,
                title = def.title,
                description = def.description,
                icon = def.icon
            )
        }
    }
}
