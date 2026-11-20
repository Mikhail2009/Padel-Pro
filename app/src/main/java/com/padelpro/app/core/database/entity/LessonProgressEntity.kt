package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "lesson_id")
    val lessonId: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
    @ColumnInfo(name = "best_quiz_score")
    val bestQuizScore: Float?,
    @ColumnInfo(name = "xp_awarded")
    val xpAwarded: Int
)
