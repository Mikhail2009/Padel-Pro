package com.padelpro.app.data.backup

import androidx.room.withTransaction
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.database.entity.AchievementUnlockedEntity
import com.padelpro.app.core.database.entity.LessonProgressEntity
import com.padelpro.app.core.database.entity.MatchEntity
import com.padelpro.app.core.database.entity.MatchEventEntity
import com.padelpro.app.core.database.entity.MatchParticipantEntity
import com.padelpro.app.core.database.entity.PlayerEntity
import com.padelpro.app.core.database.entity.QuizAttemptEntity
import com.padelpro.app.core.database.entity.RuleFavoriteEntity
import com.padelpro.app.core.database.entity.SetResultEntity
import com.padelpro.app.core.database.entity.UserProgressEntity
import com.padelpro.app.core.datastore.AppSettings
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.data.repository.MatchRepositoryImpl
import com.padelpro.app.domain.academy.ContentPack
import com.padelpro.app.domain.model.BackupImportOutcome
import com.padelpro.app.domain.model.BackupPreview
import com.padelpro.app.domain.model.BackupValidationError
import com.padelpro.app.domain.model.BackupValidationResult
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.TeamSide
import com.padelpro.app.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class BackupRepositoryImpl(
    private val database: PadelProDatabase,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val contentPackProvider: () -> ContentPack
) : BackupRepository {

    private val playerDao = database.playerDao()
    private val userProgressDao = database.userProgressDao()
    private val matchDao = database.matchDao()
    private val academyDao = database.academyDao()

    private val jsonCodec = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val currentSettings = settingsRepository.settings.first()
        val players = playerDao.getAllPlayersOnce()
        val userProgress = userProgressDao.getUserProgressOnce()
        val matches = matchDao.getAllMatchesOnce()
        val participants = matchDao.getAllMatchParticipantsOnce()
        val events = matchDao.getAllMatchEventsOnce()
        val setResults = matchDao.getAllSetResultsOnce()
        val lessonProgress = academyDao.getAllLessonProgressOnce()
        val quizAttempts = academyDao.getAllQuizAttemptsOnce()
        val achievements = academyDao.getUnlockedAchievementsOnce()
        val ruleFavorites = academyDao.getAllRuleFavoritesOnce()

        val payload = BackupPayload(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            exportedAt = timeProvider.nowMillis(),
            players = players.map {
                BackupPlayer(
                    id = it.id,
                    displayName = it.displayName,
                    avatarId = it.avatarId,
                    createdAt = it.createdAt,
                    preferredFormat = it.preferredFormat,
                    isUser = it.isUser
                )
            },
            userProgress = userProgress?.let {
                BackupUserProgress(
                    userId = it.userId,
                    totalXp = it.totalXp,
                    streakCount = it.streakCount,
                    lastLearningDate = it.lastLearningDate
                )
            },
            matches = matches.map {
                BackupMatch(
                    id = it.id,
                    format = it.format,
                    status = it.status,
                    deuceMode = it.deuceMode,
                    bestOf = it.bestOf,
                    firstServerTeam = it.firstServerTeam,
                    startedAt = it.startedAt,
                    endedAt = it.endedAt,
                    durationMs = it.durationMs,
                    winnerTeam = it.winnerTeam
                )
            },
            matchParticipants = participants.map {
                BackupMatchParticipant(
                    matchId = it.matchId,
                    playerId = it.playerId,
                    team = it.team,
                    position = it.position,
                    isUser = it.isUser
                )
            },
            matchEvents = events.map {
                BackupMatchEvent(
                    id = it.id,
                    matchId = it.matchId,
                    seq = it.seq,
                    type = it.type,
                    team = it.team,
                    timestamp = it.timestamp
                )
            },
            setResults = setResults.map {
                BackupSetResult(
                    matchId = it.matchId,
                    setIndex = it.setIndex,
                    gamesA = it.gamesA,
                    gamesB = it.gamesB,
                    tbA = it.tbA,
                    tbB = it.tbB
                )
            },
            lessonProgress = lessonProgress.map {
                BackupLessonProgress(
                    lessonId = it.lessonId,
                    status = it.status,
                    completedAt = it.completedAt,
                    bestQuizScore = it.bestQuizScore,
                    xpAwarded = it.xpAwarded
                )
            },
            quizAttempts = quizAttempts.map {
                BackupQuizAttempt(
                    id = it.id,
                    lessonId = it.lessonId,
                    attemptedAt = it.attemptedAt,
                    correct = it.correct,
                    total = it.total
                )
            },
            achievementsUnlocked = achievements.map {
                BackupAchievementUnlocked(
                    achievementId = it.achievementId,
                    unlockedAt = it.unlockedAt
                )
            },
            ruleFavorites = ruleFavorites.map {
                BackupRuleFavorite(ruleId = it.ruleId)
            },
            settings = BackupSettings(
                onboardingCompleted = currentSettings.onboardingCompleted,
                hapticsEnabled = currentSettings.hapticsEnabled,
                keepScreenOn = currentSettings.keepScreenOn,
                defaultFormat = currentSettings.defaultFormat.name,
                defaultDeuceMode = currentSettings.defaultDeuceMode.name,
                contentVersion = currentSettings.contentVersion,
                backupLastExportedAt = currentSettings.backupLastExportedAt
            )
        )

        jsonCodec.encodeToString(BackupPayload.serializer(), payload)
    }

    override fun validateBackupJson(json: String): BackupValidationResult {
        return when (val parsed = parseAndValidate(json)) {
            is ParsedBackup.Invalid -> BackupValidationResult.Invalid(parsed.error)
            is ParsedBackup.Valid -> {
                val payload = parsed.payload
                BackupValidationResult.Valid(
                    BackupPreview(
                        schemaVersion = payload.schemaVersion,
                        exportedAt = payload.exportedAt,
                        matchesCount = payload.matches.count { it.status == MatchStatus.COMPLETED.name },
                        playersCount = payload.players.size,
                        lessonsCompletedCount = payload.lessonProgress.count { it.status == "COMPLETED" },
                        hasUserProfile = payload.players.any { it.isUser == 1 }
                    )
                )
            }
        }
    }

    override suspend fun importBackupJson(json: String): BackupImportOutcome =
        withContext(Dispatchers.IO) {
            val parsed = parseAndValidate(json)
            if (parsed is ParsedBackup.Invalid) {
                return@withContext BackupImportOutcome.Error(parsed.error)
            }
            val payload = (parsed as ParsedBackup.Valid).payload

            database.withTransaction {
                matchDao.deleteAllSetResults()
                matchDao.deleteAllEvents()
                matchDao.deleteAllParticipants()
                matchDao.deleteAllMatches()
                userProgressDao.deleteAllProgress()
                academyDao.deleteAllLessonProgress()
                academyDao.deleteAllQuizAttempts()
                academyDao.deleteAllAchievements()
                academyDao.deleteAllRuleFavorites()
                playerDao.deleteAllPlayers()

                if (payload.players.isNotEmpty()) {
                    playerDao.insertPlayers(
                        payload.players.map {
                            PlayerEntity(
                                id = it.id,
                                displayName = it.displayName.trim().take(20),
                                avatarId = it.avatarId.coerceIn(0, 11),
                                createdAt = it.createdAt,
                                preferredFormat = it.preferredFormat,
                                isUser = it.isUser
                            )
                        }
                    )
                }

                payload.userProgress?.let { up ->
                    userProgressDao.upsertUserProgress(
                        UserProgressEntity(
                            userId = up.userId,
                            totalXp = up.totalXp,
                            streakCount = up.streakCount,
                            lastLearningDate = up.lastLearningDate
                        )
                    )
                }

                if (payload.matches.isNotEmpty()) {
                    matchDao.insertMatches(
                        payload.matches.map {
                            MatchEntity(
                                id = it.id,
                                format = it.format,
                                status = it.status,
                                deuceMode = it.deuceMode,
                                bestOf = it.bestOf,
                                firstServerTeam = it.firstServerTeam,
                                startedAt = it.startedAt,
                                endedAt = it.endedAt,
                                durationMs = it.durationMs,
                                winnerTeam = it.winnerTeam
                            )
                        }
                    )
                }

                if (payload.matchParticipants.isNotEmpty()) {
                    matchDao.insertParticipants(
                        payload.matchParticipants.map {
                            MatchParticipantEntity(
                                matchId = it.matchId,
                                playerId = it.playerId,
                                team = it.team,
                                position = it.position,
                                isUser = it.isUser
                            )
                        }
                    )
                }

                if (payload.matchEvents.isNotEmpty()) {
                    matchDao.insertEvents(
                        payload.matchEvents.map {
                            MatchEventEntity(
                                id = it.id,
                                matchId = it.matchId,
                                seq = it.seq,
                                type = it.type,
                                team = it.team,
                                timestamp = it.timestamp
                            )
                        }
                    )
                }

                if (payload.setResults.isNotEmpty()) {
                    matchDao.insertSetResults(
                        payload.setResults.map {
                            SetResultEntity(
                                matchId = it.matchId,
                                setIndex = it.setIndex,
                                gamesA = it.gamesA,
                                gamesB = it.gamesB,
                                tbA = it.tbA,
                                tbB = it.tbB
                            )
                        }
                    )
                }

                if (payload.lessonProgress.isNotEmpty()) {
                    academyDao.insertLessonProgressList(
                        payload.lessonProgress.map {
                            LessonProgressEntity(
                                lessonId = it.lessonId,
                                status = it.status,
                                completedAt = it.completedAt,
                                bestQuizScore = it.bestQuizScore,
                                xpAwarded = it.xpAwarded
                            )
                        }
                    )
                }

                if (payload.quizAttempts.isNotEmpty()) {
                    academyDao.insertQuizAttempts(
                        payload.quizAttempts.map {
                            QuizAttemptEntity(
                                id = it.id,
                                lessonId = it.lessonId,
                                attemptedAt = it.attemptedAt,
                                correct = it.correct,
                                total = it.total
                            )
                        }
                    )
                }

                if (payload.achievementsUnlocked.isNotEmpty()) {
                    academyDao.insertUnlockedAchievements(
                        payload.achievementsUnlocked.map {
                            AchievementUnlockedEntity(
                                achievementId = it.achievementId,
                                unlockedAt = it.unlockedAt
                            )
                        }
                    )
                }

                if (payload.ruleFavorites.isNotEmpty()) {
                    academyDao.insertRuleFavorites(
                        payload.ruleFavorites.map {
                            RuleFavoriteEntity(ruleId = it.ruleId)
                        }
                    )
                }
            }

            val hasUserProfile = payload.players.any { it.isUser == 1 }
            val importedSettings = AppSettings(
                onboardingCompleted = payload.settings.onboardingCompleted && hasUserProfile,
                hapticsEnabled = payload.settings.hapticsEnabled,
                keepScreenOn = payload.settings.keepScreenOn,
                defaultFormat = MatchFormat.valueOf(payload.settings.defaultFormat),
                defaultDeuceMode = DeuceMode.valueOf(payload.settings.defaultDeuceMode),
                contentVersion = payload.settings.contentVersion,
                backupLastExportedAt = payload.settings.backupLastExportedAt
            )
            settingsRepository.applySettings(importedSettings)

            BackupImportOutcome.Success(
                hasUserProfile = hasUserProfile,
                matchesCount = payload.matches.count { it.status == MatchStatus.COMPLETED.name },
                playersCount = payload.players.size
            )
        }

    private fun parseAndValidate(rawJson: String): ParsedBackup {
        if (rawJson.isBlank()) {
            return ParsedBackup.Invalid(BackupValidationError.INVALID_JSON)
        }

        val payload = try {
            jsonCodec.decodeFromString(BackupPayload.serializer(), rawJson)
        } catch (_: SerializationException) {
            return ParsedBackup.Invalid(BackupValidationError.INVALID_JSON)
        } catch (_: IllegalArgumentException) {
            return ParsedBackup.Invalid(BackupValidationError.INVALID_JSON)
        }

        if (payload.schemaVersion != CURRENT_SCHEMA_VERSION) {
            return ParsedBackup.Invalid(BackupValidationError.UNSUPPORTED_SCHEMA_VERSION)
        }

        if (payload.exportedAt <= 0L) {
            return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
        }

        // Validate settings enums
        val defaultFormatValid = runCatching { MatchFormat.valueOf(payload.settings.defaultFormat) }.isSuccess
        val defaultDeuceValid = runCatching { DeuceMode.valueOf(payload.settings.defaultDeuceMode) }.isSuccess
        if (!defaultFormatValid || !defaultDeuceValid || payload.settings.contentVersion < 1) {
            return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
        }

        // Validate players
        val playerIds = mutableSetOf<String>()
        var userCount = 0
        for (p in payload.players) {
            if (p.id.isBlank() || !playerIds.add(p.id)) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (p.displayName.trim().isEmpty() || p.displayName.trim().length > 20) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (p.avatarId !in 0..11 || p.isUser !in 0..1) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (p.preferredFormat != null && runCatching { MatchFormat.valueOf(p.preferredFormat) }.isFailure) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (p.isUser == 1) userCount++
        }
        if (userCount > 1) {
            return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
        }

        // Validate userProgress FK & bounds
        payload.userProgress?.let { up ->
            if (up.userId !in playerIds || up.totalXp < 0 || up.streakCount < 0) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
        }

        // Validate matches
        val matchIds = mutableSetOf<String>()
        var activeMatchesCount = 0
        for (m in payload.matches) {
            if (m.id.isBlank() || !matchIds.add(m.id)) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (runCatching { MatchFormat.valueOf(m.format) }.isFailure ||
                runCatching { MatchStatus.valueOf(m.status) }.isFailure ||
                runCatching { DeuceMode.valueOf(m.deuceMode) }.isFailure ||
                runCatching { TeamSide.valueOf(m.firstServerTeam) }.isFailure ||
                m.bestOf <= 0
            ) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (m.winnerTeam != null && runCatching { TeamSide.valueOf(m.winnerTeam) }.isFailure) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (m.status == MatchStatus.ACTIVE.name) {
                activeMatchesCount++
            }
        }
        if (activeMatchesCount > 1) {
            return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
        }

        // Validate matchParticipants FKs & composite PK uniqueness
        val participantKeys = mutableSetOf<Pair<String, String>>()
        for (mp in payload.matchParticipants) {
            if (mp.matchId !in matchIds || mp.playerId !in playerIds) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (!participantKeys.add(mp.matchId to mp.playerId)) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (runCatching { TeamSide.valueOf(mp.team) }.isFailure || mp.isUser !in 0..1) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
        }

        // Validate matchEvents FK & unique (match_id, seq)
        val eventKeys = mutableSetOf<Pair<String, Int>>()
        for (ev in payload.matchEvents) {
            if (ev.matchId !in matchIds || !eventKeys.add(ev.matchId to ev.seq)) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            when (ev.type) {
                MatchRepositoryImpl.EVENT_POINT_WON -> {
                    if (ev.team == null || runCatching { TeamSide.valueOf(ev.team) }.isFailure) {
                        return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
                    }
                }
                MatchRepositoryImpl.EVENT_PAUSED,
                MatchRepositoryImpl.EVENT_RESUMED -> {
                    if (ev.team != null && runCatching { TeamSide.valueOf(ev.team) }.isFailure) {
                        return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
                    }
                }
                else -> return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
        }

        // Validate setResults FK & composite PK uniqueness
        val setKeys = mutableSetOf<Pair<String, Int>>()
        for (sr in payload.setResults) {
            if (sr.matchId !in matchIds || !setKeys.add(sr.matchId to sr.setIndex)) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
            if (sr.setIndex < 1 || sr.gamesA < 0 || sr.gamesB < 0) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
        }

        // Validate Academy & Rulebook content ID references
        val pack = contentPackProvider()
        val validLessonIds = pack.courses.flatMap { it.lessons }.map { it.id }.toSet()
        val validAchievementIds = pack.achievements.map { it.id }.toSet()
        val validRuleIds = pack.rules.map { it.id }.toSet()

        val lessonProgressIds = mutableSetOf<String>()
        for (lp in payload.lessonProgress) {
            if (lp.lessonId !in validLessonIds) {
                return ParsedBackup.Invalid(BackupValidationError.UNKNOWN_CONTENT_REFERENCE)
            }
            if (!lessonProgressIds.add(lp.lessonId) || lp.status.isBlank() || lp.xpAwarded < 0) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
        }

        for (qa in payload.quizAttempts) {
            if (qa.lessonId !in validLessonIds) {
                return ParsedBackup.Invalid(BackupValidationError.UNKNOWN_CONTENT_REFERENCE)
            }
            if (qa.total <= 0 || qa.correct !in 0..qa.total) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
        }

        val unlockedIds = mutableSetOf<String>()
        for (ach in payload.achievementsUnlocked) {
            if (ach.achievementId !in validAchievementIds) {
                return ParsedBackup.Invalid(BackupValidationError.UNKNOWN_CONTENT_REFERENCE)
            }
            if (!unlockedIds.add(ach.achievementId)) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
        }

        val favIds = mutableSetOf<String>()
        for (fav in payload.ruleFavorites) {
            if (fav.ruleId !in validRuleIds) {
                return ParsedBackup.Invalid(BackupValidationError.UNKNOWN_CONTENT_REFERENCE)
            }
            if (!favIds.add(fav.ruleId)) {
                return ParsedBackup.Invalid(BackupValidationError.INVALID_DATA)
            }
        }

        return ParsedBackup.Valid(payload)
    }

    private sealed interface ParsedBackup {
        data class Valid(val payload: BackupPayload) : ParsedBackup
        data class Invalid(val error: BackupValidationError) : ParsedBackup
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
