
package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import java.io.File

class AudioSynthesisEngine(
    private val context: Context
) {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var progressJob: Job? = null

    private val engineScope = CoroutineScope(Dispatchers.Default)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    val isPreparing = _isPreparing.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError = _playbackError.asStateFlow()

    private val _currentBpm = MutableStateFlow(120)
    val currentBpm = _currentBpm.asStateFlow()

    private val _currentPlaybackTimeSec = MutableStateFlow(0f)
    val currentPlaybackTimeSec = _currentPlaybackTimeSec.asStateFlow()

    private val _totalDurationSec = MutableStateFlow(45f)
    val totalDurationSec = _totalDurationSec.asStateFlow()

    private val _visualizerAmplitudes =
        MutableStateFlow(List(16) { 0.15f })
    val visualizerAmplitudes = _visualizerAmplitudes.asStateFlow()

    private val _currentPlayingSection =
        MutableStateFlow("Giriş / Intro")
    val currentPlayingSection = _currentPlayingSection.asStateFlow()

    fun updateBpm(bpm: Int) {
        _currentBpm.value = bpm.coerceIn(60, 180)
    }

    // GERÇEK EROQ MP3 BAĞLANTISINI ÇAL
    fun playUrl(url: String, bpm: Int = 120) {
        if (url.isBlank()) {
            _playbackError.value = "MP3 bağlantısı boş."
            return
        }

        stopTrack()
        _playbackError.value = null
        _currentBpm.value = bpm.coerceIn(60, 180)
        _currentPlayingSection.value =
            "Eroq Music One • Hazırlanıyor"
        _isPreparing.value = true

        try {
            val player = MediaPlayer()
            mediaPlayer = player

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            player.setOnPreparedListener { preparedPlayer ->
                if (mediaPlayer !== preparedPlayer) {
                    return@setOnPreparedListener
                }

                try {
                    val duration =
                        preparedPlayer.duration.coerceAtLeast(0) / 1000f

                    if (duration > 0f) {
                        _totalDurationSec.value = duration
                    }

                    _currentPlaybackTimeSec.value = 0f
                    _isPreparing.value = false
                    _isPlaying.value = true
                    _currentPlayingSection.value =
                        "Eroq Music One • Gerçek MP3 çalıyor"

                    preparedPlayer.start()
                    startProgressUpdates()

                } catch (e: Exception) {
                    _playbackError.value =
                        "MP3 başlatılamadı: ${e.message}"
                    stopTrack()
                }
            }

            player.setOnCompletionListener { completedPlayer ->
                if (mediaPlayer === completedPlayer) {
                    stopTrack()
                    _currentPlayingSection.value = "Şarkı tamamlandı"
                }
            }

            player.setOnErrorListener { errorPlayer, what, extra ->
                if (mediaPlayer === errorPlayer) {
                    _playbackError.value =
                        "MP3 oynatma hatası. Kod: $what / $extra"
                    stopTrack()
                }
                true
            }

            player.setDataSource(url)
            player.prepareAsync()

        } catch (e: Exception) {
            _playbackError.value =
                "MP3 bağlantısı açılamadı: ${e.message}"
            stopTrack()
        }
    }

    // TELEFONDAKİ DOSYAYI VEYA CONTENT URI'Yİ ÇAL
    fun playFile(filePath: String, bpm: Int = 120) {
        if (filePath.isBlank()) {
            _playbackError.value = "MP3 dosya yolu boş."
            return
        }

        stopTrack()
        _playbackError.value = null
        _currentBpm.value = bpm.coerceIn(60, 180)
        _currentPlayingSection.value = "Telefon MP3 • Hazırlanıyor"
        _isPreparing.value = true

        try {
            val player = MediaPlayer()
            mediaPlayer = player

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            player.setOnPreparedListener { preparedPlayer ->
                if (mediaPlayer !== preparedPlayer) {
                    return@setOnPreparedListener
                }

                try {
                    val duration =
                        preparedPlayer.duration.coerceAtLeast(0) / 1000f

                    if (duration > 0f) {
                        _totalDurationSec.value = duration
                    }

                    _currentPlaybackTimeSec.value = 0f
                    _isPreparing.value = false
                    _isPlaying.value = true
                    _currentPlayingSection.value =
                        "Telefon MP3 • Çalıyor"

                    preparedPlayer.start()
                    startProgressUpdates()

                } catch (e: Exception) {
                    _playbackError.value =
                        "İndirilen MP3 başlatılamadı: ${e.message}"
                    stopTrack()
                }
            }

            player.setOnCompletionListener { completedPlayer ->
                if (mediaPlayer === completedPlayer) {
                    stopTrack()
                    _currentPlayingSection.value = "Şarkı tamamlandı"
                }
            }

            player.setOnErrorListener { errorPlayer, what, extra ->
                if (mediaPlayer === errorPlayer) {
                    _playbackError.value =
                        "Yerel MP3 oynatma hatası. Kod: $what / $extra"
                    stopTrack()
                }
                true
            }

            if (filePath.startsWith("content://")) {
                player.setDataSource(
                    context,
                    Uri.parse(filePath)
                )
            } else {
                val file = File(filePath)

                if (!file.exists() || !file.isFile) {
                    throw IllegalStateException(
                        "MP3 dosyası telefonda bulunamadı."
                    )
                }

                player.setDataSource(file.absolutePath)
            }

            player.prepareAsync()

        } catch (e: Exception) {
            _playbackError.value =
                "Telefon MP3 dosyası açılamadı: ${e.message}"
            stopTrack()
        }
    }

    // DURAKLAT
    fun pauseTrack() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                    _currentPlayingSection.value = "Duraklatıldı"
                }
            }

            if (audioTrack != null && _isPlaying.value) {
                audioTrack?.pause()
                _isPlaying.value = false
            }
        } catch (e: Exception) {
            _playbackError.value =
                "Duraklatma hatası: ${e.message}"
        }
    }

    // DEVAM ETTİR
    fun resumeTrack() {
        try {
            mediaPlayer?.let { player ->
                player.start()
                _isPlaying.value = true
                _currentPlayingSection.value =
                    "Gerçek MP3 çalıyor"
                startProgressUpdates()
                return
            }

            audioTrack?.let { track ->
                track.play()
                _isPlaying.value = true
            }
        } catch (e: Exception) {
            _playbackError.value =
                "Devam ettirme hatası: ${e.message}"
        }
    }

    fun toggleRealPlayback() {
        if (_isPreparing.value) return

        if (_isPlaying.value) {
            pauseTrack()
        } else {
            resumeTrack()
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()

        progressJob = engineScope.launch {
            while (isActive && _isPlaying.value) {
                val player = mediaPlayer ?: break

                try {
                    val position = player.currentPosition / 1000f
                    _currentPlaybackTimeSec.value = position

                    _visualizerAmplitudes.value = List(16) { index ->
                        val wave = (
                            sin(position * 5.0 + index * 0.5) * 0.4 + 0.5
                        ).toFloat()

                        wave.coerceIn(0.1f, 1f)
                    }

                    delay(300)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    // ESKİ SENTETİK SES MOTORU
    fun playTrack(
        genre: String = "Pop & Dance",
        musicalKey: String = "A Minör",
        isFemaleVocal: Boolean = true,
        isCustomUserVoice: Boolean = false,
        bpm: Int = 120
    ) {
        stopTrack()
        _playbackError.value = null
        _currentBpm.value = bpm
        _isPlaying.value = true
        _totalDurationSec.value = 45f

        playbackJob = engineScope.launch {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (minBufferSize <= 0) {
                _playbackError.value = "Ses motoru başlatılamadı."
                _isPlaying.value = false
                return@launch
            }

            try {
                val bufferSize = minBufferSize * 4

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(
                                AudioAttributes.CONTENT_TYPE_MUSIC
                            )
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(
                                AudioFormat.CHANNEL_OUT_MONO
                            )
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val secondsPerBeat = 60.0 / _currentBpm.value
                val chordScale = getChordFrequencies(musicalKey)
                var sampleIndex = 0L
                val audioBuffer = ShortArray(bufferSize / 2)

                val vocalBasePitch = when {
                    isCustomUserVoice -> 330.0
                    isFemaleVocal -> 440.0
                    else -> 220.0
                }

                while (isActive && _isPlaying.value) {
                    val totalElapsedSec =
                        sampleIndex.toDouble() / sampleRate

                    _currentPlaybackTimeSec.value =
                        (totalElapsedSec % 45.0).toFloat()

                    val cyclePos =
                        (totalElapsedSec % 32.0).toInt()

                    val voicePrefix =
                        if (isCustomUserVoice) {
                            "Kendi Ses Klonun: "
                        } else {
                            ""
                        }

                    _currentPlayingSection.value = when {
                        cyclePos < 8 ->
                            "${voicePrefix}Giriş / Intro"
                        cyclePos < 16 ->
                            "${voicePrefix}1. Bölüm / Verse"
                        cyclePos < 24 ->
                            "${voicePrefix}Nakarat / Chorus"
                        else ->
                            "${voicePrefix}Köprü & Solo / Outro"
                    }

                    for (i in audioBuffer.indices) {
                        val time =
                            (sampleIndex + i).toDouble() / sampleRate

                        val beatIndex =
                            ((time / secondsPerBeat).toInt()) % 16

                        val chordIdx =
                            (beatIndex / 4) % chordScale.size

                        val baseChord = chordScale[chordIdx]

                        val beatFraction =
                            (time % secondsPerBeat) / secondsPerBeat

                        val isKick =
                            (beatIndex % 2 == 0) &&
                                (beatFraction < 0.15)

                        val isSnare =
                            (beatIndex % 4 == 2) &&
                                (beatFraction < 0.2)

                        val isHat = beatFraction < 0.05

                        var drumSignal = 0.0

                        if (isKick) {
                            val kickPitch =
                                120.0 * (
                                    1.0 - beatFraction * 4.0
                                ).coerceAtLeast(0.2)

                            drumSignal +=
                                0.35 *
                                sin(2.0 * PI * kickPitch * time) *
                                (1.0 - beatFraction / 0.15)
                        }

                        if (isSnare) {
                            val noise = Math.random() * 2.0 - 1.0
                            drumSignal +=
                                0.2 * noise *
                                (1.0 - beatFraction / 0.2)
                        }

                        if (isHat) {
                            val hatNoise =
                                Math.random() * 2.0 - 1.0

                            drumSignal +=
                                0.08 * hatNoise *
                                (1.0 - beatFraction / 0.05)
                        }

                        val arpeggioNote =
                            baseChord[beatIndex % baseChord.size]

                        val padSignal =
                            0.18 * sin(
                                2.0 * PI * arpeggioNote * time
                            ) +
                            0.08 * sin(
                                2.0 * PI * arpeggioNote * 1.5 * time
                            )

                        val vibrato =
                            sin(2.0 * PI * 5.5 * time) * 3.0

                        val vocalFreq =
                            vocalBasePitch +
                                (baseChord[0] % 150) +
                                vibrato

                        val vocalSignal =
                            if (cyclePos in 8..30) {
                                0.22 * sin(
                                    2.0 * PI * vocalFreq * time
                                ) +
                                0.10 * sin(
                                    2.0 * PI * vocalFreq * 2.0 * time
                                ) +
                                0.05 * sin(
                                    2.0 * PI * vocalFreq * 3.0 * time
                                )
                            } else {
                                0.0
                            }

                        val bassFreq = baseChord[0] / 2.0

                        val bassSignal =
                            0.25 * sin(
                                2.0 * PI * bassFreq * time
                            )

                        val mixed =
                            (drumSignal + padSignal +
                                vocalSignal + bassSignal) * 0.75

                        audioBuffer[i] =
                            (
                                mixed.coerceIn(-0.95, 0.95) *
                                    Short.MAX_VALUE
                            ).toInt().toShort()
                    }

                    audioTrack?.write(
                        audioBuffer,
                        0,
                        audioBuffer.size
                    )

                    sampleIndex += audioBuffer.size

                    _visualizerAmplitudes.value =
                        List(16) { index ->
                            val wave = (
                                sin(
                                    sampleIndex.toDouble() / 2000.0 +
                                        index * 0.5
                                ) * 0.4 + 0.5
                            ).toFloat()

                            val pulse =
                                if (
                                    (sampleIndex / 8000) % 2 == 0L &&
                                    index in 4..11
                                ) {
                                    0.35f
                                } else {
                                    0.05f
                                }

                            (wave + pulse).coerceIn(0.1f, 1f)
                        }
                }
            } catch (e: Exception) {
                _playbackError.value =
                    "Ses motoru hatası: ${e.message}"
                _isPlaying.value = false
            }
        }
    }

    // TÜM SESİ DURDUR
    fun stopTrack() {
        _isPlaying.value = false
        _isPreparing.value = false

        playbackJob?.cancel()
        playbackJob = null

        progressJob?.cancel()
        progressJob = null

        try {
            mediaPlayer?.setOnPreparedListener(null)
            mediaPlayer?.setOnCompletionListener(null)
            mediaPlayer?.setOnErrorListener(null)
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }

        mediaPlayer = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }

        audioTrack = null

        _visualizerAmplitudes.value = List(16) { 0.1f }
        _currentPlaybackTimeSec.value = 0f
    }

    private fun getChordFrequencies(
        key: String
    ): List<List<Double>> {
        return when {
            key.contains("A Minör", ignoreCase = true) ||
                key.contains("Am", ignoreCase = true) -> listOf(
                listOf(220.0, 261.63, 329.63),
                listOf(174.61, 220.0, 261.63),
                listOf(261.63, 329.63, 392.00),
                listOf(196.00, 246.94, 293.66)
            )

            key.contains("E Minör", ignoreCase = true) ||
                key.contains("Em", ignoreCase = true) -> listOf(
                listOf(164.81, 196.00, 246.94),
                listOf(261.63, 329.63, 392.00),
                listOf(196.00, 246.94, 293.66),
                listOf(146.83, 185.00, 220.00)
            )

            key.contains("D Minör", ignoreCase = true) ||
                key.contains("Dm", ignoreCase = true) -> listOf(
                listOf(146.83, 174.61, 220.00),
                listOf(233.08, 293.66, 349.23),
                listOf(174.61, 220.0, 261.63),
                listOf(261.63, 329.63, 392.00)
            )

            else -> listOf(
                listOf(261.63, 329.63, 392.00),
                listOf(196.00, 246.94, 293.66),
                listOf(220.0, 261.63, 329.63),
                listOf(174.61, 220.0, 261.63)
            )
        }
    }
}
