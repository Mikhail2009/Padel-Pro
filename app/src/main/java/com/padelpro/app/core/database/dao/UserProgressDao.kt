package com.padelpro.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.padelpro.app.core.database.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress LIMIT 1")
    fun observeUserProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress LIMIT 1")
    suspend fun getUserProgressOnce(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProgress(progress: UserProgressEntity)

    @Query("UPDATE user_progress SET total_xp = total_xp + :xpDelta WHERE user_id = :userId")
    suspend fun addXp(userId: String, xpDelta: Int): Int

    @Query("DELETE FROM user_progress")
    suspend fun deleteAllProgress()
}
