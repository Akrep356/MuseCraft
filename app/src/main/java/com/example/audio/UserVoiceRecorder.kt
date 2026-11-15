package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class VoiceRecordingType(
    val folderName: String,
    val displayName: String
) {
    SPEECH("speech", "Türkçe Konuşma"),
    SINGING("singing", "Şarkı Söyleme")
}

data class VoiceRecordingItem(
    val id: String,
    val type: VoiceRecordingType,
    val file: File,
    val createdAt: Long,
    val durationSeconds: Int
)

class UserVoiceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var amplitudeJob: Job? = null

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val datasetDirectory =
        File(context.filesDir, "voice_dataset")

    private var currentRecordingFile: File? = null
    private var currentRecordingType: VoiceRecordingType? = null

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

    private val _recordings =
        MutableStateFlow<List<VoiceRecordingItem>>(emptyList())
    val recordings = _recordings.asStateFlow()

    private val _isPlayingRecordingId =
        MutableStateFlow<String?>(null)
    val isPlayingRecordingId = _isPlayingRecordingId.asStateFlow()

    // Eski ekranlarla uyumluluk için korunmuştur.
    val recordingFile: File by lazy {
        File(context.filesDir, "user_voice_model_sample.m4a")
    }

    init {
        migrateLegacyRecording()
        refreshRecordings()
    }

    private fun getDirectory(type: VoiceRecordingType): File {
        return File(datasetDirectory, type.folderName).apply {
            if (!exists()) mkdirs()
        }
    }

    private fun migrateLegacyRecording() {
        try {
            if (!recordingFile.exists() || recordingFile.length() == 0L) {
                return
            }

            val speechDirectory = getDirectory(VoiceRecordingType.SPEECH)

            val destination = File(
                speechDirectory,
                "speech_legacy_${System.currentTimeMillis()}.m4a"
            )

            if (recordingFile.copyTo(destination, overwrite = false).exists()) {
                recordingFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshRecordings() {
        val result = mutableListOf<VoiceRecordingItem>()

        VoiceRecordingType.values().forEach { type ->
            val directory = getDirectory(type)

            directory.listFiles()
                ?.filter {
                    it.isFile &&
                    it.extension.equals("m4a", ignoreCase = true) &&
                    it.length() > 0L
                }
                ?.forEach { file ->
                    result.add(
                        VoiceRecordingItem(
                            id = file.name,
                            type = type,
                            file = file,
                            createdAt = file.lastModified(),
                            durationSeconds = getDuration(file)
                        )
                    )
                }
        }

        _recordings.value = result.sortedByDescending { it.createdAt }
        _hasRecording.value = result.isNotEmpty()
    }

    private fun getDuration(file: File): Int {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(file.absolutePath)

            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            (duration / 1000L).toInt()
        } catch (e: Exception) {
            0
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    fun startRecording(): Boolean {
        return startRecording(VoiceRecordingType.SPEECH)
    }

    fun startRecording(type: VoiceRecordingType): Boolean {
        if (_isRecording.value) return false

        try {
            stopPlayingSample()

            val directory = getDirectory(type)
            val fileName =
                "${type.folderName}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.m4a"

            val outputFile = File(directory, fileName)

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentRecordingFile = outputFile
            currentRecordingType = type

            _isRecording.value = true
            _recordingDurationSec.value = 0
            _currentAmplitude.value = 0f

            amplitudeJob = scope.launch {
                var tick = 0

                while (isActive && _isRecording.value) {
                    delay(100)

                    val amplitude = try {
                        val maxAmplitude =
                            mediaRecorder?.maxAmplitude ?: 0

                        (maxAmplitude / 32767f)
                            .coerceIn(0.08f, 1.0f)
                    } catch (_: Exception) {
                        0.15f
                    }

                    _currentAmplitude.value = amplitude
                    tick++

                    if (tick % 10 == 0) {
                        _recordingDurationSec.value = tick / 10
                    }
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()

            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
            }

            mediaRecorder = null
            currentRecordingFile?.delete()
            currentRecordingFile = null
            currentRecordingType = null
            _isRecording.value = false

            return false
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return

        amplitudeJob?.cancel()
        amplitudeJob = null

        val outputFile = currentRecordingFile

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
            outputFile?.delete()
        } finally {
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
            }

            mediaRecorder = null
            currentRecordingFile = null
            currentRecordingType = null

            _isRecording.value = false
            _currentAmplitude.value = 0f

            if (outputFile == null ||
                !outputFile.exists() ||
                outputFile.length() == 0L
            ) {
                outputFile?.delete()
            }

            refreshRecordings()
        }
    }

    fun playRecording(
        item: VoiceRecordingItem,
        onFinished: (() -> Unit)? = null
    ) {
        if (!item.file.exists() || item.file.length() == 0L) return

        stopPlayingSample()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(item.file.absolutePath)

                setOnCompletionListener {
                    _isPlayingSample.value = false
                    _isPlayingRecordingId.value = null

                    try {
                        release()
                    } catch (_: Exception) {
                    }

                    mediaPlayer = null
                    onFinished?.invoke()
                }

                prepare()
                start()
            }

            _isPlayingSample.value = true
            _isPlayingRecordingId.value = item.id
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayingSample()
        }
    }

    fun playRecordedSample(onFinished: (() -> Unit)? = null) {
        val latestRecording = _recordings.value.firstOrNull()

        if (latestRecording != null) {
            playRecording(latestRecording, onFinished)
            return
        }

        // Eski kayıt dosyası varsa eski ekranlar da oynatabilir.
        if (!recordingFile.exists() || recordingFile.length() == 0L) {
            return
        }

        stopPlayingSample()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(recordingFile.absolutePath)

                setOnCompletionListener {
                    _isPlayingSample.value = false
                    _isPlayingRecordingId.value = null

                    try {
                        release()
                    } catch (_: Exception) {
                    }

                    mediaPlayer = null
                    onFinished?.invoke()
                }

                prepare()
                start()
            }

            _isPlayingSample.value = true
            _isPlayingRecordingId.value = "legacy"
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayingSample()
        }
    }

    fun stopPlayingSample() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }

        mediaPlayer = null
        _isPlayingSample.value = false
        _isPlayingRecordingId.value = null
    }

    fun deleteRecording(item: VoiceRecordingItem): Boolean {
        if (_isPlayingRecordingId.value == item.id) {
            stopPlayingSample()
        }

        return try {
            val deleted = item.file.delete()
            refreshRecordings()
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteRecordings(type: VoiceRecordingType) {
        if (_isPlayingRecordingId.value != null) {
            val playing = _recordings.value.firstOrNull {
                it.id == _isPlayingRecordingId.value
            }

            if (playing?.type == type) {
                stopPlayingSample()
            }
        }

        try {
            getDirectory(type).listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        refreshRecordings()
    }

    fun clearRecording() {
        stopPlayingSample()

        if (_isRecording.value) {
            stopRecording()
        }

        val latestRecording = _recordings.value.firstOrNull()

        if (latestRecording != null) {
            latestRecording.file.delete()
        } else if (recordingFile.exists()) {
            recordingFile.delete()
        }

        _recordingDurationSec.value = 0
        refreshRecordings()
    }

    fun getRecordings(
        type: VoiceRecordingType
    ): List<VoiceRecordingItem> {
        return _recordings.value.filter { it.type == type }
    }

    fun getRecordingCount(
        type: VoiceRecordingType
    ): Int {
        return _recordings.value.count { it.type == type }
    }

    fun release() {
        stopPlayingSample()

        if (_isRecording.value) {
            stopRecording()
        }

        amplitudeJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }
}
