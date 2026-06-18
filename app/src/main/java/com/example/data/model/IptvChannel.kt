package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_channels")
data class FavoriteChannel(
    @PrimaryKey val url: String,
    val name: String,
    val logoUrl: String?,
    val category: String?,
    val tvgId: String?,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_playlists")
data class CustomPlaylist(
    @PrimaryKey val url: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis()
)

data class IptvChannel(
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val category: String? = null,
    val tvgId: String? = null,
    val isFavorite: Boolean = false,
    val country: String? = null
)

data class PlaylistCategory(
    val name: String,
    val count: Int,
    val iconName: String
)
