
package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SongItem
import com.example.ui.components.AudioVisualizerWaveform
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MusicStudioViewModel

@Composable
fun VideoClipScreen(
    viewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val selectedSong by viewModel.videoSelectedSong.collectAsState()
    val visualStyle by viewModel.videoVisualStyle.collectAsState()
    val isGenerating by viewModel.isGeneratingVideo.collectAsState()
    val generatedStoryboard by viewModel.generatedStoryboard.collectAsState()

    // ViewModel mesajlarını ekranda göstermek için.
    val generationMessage by viewModel.generationMessage.collectAsState()

    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val amplitudes by viewModel.audioEngine.visualizerAmplitudes.collectAsState()
    val currentSection by viewModel.audioEngine.currentPlayingSection.collectAsState()

    val videoStyles = listOf(
        "Cyberpunk Neon & Hologram",
        "Sinematik 35mm Film",
        "3D Anime Sahne Şovu",
        "Retro VHS 80s Synthwave",
        "Fütüristik Stadyum Konseri",
        "Sürrealist Sanat & Görselleştirici"
    )

    val infiniteTransition =
        rememberInfiniteTransition(label = "video_zoom")

    val stageZoom by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.06f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // VIDEO PREVIEW
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(
                                NeonCyan,
                                NeonViolet,
                                NeonMagenta
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    val avatarRes =
                        if (selectedSong?.vocalGender == "MALE") {
                            R.drawable.img_virtual_male_artist
                        } else {
                            R.drawable.img_virtual_female_artist
                        }

                    Image(
                        painter = painterResource(id = avatarRes),
                        contentDescription = "Video Stage",
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(stageZoom),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x770C0A17),
                                        Color(0x33000000),
                                        Color(0xEE0C0A17)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlaying) Color.Red
                                        else TextMuted
                                    )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = if (isPlaying) {
                                    "CANLI KLİP OYNATILIYOR"
                                } else {
                                    "KLİP ÖNİZLEME"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isPlaying) {
                                    Color.Red
                                } else {
                                    TextSecondary
                                }
                            )
                        }

                        Text(
                            text = visualStyle,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 2.dp
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NeonCyan, NeonMagenta)
                                )
                            )
                            .align(Alignment.Center)
                            .clickable {
                                if (selectedSong != null) {
                                    viewModel.playSongAudio(selectedSong!!)
                                } else if (allSongs.isNotEmpty()) {
                                    viewModel.playSongAudio(allSongs.first())
                                } else {
                                    viewModel.togglePlayback()
                                }
                            }
                            .testTag("video_play_toggle_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) {
                                Icons.Default.Stop
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = "Oynat / Durdur",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isPlaying) {
                                "🎤 \"$currentSection\""
                            } else {
                                "🎵 Şarkı ve klip seçip oynatın"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonAmber,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        AudioVisualizerWaveform(
                            amplitudes = amplitudes,
                            isPlaying = isPlaying,
                            modifier = Modifier.height(28.dp),
                            barCount = 20
                        )
                    }
                }
            }
        }

        // SONG SELECTION
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
                        text = "Klip İçin Şarkı Seçin",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (allSongs.isEmpty()) {
                        Text(
                            text = "Henüz bestelenmiş bir şarkı yok. 'Beste & Söz' sekmesinden hemen bir şarkı oluşturabilir veya örnek parçayı kullanabilirsiniz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allSongs) { song ->
                                val isSelected =
                                    song.id == selectedSong?.id

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) {
                                                NeonViolet
                                            } else {
                                                StudioSurfaceVariant
                                            }
                                        )
                                        .clickable {
                                            viewModel.videoSelectedSong.value =
                                                song
                                        }
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        )
                                ) {
                                    Column {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isSelected) {
                                                Color.White
                                            } else {
                                                TextPrimary
                                            }
                                        )

                                        Text(
                                            text = song.genre,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) {
                                                NeonCyan
                                            } else {
                                                TextMuted
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // VIDEO STYLE
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Video Klip Görsel Yönetmenliği & Tarz",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    )
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(videoStyles) { style ->
                        val isSelected = style == visualStyle

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        NeonCyan.copy(alpha = 0.2f)
                                    } else {
                                        StudioCardBg
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) {
                                        NeonCyan
                                    } else {
                                        StudioSurfaceVariant
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.videoVisualStyle.value = style
                                }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                )
                        ) {
                            Text(
                                text = style,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                ),
                                color = if (isSelected) {
                                    NeonCyan
                                } else {
                                    TextSecondary
                                }
                            )
                        }
                    }
                }
            }
        }

        // GENERATE STORYBOARD
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = {
                        val songToUse =
                            selectedSong
                                ?: allSongs.firstOrNull()
                                ?: SongItem(
                                    title = "Gece Yolculuğu",
                                    lyrics = "Gece çökerken şehre...",
                                    genre = "Pop & EDM",
                                    mood = "Enerjik"
                                )

                        viewModel.generateVideoStoryboard(songToUse)
                    },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_video_button"),
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
                                    text = "AI Video Storyboard Üretiliyor...",
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
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "AI VİDEO KLİP STORYBOARD OLUŞTUR",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // GENERATION STATUS AND ERROR MESSAGE
        if (generationMessage.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(
                            1.dp,
                            if (
                                generationMessage.contains(
                                    "hata",
                                    ignoreCase = true
                                )
                            ) {
                                NeonMagenta
                            } else {
                                NeonCyan
                            },
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StudioCardBg
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Text(
                            text = generationMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (
                                generationMessage.contains(
                                    "hata",
                                    ignoreCase = true
                                )
                            ) {
                                NeonMagenta
                            } else {
                                NeonCyan
                            }
                        )
                    }
                }
            }
        }

        // STORYBOARD RESULT
        if (generatedStoryboard != null) {
            val sb = generatedStoryboard!!

            item {
                Text(
                    text = "🎬 ${sb.conceptTitle}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    )
                )
            }

            items(sb.scenes) { scene ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StudioCardBg
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sahne ${scene.sceneNumber}: ${scene.sectionName}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = NeonAmber
                            )

                            Text(
                                text = scene.timeCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(StudioSurfaceVariant)
                                    .padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = NeonMagenta,
                                modifier = Modifier.size(14.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = scene.cameraAngle,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = NeonMagenta
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = scene.visualDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    StudioSurfaceVariant.copy(alpha = 0.6f)
                                )
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 Işık & Atmosfer: ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextMuted
                                )

                                Text(
                                    text = scene.lightingVibe,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
