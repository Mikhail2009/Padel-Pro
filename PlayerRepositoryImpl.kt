package com.padelpro.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.padelpro.app.core.common.TimeProvider
import com.padelpro.app.core.database.PadelProDatabase
import com.padelpro.app.core.database.entity.PlayerEntity
import com.padelpro.app.core.database.entity.UserProgressEntity
import com.padelpro.app.core.datastore.SettingsRepository
import com.padelpro.app.domain.model.DeleteFriendResult
import com.padelpro.app.domain.model.MatchFormat
import com.padelpro.app.domain.model.Player
import com.padelpro.app.domain.model.UserProgress
import com.padelpro.app.domain.repository.PlayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class PlayerRepositoryImpl(
    private val database: PadelProDatabase,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : PlayerRepository {

    private val playerDao = database.playerDao()
    private val userProgressDao = database.userProgressDao()
    private val matchDao = database.matchDao()

    override fun observeUserProfile(): Flow<Player?> =
        playerDao.observeUserProfile().map { it?.toDomain() }

    override fun observeFriends(): Flow<List<Player>> =
        playerDao.observeFriends().map { list -> list.map { it.toDomain() } }

    override fun observeUserProgress(): Flow<UserProgress?> =
        userProgressDao.observeUserProgress().map { it?.toDomain() }

    override suspend fun createLocalProfile(
        displayName: String,
        avatarId: Int,
        preferredFormat: MatchFormat
    ): Player = withContext(Dispatchers.IO) {
        val sanitizedName = displayName.trim().take(20)
        val now = timeProvider.nowMillis()
        val existing = playerDao.getUserProfileOnce()
        val playerId = existing?.id ?: UUID.randomUUID().toString()
        val entity = PlayerEntity(
            id = playerId,
            displayName = sanitizedName,
            avatarId = avatarId.coerceIn(0, 11),
            createdAt = existing?.createdAt ?: now,
            preferredFormat = preferredFormat.name,
            isUser = 1
        )
        database.withTransaction {
            if (existing == null) {
                playerDao.deleteUserPlayers()
            }
            playerDao.upsertPlayer(entity)
            if (existing == null) {
                userProgressDao.upsertUserProgress(
                    UserProgressEntity(
                        userId = playerId,
                        totalXp = 0,
                        streakCount = 0,
                        lastLearningDate = null
                    )
                )
            }
        }
        settingsRepository.setDefaultFormat(preferredFormat)
        settingsRepository.setOnboardingCompleted(true)
        entity.toDomain()
    }

    override suspend fun updateUserProfile(displayName: String, avatarId: Int) =
        withContext(Dispatchers.IO) {
            val sanitizedName = displayName.trim().take(20)
            if (sanitizedName.isNotEmpty()) {
                playerDao.updateUserProfile(sanitizedName, avatarId.coerceIn(0, 11))
            }
        }

    override suspend fun updatePreferredFormat(format: MatchFormat) =
        withContext(Dispatchers.IO) {
            playerDao.updatePreferredFormat(format.name)
            settingsRepository.setDefaultFormat(format)
        }

    override suspend fun createFriend(displayName: String, avatarId: Int): Player =
        withContext(Dispatchers.IO) {
            val sanitizedName = displayName.trim().take(20)
            require(sanitizedName.isNotEmpty()) { "Friend display name cannot be blank" }
            val entity = PlayerEntity(
                id = UUID.randomUUID().toString(),
                displayName = sanitizedName,
                avatarId = avatarId.coerceIn(0, 11),
                createdAt = timeProvider.nowMillis(),
                preferredFormat = null,
                isUser = 0
            )
            playerDao.upsertPlayer(entity)
            entity.toDomain()
        }

    override suspend fun updateFriend(playerId: String, displayName: String, avatarId: Int) {
        withContext(Dispatchers.IO) {
            val sanitizedName = displayName.trim().take(20)
            if (sanitizedName.isNotEmpty()) {
                playerDao.updateFriend(playerId, sanitizedName, avatarId.coerceIn(0, 11))
            }
        }
    }

    override suspend fun deleteFriend(playerId: String): DeleteFriendResult =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val player = playerDao.getPlayerById(playerId)
                    ?: return@withTransaction DeleteFriendResult.NOT_FOUND
                if (player.isUser == 1) {
                    return@withTransaction DeleteFriendResult.NOT_FOUND
                }
                val participations = matchDao.countParticipationsForPlayer(playerId)
                if (participations > 0) {
                    return@withTransaction DeleteFriendResult.IN_USE_BY_MATCHES
                }
                return@withTransaction try {
                    val deleted = playerDao.deleteFriendById(playerId)
                    if (deleted > 0) DeleteFriendResult.DELETED else DeleteFriendResult.NOT_FOUND
                } catch (_: SQLiteConstraintException) {
                    DeleteFriendResult.IN_USE_BY_MATCHES
                }
            }
        }

    override suspend fun resetAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        settingsRepository.clearAll()
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

    private fun UserProgressEntity.toDomain(): UserProgress = UserProgress(
        userId = userId,
        totalXp = totalXp,
        streakCount = streakCount,
        lastLearningDate = lastLearningDate
    )
}
