package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CustomPlaylist
import com.example.data.model.FavoriteChannel
import com.example.data.model.IptvChannel
import com.example.data.repository.IptvRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

enum class ChannelRegion(val displayName: String, val flagEmoji: String) {
    ALL("All Regions", "🌍"),
    JORDAN("Jordan", "🇯🇴"),
    OMAN("Oman", "🇴🇲"),
    SAUDI("Saudi Arabia", "🇸🇦"),
    EGYPT("Egypt", "🇪🇬"),
    UAE("UAE", "🇦🇪"),
    PALESTINE("Palestine", "🇵🇸"),
    US("United States", "🇺🇸"),
    UK("United Kingdom", "🇬🇧"),
    CANADA("Canada", "🇨🇦");

    companion object {
        fun fromChannel(channel: IptvChannel): ChannelRegion {
            val countryCode = channel.country?.uppercase() ?: ""
            if (countryCode == "SA") return SAUDI
            if (countryCode == "JO") return JORDAN
            if (countryCode == "OM") return OMAN
            if (countryCode == "EG") return EGYPT
            if (countryCode == "AE") return UAE
            if (countryCode == "PS") return PALESTINE
            if (countryCode == "US") return US
            if (countryCode == "GB" || countryCode == "UK") return UK
            if (countryCode == "CA" || countryCode == "CAN") return CANADA

            val nameLower = channel.name.lowercase()
            val categoryLower = channel.category?.lowercase() ?: ""
            val urlLower = channel.url.lowercase()

            if (nameLower.contains("saudi") || nameLower.contains("ksa") || nameLower.contains("السعودية") || 
                nameLower.contains("الرياضية") || nameLower.contains("الإخبارية") || 
                categoryLower.contains("saudi") || categoryLower.contains("ksa") || 
                nameLower.contains(Regex("\\b(sa)\\b")) || nameLower.contains("[sa]") || nameLower.contains("(sa)")) {
                return SAUDI
            }

            if (nameLower.contains("jordan") || nameLower.contains("الأردن") || nameLower.contains("أردنية") ||
                nameLower.contains("رؤيا") || nameLower.contains("roya") || nameLower.contains("amman") ||
                categoryLower.contains("jordan") || categoryLower.contains("jo") ||
                nameLower.contains(Regex("\\b(jo)\\b")) || nameLower.contains("[jo]") || nameLower.contains("(jo)")) {
                return JORDAN
            }

            if (nameLower.contains("oman") || nameLower.contains("عمان") || nameLower.contains("سلطنة") ||
                categoryLower.contains("oman") || categoryLower.contains("om") ||
                nameLower.contains(Regex("\\b(om)\\b")) || nameLower.contains("[om]") || nameLower.contains("(om)")) {
                if (nameLower.contains("amman") || nameLower.contains("عمان tv")) {
                    return JORDAN
                }
                return OMAN
            }

            if (nameLower.contains("egypt") || nameLower.contains("مصر") || nameLower.contains("المصرية") ||
                categoryLower.contains("egypt") || categoryLower.contains("eg") || categoryLower.contains("egy") ||
                nameLower.contains(Regex("\\b(eg)\\b")) || nameLower.contains("[eg]") || nameLower.contains("(eg)")) {
                return EGYPT
            }

            if (nameLower.contains("uae") || nameLower.contains("dubai") || nameLower.contains("abu dhabi") || 
                nameLower.contains("abudhabi") || nameLower.contains("الإمارات") || nameLower.contains("شارجة") || 
                categoryLower.contains("uae") || categoryLower.contains("emirates") ||
                nameLower.contains(Regex("\\b(ae)\\b")) || nameLower.contains("[ae]") || nameLower.contains("(ae)")) {
                return UAE
            }

            if (nameLower.contains("palestine") || nameLower.contains("فلسطين") || nameLower.contains("القدس") || 
                nameLower.contains("قدس") || nameLower.contains("معا") || 
                categoryLower.contains("palestine") || categoryLower.contains("ps") ||
                nameLower.contains(Regex("\\b(ps)\\b")) || nameLower.contains("[ps]") || nameLower.contains("(ps)")) {
                return PALESTINE
            }

            if (nameLower.contains("usa") || nameLower.contains("united states") || 
                categoryLower.contains("us") || categoryLower.contains("usa") ||
                nameLower.contains(Regex("\\b(us)\\b")) || nameLower.contains("[us]") || nameLower.contains("(us)") ||
                urlLower.contains("/us.m3u")) {
                return US
            }

            if (nameLower.contains("uk") || nameLower.contains("united kingdom") || nameLower.contains("british") ||
                categoryLower.contains("uk") || categoryLower.contains("united kingdom") || categoryLower.contains("gb") ||
                nameLower.contains(Regex("\\b(uk)\\b")) || nameLower.contains("[uk]") || nameLower.contains("(uk)") ||
                nameLower.contains(Regex("\\b(gb)\\b")) || nameLower.contains("[gb]") || nameLower.contains("(gb)") ||
                urlLower.contains("/uk.m3u")) {
                return UK
            }

            if (nameLower.contains("canada") || 
                categoryLower.contains("ca") || categoryLower.contains("canada") ||
                nameLower.contains(Regex("\\b(ca)\\b")) || nameLower.contains("[ca]") || nameLower.contains("(ca)") ||
                urlLower.contains("/ca.m3u")) {
                return CANADA
            }

            return ALL
        }
    }
}

