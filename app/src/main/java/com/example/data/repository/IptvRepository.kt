package com.example.data.repository

import com.example.data.api.IptvNetworkDataSource
import com.example.data.database.FavoriteDao
import com.example.data.database.PlaylistDao
import com.example.data.model.CustomPlaylist
import com.example.data.model.FavoriteChannel
import com.example.data.model.IptvChannel
import com.example.data.parser.M3uParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IptvRepository(
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val networkDataSource: IptvNetworkDataSource
) {
    val favoriteChannels: Flow<List<FavoriteChannel>> = favoriteDao.getAllFavorites()
    val customPlaylists: Flow<List<CustomPlaylist>> = playlistDao.getAllPlaylists()

    private fun resolvePlaylistUrl(url: String): String {
        val trimmed = url.trim()
        
        // Let's resolve the user's specific request for the streams directory
        if (trimmed.equals("https://github.com/iptv-org/iptv/tree/master/streams", ignoreCase = true) ||
            trimmed.equals("https://github.com/iptv-org/iptv/tree/master", ignoreCase = true) ||
            trimmed.equals("https://github.com/iptv-org/iptv", ignoreCase = true) ||
            trimmed.startsWith("https://github.com/iptv-org/iptv/tree/master/streams/", ignoreCase = true)) {
            return "https://iptv-org.github.io/iptv/index.m3u"
        }

        // Generic GitHub URL corrector (blob to raw.githubusercontent.com)
        if (trimmed.contains("github.com", ignoreCase = true) && !trimmed.contains("raw.githubusercontent.com", ignoreCase = true)) {
            if (trimmed.endsWith(".m3u", ignoreCase = true)) {
                return trimmed
                    .replace("github.com", "raw.githubusercontent.com")
                    .replace("/blob/", "/")
            }
        }
        return trimmed
    }

    suspend fun getChannelsFromUrl(playlistUrl: String): List<IptvChannel> {
        val resolved = resolvePlaylistUrl(playlistUrl)
        val rawContent = networkDataSource.fetchPlaylist(resolved)
        return M3uParser.parse(rawContent)
    }

    suspend fun addFavorite(channel: IptvChannel) {
        favoriteDao.insertFavorite(
            FavoriteChannel(
                url = channel.url,
                name = channel.name,
                logoUrl = channel.logoUrl,
                category = channel.category,
                tvgId = channel.tvgId
            )
        )
    }

    suspend fun removeFavorite(url: String) {
        favoriteDao.deleteByUrl(url)
    }

    suspend fun isFavorite(url: String): Boolean {
        return favoriteDao.isFavorite(url)
    }

    suspend fun addCustomPlaylist(name: String, url: String) {
        val resolved = resolvePlaylistUrl(url)
        playlistDao.insertPlaylist(
            CustomPlaylist(name = name.trim(), url = resolved)
        )
    }

    suspend fun removeCustomPlaylist(url: String) {
        playlistDao.deleteByUrl(url)
    }
}
