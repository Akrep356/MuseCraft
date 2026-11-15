package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.audio.UserVoiceRecorder
import com.example.audio.VoiceRecordingItem
import com.example.audio.VoiceRecordingType
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MusicStudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserVoiceRecordingDialog(
    viewModel: MusicStudioViewModel,
    onDismiss: () -> Unit,
    onVoiceSelected: () -> Unit
) {
    val context = LocalContext.current
    val recorder = viewModel.voiceRecorder

    val isRecording by recorder.isRecording.collectAsState()
    val durationSec by recorder.recordingDurationSec.collectAsState()
    val amplitude by recorder.currentAmplitude.collectAsState()
    val isPlayingSample by recorder.isPlayingSample.collectAsState()
    val playingId by recorder.isPlayingRecordingId.collectAsState()
    val recordings by recorder.recordings.collectAsState()

    var selectedType by remember {
        mutableStateOf(VoiceRecordingType.SPEECH)
    }

    var permissionDeniedAlert by remember {
        mutableStateOf(false)
    }

    val selectedRecordings = recordings.filter {
        it.type == selectedType
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val started = recorder.startRecording(selectedType)

            if (!started) {
                Toast.makeText(
                    context,
                    "Kayıt başlatılamadı. Mikrofonu kontrol edin.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            permissionDeniedAlert = true
            Toast.makeText(
                context,
                "Ses kaydı için mikrofon izni gerekiyor.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val infiniteTransition =
        rememberInfiniteTransition(label = "voice_recording_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_recording_scale"
    )

    fun startRecording() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            val started = recorder.startRecording(selectedType)

            if (!started) {
                Toast.makeText(
                    context,
                    "Kayıt başlatılamadı.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Dialog(
        onDismissRequest = {
            recorder.stopRecording()
            recorder.stopPlayingSample()
            onDismiss()
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(NeonMagenta, NeonCyan)
                    ),
                    RoundedCornerShape(24.dp)
                ),
            color = StudioBackground,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kendi Ses Arşivim",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )

                        Text(
                            text = "Konuşma ve şarkı kayıtları",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }

                    IconButton(
                        onClick = {
                            recorder.stopRecording()
                            recorder.stopPlayingSample()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Açıklama
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StudioSurfaceVariant.copy(
                            alpha = 0.5f
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Ses veri seti hazırlığı",
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "Konuşma ve şarkı kayıtlarını ayrı ayrı biriktirebilirsin. Her kayıt telefonda uygulamanın özel alanında saklanır. Daha sonra bu kayıtları ses modeli eğitimi için hazırlayacağız.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kategori seçimi
                Text(
                    text = "KAYIT KATEGORİSİ",
                    modifier = Modifier.fillMaxWidth(),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryButton(
                        title = "Türkçe Konuşma",
                        count = recordings.count {
                            it.type == VoiceRecordingType.SPEECH
                        },
                        selected = selectedType == VoiceRecordingType.SPEECH,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        enabled = !isRecording
                    ) {
                        recorder.stopPlayingSample()
                        selectedType = VoiceRecordingType.SPEECH
                    }

                    CategoryButton(
                        title = "Şarkı Söyleme",
                        count = recordings.count {
                            it.type == VoiceRecordingType.SINGING
                        },
                        selected = selectedType == VoiceRecordingType.SINGING,
                        color = NeonMagenta,
                        modifier = Modifier.weight(1f),
                        enabled = !isRecording
                    ) {
                        recorder.stopPlayingSample()
                        selectedType = VoiceRecordingType.SINGING
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Kayıt butonu
                Box(
                    modifier = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    NeonMagenta.copy(alpha = 0.22f)
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (isRecording) {
                                        listOf(
                                            Color(0xFFEF4444),
                                            NeonMagenta
                                        )
                                    } else {
                                        listOf(
                                            NeonViolet,
                                            NeonMagenta
                                        )
                                    }
                                )
                            )
                            .clickable {
                                if (isRecording) {
                                    recorder.stopRecording()
                                } else {
                                    startRecording()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) {
                                Icons.Default.Stop
                            } else {
                                Icons.Default.Mic
                            },
                            contentDescription = if (isRecording) {
                                "Kaydı durdur"
                            } else {
                                "Kayıt başlat"
                            },
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isRecording) {
                    Text(
                        text = "Kayıt alınıyor: 00:${String.format("%02d", durationSec)}",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (selectedType == VoiceRecordingType.SPEECH) {
                            "Türkçe konuşmaya devam edin..."
                        } else {
                            "Şarkınızı söylemeye devam edin..."
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = if (selectedType == VoiceRecordingType.SPEECH) {
                            "Konuşma kaydı başlat"
                        } else {
                            "Şarkı kaydı başlat"
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ses seviyesi göstergesi
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurface)
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(18) { index ->
                        val barHeight = if (isRecording) {
                            (
                                amplitude *
                                    (0.4f + (index % 5) * 0.15f)
                                ).coerceIn(0.12f, 1f)
                        } else {
                            0.15f
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.5.dp)
                                .height((25 * barHeight).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isRecording) {
                                        Color(0xFFEF4444)
                                    } else {
                                        NeonCyan.copy(alpha = 0.6f)
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Kayıt arşivi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedType == VoiceRecordingType.SPEECH) {
                            "TÜRKÇE KONUŞMA KAYITLARI"
                        } else {
                            "ŞARKI KAYITLARI"
                        },
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        text = "${selectedRecordings.size} kayıt",
                        color = NeonCyan,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedRecordings.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StudioSurfaceVariant.copy(
                                alpha = 0.35f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(28.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Henüz kayıt yok",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    selectedRecordings.forEach { item ->
                        RecordingRow(
                            item = item,
                            isPlaying = playingId == item.id,
                            onPlay = {
                                if (playingId == item.id) {
                                    recorder.stopPlayingSample()
                                } else {
                                    recorder.playRecording(item)
                                }
                            },
                            onDelete = {
                                val deleted = recorder.deleteRecording(item)

                                Toast.makeText(
                                    context,
                                    if (deleted) {
                                        "Kayıt silindi"
                                    } else {
                                        "Kayıt silinemedi"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        Spacer(modifier = Modifier.height(7.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Alt bilgi
                Text(
                    text = "Toplam ${recordings.size} ses kaydı saklanıyor.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        recorder.stopRecording()
                        recorder.stopPlayingSample()
                        onVoiceSelected()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonViolet
                    )
                ) {
                    Text(
                        text = "TAMAM",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(
    title: String,
    count: Int,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    color.copy(alpha = 0.16f)
                } else {
                    StudioSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) color else TextMuted.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) {
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (selected) color else TextSecondary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$count kayıt",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun RecordingRow(
    item: VoiceRecordingItem,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = StudioSurfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlay
            ) {
                Icon(
                    imageVector = if (isPlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = if (isPlaying) {
                        "Durdur"
                    } else {
                        "Dinle"
                    },
                    tint = NeonCyan
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (item.type == VoiceRecordingType.SPEECH) {
                        "Konuşma kaydı"
                    } else {
                        "Şarkı kaydı"
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )

                val dateText = remember(item.createdAt) {
                    SimpleDateFormat(
                        "dd.MM.yyyy HH:mm",
                        Locale.getDefault()
                    ).format(Date(item.createdAt))
                }

                Text(
                    text = "$dateText • ${item.durationSeconds} sn",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Kaydı sil",
                    tint = NeonMagenta
                )
            }
        }
    }
}
