package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "match_participants",
    primaryKeys = ["match_id", "player_id"],
    indices = [Index(value = ["player_id"])],
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["match_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class MatchParticipantEntity(
    @ColumnInfo(name = "match_id")
    val matchId: String,
    @ColumnInfo(name = "player_id")
    val playerId: String,
    @ColumnInfo(name = "team")
    val team: String,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "is_user")
    val isUser: Int
)
