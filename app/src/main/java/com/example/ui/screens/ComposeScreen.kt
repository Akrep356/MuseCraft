
package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AudioVisualizerWaveform
import com.example.ui.components.GenreSelectorChips
import com.example.ui.components.VocalToneSelectorCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MusicStudioViewModel

@Composable
fun ComposeScreen(
    viewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val themeText by viewModel.composeTheme.collectAsState()
    val selectedGenre by viewModel.composeGenre.collectAsState()
    val moodText by viewModel.composeMood.collectAsState()
    val selectedVocal by viewModel.composeVocalTone.collectAsState()
    val selectedKey by viewModel.composeKey.collectAsState()
    val bpm by viewModel.composeBpm.collectAsState()

    val isGenerating by viewModel.isGeneratingSong.collectAsState()
    val generatedResult by viewModel.generatedSong.collectAsState()
    val generatedAudioUrl by viewModel.generatedSongAudioUrl.collectAsState()
    val generationMessage by viewModel.generationMessage.collectAsState()

    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val isPreparing by viewModel.isAudioPreparing.collectAsState()
    val playbackError by viewModel.audioPlaybackError.collectAsState()
    val amplitudes by viewModel.visualizerAmplitudes.collectAsState()

    val downloadingSongId by viewModel.downloadingSongId.collectAsState()
    val downloadMessage by viewModel.downloadMessage.collectAsState()

    // ID alanına ihtiyaç duymadan indirme durumunu kontrol et.
    val isDownloadingGeneratedSong = downloadingSongId != null

    val quickThemes = listOf(
        "Gece sürüşü ve neon ışıklar",
        "Sonsuz aşk ve melankoli",
        "Cyberpunk sokakları ve zafer",
        "Anadolu ezgileri ve hasret",
        "Yaz festivali ve dans"
    )

    val musicalKeys = listOf(
        "A Minör",
        "E Minör",
        "D Minör",
        "C Majör",
        "G Majör",
        "F# Minör"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 24.dp,
                            bottomEnd = 24.dp
                        )
                    )
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.img_studio_banner
                    ),
                    contentDescription = "Studio Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xDD0C0A17)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "AI BESTE & ŞARKI SÖZÜ STÜDYOSU",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "Melodi, Akor ve Sözleri Anında Üret",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                }
            }
        }

        // SONG THEME
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StudioCardBg
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Şarkı Teması / Konusu",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = themeText,
                        onValueChange = {
                            viewModel.composeTheme.value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compose_theme_input"),
                        placeholder = {
                            Text(
                                "Örn: Geceleyin yağmurlu İstanbul sokaklarında kaybolmak...",
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = StudioSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Hızlı İlham Temaları:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickThemes) { quick ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioSurfaceVariant)
                                    .clickable {
                                        viewModel.composeTheme.value = quick
                                    }
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                            ) {
                                Text(
                                    text = quick,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // GENRE
        item {
            Column {
                Text(
                    text = "Müzik Stili & Türü",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    )
                )

                GenreSelectorChips(
                    selectedGenre = selectedGenre,
                    onSelectGenre = {
                        viewModel.composeGenre.value = it
                        viewModel.composeBpm.value =
                            it.bpmRange.first +
                                (it.bpmRange.last - it.bpmRange.first) / 2
                    }
                )
            }
        }

        // VOCAL
        item {
            VocalToneSelectorCard(
                selectedTone = selectedVocal,
                onSelectTone = {
                    viewModel.composeVocalTone.value = it
                },
                onRecordVoiceClick = {
                    viewModel.showVoiceRecordingDialog.value = true
                }
            )
        }

        // USER VOICE
        item {
            val hasRecordedUserVoice by
                viewModel.voiceRecorder.hasRecording.collectAsState()

            val isCustomVoiceSelected =
                selectedVocal.id == "user_own_voice"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(NeonMagenta, NeonCyan)
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        viewModel.showVoiceRecordingDialog.value = true
                    }
                    .testTag("user_voice_banner_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StudioSurfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NeonMagenta, NeonViolet)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kendi Sesinle Söylet",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            if (isCustomVoiceSelected) {
                                Text(
                                    text = "AKTİF",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = NeonGreen,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            NeonGreen.copy(alpha = 0.2f)
                                        )
                                        .padding(
                                            horizontal = 6.dp,
                                            vertical = 1.dp
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (hasRecordedUserVoice) {
                                "Kaydedilmiş ses örneğiniz hazır! Dokunarak şarkıyı kendi sesinizle söyletin."
                            } else {
                                "Kendi sesini mikrofondan kaydet, şarkıyı kendi sesinle seslendir."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.showVoiceRecordingDialog.value = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonMagenta
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        modifier = Modifier.testTag("user_voice_record_btn")
                    ) {
                        Text(
                            text = if (hasRecordedUserVoice) {
                                "Düzenle"
                            } else {
                                "Kaydet"
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // BPM AND KEY
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StudioCardBg
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aranje & Tempo Ayarları",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )

                        Text(
                            text = "$bpm BPM",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = {
                            viewModel.composeBpm.value = it.toInt()
                            viewModel.audioEngine.updateBpm(it.toInt())
                        },
                        valueRange = 60f..180f,
                        steps = 24,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonAmber,
                            activeTrackColor = NeonAmber,
                            inactiveTrackColor = StudioSurfaceVariant
                        ),
                        modifier = Modifier.testTag("bpm_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Müzikal Ton (Key):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(musicalKeys) { key ->
                            val isKeySelected = key == selectedKey

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isKeySelected) {
                                            NeonViolet
                                        } else {
                                            StudioSurfaceVariant
                                        }
                                    )
                                    .clickable {
                                        viewModel.composeKey.value = key
                                    }
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 6.dp
                                    )
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isKeySelected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    ),
                                    color = if (isKeySelected) {
                                        Color.White
                                    } else {
                                        TextSecondary
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // GENERATE SONG
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.generateSong()
                    },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_song_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        NeonViolet,
                                        NeonMagenta,
                                        NeonCyan
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGenerating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "Şarkı hazırlanıyor...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "GERÇEK ŞARKI ÜRET",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // GENERATION MESSAGE
        if (generationMessage.isNotBlank()) {
            item {
                Text(
                    text = generationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // GENERATED SONG
        if (generatedResult != null) {
            val song = generatedResult!!

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(NeonViolet, NeonCyan)
                            ),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StudioCardBg
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🎵 ${song.title}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${selectedGenre.displayName} • ${selectedVocal.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // REAL MP3 PLAY BUTTON
                        Button(
                            onClick = {
                                if (isPlaying) {
                                    viewModel.pausePlayback()
                                } else if (!isPreparing) {
                                    viewModel.playGeneratedSong()
                                }
                            },
                            enabled = generatedAudioUrl != null &&
                                !isPreparing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("play_real_mp3_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonViolet,
                                disabledContainerColor =
                                    NeonViolet.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isPreparing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text("MP3 hazırlanıyor...")
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) {
                                        Icons.Default.Stop
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    if (isPlaying) {
                                        "Duraklat"
                                    } else {
                                        "Gerçek Şarkıyı Dinle"
                                    }
                                )
                            }
                        }

                        if (generatedAudioUrl == null) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Gerçek MP3 bağlantısı henüz hazır değil.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        playbackError
                            ?.takeIf { it.isNotBlank() }
                            ?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonMagenta
                                )
                            }

                        Spacer(modifier = Modifier.height(10.dp))

                        AudioVisualizerWaveform(
                            amplitudes = amplitudes,
                            isPlaying = isPlaying
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioSurfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        "🎹 Akor Dizisi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )

                                    Text(
                                        song.chordProgression,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = NeonAmber
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioSurfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        "🎙️ Vokal Karakteri",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )

                                    Text(
                                        selectedVocal.tag,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = NeonMagenta
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SAVE SONG
                        Button(
                            onClick = {
                                viewModel.saveGeneratedSong()
                            },
                            enabled = generatedAudioUrl != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_song_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen.copy(alpha = 0.2f),
                                contentColor = NeonGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text("Şarkıyı Kütüphaneye Kaydet")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // DOWNLOAD MP3 TO PHONE
                        Button(
                            onClick = {
                                viewModel.downloadGeneratedSong()
                            },
                            enabled = generatedAudioUrl != null &&
                                !isDownloadingGeneratedSong,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_generated_mp3_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan.copy(alpha = 0.2f),
                                contentColor = NeonCyan,
                                disabledContainerColor =
                                    NeonCyan.copy(alpha = 0.08f),
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isDownloadingGeneratedSong) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = NeonCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                if (isDownloadingGeneratedSong) {
                                    "MP3 TELEFONA İNDİRİLİYOR..."
                                } else {
                                    "MP3'Ü TELEFONA İNDİR"
                                }
                            )
                        }

                        if (!downloadMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = downloadMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Şarkı Sözleri",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    StudioSurfaceVariant.copy(alpha = 0.5f)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = song.lyrics,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 22.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
