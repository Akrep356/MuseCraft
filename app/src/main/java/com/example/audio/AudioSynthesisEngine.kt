package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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

class AudioSynthesisEngine {
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentBpm = MutableStateFlow(120)
    val currentBpm = _currentBpm.asStateFlow()

    private val _currentPlaybackTimeSec = MutableStateFlow(0f)
    val currentPlaybackTimeSec = _currentPlaybackTimeSec.asStateFlow()

    private val _totalDurationSec = MutableStateFlow(45f)
    val totalDurationSec = _totalDurationSec.asStateFlow()

    private val _visualizerAmplitudes = MutableStateFlow(List(16) { 0.15f })
    val visualizerAmplitudes = _visualizerAmplitudes.asStateFlow()

    private val _currentPlayingSection = MutableStateFlow("Giriş / Intro")
    val currentPlayingSection = _currentPlayingSection.asStateFlow()

    fun updateBpm(bpm: Int) {
        _currentBpm.value = bpm.coerceIn(60, 180)
    }

    fun playTrack(
        genre: String = "Pop & Dance",
        musicalKey: String = "A Minör",
        isFemaleVocal: Boolean = true,
        isCustomUserVoice: Boolean = false,
        bpm: Int = 120
    ) {
        stopTrack()
        _currentBpm.value = bpm
        _isPlaying.value = true

        playbackJob = engineScope.launch {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufferSize * 4

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            val secondsPerBeat = 60.0 / _currentBpm.value
            val chordScale = getChordFrequencies(musicalKey)
            var currentBeat = 0
            var sampleIndex = 0L
            val audioBuffer = ShortArray(bufferSize / 2)
            val vocalBasePitch = when {
                isCustomUserVoice -> 330.0 // Human personalized vocal range
                isFemaleVocal -> 440.0
                else -> 220.0
            }

            while (isActive && _isPlaying.value) {
                val totalElapsedSec = sampleIndex.toDouble() / sampleRate
                _currentPlaybackTimeSec.value = (totalElapsedSec % 45.0).toFloat()

                // Update current song section according to elapsed time
                val cyclePos = (totalElapsedSec % 32.0).toInt()
                val voicePrefix = if (isCustomUserVoice) "🎙️ Kendi Ses Klonun: " else ""
                _currentPlayingSection.value = when {
                    cyclePos < 8 -> "${voicePrefix}Giriş / Intro (Enstrümantal & Beat)"
                    cyclePos < 16 -> "${voicePrefix}1. Bölüm / Verse (Ses Klonu & Melodi)"
                    cyclePos < 24 -> "${voicePrefix}Nakarat / Chorus (Kendi Sesinle Şarkı Söylüyor)"
                    else -> "${voicePrefix}Köprü & Solo / Outro"
                }

                // Fill synthetic audio buffer
                for (i in audioBuffer.indices) {
                    val time = (sampleIndex + i).toDouble() / sampleRate
                    val beatIndex = ((time / secondsPerBeat).toInt()) % 16
                    val chordIdx = (beatIndex / 4) % chordScale.size
                    val baseChord = chordScale[chordIdx]

                    // Rhythm Beat (Kick & Snare & Hi-Hat)
                    val beatFraction = (time % secondsPerBeat) / secondsPerBeat
                    val isKick = (beatIndex % 2 == 0) && (beatFraction < 0.15)
                    val isSnare = (beatIndex % 4 == 2) && (beatFraction < 0.2)
                    val isHat = (beatFraction < 0.05)

                    var drumSignal = 0.0
                    if (isKick) {
                        val kickPitch = 120.0 * (1.0 - beatFraction * 4.0).coerceAtLeast(0.2)
                        drumSignal += 0.35 * sin(2.0 * PI * kickPitch * time) * (1.0 - beatFraction / 0.15)
                    }
                    if (isSnare) {
                        val noise = (Math.random() * 2.0 - 1.0)
                        drumSignal += 0.2 * noise * (1.0 - beatFraction / 0.2)
                    }
                    if (isHat) {
                        val hatNoise = (Math.random() * 2.0 - 1.0)
                        drumSignal += 0.08 * hatNoise * (1.0 - beatFraction / 0.05)
                    }

                    // Pad / Chord Arpeggio
                    val arpeggioNote = baseChord[(beatIndex % baseChord.size)]
                    val padSignal = 0.18 * sin(2.0 * PI * arpeggioNote * time) +
                            0.08 * sin(2.0 * PI * (arpeggioNote * 1.5) * time)

                    // Vocal Lead Melody Simulation (with vibrato)
                    val vibrato = sin(2.0 * PI * 5.5 * time) * 3.0
                    val vocalFreq = (vocalBasePitch + (baseChord[0] % 150) + vibrato)
                    val vocalSignal = if (cyclePos in 8..30) {
                        0.22 * sin(2.0 * PI * vocalFreq * time) +
                                0.10 * sin(2.0 * PI * (vocalFreq * 2.0) * time) +
                                0.05 * sin(2.0 * PI * (vocalFreq * 3.0) * time)
                    } else 0.0

                    // Bassline
                    val bassFreq = baseChord[0] / 2.0
                    val bassSignal = 0.25 * sin(2.0 * PI * bassFreq * time)

                    val mixed = (drumSignal + padSignal + vocalSignal + bassSignal) * 0.75
                    audioBuffer[i] = (mixed.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
                }

                audioTrack?.write(audioBuffer, 0, audioBuffer.size)
                sampleIndex += audioBuffer.size

                // Dynamic reactive visualizer waveform update
                val dynamicAmps = List(16) { idx ->
                    val wave = (sin(sampleIndex.toDouble() / 2000.0 + idx * 0.5) * 0.4 + 0.5).toFloat()
                    val pulse = if ((sampleIndex / 8000) % 2 == 0L && idx in 4..11) 0.35f else 0.05f
                    (wave + pulse).coerceIn(0.1f, 1.0f)
                }
                _visualizerAmplitudes.value = dynamicAmps
            }
        }
    }

    fun stopTrack() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        _visualizerAmplitudes.value = List(16) { 0.1f }
        _currentPlaybackTimeSec.value = 0f
    }

    private fun getChordFrequencies(key: String): List<List<Double>> {
        return when {
            key.contains("A Minör", ignoreCase = true) || key.contains("Am", ignoreCase = true) -> listOf(
                listOf(220.0, 261.63, 329.63), // Am
                listOf(174.61, 220.0, 261.63),  // F
                listOf(261.63, 329.63, 392.00), // C
                listOf(196.00, 246.94, 293.66)  // G
            )
            key.contains("E Minör", ignoreCase = true) || key.contains("Em", ignoreCase = true) -> listOf(
                listOf(164.81, 196.00, 246.94), // Em
                listOf(261.63, 329.63, 392.00), // C
                listOf(196.00, 246.94, 293.66), // G
                listOf(146.83, 185.00, 220.00)  // D
            )
            key.contains("D Minör", ignoreCase = true) || key.contains("Dm", ignoreCase = true) -> listOf(
                listOf(146.83, 174.61, 220.00), // Dm
                listOf(233.08, 293.66, 349.23), // Bb
                listOf(174.61, 220.0, 261.63),  // F
                listOf(261.63, 329.63, 392.00)  // C
            )
            else -> listOf(
                listOf(261.63, 329.63, 392.00), // C
                listOf(196.00, 246.94, 293.66), // G
                listOf(220.0, 261.63, 329.63),  // Am
                listOf(174.61, 220.0, 261.63)   // F
            )
        }
    }
}
