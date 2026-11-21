package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "match_events",
    indices = [
        Index(value = ["match_id"]),
        Index(value = ["match_id", "seq"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["match_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MatchEventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "match_id")
    val matchId: String,
    @ColumnInfo(name = "seq")
    val seq: Int,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "team")
    val team: String?,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
