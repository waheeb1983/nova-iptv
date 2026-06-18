package com.example.data.parser

import com.example.data.model.IptvChannel
import java.io.BufferedReader
import java.io.StringReader

object M3uParser {
    private val attributeRegex = """([a-zA-Z0-9_-]+)="([^"]*)"""".toRegex()

    fun parse(m3uContent: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        try {
            val reader = BufferedReader(StringReader(m3uContent))
            var line = reader.readLine()
            
            // Check for EXTM3U header
            if (line == null || !line.trim().startsWith("#EXTM3U")) {
                // Not a valid M3U file, but we can try to parse it anyway
            }
            
            var currentMediaInfo: MediaInfo? = null

            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    line = reader.readLine()
                    continue
                }

                if (trimmed.startsWith("#EXTINF:")) {
                    currentMediaInfo = parseExtInf(trimmed)
                } else if (!trimmed.startsWith("#") && trimmed.contains("://")) {
                    // This is a stream URL line
                    val mediaInfo = currentMediaInfo
                    val channel = if (mediaInfo != null) {
                        IptvChannel(
                            name = mediaInfo.displayName,
                            url = trimmed,
                            logoUrl = mediaInfo.logoUrl,
                            category = mediaInfo.category,
                            tvgId = mediaInfo.tvgId,
                            country = mediaInfo.country
                        )
                    } else {
                        // Bare URL, fall back to extracting name from URL
                        val fallbackName = trimmed.substringAfterLast("/").substringBefore("?")
                        IptvChannel(
                            name = fallbackName.ifEmpty { "Live Stream" },
                            url = trimmed
                        )
                    }
                    channels.add(channel)
                    currentMediaInfo = null // reset for next channel
                }
                
                line = reader.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return channels
    }

    private fun parseExtInf(line: String): MediaInfo {
        // Example: #EXTINF:-1 tvg-id="id" tvg-logo="url" group-title="News",Channel Name
        val extInfContent = line.substringAfter("#EXTINF:")
        val commaIndex = extInfContent.lastIndexOf(',')
        
        val attributesPart = if (commaIndex != -1) {
            extInfContent.substring(0, commaIndex)
        } else {
            extInfContent
        }
        
        val displayName = if (commaIndex != -1) {
            extInfContent.substring(commaIndex + 1).trim()
        } else {
            "Untitled Channel"
        }

        val attributes = mutableMapOf<String, String>()
        val matches = attributeRegex.findAll(attributesPart)
        for (match in matches) {
            val (key, value) = match.destructured
            attributes[key.lowercase()] = value
        }

        // Search common tag identifiers
        val tvgId = attributes["tvg-id"] ?: attributes["tvg-name"]
        val logoUrl = attributes["tvg-logo"] ?: attributes["logo"]
        val category = attributes["group-title"] ?: attributes["category"]
        val country = attributes["tvg-country"] ?: attributes["country"]

        return MediaInfo(
            displayName = displayName,
            tvgId = tvgId?.ifEmpty { null },
            logoUrl = logoUrl?.ifEmpty { null },
            category = category?.ifEmpty { null },
            country = country?.ifEmpty { null }
        )
    }

    private data class MediaInfo(
        val displayName: String,
        val tvgId: String?,
        val logoUrl: String?,
        val category: String?,
        val country: String?
    )
}
