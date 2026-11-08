package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GenreSelectorChips
import com.example.ui.components.VocalToneSelectorCard
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
fun CoverVocalScreen(
    viewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val originalTitle by viewModel.coverOriginalTitle.collectAsState()
    val targetGenre by viewModel.coverTargetGenre.collectAsState()
    val targetVocal by viewModel.coverTargetVocal.collectAsState()
    val coverNotes by viewModel.coverNotes.collectAsState()

    val isGenerating by viewModel.isGeneratingCover.collectAsState()
    val generatedCover by viewModel.generatedCover.collectAsState()
    val coverMessage by viewModel.coverMessage.collectAsState()

    var autotuneLevel by remember { mutableStateOf(65f) }
    var reverbLevel by remember { mutableStateOf(50f) }

    val presetPopularSongs = listOf(
        "Dönence (Barış Manço)",
        "Ah Yalan Dünya (Neşet Ertaş)",
        "Gülümse (Sezen Aksu)",
        "Ele Güne Karşı (MFÖ)",
        "Unutabilsem (Emrah)"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StudioCardBg
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = NeonMagenta,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "AI COVER & VOKAL STÜDYOSU",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Bir şarkı temasını yeni müzik türü ve vokal düzeniyle yeniden yorumla. Gemini şarkı sözlerini ve aranje notlarını hazırlar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cover Yapılacak Şarkı veya Tema",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = originalTitle,
                        onValueChange = {
                            viewModel.coverOriginalTitle.value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cover_original_input"),
                        placeholder = {
                            Text(
                                "Şarkı adı veya tema gir...",
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMagenta,
                            unfocusedBorderColor = StudioSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Popüler Şarkı Örnekleri:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presetPopularSongs) { song ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioSurfaceVariant)
                                    .clickable {
                                        viewModel.coverOriginalTitle.value = song
                                    }
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                            ) {
                                Text(
                                    text = song,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonMagenta
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Dönüştürülecek Yeni Müzik Türü",
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
                    selectedGenre = targetGenre,
                    onSelectGenre = {
                        viewModel.coverTargetGenre.value = it
                    }
                )
            }
        }

        item {
            VocalToneSelectorCard(
                selectedTone = targetVocal,
                onSelectTone = {
                    viewModel.coverTargetVocal.value = it
                },
                onRecordVoiceClick = {
                    viewModel.showVoiceRecordingDialog.value = true
                }
            )
        }

        item {
            val hasRecordedUserVoice by
                viewModel.voiceRecorder.hasRecording.collectAsState()

            val isCustomVoiceSelected =
                targetVocal.id == "user_own_voice"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(NeonCyan, NeonMagenta)
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        viewModel.showVoiceRecordingDialog.value = true
                    }
                    .testTag("cover_user_voice_banner_card"),
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
                                    listOf(NeonCyan, NeonMagenta)
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

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kendi Sesini Kaydet",
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
                                "Ses kaydın mevcut. Bu Gemini Cover akışında ses klonlama henüz kullanılmıyor."
                            } else {
                                "Ses kaydı özelliği mevcut; Gemini Cover üretiminde henüz kullanılmıyor."
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
                            containerColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        modifier = Modifier.testTag("cover_voice_record_btn")
                    ) {
                        Text(
                            text = if (hasRecordedUserVoice) {
                                "Dinle / Düzenle"
                            } else {
                                "Kaydet"
                            },
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vokal Efektleri & Tonlama",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🎤 Autotune / Pitch Düzeltme",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Text(
                            "%${autotuneLevel.toInt()}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonCyan
                        )
                    }

                    Slider(
                        value = autotuneLevel,
                        onValueChange = { autotuneLevel = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = StudioSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🌌 Stüdyo Reverb & Atmosfer",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Text(
                            "%${reverbLevel.toInt()}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonMagenta
                        )
                    }

                    Slider(
                        value = reverbLevel,
                        onValueChange = { reverbLevel = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonMagenta,
                            activeTrackColor = NeonMagenta,
                            inactiveTrackColor = StudioSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Not: Bu efekt ayarları şimdilik görsel kontrollerdir; Gemini söz üretimini veya gerçek ses kaydını değiştirmez.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.generateCover()
                },
                enabled = !isGenerating && targetVocal.id != "user_own_voice",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp)
                    .testTag("generate_cover_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = StudioSurfaceVariant
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    NeonMagenta,
                                    NeonViolet,
                                    NeonCyan
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "Gemini sözleri hazırlıyor...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "GEMINI İLE COVER SÖZLERİ OLUŞTUR",
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

        item {
            Text(
                text = "Bu bölüm yalnızca Gemini ile şarkı sözleri ve aranje metni oluşturur. Eroq kullanılmaz ve kredi harcanmaz. MP3 üretimi yapılmaz.",
                modifier = Modifier.padding(horizontal = 18.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (coverMessage.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StudioSurfaceVariant
                    )
                ) {
                    Text(
                        text = coverMessage,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        if (generatedCover != null) {
            val cover = generatedCover!!

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(NeonMagenta, NeonCyan)
                            ),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StudioCardBg
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎙️ ${cover.title}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${targetGenre.displayName} • ${targetVocal.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonMagenta
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Gemini şarkı sözleri ve aranje notu hazır. Eroq kullanılmadı; MP3 üretilmedi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreen
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.saveGeneratedCover()
                            },
                            enabled = generatedCover != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_cover_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen.copy(alpha = 0.2f),
                                contentColor = NeonGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 10.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                "Sözleri Kütüphaneye Kaydet",
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    "🎯 Aranje & Vokal Notu:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    cover.vocalDirection,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "🎼 Enstrümantasyon:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    cover.instrumentation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "🎹 Akorlar:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    cover.chordProgression,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Cover Şarkı Sözleri & Bölümler",
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
                                text = cover.lyrics,
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
