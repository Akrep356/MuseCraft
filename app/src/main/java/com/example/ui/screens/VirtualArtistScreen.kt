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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.MusicGenre
import com.example.model.VirtualArtist
import com.example.model.VocalGender
import com.example.ui.components.GenreSelectorChips
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
fun VirtualArtistScreen(
    viewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val artistName by viewModel.artistName.collectAsState()
    val artistGenre by viewModel.artistGenre.collectAsState()
    val artistGender by viewModel.artistGender.collectAsState()
    val artistVisualStyle by viewModel.artistVisualStyle.collectAsState()
    val isGenerating by viewModel.isGeneratingArtist.collectAsState()
    val generatedResult by viewModel.generatedArtist.collectAsState()
    val allArtists by viewModel.allArtists.collectAsState()

    val visualStyles = listOf(
        "Cyberpunk Hologram",
        "Futuristic Rocker",
        "3D Anime Pop Idol",
        "Acoustic Minimalist",
        "Retro 80s Synth Star",
        "Urban Streetwear"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SANAL AI SANATÇI & KARAKTER YARATICI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kendi sanal pop yıldızını, rock grubunu veya sanal avatarını üret, hikayesini ve sahne imajını belirle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Artist Name & Identity
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sanal Sanatçı İsmi",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = artistName,
                        onValueChange = { viewModel.artistName.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("artist_name_input"),
                        placeholder = { Text("Örn: NOVA, KRONOS, AURA-7, SelenAI...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = StudioSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Karakter Cinsiyeti & Vokal Tabanı:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (artistGender == VocalGender.FEMALE) NeonMagenta else StudioSurfaceVariant)
                                .clickable { viewModel.artistGender.value = VocalGender.FEMALE }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👩‍🎤 Kadın Sanatçı",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (artistGender == VocalGender.FEMALE) Color.White else TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (artistGender == VocalGender.MALE) NeonCyan else StudioSurfaceVariant)
                                .clickable { viewModel.artistGender.value = VocalGender.MALE }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👨‍🎤 Erkek Sanatçı",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (artistGender == VocalGender.MALE) Color.Black else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Genre Selector
        item {
            Column {
                Text(
                    text = "Sanatçının Ana Müzik Türü",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                GenreSelectorChips(
                    selectedGenre = artistGenre,
                    onSelectGenre = { viewModel.artistGenre.value = it }
                )
            }
        }

        // Visual Style Selector
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Görsel Sahne Tarzı & Estetik",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visualStyles) { style ->
                        val isSelected = style == artistVisualStyle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonViolet else StudioCardBg)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else StudioSurfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.artistVisualStyle.value = style }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = style,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Generate Artist Action
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = { viewModel.generateArtist() },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_artist_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonCyan, NeonViolet, NeonMagenta)
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
                                    text = "AI Sanal Karakter Oluşturuluyor...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
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
                                    text = "AI İLE SANAL SANATÇI YARAT",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Generated Artist Preview Card
        if (generatedResult != null) {
            val artist = generatedResult!!
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta)),
                            RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Image
                            val avatarRes = if (artistGender == VocalGender.FEMALE) {
                                R.drawable.img_virtual_female_artist
                            } else {
                                R.drawable.img_virtual_male_artist
                            }

                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = "Artist Avatar",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, NeonCyan, CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = artist.stageTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonAmber
                                )
                                Text(
                                    text = "${artistGenre.displayName} • $artistVisualStyle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Button(
                                onClick = { viewModel.saveGeneratedArtist() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("save_artist_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kaydet", color = NeonGreen, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bio
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioSurfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("📖 Sanatçı Biyografisi:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(artist.bio, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Signature Look
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioSurfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("✨ Sahne İmajı & Aksesuarlar:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(artist.signatureLook, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                            }
                        }
                    }
                }
            }
        }

        // Saved Artists List
        if (allArtists.isNotEmpty()) {
            item {
                Text(
                    text = "Kayıtlı Sanal Müzisyenler (${allArtists.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            items(allArtists) { artistItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatarRes = if (artistItem.gender == "FEMALE" || artistItem.avatarDrawableRes.contains("female")) {
                            R.drawable.img_virtual_female_artist
                        } else {
                            R.drawable.img_virtual_male_artist
                        }

                        Image(
                            painter = painterResource(id = avatarRes),
                            contentDescription = artistItem.name,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (artistItem.gender == "FEMALE") NeonMagenta else NeonCyan, CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artistItem.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = artistItem.stageTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonAmber
                            )
                            Text(
                                text = "${artistItem.genre} • ${artistItem.visualStyle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }

                        IconButton(onClick = { viewModel.deleteArtist(artistItem) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
