package com.padelpro.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.padelpro.app.core.database.entity.AchievementUnlockedEntity
import com.padelpro.app.core.database.entity.LessonProgressEntity
import com.padelpro.app.core.database.entity.QuizAttemptEntity
import com.padelpro.app.core.database.entity.RuleFavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademyDao {
    @Query("SELECT * FROM lesson_progress")
    fun observeLessonProgress(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress")
    suspend fun getAllLessonProgressOnce(): List<LessonProgressEntity>

    @Query("SELECT * FROM lesson_progress WHERE lesson_id = :lessonId LIMIT 1")
    suspend fun getLessonProgressOnce(lessonId: String): LessonProgressEntity?

    @Query("SELECT * FROM quiz_attempts")
    fun observeQuizAttempts(): Flow<List<QuizAttemptEntity>>

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE lesson_id = :lessonId")
    suspend fun getQuizAttemptCountForLesson(lessonId: String): Int

    @Query("SELECT * FROM quiz_attempts WHERE lesson_id = :lessonId ORDER BY attempted_at DESC LIMIT 1")
    suspend fun getLatestQuizAttemptForLesson(lessonId: String): QuizAttemptEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM quiz_attempts WHERE correct = total AND total > 0)")
    suspend fun hasPerfectQuizAttemptOnce(): Boolean

    @Query("SELECT * FROM achievements_unlocked")
    fun observeUnlockedAchievements(): Flow<List<AchievementUnlockedEntity>>

    @Query("SELECT * FROM achievements_unlocked")
    suspend fun getUnlockedAchievementsOnce(): List<AchievementUnlockedEntity>

    @Query("SELECT * FROM rule_favorites")
    fun observeRuleFavorites(): Flow<List<RuleFavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM rule_favorites WHERE rule_id = :ruleId)")
    suspend fun isRuleFavorite(ruleId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(progress: LessonProgressEntity)

    @Insert
    suspend fun insertQuizAttempt(attempt: QuizAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockedAchievement(achievement: AchievementUnlockedEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRuleFavorite(favorite: RuleFavoriteEntity)

    @Query("DELETE FROM rule_favorites WHERE rule_id = :ruleId")
    suspend fun deleteRuleFavorite(ruleId: String): Int

    @Query("DELETE FROM lesson_progress")
    suspend fun deleteAllLessonProgress()

    @Query("DELETE FROM quiz_attempts")
    suspend fun deleteAllQuizAttempts()

    @Query("DELETE FROM achievements_unlocked")
    suspend fun deleteAllAchievements()

    @Query("DELETE FROM rule_favorites")
    suspend fun deleteAllRuleFavorites()
}