enum class ChannelSortOption(val displayName: String) {
    DEFAULT("Default Order"),
    NAME_ASC("Name (A-Z)"),
    REGION_ASC("Group by Region"),
    CATEGORY_ASC("Group by Category")
}

sealed interface PlaylistLoadState {
    object Idle : PlaylistLoadState
    object Loading : PlaylistLoadState
    data class Success(val channels: List<IptvChannel>) : PlaylistLoadState
    data class Error(val message: String) : PlaylistLoadState
}

sealed interface ScanState {
    object Idle : ScanState
    data class Scanning(val current: Int, val total: Int, val lastCheckedName: String) : ScanState
    data class Completed(val workingCount: Int, val brokenCount: Int) : ScanState
}

data class PresetPlaylist(
    val name: String,
    val url: String,
    val description: String,
    val category: String // "Country" or "Category"
)

class IptvViewModel(private val repository: IptvRepository) : ViewModel() {

    val presets = listOf(
        PresetPlaylist(
            name = "Sports Arena",
            url = "https://iptv-org.github.io/iptv/categories/sports.m3u",
            description = "Sports leagues, races, athletic streams",
            category = "Sports"
        ),
        PresetPlaylist(
            name = "All Channels",
            url = "https://iptv-org.github.io/iptv/index.m3u",
            description = "Gigantic combined playlist of all live channels around the world",
            category = "Global"
        ),
        PresetPlaylist(
            name = "Saudi Arabia TV",
            url = "https://iptv-org.github.io/iptv/countries/sa.m3u",
            description = "Live channels broadcasted from Saudi Arabia (KSA)",
            category = "Country"
        ),
        PresetPlaylist(
            name = "Jordan TV Streams",
            url = "https://iptv-org.github.io/iptv/countries/jo.m3u",
            description = "Live channels broadcasted from Jordan",
            category = "Country"
        ),
        PresetPlaylist(
            name = "Oman TV Streams",
            url = "https://iptv-org.github.io/iptv/countries/om.m3u",
            description = "Live channels broadcasted from Oman",
            category = "Country"
        ),
        PresetPlaylist(
            name = "Egypt TV Streams",
            url = "https://iptv-org.github.io/iptv/countries/eg.m3u",
            description = "Live channels broadcasted from Egypt",
            category = "Country"
        ),
        PresetPlaylist(
            name = "UAE TV Streams",
            url = "https://iptv-org.github.io/iptv/countries/ae.m3u",
            description = "Live channels broadcasted from UAE",
            category = "Country"
        ),
        PresetPlaylist(
            name = "Palestine TV Streams",
            url = "https://iptv-org.github.io/iptv/countries/ps.m3u",
            description = "Live channels broadcasted from Palestine",
            category = "Country"
        ),
        PresetPlaylist(
            name = "US TV Streams",
            url = "https://iptv-org.github.io/iptv/countries/us.m3u",
            description = "Live channels broadcasted from the United States",
            category = "Country"
        ),
        PresetPlaylist(
            name = "UK TV Streams",
            url = "https://iptv-org.github.io/iptv/countries/uk.m3u",
            description = "Live channels broadcasted from the United Kingdom",
            category = "Country"
        ),
        PresetPlaylist(
            name = "Canada TV Streams",
            url = "https://iptv-org.github.io/iptv/countries/ca.m3u",
            description = "Live channels broadcasted from Canada",
            category = "Country"
        ),
        PresetPlaylist(
            name = "Live News Hub",
            url = "https://iptv-org.github.io/iptv/categories/news.m3u",
            description = "Global live news networks and weather reports",
            category = "Category"
        ),
        PresetPlaylist(
            name = "Action & Movies",
            url = "https://iptv-org.github.io/iptv/categories/movies.m3u",
            description = "Movie loops, cinema channels, indie streams",
            category = "Category"
        ),
        PresetPlaylist(
            name = "Music Station",
            url = "https://iptv-org.github.io/iptv/categories/music.m3u",
            description = "Music videos, festivals, non-stop loops",
            category = "Category"
        ),
        PresetPlaylist(
            name = "Kids & Cartoons",
            url = "https://iptv-org.github.io/iptv/categories/kids.m3u",
            description = "Safe and entertaining kids shows and animation",
            category = "Category"
        ),
        PresetPlaylist(
            name = "Entertainment",
            url = "https://iptv-org.github.io/iptv/categories/entertainment.m3u",
            description = "Broad variety programming, comedy, and series",
            category = "Category"
        ),
        PresetPlaylist(
            name = "Documentaries",
            url = "https://iptv-org.github.io/iptv/categories/documentary.m3u",
            description = "Science, travel, history, and nature channels",
            category = "Category"
        )
    )

