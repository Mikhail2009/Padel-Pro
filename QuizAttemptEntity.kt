package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_attempts",
    indices = [Index(value = ["lesson_id"])]
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "lesson_id")
    val lessonId: String,
    @ColumnInfo(name = "attempted_at")
    val attemptedAt: Long,
    @ColumnInfo(name = "correct")
    val correct: Int,
    @ColumnInfo(name = "total")
    val total: Int
)
