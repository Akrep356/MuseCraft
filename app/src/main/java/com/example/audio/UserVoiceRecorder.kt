package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class UserVoiceRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec = _recordingDurationSec.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude = _currentAmplitude.asStateFlow()

    private val _hasRecording = MutableStateFlow(false)
    val hasRecording = _hasRecording.asStateFlow()

    private val _isPlayingSample = MutableStateFlow(false)
    val isPlayingSample = _isPlayingSample.asStateFlow()

    val recordingFile: File by lazy {
        File(context.filesDir, "user_voice_model_sample.m4a")
    }

    init {
        _hasRecording.value = recordingFile.exists() && recordingFile.length() > 0
    }

    fun startRecording(): Boolean {
        try {
            stopPlayingSample()
            if (recordingFile.exists()) {
                recordingFile.delete()
            }

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(recordingFile.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            _recordingDurationSec.value = 0

            amplitudeJob = scope.launch {
                var tick = 0
                while (isActive && _isRecording.value) {
                    delay(100)
                    val amp = try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        (maxAmp / 32767f).coerceIn(0.08f, 1.0f)
                    } catch (_: Exception) {
                        0.15f
                    }
                    _currentAmplitude.value = amp
                    tick++
                    if (tick % 10 == 0) {
                        _recordingDurationSec.value = tick / 10
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _isRecording.value = false
            return false
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        try {
            amplitudeJob?.cancel()
            amplitudeJob = null
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            _currentAmplitude.value = 0f
            _hasRecording.value = recordingFile.exists() && recordingFile.length() > 0
        }
    }

    fun playRecordedSample(onFinished: (() -> Unit)? = null) {
        if (!recordingFile.exists() || recordingFile.length() == 0L) return
        stopPlayingSample()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(recordingFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlayingSample.value = false
                    onFinished?.invoke()
                }
                start()
            }
            _isPlayingSample.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlayingSample.value = false
        }
    }

    fun stopPlayingSample() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlayingSample.value = false
    }

    fun clearRecording() {
        stopPlayingSample()
        stopRecording()
        if (recordingFile.exists()) {
            recordingFile.delete()
        }
        _hasRecording.value = false
        _recordingDurationSec.value = 0
    }
}