    // Flow of custom added playlists from Room Database
    val customPlaylists = repository.customPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Flow of favorite channel entities from DB
    val dbFavorites = repository.favoriteChannels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current selected playlist URL and Name
    private val _currentPlaylistUrl = MutableStateFlow(presets.first().url)
    val currentPlaylistUrl: StateFlow<String> = _currentPlaylistUrl.asStateFlow()

    private val _currentPlaylistName = MutableStateFlow(presets.first().name)
    val currentPlaylistName: StateFlow<String> = _currentPlaylistName.asStateFlow()

    // Remote channels state representation
    private val _playlistState = MutableStateFlow<PlaylistLoadState>(PlaylistLoadState.Idle)
    val playlistState: StateFlow<PlaylistLoadState> = _playlistState.asStateFlow()

    // Raw loaded channels list for formatting
    private val _rawChannels = MutableStateFlow<List<IptvChannel>>(emptyList())

    // Search text state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Non-working/broken channels list
    private val _brokenChannels = MutableStateFlow<List<IptvChannel>>(emptyList())
    val brokenChannels: StateFlow<List<IptvChannel>> = _brokenChannels.asStateFlow()

    // Blocklist of permanently deleted/hidden channel URLs
    private val _blockedChannelUrls = MutableStateFlow<Set<String>>(emptySet())
    val blockedChannelUrls: StateFlow<Set<String>> = _blockedChannelUrls.asStateFlow()

    // Playlist Channel Scanner state representation
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    private var scanJob: kotlinx.coroutines.Job? = null

