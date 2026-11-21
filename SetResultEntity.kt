package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "set_results",
    primaryKeys = ["match_id", "set_index"],
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["match_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SetResultEntity(
    @ColumnInfo(name = "match_id")
    val matchId: String,
    @ColumnInfo(name = "set_index")
    val setIndex: Int,
    @ColumnInfo(name = "games_a")
    val gamesA: Int,
    @ColumnInfo(name = "games_b")
    val gamesB: Int,
    @ColumnInfo(name = "tb_a")
    val tbA: Int?,
    @ColumnInfo(name = "tb_b")
    val tbB: Int?
)
