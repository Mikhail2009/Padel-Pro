package com.padelpro.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.padelpro.app.core.database.dao.AcademyDao
import com.padelpro.app.core.database.dao.MatchDao
import com.padelpro.app.core.database.dao.PlayerDao
import com.padelpro.app.core.database.dao.UserProgressDao
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

@Database(
    entities = [
        PlayerEntity::class,
        UserProgressEntity::class,
        MatchEntity::class,
        MatchParticipantEntity::class,
        MatchEventEntity::class,
        SetResultEntity::class,
        LessonProgressEntity::class,
        QuizAttemptEntity::class,
        AchievementUnlockedEntity::class,
        RuleFavoriteEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PadelProDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun matchDao(): MatchDao
    abstract fun academyDao(): AcademyDao
}
