package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MusicGenre
import com.example.model.VocalGender
import com.example.model.VocalTone
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

@Composable
fun AudioVisualizerWaveform(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 16
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_idle")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bars = if (amplitudes.isNotEmpty()) amplitudes else List(barCount) { 0.2f }
        bars.take(barCount).forEachIndexed { idx, amp ->
            val heightFraction = if (isPlaying) amp.coerceIn(0.12f, 1.0f) else idlePulse
            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    if (idx % 2 == 0) NeonCyan else NeonViolet,
                    if (idx % 3 == 0) NeonMagenta else NeonCyan
                )
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .height((32 * heightFraction).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barBrush)
            )
        }
    }
}

@Composable
fun MiniStudioPlayerBar(
    isPlaying: Boolean,
    currentSection: String,
    elapsedSec: Float,
    bpm: Int,
    songTitle: String?,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(NeonViolet.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.5f))),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = StudioSurface.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonViolet, NeonMagenta))
                    )
                    .clickable { onTogglePlay() }
                    .testTag("mini_player_toggle_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Durdur" else "Çal",
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
                        text = songTitle ?: "Canlı Synth & Vokal Stüdyosu",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$bpm BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonAmber,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (isPlaying) NeonCyan else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) currentSection else "Hazır • Çalmak için dokun",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPlaying) NeonCyan else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = String.format("%02d:%02d", (elapsedSec / 60).toInt(), (elapsedSec % 60).toInt()),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextSecondary
            )
        }
    }
}

@Composable
fun GenreSelectorChips(
    selectedGenre: MusicGenre,
    onSelectGenre: (MusicGenre) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MusicGenre.values()) { genre ->
            val isSelected = genre == selectedGenre
            val chipBg = if (isSelected) {
                Brush.horizontalGradient(listOf(NeonViolet, NeonMagenta))
            } else {
                Brush.horizontalGradient(listOf(StudioCardBg, StudioCardBg))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipBg)
                    .border(
                        1.dp,
                        if (isSelected) NeonCyan else StudioSurfaceVariant,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelectGenre(genre) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("genre_chip_${genre.name}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = genre.emoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = genre.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun VocalToneSelectorCard(
    selectedTone: VocalTone,
    onSelectTone: (VocalTone) -> Unit,
    modifier: Modifier = Modifier,
    onRecordVoiceClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vokal Karakteri & Ses Rengi",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            if (onRecordVoiceClick != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(listOf(NeonMagenta, NeonViolet))
                        )
                        .clickable { onRecordVoiceClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("record_own_voice_shortcut_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sesini Kaydet 🎙️",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(com.example.model.PresetVocalTones.tones) { tone ->
                val isSelected = tone.id == selectedTone.id
                val isCustomUserVoice = tone.id == "user_own_voice"
                val isFemale = tone.gender == VocalGender.FEMALE
                val accentColor = when {
                    isCustomUserVoice -> NeonMagenta
                    isFemale -> NeonMagenta
                    else -> NeonCyan
                }

                Card(
                    modifier = Modifier
                        .width(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTone(tone) }
                        .border(
                            width = if (isSelected) 2.dp else if (isCustomUserVoice) 1.dp else 0.dp,
                            brush = if (isSelected && isCustomUserVoice) {
                                Brush.horizontalGradient(listOf(NeonMagenta, NeonCyan))
                            } else if (isSelected) {
                                Brush.horizontalGradient(listOf(accentColor, accentColor))
                            } else if (isCustomUserVoice) {
                                Brush.horizontalGradient(listOf(NeonMagenta.copy(alpha = 0.4f), NeonViolet.copy(alpha = 0.4f)))
                            } else {
                                Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("vocal_tone_card_${tone.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) StudioSurfaceVariant else StudioCardBg
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    isCustomUserVoice -> "🌟 Kendi AI Sesin"
                                    isFemale -> "Kadın 👩‍🎤"
                                    else -> "Erkek 👨‍🎤"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text(
                                text = tone.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCustomUserVoice) NeonCyan else TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = tone.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = tone.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isCustomUserVoice && onRecordVoiceClick != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonMagenta.copy(alpha = 0.2f))
                                    .clickable { onRecordVoiceClick() }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🎙️ Sesi Kaydet / Dinle",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeonMagenta
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
