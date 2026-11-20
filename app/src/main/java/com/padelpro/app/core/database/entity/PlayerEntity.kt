package com.padelpro.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "avatar_id")
    val avatarId: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "preferred_format")
    val preferredFormat: String?,
    @ColumnInfo(name = "is_user")
    val isUser: Int
)
