package com.padelpro.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.padelpro.app.core.database.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE is_user = 1 LIMIT 1")
    fun observeUserProfile(): Flow<PlayerEntity?>

    @Query("SELECT * FROM players WHERE is_user = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): PlayerEntity?

    @Query("SELECT * FROM players WHERE is_user = 0 ORDER BY display_name ASC")
    fun observeFriends(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id IN (:ids)")
    suspend fun getPlayersByIds(ids: List<String>): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    suspend fun getPlayerById(id: String): PlayerEntity?

    @Query("SELECT * FROM players ORDER BY is_user DESC, created_at ASC")
    suspend fun getAllPlayersOnce(): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayer(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Query("UPDATE players SET display_name = :displayName, avatar_id = :avatarId WHERE is_user = 1")
    suspend fun updateUserProfile(displayName: String, avatarId: Int)

    @Query("UPDATE players SET display_name = :displayName, avatar_id = :avatarId WHERE id = :playerId AND is_user = 0")
    suspend fun updateFriend(playerId: String, displayName: String, avatarId: Int): Int

    @Query("UPDATE players SET preferred_format = :format WHERE is_user = 1")
    suspend fun updatePreferredFormat(format: String)

    @Query("DELETE FROM players WHERE id = :playerId AND is_user = 0")
    suspend fun deleteFriendById(playerId: String): Int

    @Query("DELETE FROM players WHERE is_user = 1")
    suspend fun deleteUserPlayers()

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()
}
