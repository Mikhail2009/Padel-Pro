package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_progress",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "total_xp")
    val totalXp: Int,
    @ColumnInfo(name = "streak_count")
    val streakCount: Int,
    @ColumnInfo(name = "last_learning_date")
    val lastLearningDate: String?
)
