package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.IptvNetworkDataSource
import com.example.data.database.AppDatabase
import com.example.data.repository.IptvRepository
import com.example.ui.components.VideoPlayerScreen
import com.example.ui.screens.IptvDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IptvViewModel
import com.example.ui.viewmodel.IptvViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to edge immersive displays
        enableEdgeToEdge()

        // Core data initializations
        val database = AppDatabase.getDatabase(this)
        val repository = IptvRepository(
            favoriteDao = database.favoriteDao(),
            playlistDao = database.playlistDao(),
            networkDataSource = IptvNetworkDataSource()
        )
        val factory = IptvViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[IptvViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IptvApp(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun IptvApp(
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val currentPlaying by viewModel.currentPlayingChannel.collectAsStateWithLifecycle()
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()

    if (currentPlaying != null) {
        BackHandler {
            viewModel.selectTab(1) // Return back to Live Feed tab
            viewModel.selectChannel(null) // Stop playing and close
        }
        VideoPlayerScreen(
            channel = currentPlaying!!,
            channelList = channels,
            onClose = {
                viewModel.selectTab(1) // Always land back on Live Feed tab
                viewModel.selectChannel(null)
            },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onSelectChannel = { viewModel.selectChannel(it) },
            onNextChannel = { viewModel.skipToNextChannel() },
            onPreviousChannel = { viewModel.skipToPreviousChannel() },
            onPlaybackError = { viewModel.reportBrokenChannel(it) },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        IptvDashboardScreen(
            viewModel = viewModel,
            onSelectChannel = { viewModel.selectChannel(it) },
            modifier = modifier
        )
    }
}

