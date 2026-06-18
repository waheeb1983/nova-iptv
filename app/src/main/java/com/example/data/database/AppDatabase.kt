package com.example.data.database

import android.content.Context
import androidx.room.*
import com.example.data.model.CustomPlaylist
import com.example.data.model.FavoriteChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_channels ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(channel: FavoriteChannel)

    @Delete
    suspend fun deleteFavorite(channel: FavoriteChannel)

    @Query("DELETE FROM favorite_channels WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("SELECT EXISTS(SELECT * FROM favorite_channels WHERE url = :url)")
    suspend fun isFavorite(url: String): Boolean
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM custom_playlists ORDER BY addedAt DESC")
    fun getAllPlaylists(): Flow<List<CustomPlaylist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: CustomPlaylist)

    @Delete
    suspend fun deletePlaylist(playlist: CustomPlaylist)

    @Query("DELETE FROM custom_playlists WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}

@Database(
    entities = [FavoriteChannel::class, CustomPlaylist::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nova_iptv_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
