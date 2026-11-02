package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SongItem
import com.example.ui.components.AudioVisualizerWaveform
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MusicStudioViewModel

@Composable
fun LibraryScreen(
    viewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allSongs by viewModel.allSongs.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val activeSong by viewModel.activePlayingSong.collectAsState()
    val amplitudes by viewModel.audioEngine.visualizerAmplitudes.collectAsState()

    var selectedTabIdx by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tüm Besteler", "AI Coverlar", "Video Projeleri")

    var expandedSongId by remember { mutableStateOf<String?>(null) }

    val filteredSongs = when (selectedTabIdx) {
        1 -> allSongs.filter { it.isCover }
        else -> allSongs
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Library Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STÜDYO KÜTÜPHANESİ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kaydettiğin tüm besteler, akor dizileri, AI coverlar ve video senaryoları.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Sub Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTabIdx,
                containerColor = StudioSurface,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIdx]),
                        color = NeonCyan
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIdx == index,
                        onClick = { selectedTabIdx = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTabIdx == index) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (selectedTabIdx == index) NeonCyan else TextSecondary
                            )
                        }
                    )
                }
            }
        }

        if (filteredSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kayıtlı şarkı bulunamadı",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "'Beste & Söz' veya 'Cover' sekmesinden hemen oluşturabilirsin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredSongs) { song ->
                val isThisPlaying = isPlaying && activeSong?.id == song.id
                val isExpanded = expandedSongId == song.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(
                            1.dp,
                            if (isThisPlaying) NeonCyan else StudioSurfaceVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            expandedSongId = if (isExpanded) null else song.id
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isThisPlaying) StudioSurfaceVariant else StudioCardBg
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isThisPlaying) {
                                        viewModel.audioEngine.stopTrack()
                                    } else {
                                        viewModel.playSongAudio(song)
                                    }
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (song.isCover) NeonMagenta else NeonViolet)
                                    .testTag("lib_play_btn_${song.id}")
                            ) {
                                Icon(
                                    imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Oynat / Durdur",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (song.isCover) {
                                        Text(
                                            text = "COVER",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NeonMagenta,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NeonMagenta.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${song.genre} • ${song.vocalToneName} • ${song.tempoBpm} BPM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Lyrics", "${song.title}\n\n${song.lyrics}")
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Sözler panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Kopyala",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.deleteSong(song) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Sil",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (isThisPlaying) {
                            Spacer(modifier = Modifier.height(10.dp))
                            AudioVisualizerWaveform(
                                amplitudes = amplitudes,
                                isPlaying = true
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioSurfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🎹 Akor: ${song.chordProgression}", style = MaterialTheme.typography.labelSmall, color = NeonAmber)
                                    Text("🎼 Ton: ${song.musicalKey}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioSurface.copy(alpha = 0.8f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = song.lyrics,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
