package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements_unlocked")
data class AchievementUnlockedEntity(
    @PrimaryKey
    @ColumnInfo(name = "achievement_id")
    val achievementId: String,
    @ColumnInfo(name = "unlocked_at")
    val unlockedAt: Long
)
