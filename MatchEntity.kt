package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "format")
    val format: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "deuce_mode")
    val deuceMode: String,
    @ColumnInfo(name = "best_of")
    val bestOf: Int = 3,
    @ColumnInfo(name = "first_server_team")
    val firstServerTeam: String,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "ended_at")
    val endedAt: Long?,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long?,
    @ColumnInfo(name = "winner_team")
    val winnerTeam: String?
)
