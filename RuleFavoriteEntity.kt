package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rule_favorites")
data class RuleFavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "rule_id")
    val ruleId: String
)
