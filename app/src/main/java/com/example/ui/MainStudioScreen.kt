
package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MiniStudioPlayerBar
import com.example.ui.components.UserVoiceRecordingDialog
import com.example.ui.screens.ComposeScreen
import com.example.ui.screens.CoverVocalScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.VideoClipScreen
import com.example.ui.screens.VirtualArtistScreen
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.MusicStudioViewModel
import com.example.viewmodel.StudioTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudioScreen(
    viewModel: MusicStudioViewModel
) {
    val currentTab by viewModel.currentTab.collectAsState()

    // Gerçek MP3 oynatma durumu
    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val activeSong by viewModel.activePlayingSong.collectAsState()

    // Mini oynatıcı bilgileri
    val elapsedSec by viewModel.audioEngine.currentPlaybackTimeSec.collectAsState()
    val currentSection by viewModel.audioEngine.currentPlayingSection.collectAsState()
    val bpm by viewModel.audioEngine.currentBpm.collectAsState()

    Scaffold(
        containerColor = StudioBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(NeonViolet, NeonCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "AI MUSIC STUDIO",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = TextPrimary
                            )

                            Text(
                                text = "Beste • Vokal • Karakter • Klip",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }
                },
                actions = {
                    if (isPlaying) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(CircleShape)
                                .background(
                                    NeonViolet.copy(alpha = 0.2f)
                                )
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonMagenta)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "CANLI SES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = NeonMagenta
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioSurface
                )
            )
        },
        bottomBar = {
            Column {
                MiniStudioPlayerBar(
                    isPlaying = isPlaying,
                    currentSection = if (isPlaying) {
                        "Eroq Music One • Gerçek MP3"
                    } else {
                        currentSection
                    },
                    elapsedSec = elapsedSec,
                    bpm = bpm,
                    songTitle = activeSong?.title,
                    onTogglePlay = {
                        if (isPlaying) {
                            viewModel.pausePlayback()
                        } else {
                            activeSong?.let { song ->
                                viewModel.playSongAudio(song)
                            }
                        }
                    }
                )

                NavigationBar(
                    containerColor = StudioSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 10.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == StudioTab.COMPOSE,
                        onClick = {
                            viewModel.selectTab(StudioTab.COMPOSE)
                        },
                        icon = {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = "Beste"
                            )
                        },
                        label = {
                            Text("Beste & Söz", fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonViolet,
                            selectedTextColor = NeonViolet,
                            indicatorColor = StudioSurfaceVariant,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_compose")
                    )

                    NavigationBarItem(
                        selected = currentTab == StudioTab.COVER,
                        onClick = {
                            viewModel.selectTab(StudioTab.COVER)
                        },
                        icon = {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Cover"
                            )
                        },
                        label = {
                            Text("Cover & Vokal", fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonMagenta,
                            selectedTextColor = NeonMagenta,
                            indicatorColor = StudioSurfaceVariant,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_cover")
                    )

                    NavigationBarItem(
                        selected = currentTab == StudioTab.VIRTUAL_ARTIST,
                        onClick = {
                            viewModel.selectTab(StudioTab.VIRTUAL_ARTIST)
                        },
                        icon = {
                            Icon(
                                Icons.Default.Face,
                                contentDescription = "Karakter"
                            )
                        },
                        label = {
                            Text("Sanal Sanatçı", fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = StudioSurfaceVariant,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_artist")
                    )

                    NavigationBarItem(
                        selected = currentTab == StudioTab.VIDEO_CLIP,
                        onClick = {
                            viewModel.selectTab(StudioTab.VIDEO_CLIP)
                        },
                        icon = {
                            Icon(
                                Icons.Default.Movie,
                                contentDescription = "Video"
                            )
                        },
                        label = {
                            Text("Video Klip", fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonAmber,
                            selectedTextColor = NeonAmber,
                            indicatorColor = StudioSurfaceVariant,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_video")
                    )

                    NavigationBarItem(
                        selected = currentTab == StudioTab.LIBRARY,
                        onClick = {
                            viewModel.selectTab(StudioTab.LIBRARY)
                        },
                        icon = {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = "Kütüphane"
                            )
                        },
                        label = {
                            Text("Kütüphane", fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = StudioSurfaceVariant,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_library")
                    )
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                StudioTab.COMPOSE -> {
                    ComposeScreen(viewModel = viewModel)
                }

                StudioTab.COVER -> {
                    CoverVocalScreen(viewModel = viewModel)
                }

                StudioTab.VIRTUAL_ARTIST -> {
                    VirtualArtistScreen(viewModel = viewModel)
                }

                StudioTab.VIDEO_CLIP -> {
                    VideoClipScreen(viewModel = viewModel)
                }

                StudioTab.LIBRARY -> {
                    LibraryScreen(viewModel = viewModel)
                }
            }
        }
    }

    val showVoiceDialog by
        viewModel.showVoiceRecordingDialog.collectAsState()

    if (showVoiceDialog) {
        UserVoiceRecordingDialog(
            viewModel = viewModel,
            onDismiss = {
                viewModel.showVoiceRecordingDialog.value = false
            },
            onVoiceSelected = {
                viewModel.showVoiceRecordingDialog.value = false
            }
        )
    }
}
