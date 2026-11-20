package com.padelpro.app.data.repository

import androidx.room.withTransaction
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.database.entity.MatchEntity
import com.padelpro.app.core.database.entity.MatchEventEntity
import com.padelpro.app.core.database.entity.MatchParticipantEntity
import com.padelpro.app.core.database.entity.PlayerEntity
import com.padelpro.app.core.database.entity.SetResultEntity
import com.padelpro.app.core.database.entity.UserProgressEntity
import com.padelpro.app.domain.engine.EngineReduceResult
import com.padelpro.app.domain.engine.MatchConfig
import com.padelpro.app.domain.engine.MatchEngine
import com.padelpro.app.domain.engine.MatchEngineEvent
import com.padelpro.app.domain.engine.MatchEngineState
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.LiveMatchSnapshot
import com.padelpro.app.domain.model.MatchCompletionResult
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.MatchStatus
import com.padelpro.app.domain.model.MatchSummary
import com.padelpro.app.domain.model.Player
import com.padelpro.app.domain.model.SetScore
import com.padelpro.app.domain.model.StartMatchRequest
import com.padelpro.app.domain.model.StartMatchResult
import com.padelpro.app.domain.model.TeamSide
import com.padelpro.app.domain.repository.MatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID

class MatchRepositoryImpl(
    private val database: PadelProDatabase,
    private val timeProvider: TimeProvider
) : MatchRepository {

    private val matchDao = database.matchDao()
    private val playerDao = database.playerDao()
    private val userProgressDao = database.userProgressDao()

    private val sessionStartedMatchIds: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())
    private val dismissedRestoredMatchIdsFlow = MutableStateFlow<Set<String>>(emptySet())

    override fun observeActiveMatch(): Flow<MatchSummary?> =
        matchDao.observeActiveMatch().map { entity ->
            entity?.let { buildMatchSummary(it) }
        }

    override fun observeRecentMatches(limit: Int): Flow<List<MatchSummary>> =
        matchDao.observeCompletedMatches().map { list ->
            list.take(limit).map { buildMatchSummary(it) }
        }

    override fun observeCompletedMatches(): Flow<List<MatchSummary>> =
        matchDao.observeCompletedMatches().map { list ->
            list.map { buildMatchSummary(it) }
        }

    override fun observeMatchById(matchId: String): Flow<MatchSummary?> =
        matchDao.observeMatchById(matchId).map { entity ->
            entity?.let { buildMatchSummary(it) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveMatchLive(): Flow<LiveMatchSnapshot?> =
        matchDao.observeActiveMatch().flatMapLatest { activeEntity ->
            if (activeEntity == null) {
                flowOf(null)
            } else {
                combine(
                    matchDao.observeEventsForMatch(activeEntity.id),
                    dismissedRestoredMatchIdsFlow
                ) { eventEntities, dismissedIds ->
                    val participants = matchDao.getParticipantsForMatch(activeEntity.id)
                    val playersById = loadPlayersMap(participants.map { it.playerId })
                    val teamAPlayers = participants
                        .filter { it.team == TeamSide.A.name }
                        .sortedBy { it.position }
                        .map { p -> playersById[p.playerId] ?: fallbackPlayer(p) }
                    val teamBPlayers = participants
                        .filter { it.team == TeamSide.B.name }
                        .sortedBy { it.position }
                        .map { p -> playersById[p.playerId] ?: fallbackPlayer(p) }

                    val config = buildConfig(activeEntity, teamAPlayers, teamBPlayers)
                    val engineEvents = eventEntities.mapNotNull { it.toEngineEvent() }
                    val engineState = MatchEngine.replay(config, engineEvents)
                    val canUndo = engineEvents.any { it is MatchEngineEvent.PointWon }
                    val wasRestored =
                        activeEntity.id !in sessionStartedMatchIds &&
                            activeEntity.id !in dismissedIds

                    LiveMatchSnapshot(
                        matchId = activeEntity.id,
                        state = engineState,
                        teamAPlayers = teamAPlayers,
                        teamBPlayers = teamBPlayers,
                        canUndo = canUndo,
                        wasRestored = wasRestored
                    )
                }
            }
        }

    override suspend fun startMatch(
        request: StartMatchRequest,
        cancelExisting: Boolean
    ): StartMatchResult = withContext(Dispatchers.IO) {
        val expectedCount = if (request.format == MatchFormat.SINGLES) 2 else 4
        require(request.participants.size == expectedCount) {
            "Expected $expectedCount participants for ${request.format}, got ${request.participants.size}"
        }
        require(request.participants.map { it.playerId }.distinct().size == expectedCount) {
            "All match participants must be distinct players"
        }

        val now = timeProvider.nowMillis()
        val matchId = UUID.randomUUID().toString()

        database.withTransaction {
            val existing = matchDao.getActiveMatchOnce()
            if (existing != null) {
                if (!cancelExisting) {
                    return@withTransaction StartMatchResult.ActiveMatchExists(existing.id)
                }
                matchDao.deleteAllActiveMatches()
            }

            val matchEntity = MatchEntity(
                id = matchId,
                format = request.format.name,
                status = MatchStatus.ACTIVE.name,
                deuceMode = request.deuceMode.name,
                bestOf = request.bestOf,
                firstServerTeam = request.firstServerTeam.name,
                startedAt = now,
                endedAt = null,
                durationMs = null,
                winnerTeam = null
            )
            matchDao.insertMatchStrict(matchEntity)

            val participantEntities = request.participants.map { p ->
                MatchParticipantEntity(
                    matchId = matchId,
                    playerId = p.playerId,
                    team = p.team.name,
                    position = p.position,
                    isUser = if (p.isUser) 1 else 0
                )
            }
            matchDao.insertParticipants(participantEntities)

            sessionStartedMatchIds.add(matchId)
            StartMatchResult.Started(matchId)
        }
    }

    override suspend fun addPoint(team: TeamSide): MatchCompletionResult? =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val active = matchDao.getActiveMatchOnce() ?: return@withTransaction null
                val (config, events) = loadConfigAndEvents(active)
                val currentState = MatchEngine.replay(config, events)
                val now = timeProvider.nowMillis()
                val result = MatchEngine.reduce(
                    currentState,
                    MatchEngineEvent.PointWon(team = team, timestampMillis = now)
                )
                when (result) {
                    is EngineReduceResult.Rejected -> null
                    is EngineReduceResult.Accepted -> {
                        val nextSeq = matchDao.getMaxEventSeq(active.id) + 1
                        matchDao.insertEvent(
                            MatchEventEntity(
                                matchId = active.id,
                                seq = nextSeq,
                                type = EVENT_POINT_WON,
                                team = team.name,
                                timestamp = now
                            )
                        )
                        if (result.state.status == MatchStatus.COMPLETED) {
                            completeMatchInTransaction(active, result.state, now)
                        } else {
                            null
                        }
                    }
                }
            }
        }

    override suspend fun undoLastPoint(): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val active = matchDao.getActiveMatchOnce() ?: return@withTransaction false
            val lastPoint = matchDao.getLastPointWonEvent(active.id)
                ?: return@withTransaction false
            matchDao.deleteEventById(lastPoint.id) > 0
        }
    }

    override suspend fun pauseActiveMatch(): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val active = matchDao.getActiveMatchOnce() ?: return@withTransaction false
            val (config, events) = loadConfigAndEvents(active)
            val currentState = MatchEngine.replay(config, events)
            val now = timeProvider.nowMillis()
            val result = MatchEngine.reduce(currentState, MatchEngineEvent.Paused(now))
            if (result is EngineReduceResult.Accepted) {
                val nextSeq = matchDao.getMaxEventSeq(active.id) + 1
                matchDao.insertEvent(
                    MatchEventEntity(
                        matchId = active.id,
                        seq = nextSeq,
                        type = EVENT_PAUSED,
                        team = null,
                        timestamp = now
                    )
                )
                true
            } else {
                false
            }
        }
    }

    override suspend fun resumeActiveMatch(): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val active = matchDao.getActiveMatchOnce() ?: return@withTransaction false
            val (config, events) = loadConfigAndEvents(active)
            val currentState = MatchEngine.replay(config, events)
            val now = timeProvider.nowMillis()
            val result = MatchEngine.reduce(currentState, MatchEngineEvent.Resumed(now))
            if (result is EngineReduceResult.Accepted) {
                val nextSeq = matchDao.getMaxEventSeq(active.id) + 1
                matchDao.insertEvent(
                    MatchEventEntity(
                        matchId = active.id,
                        seq = nextSeq,
                        type = EVENT_RESUMED,
                        team = null,
                        timestamp = now
                    )
                )
                true
            } else {
                false
            }
        }
    }

    override suspend fun cancelActiveMatch(): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            matchDao.deleteAllActiveMatches() > 0
        }
    }

    override suspend fun completeMatchIfFinished(matchId: String): MatchCompletionResult? =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val entity = matchDao.getMatchByIdOnce(matchId) ?: return@withTransaction null
                if (entity.status != MatchStatus.ACTIVE.name) {
                    val winner = entity.winnerTeam?.let {
                        runCatching { TeamSide.valueOf(it) }.getOrNull()
                    } ?: TeamSide.A
                    return@withTransaction MatchCompletionResult(
                        matchId = entity.id,
                        winnerTeam = winner,
                        xpAwarded = 0,
                        alreadyCompleted = true
                    )
                }

                val (config, events) = loadConfigAndEvents(entity)
                val finalState = MatchEngine.replay(config, events)
                if (finalState.status != MatchStatus.COMPLETED || finalState.winnerTeam == null) {
                    return@withTransaction null
                }

                completeMatchInTransaction(entity, finalState, timeProvider.nowMillis())
            }
        }

    override fun dismissRestoredBanner(matchId: String) {
        dismissedRestoredMatchIdsFlow.value = dismissedRestoredMatchIdsFlow.value + matchId
    }

    private suspend fun completeMatchInTransaction(
        entity: MatchEntity,
        finalState: MatchEngineState,
        nowMillis: Long
    ): MatchCompletionResult? {
        val winner = finalState.winnerTeam ?: return null
        val durationMs = finalState.elapsedDurationMillis(nowMillis)
        val updatedRows = matchDao.completeMatchIfActive(
            matchId = entity.id,
            winnerTeam = winner.name,
            endedAt = nowMillis,
            durationMs = durationMs
        )
        if (updatedRows != 1) {
            return MatchCompletionResult(
                matchId = entity.id,
                winnerTeam = winner,
                xpAwarded = 0,
                alreadyCompleted = true
            )
        }

        val setEntities = finalState.completedSets.map { set ->
            SetResultEntity(
                matchId = entity.id,
                setIndex = set.setIndex,
                gamesA = set.gamesA,
                gamesB = set.gamesB,
                tbA = set.tbA,
                tbB = set.tbB
            )
        }
        if (setEntities.isNotEmpty()) {
            matchDao.insertSetResults(setEntities)
        }

        val participants = matchDao.getParticipantsForMatch(entity.id)
        val userParticipant = participants.firstOrNull { it.isUser == 1 }
        val xpAwarded = if (userParticipant != null) {
            val userWon = userParticipant.team == winner.name
            XP_MATCH_PARTICIPATION + if (userWon) XP_MATCH_WIN_BONUS else 0
        } else {
            0
        }

        if (xpAwarded > 0) {
            val existingProgress = userProgressDao.getUserProgressOnce()
            if (existingProgress != null) {
                userProgressDao.addXp(existingProgress.userId, xpAwarded)
            } else {
                val userId = userParticipant?.playerId ?: playerDao.getUserProfileOnce()?.id
                if (userId != null) {
                    userProgressDao.upsertUserProgress(
                        UserProgressEntity(
                            userId = userId,
                            totalXp = xpAwarded,
                            streakCount = 0,
                            lastLearningDate = null
                        )
                    )
                }
            }
        }

        return MatchCompletionResult(
            matchId = entity.id,
            winnerTeam = winner,
            xpAwarded = xpAwarded,
            alreadyCompleted = false
        )
    }

    private suspend fun loadConfigAndEvents(entity: MatchEntity): Pair<MatchConfig, List<MatchEngineEvent>> {
        val participants = matchDao.getParticipantsForMatch(entity.id)
        val playersById = loadPlayersMap(participants.map { it.playerId })
        val teamAPlayers = participants
            .filter { it.team == TeamSide.A.name }
            .sortedBy { it.position }
            .map { p -> playersById[p.playerId] ?: fallbackPlayer(p) }
        val teamBPlayers = participants
            .filter { it.team == TeamSide.B.name }
            .sortedBy { it.position }
            .map { p -> playersById[p.playerId] ?: fallbackPlayer(p) }
        val config = buildConfig(entity, teamAPlayers, teamBPlayers)
        val events = matchDao.getEventsForMatch(entity.id).mapNotNull { it.toEngineEvent() }
        return config to events
    }

    private fun buildConfig(
        entity: MatchEntity,
        teamAPlayers: List<Player>,
        teamBPlayers: List<Player>
    ): MatchConfig {
        return MatchConfig(
            matchId = entity.id,
            format = runCatching { MatchFormat.valueOf(entity.format) }.getOrDefault(MatchFormat.DOUBLES),
            deuceMode = runCatching { DeuceMode.valueOf(entity.deuceMode) }.getOrDefault(DeuceMode.GOLDEN_POINT),
            bestOf = entity.bestOf,
            firstServerTeam = runCatching { TeamSide.valueOf(entity.firstServerTeam) }.getOrDefault(TeamSide.A),
            startedAtMillis = entity.startedAt,
            teamAPlayerNames = teamAPlayers.map { it.displayName },
            teamBPlayerNames = teamBPlayers.map { it.displayName }
        )
    }

    private suspend fun loadPlayersMap(playerIds: List<String>): Map<String, Player> {
        if (playerIds.isEmpty()) return emptyMap()
        return playerDao.getPlayersByIds(playerIds).associate { it.id to it.toDomain() }
    }

    private suspend fun buildMatchSummary(entity: MatchEntity): MatchSummary {
        val participants = matchDao.getParticipantsForMatch(entity.id)
        val playersById = loadPlayersMap(participants.map { it.playerId })
        val sets = matchDao.getSetResultsForMatch(entity.id).map {
            SetScore(
                setIndex = it.setIndex,
                gamesA = it.gamesA,
                gamesB = it.gamesB,
                tbA = it.tbA,
                tbB = it.tbB
            )
        }
        val winner = entity.winnerTeam?.let { runCatching { TeamSide.valueOf(it) }.getOrNull() }
        val userTeam = participants.firstOrNull { it.isUser == 1 }?.team
            ?.let { runCatching { TeamSide.valueOf(it) }.getOrNull() }
        val userWon = if (winner != null && userTeam != null) winner == userTeam else null

        val teamANames = participants
            .filter { it.team == TeamSide.A.name }
            .sortedBy { it.position }
            .map { playersById[it.playerId]?.displayName ?: it.playerId }
        val teamBNames = participants
            .filter { it.team == TeamSide.B.name }
            .sortedBy { it.position }
            .map { playersById[it.playerId]?.displayName ?: it.playerId }

        return MatchSummary(
            id = entity.id,
            format = runCatching { MatchFormat.valueOf(entity.format) }.getOrDefault(MatchFormat.DOUBLES),
            deuceMode = runCatching { DeuceMode.valueOf(entity.deuceMode) }.getOrDefault(DeuceMode.GOLDEN_POINT),
            status = runCatching { MatchStatus.valueOf(entity.status) }.getOrDefault(MatchStatus.COMPLETED),
            startedAt = entity.startedAt,
            endedAt = entity.endedAt,
            durationMs = entity.durationMs,
            winnerTeam = winner,
            userWon = userWon,
            teamANames = teamANames,
            teamBNames = teamBNames,
            sets = sets
        )
    }

    private fun PlayerEntity.toDomain(): Player {
        val format = runCatching {
            MatchFormat.valueOf(preferredFormat ?: MatchFormat.DOUBLES.name)
        }.getOrDefault(MatchFormat.DOUBLES)
        return Player(
            id = id,
            displayName = displayName,
            avatarId = avatarId,
            createdAt = createdAt,
            preferredFormat = format,
            isUser = isUser == 1
        )
    }

    private fun fallbackPlayer(participant: MatchParticipantEntity): Player = Player(
        id = participant.playerId,
        displayName = participant.team,
        avatarId = 0,
        createdAt = 0L,
        preferredFormat = MatchFormat.DOUBLES,
        isUser = participant.isUser == 1
    )

    private fun MatchEventEntity.toEngineEvent(): MatchEngineEvent? = when (type) {
        EVENT_POINT_WON -> {
            val side = team?.let { runCatching { TeamSide.valueOf(it) }.getOrNull() }
            side?.let { MatchEngineEvent.PointWon(team = it, timestampMillis = timestamp) }
        }
        EVENT_PAUSED -> MatchEngineEvent.Paused(timestampMillis = timestamp)
        EVENT_RESUMED -> MatchEngineEvent.Resumed(timestampMillis = timestamp)
        else -> null
    }

    companion object {
        const val EVENT_POINT_WON = "POINT_WON"
        const val EVENT_PAUSED = "PAUSED"
        const val EVENT_RESUMED = "RESUMED"
        const val XP_MATCH_PARTICIPATION = 30
        const val XP_MATCH_WIN_BONUS = 20
    }
}