    // Continuous error count safeguard
    private var continuousErrorCount = 0

    // Selected group-title category filter chip
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Selected Region Filter
    private val _selectedRegionFilter = MutableStateFlow(ChannelRegion.ALL)
    val selectedRegionFilter: StateFlow<ChannelRegion> = _selectedRegionFilter.asStateFlow()

    // Current Sort Option
    private val _currentSortOption = MutableStateFlow(ChannelSortOption.DEFAULT)
    val currentSortOption: StateFlow<ChannelSortOption> = _currentSortOption.asStateFlow()

    private data class FilterState(
        val query: String,
        val category: String?,
        val region: ChannelRegion,
        val sortOption: ChannelSortOption
    )

    private val _filterState: Flow<FilterState> = combine(
        _searchQuery,
        _selectedCategoryFilter,
        _selectedRegionFilter,
        _currentSortOption
    ) { query, category, region, sortOption ->
        FilterState(query, category, region, sortOption)
    }

    // Final visible filtered channels combined with favorites lookup, region, sorting, and broken/blocked filters
    val filteredChannels: StateFlow<List<IptvChannel>> = combine(
        _rawChannels,
        _filterState,
        dbFavorites,
        _brokenChannels,
        _blockedChannelUrls
    ) { channels, filters, faves, broken, blocked ->
        val faveUrls = faves.map { it.url }.toSet()
        val brokenUrls = broken.map { it.url }.toSet()
        
        val filtered = channels.map { channel ->
            channel.copy(isFavorite = faveUrls.contains(channel.url))
        }.filter { channel ->
            val isBroken = brokenUrls.contains(channel.url)
            val isBlocked = blocked.contains(channel.url)
            val matchesSearch = filters.query.isEmpty() || channel.name.contains(filters.query, ignoreCase = true)
            val matchesCategory = filters.category == null || channel.category == filters.category
            val matchesRegion = filters.region == ChannelRegion.ALL || ChannelRegion.fromChannel(channel) == filters.region
            !isBroken && !isBlocked && matchesSearch && matchesCategory && matchesRegion
        }

        when (filters.sortOption) {
            ChannelSortOption.DEFAULT -> filtered
            ChannelSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            ChannelSortOption.REGION_ASC -> filtered.sortedWith(
                compareBy<IptvChannel> { ChannelRegion.fromChannel(it).displayName }
                    .thenBy { it.name.lowercase() }
            )
            ChannelSortOption.CATEGORY_ASC -> filtered.sortedWith(
                compareBy<IptvChannel> { it.category?.lowercase() ?: "zzzz" }
                    .thenBy { it.name.lowercase() }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic list of regions present in loaded channels
    val availableRegions: StateFlow<List<ChannelRegion>> = _rawChannels.map { channels ->
        val regions = mutableSetOf<ChannelRegion>()
        regions.add(ChannelRegion.ALL)
        
        channels.forEach { channel ->
            val region = ChannelRegion.fromChannel(channel)
            if (region != ChannelRegion.ALL) {
                regions.add(region)
            }
        }
        regions.toList().sortedBy { it.ordinal }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(ChannelRegion.ALL)
    )

    // Horizontal chips of all category tags available in current playlist
    val availableCategories: StateFlow<List<String>> = _rawChannels.map { channels ->
        channels.mapNotNull { it.category }
            .distinct()
            .sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current playing channel
    private val _currentPlayingChannel = MutableStateFlow<IptvChannel?>(null)
    val currentPlayingChannel: StateFlow<IptvChannel?> = _currentPlayingChannel.asStateFlow()

    // Current selected tab index in dashboard
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    init {
        // Automatically load initial preset playlist
        loadPlaylist(presets.first().url, presets.first().name)
    }

    fun loadPlaylist(url: String, name: String) {
        _currentPlaylistUrl.value = url
        _currentPlaylistName.value = name
        _selectedCategoryFilter.value = null
        _selectedRegionFilter.value = ChannelRegion.ALL
        _currentSortOption.value = ChannelSortOption.DEFAULT
        _searchQuery.value = ""

        viewModelScope.launch {
            _playlistState.value = PlaylistLoadState.Loading
            try {
                val channels = repository.getChannelsFromUrl(url)
                _rawChannels.value = channels
                if (channels.isEmpty()) {
                    _playlistState.value = PlaylistLoadState.Error("Contains no active streamline URLs")
                } else {
                    _playlistState.value = PlaylistLoadState.Success(channels)
                }
            } catch (e: Exception) {
                _playlistState.value = PlaylistLoadState.Error(e.localizedMessage ?: "Network connection error")
            }
        }
    }

    fun selectChannel(channel: IptvChannel?) {
        _currentPlayingChannel.value = channel
        continuousErrorCount = 0 // Reset error safeguard on select
    }

    fun reportBrokenChannel(channel: IptvChannel) {
        val currentList = _brokenChannels.value
        if (!currentList.any { it.url == channel.url }) {
            _brokenChannels.value = currentList + channel
        }
        
        continuousErrorCount++
        if (continuousErrorCount > 10) {
            // Prevent infinite auto-skipping loop (e.g. if completely offline)
            _currentPlayingChannel.value = null
            continuousErrorCount = 0
            return
        }

        // Find next channel before/during exclusion
        val activeChannels = filteredChannels.value.filter { it.url != channel.url }
        if (activeChannels.isNotEmpty()) {
            val currentIndex = filteredChannels.value.indexOfFirst { it.url == channel.url }
            val nextIndex = if (currentIndex != -1 && currentIndex < filteredChannels.value.size - 1) {
                if (currentIndex < activeChannels.size) currentIndex else 0
            } else {
                0
            }
            _currentPlayingChannel.value = activeChannels[nextIndex]
        } else {
            _currentPlayingChannel.value = null // No remaining working channels
        }
    }

    fun deleteBrokenChannel(channel: IptvChannel) {
        // Discard from broken links state
        _brokenChannels.value = _brokenChannels.value.filter { it.url != channel.url }
        // Store in permanent session blocklist so it remains gone
        _blockedChannelUrls.value = _blockedChannelUrls.value + channel.url
    }

    fun deleteAllBrokenChannels() {
        val urlsToBlock = _brokenChannels.value.map { it.url }
        _blockedChannelUrls.value = _blockedChannelUrls.value + urlsToBlock
        _brokenChannels.value = emptyList()
    }

    fun restoreBrokenChannel(channel: IptvChannel) {
        _brokenChannels.value = _brokenChannels.value.filter { it.url != channel.url }
        _blockedChannelUrls.value = _blockedChannelUrls.value - channel.url
    }

    fun checkAndHealChannel(channel: IptvChannel, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isWorking = checkStreamUrl(channel.url)
            if (isWorking) {
                restoreBrokenChannel(channel)
            }
            onResult(isWorking)
        }
    }

    private suspend fun checkStreamUrl(urlString: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.useCaches = false
            val responseCode = connection.responseCode
            val isValid = responseCode in 200..399
            connection.disconnect()
            isValid
        } catch (e: Exception) {
            false
        }
    }

    fun startChannelScan() {
        if (scanJob?.isActive == true) return
        
        scanJob = viewModelScope.launch {
            val allChannels = _rawChannels.value
            if (allChannels.isEmpty()) {
                _scanState.value = ScanState.Completed(0, 0)
                return@launch
            }

            _scanState.value = ScanState.Scanning(0, allChannels.size, "Starting scan...")
            var workingCount = 0
            var brokenCount = 0
            val currentBrokenList = _brokenChannels.value.toMutableList()

            val chunkSize = 5
            val channelsWithIndices = allChannels.withIndex().toList()
            
            for (chunk in channelsWithIndices.chunked(chunkSize)) {
                val results = kotlinx.coroutines.coroutineScope {
                    chunk.map { iv ->
                        async {
                            val isWorking = checkStreamUrl(iv.value.url)
                            Triple(iv.index, iv.value, isWorking)
                        }
                    }.let { deferredList ->
                        kotlinx.coroutines.awaitAll(*deferredList.toTypedArray())
                    }
                }
                
                results.forEach { result ->
                    val index = result.first
                    val channel = result.second
                    val isWorking = result.third
                    
                    if (isWorking) {
                        workingCount++
                        if (currentBrokenList.any { it.url == channel.url }) {
                            currentBrokenList.removeAll { it.url == channel.url }
                            _brokenChannels.value = currentBrokenList.toList()
                        }
                    } else {
                        brokenCount++
                        if (!currentBrokenList.any { it.url == channel.url }) {
                            currentBrokenList.add(channel)
                            _brokenChannels.value = currentBrokenList.toList()
                        }
                    }
                    _scanState.value = ScanState.Scanning(index + 1, allChannels.size, channel.name)
                }
            }

            _scanState.value = ScanState.Completed(workingCount, brokenCount)
        }
    }

    fun stopChannelScan() {
        scanJob?.cancel()
        scanJob = null
        _scanState.value = ScanState.Idle
    }

    fun dismissScanResult() {
        _scanState.value = ScanState.Idle
    }

    fun skipToNextChannel() {
        val activeChannels = filteredChannels.value
        val playing = _currentPlayingChannel.value ?: return
        if (activeChannels.isEmpty()) return
        
        val currentIndex = activeChannels.indexOfFirst { it.url == playing.url }
        val nextIndex = if (currentIndex != -1 && currentIndex < activeChannels.lastIndex) {
            currentIndex + 1
        } else {
            0
        }
        _currentPlayingChannel.value = activeChannels[nextIndex]
        continuousErrorCount = 0
    }

    fun skipToPreviousChannel() {
        val activeChannels = filteredChannels.value
        val playing = _currentPlayingChannel.value ?: return
        if (activeChannels.isEmpty()) return
        
        val currentIndex = activeChannels.indexOfFirst { it.url == playing.url }
        val prevIndex = if (currentIndex != -1 && currentIndex > 0) {
            currentIndex - 1
        } else {
            activeChannels.lastIndex
        }
        _currentPlayingChannel.value = activeChannels[prevIndex]
        continuousErrorCount = 0
    }

    fun toggleFavorite(channel: IptvChannel) {
        viewModelScope.launch {
            val faveList = dbFavorites.value
            val isCurrentlyFave = faveList.any { it.url == channel.url }

            if (isCurrentlyFave) {
                repository.removeFavorite(channel.url)
            } else {
                repository.addFavorite(channel)
            }
            
            // Also update rawChannels flow if needed
            _rawChannels.value = _rawChannels.value.map {
                if (it.url == channel.url) it.copy(isFavorite = !isCurrentlyFave) else it
            }
            // Update active playing channel favorite indicator if playing
            val playing = _currentPlayingChannel.value
            if (playing != null && playing.url == channel.url) {
                _currentPlayingChannel.value = playing.copy(isFavorite = !isCurrentlyFave)
            }
        }
    }

    fun searchChannels(query: String) {
        _searchQuery.value = query
    }

    fun filterByCategory(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun filterByRegion(region: ChannelRegion) {
        _selectedRegionFilter.value = region
    }

    fun setSortOption(sortOption: ChannelSortOption) {
        _currentSortOption.value = sortOption
    }

    fun addCustomPlaylist(name: String, url: String) {
        viewModelScope.launch {
            repository.addCustomPlaylist(name, url)
        }
    }

    fun removeCustomPlaylist(url: String) {
        viewModelScope.launch {
            repository.removeCustomPlaylist(url)
        }
    }
}

class IptvViewModelFactory(private val repository: IptvRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IptvViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IptvViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
