package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
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
    val file: File?,
    val uri: Uri?,
    val createdAt: Long,
    val durationSeconds: Int
)

class UserVoiceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var amplitudeJob: Job? = null

    private var currentRecordingFile: File? = null
    private var currentRecordingUri: Uri? = null
    private var currentRecordingPfd: ParcelFileDescriptor? = null
    private var currentRecordingType: VoiceRecordingType? = null

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val datasetDirectory =
        File(context.filesDir, "voice_dataset")

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

    // Eski sürümle uyumluluk için korunur.
    val recordingFile: File by lazy {
        File(context.filesDir, "user_voice_model_sample.m4a")
    }

    private fun relativePath(type: VoiceRecordingType): String {
        return Environment.DIRECTORY_MUSIC +
            "/MuseCraft/VoiceProfile/${type.folderName}/"
    }

    private fun getPrivateDirectory(type: VoiceRecordingType): File {
        return File(datasetDirectory, type.folderName).apply {
            if (!exists()) mkdirs()
        }
    }

    init {
        migrateLegacyRecordings()
        refreshRecordings()
    }

    // Eski uygulama sürümündeki kayıtları yeni arşive taşır.
    // Kopyalama doğrulanmadan eski dosya silinmez.
    private fun migrateLegacyRecordings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                VoiceRecordingType.values().forEach { type ->

                    val oldDirectory = getPrivateDirectory(type)

                    oldDirectory.listFiles()
                        ?.filter {
                            it.isFile &&
                                it.extension.equals("m4a", true) &&
                                it.length() > 0L
                        }
                        ?.forEach { oldFile ->
                            copyLegacyFileToMediaStore(oldFile, type)
                        }
                }

                if (recordingFile.exists() && recordingFile.length() > 0L) {
                    copyLegacyFileToMediaStore(
                        recordingFile,
                        VoiceRecordingType.SPEECH
                    )
                }

            } else {
                if (recordingFile.exists() && recordingFile.length() > 0L) {
                    val destination = File(
                        getPrivateDirectory(VoiceRecordingType.SPEECH),
                        "speech_legacy_${System.currentTimeMillis()}.m4a"
                    )

                    recordingFile.copyTo(destination, overwrite = false)

                    if (destination.exists() &&
                        destination.length() == recordingFile.length()
                    ) {
                        recordingFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyLegacyFileToMediaStore(
        source: File,
        type: VoiceRecordingType
    ) {
        val values = ContentValues().apply {
            put(
                MediaStore.Audio.Media.DISPLAY_NAME,
                "${type.folderName}_migrated_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a"
            )
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath(type))
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return

        try {
            val copiedBytes = context.contentResolver
                .openOutputStream(uri, "w")
                ?.use { output ->
                    source.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: 0L

            if (copiedBytes > 0L && copiedBytes == source.length()) {

                val readyValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }

                context.contentResolver.update(
                    uri,
                    readyValues,
                    null,
                    null
                )

                source.delete()

            } else {
                context.contentResolver.delete(uri, null, null)
            }

        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            e.printStackTrace()
        }
    }

    fun refreshRecordings() {
        val result = mutableListOf<VoiceRecordingItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            VoiceRecordingType.values().forEach { type ->

                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.DURATION
                )

                val selection =
                    "${MediaStore.Audio.Media.RELATIVE_PATH} = ?"

                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    arrayOf(relativePath(type)),
                    "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                )?.use { cursor ->

                    val idColumn = cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media._ID
                    )

                    val dateColumn = cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.DATE_ADDED
                    )

                    val durationColumn = cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.DURATION
                    )

                    while (cursor.moveToNext()) {

                        val mediaId = cursor.getLong(idColumn)

                        val uri = Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            mediaId.toString()
                        )

                        result.add(
                            VoiceRecordingItem(
                                id = uri.toString(),
                                type = type,
                                file = null,
                                uri = uri,
                                createdAt = cursor.getLong(dateColumn) * 1000L,
                                durationSeconds =
                                    (cursor.getLong(durationColumn) / 1000L).toInt()
                            )
                        )
                    }
                }
            }

        } else {

            VoiceRecordingType.values().forEach { type ->

                getPrivateDirectory(type).listFiles()
                    ?.filter {
                        it.isFile &&
                            it.extension.equals("m4a", true) &&
                            it.length() > 0L
                    }
                    ?.forEach { file ->

                        result.add(
                            VoiceRecordingItem(
                                id = file.name,
                                type = type,
                                file = file,
                                uri = null,
                                createdAt = file.lastModified(),
                                durationSeconds = getDuration(file)
                            )
                        )
                    }
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

            val fileName =
                "${type.folderName}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.m4a"

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath(type))
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: throw IllegalStateException("Kayıt dosyası oluşturulamadı")

                val pfd = context.contentResolver.openFileDescriptor(uri, "w")
                    ?: throw IllegalStateException("Kayıt dosyası açılamadı")

                currentRecordingUri = uri
                currentRecordingPfd = pfd

                recorder.setOutputFile(pfd.fileDescriptor)

            } else {

                val outputFile = File(
                    getPrivateDirectory(type),
                    fileName
                )

                currentRecordingFile = outputFile
                recorder.setOutputFile(outputFile.absolutePath)
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentRecordingType = type

            _isRecording.value = true
            _recordingDurationSec.value = 0
            _currentAmplitude.value = 0f

            amplitudeJob = scope.launch {
                var tick = 0

                while (isActive && _isRecording.value) {
                    delay(100)

                    val amplitude = try {
                        ((mediaRecorder?.maxAmplitude ?: 0) / 32767f)
                            .coerceIn(0.08f, 1f)
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

            try {
                currentRecordingPfd?.close()
            } catch (_: Exception) {
            }

            currentRecordingPfd = null

            currentRecordingUri?.let {
                context.contentResolver.delete(it, null, null)
            }

            currentRecordingUri = null
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

        var stoppedSuccessfully = false

        try {
            mediaRecorder?.stop()
            stoppedSuccessfully = true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {

            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
            }

            mediaRecorder = null

            try {
                currentRecordingPfd?.close()
            } catch (_: Exception) {
            }

            currentRecordingPfd = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                currentRecordingUri?.let { uri ->

                    if (stoppedSuccessfully) {

                        val values = ContentValues().apply {
                            put(MediaStore.Audio.Media.IS_PENDING, 0)
                        }

                        context.contentResolver.update(
                            uri,
                            values,
                            null,
                            null
                        )

                    } else {
                        context.contentResolver.delete(uri, null, null)
                    }
                }

            } else if (!stoppedSuccessfully) {
                currentRecordingFile?.delete()
            }

            currentRecordingUri = null
            currentRecordingFile = null
            currentRecordingType = null

            _isRecording.value = false
            _currentAmplitude.value = 0f

            refreshRecordings()
        }
    }

    fun playRecording(
        item: VoiceRecordingItem,
        onFinished: (() -> Unit)? = null
    ) {
        if (item.file == null && item.uri == null) return

        stopPlayingSample()

        try {
            mediaPlayer = MediaPlayer().apply {

                if (item.uri != null) {
                    setDataSource(context, item.uri)
                } else {
                    setDataSource(item.file!!.absolutePath)
                }

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
            val deleted = if (item.uri != null) {
                context.contentResolver.delete(item.uri, null, null) > 0
            } else {
                item.file?.delete() == true
            }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val selection =
                "${MediaStore.Audio.Media.RELATIVE_PATH} = ?"

            context.contentResolver.delete(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                selection,
                arrayOf(relativePath(type))
            )

        } else {

            getPrivateDirectory(type).listFiles()?.forEach {
                if (it.isFile) it.delete()
            }
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
            deleteRecording(latestRecording)
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
