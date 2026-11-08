package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSynthesisEngine
import com.example.audio.UserVoiceRecorder
import com.example.data.api.ArtistPersonaResult
import com.example.data.api.EroqMusicService
import com.example.data.api.GeminiMusicService
import com.example.data.api.SongGenerationResult
import com.example.data.api.VideoStoryboardResult
import com.example.data.db.AppDatabase
import com.example.model.MusicGenre
import com.example.model.PresetVocalTones
import com.example.model.SongItem
import com.example.model.VideoStoryboard
import com.example.model.VirtualArtist
import com.example.model.VocalGender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class StudioTab(val title: String) {
    COMPOSE("Beste & Söz"),
    COVER("Cover & Vokal"),
    VIRTUAL_ARTIST("Sanal Sanatçı"),
    VIDEO_CLIP("Video Klip"),
    LIBRARY("Kütüphane")
}

class MusicStudioViewModel(application: Application) :
    AndroidViewModel(application) {

    private val app = application
    private val db = AppDatabase.getInstance(application)
    private val geminiService = GeminiMusicService()
    private val eroqService = EroqMusicService()

    val audioEngine = AudioSynthesisEngine(application)
    val voiceRecorder = UserVoiceRecorder(application)
    val showVoiceRecordingDialog = MutableStateFlow(false)

    private val _currentTab = MutableStateFlow(StudioTab.COMPOSE)
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(tab: StudioTab) {
        _currentTab.value = tab
    }

    val allSongs = db.songDao().getAllSongs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allArtists = db.virtualArtistDao().getAllArtists().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allStoryboards = db.videoStoryboardDao().getAllStoryboards().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // COMPOSE
    val composeTheme = MutableStateFlow("Gece sürüşü, neon ışıklar ve özlem")
    val composeGenre = MutableStateFlow(MusicGenre.POP)
    val composeMood = MutableStateFlow("Enerjik & Melankolik")
    val composeVocalTone = MutableStateFlow(PresetVocalTones.tones[4])
    val composeKey = MutableStateFlow("A Minör")
    val composeBpm = MutableStateFlow(124)

    private val _isGeneratingSong = MutableStateFlow(false)
    val isGeneratingSong = _isGeneratingSong.asStateFlow()

    private val _generatedSong = MutableStateFlow<SongGenerationResult?>(null)
    val generatedSong = _generatedSong.asStateFlow()

    private val _generatedSongAudioUrl = MutableStateFlow<String?>(null)
    val generatedSongAudioUrl = _generatedSongAudioUrl.asStateFlow()

    private val _generationMessage = MutableStateFlow("")
    val generationMessage = _generationMessage.asStateFlow()

    // AUDIO
    val isAudioPlaying = audioEngine.isPlaying
    val isAudioPreparing = audioEngine.isPreparing
    val audioPlaybackError = audioEngine.playbackError
    val currentPlaybackTime = audioEngine.currentPlaybackTimeSec
    val totalPlaybackDuration = audioEngine.totalDurationSec
    val visualizerAmplitudes = audioEngine.visualizerAmplitudes

    // DOWNLOAD
    private val _downloadingSongId = MutableStateFlow<String?>(null)
    val downloadingSongId = _downloadingSongId.asStateFlow()

    private val _downloadMessage = MutableStateFlow("")
    val downloadMessage = _downloadMessage.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress = _downloadProgress.asStateFlow()

    // COVER
    val coverOriginalTitle = MutableStateFlow("Unutabilsem / Gece Masalı")
    val coverTargetGenre = MutableStateFlow(MusicGenre.EDM_ELECTRONIC)
    val coverTargetVocal = MutableStateFlow(PresetVocalTones.tones[0])
    val coverNotes = MutableStateFlow(
        "Daha yüksek BPM, synthwave basları ve duygusal tiz vokaller"
    )

    private val _isGeneratingCover = MutableStateFlow(false)
    val isGeneratingCover = _isGeneratingCover.asStateFlow()

    private val _generatedCover = MutableStateFlow<SongGenerationResult?>(null)
    val generatedCover = _generatedCover.asStateFlow()

    private val _generatedCoverAudioUrl = MutableStateFlow<String?>(null)
    val generatedCoverAudioUrl = _generatedCoverAudioUrl.asStateFlow()

    private val _coverMessage = MutableStateFlow("")
    val coverMessage = _coverMessage.asStateFlow()

    // VIRTUAL ARTIST
    val artistName = MutableStateFlow("AURA-7")
    val artistGenre = MutableStateFlow(MusicGenre.EDM_ELECTRONIC)
    val artistGender = MutableStateFlow(VocalGender.FEMALE)
    val artistVisualStyle = MutableStateFlow("Cyberpunk Hologram")

    private val _isGeneratingArtist = MutableStateFlow(false)
    val isGeneratingArtist = _isGeneratingArtist.asStateFlow()

    private val _generatedArtist = MutableStateFlow<ArtistPersonaResult?>(null)
    val generatedArtist = _generatedArtist.asStateFlow()

    // VIDEO
    val videoSelectedSong = MutableStateFlow<SongItem?>(null)
    val videoVisualStyle = MutableStateFlow("Cyberpunk Neon & Hologram")

    private val _isGeneratingVideo = MutableStateFlow(false)
    val isGeneratingVideo = _isGeneratingVideo.asStateFlow()

    private val _generatedStoryboard =
        MutableStateFlow<VideoStoryboardResult?>(null)

    val generatedStoryboard = _generatedStoryboard.asStateFlow()

    // ACTIVE SONG
    private val _activePlayingSong = MutableStateFlow<SongItem?>(null)
    val activePlayingSong = _activePlayingSong.asStateFlow()

    init {
        viewModelScope.launch {
            db.virtualArtistDao().insertArtist(
                VirtualArtist(
                    name = "AURA-7",
                    stageTitle = "Cyber Pop Idol",
                    genre = "EDM & Synthwave",
                    gender = "FEMALE",
                    bio = "AURA-7, yapay zeka ile insan duygularının kesişim noktasında parlayan yeni nesil bir sanal pop yıldızıdır.",
                    visualStyle = "Cyberpunk Hologram",
                    signatureLook = "Neon saçlar, holografik kulaklık ve parıltılı sahne kostümleri.",
                    vocalToneId = "female_elif",
                    avatarDrawableRes = "img_virtual_female_artist"
                )
            )

            db.virtualArtistDao().insertArtist(
                VirtualArtist(
                    name = "KRONOS",
                    stageTitle = "Anatolian Rock & Grunt",
                    genre = "Rock & Anadolu Rock",
                    gender = "MALE",
                    bio = "KRONOS, geleneksel Anadolu tınılarını fütüristik synthesizer ve güçlü rock vokalleriyle harmanlayan sanal bir müzisyendir.",
                    visualStyle = "Futuristic Rocker",
                    signatureLook = "Deri ceket, neon gitar ve cyber gözlükler.",
                    vocalToneId = "male_kaan",
                    avatarDrawableRes = "img_virtual_male_artist"
                )
            )
        }
    }

    // GEMINI LYRICS ONLY
    fun generateSong() {
        if (_isGeneratingSong.value) return

        viewModelScope.launch {
            _isGeneratingSong.value = true
            _generationMessage.value =
                "Gemini şarkı sözlerini ve aranjeyi hazırlıyor..."

            _generatedSongAudioUrl.value = null
            _generatedSong.value = null

            try {
                val result = geminiService.generateSong(
                    theme = composeTheme.value,
                    genre = composeGenre.value.displayName,
                    mood = composeMood.value,
                    vocalGender = composeVocalTone.value.gender.displayName,
                    vocalTone = composeVocalTone.value.name,
                    musicalKey = composeKey.value,
                    tempoBpm = composeBpm.value
                )

                _generatedSong.value = result
                _generationMessage.value =
                    "Şarkı sözleri hazır. Gerçek müzik için ikinci üretim adımını kullan."
            } catch (e: Exception) {
                _generationMessage.value =
                    "Şarkı sözü hazırlama hatası: ${e.message ?: "Bilinmeyen hata"}"
            } finally {
                _isGeneratingSong.value = false
            }
        }
    }

    // REAL SONG WITH EROQ
    fun generateRealSong() {
        if (_isGeneratingSong.value) return

        val lyricsResult = _generatedSong.value
        if (lyricsResult == null) {
            _generationMessage.value = "Önce şarkı sözlerini oluşturmalısın."
            return
        }

        if (!_generatedSongAudioUrl.value.isNullOrBlank()) {
            _generationMessage.value = "Bu şarkının MP3 dosyası zaten hazır."
            return
        }

        viewModelScope.launch {
            _isGeneratingSong.value = true
            _generationMessage.value = "Eroq Music One gerçek müziği üretiyor..."

            try {
                val vocal = composeVocalTone.value
                val style = buildString {
                    append("Turkish language, ")
                    append(vocal.gender.displayName)
                    append(", ")
                    append(vocal.name)
                    append(", ")
                    append(composeGenre.value.displayName)
                    append(", ")
                    append(composeMood.value)
                    append(", ")
                    append(composeBpm.value)
                    append(" BPM, ")
                    append(composeKey.value)
                    append(", professional studio production, ")
                    append(lyricsResult.instrumentation)
                    append(", natural Turkish pronunciation")
                }

                val prompt = "Türkçe ${composeGenre.value.displayName} şarkısı. " +
                    "${composeTheme.value}. Ruh hali: ${composeMood.value}. " +
                    "${vocal.gender.displayName} vokal, ${vocal.name}, " +
                    "doğal Türkçe telaffuz, ${composeBpm.value} BPM."

                val result = eroqService.generateMusicAndWait(
                    prompt = prompt,
                    style = style,
                    lyrics = lyricsResult.lyrics,
                    title = lyricsResult.title,
                    seconds = 60,
                    instrumental = false
                )

                val audioUrl = result.optString("audioUrl", "")
                require(audioUrl.startsWith("http")) {
                    "Eroq geçerli MP3 bağlantısı döndürmedi."
                }

                _generatedSongAudioUrl.value = audioUrl
                _generatedSong.value = lyricsResult.copy(
                    title = result.optString("title", lyricsResult.title)
                )
                _generationMessage.value = "Gerçek şarkı hazır. Dinleyebilir ve kaydedebilirsin."
            } catch (e: Exception) {
                _generationMessage.value =
                    "Gerçek müzik üretim hatası: ${e.message ?: "Bilinmeyen hata"}"
            } finally {
                _isGeneratingSong.value = false
            }
        }
    }

    fun playGeneratedSong() {
        val url = _generatedSongAudioUrl.value
        if (url.isNullOrBlank()) {
            _generationMessage.value = "Önce gerçek şarkı üretmelisin."
            return
        }

        audioEngine.playUrl(url = url, bpm = composeBpm.value)
        _generationMessage.value = "Gerçek MP3 oynatılıyor."
    }

    fun saveGeneratedSong(
        artistId: String? = null,
        artistName: String? = null
    ) {
        val song = _generatedSong.value ?: return

        viewModelScope.launch {
            val item = SongItem(
                title = song.title,
                lyrics = song.lyrics,
                genre = composeGenre.value.displayName,
                mood = composeMood.value,
                tempoBpm = composeBpm.value,
                musicalKey = composeKey.value,
                chordProgression = song.chordProgression,
                vocalToneName = composeVocalTone.value.name,
                vocalGender = composeVocalTone.value.gender.name,
                virtualArtistId = artistId,
                virtualArtistName = artistName,
                isCover = false,
                audioUrl = _generatedSongAudioUrl.value,
                audioFilePath = null,
                durationSeconds = if (_generatedSongAudioUrl.value != null) 60 else null
            )

            db.songDao().insertSong(item)
            _activePlayingSong.value = item
            _generationMessage.value =
                if (_generatedSongAudioUrl.value != null) {
                    "Gerçek şarkı kütüphaneye kaydedildi."
                } else {
                    "Şarkı sözleri kaydedildi; MP3 henüz üretilmedi."
                }
        }
    }

    fun downloadGeneratedSong() {
        val song = _generatedSong.value ?: run {
            _downloadMessage.value = "İndirilecek şarkı bulunamadı."
            return
        }

        val url = _generatedSongAudioUrl.value
        if (url.isNullOrBlank()) {
            _downloadMessage.value = "Önce gerçek müzik üretmelisin."
            return
        }

        if (_downloadingSongId.value != null) return

        viewModelScope.launch {
            _downloadingSongId.value = "generated_song"
            _downloadProgress.value = 0
            _downloadMessage.value = "MP3 indiriliyor..."

            try {
                val file = downloadMp3ToPhone(url, song.title)
                val item = SongItem(
                    title = song.title,
                    lyrics = song.lyrics,
                    genre = composeGenre.value.displayName,
                    mood = composeMood.value,
                    tempoBpm = composeBpm.value,
                    musicalKey = composeKey.value,
                    chordProgression = song.chordProgression,
                    vocalToneName = composeVocalTone.value.name,
                    vocalGender = composeVocalTone.value.gender.name,
                    isCover = false,
                    audioUrl = url,
                    audioFilePath = file.absolutePath,
                    durationSeconds = 60
                )

                db.songDao().insertSong(item)
                _activePlayingSong.value = item
                _downloadProgress.value = 100
                _downloadMessage.value = "Şarkı indirildi ve kütüphaneye kaydedildi."
            } catch (e: Exception) {
                _downloadMessage.value =
                    "İndirme hatası: ${e.message ?: "Bilinmeyen hata"}"
            } finally {
                _downloadingSongId.value = null
            }
        }
    }

    fun downloadSong(song: SongItem) {
        if (!song.audioFilePath.isNullOrBlank() &&
            File(song.audioFilePath).exists()
        ) {
            _downloadMessage.value = "Bu şarkı zaten telefona indirilmiş."
            return
        }

        val url = song.audioUrl
        if (url.isNullOrBlank()) {
            _downloadMessage.value = "Bu şarkının indirilebilir MP3 bağlantısı yok."
            return
        }

        if (_downloadingSongId.value != null) return

        viewModelScope.launch {
            _downloadingSongId.value = song.id
            _downloadProgress.value = 0
            _downloadMessage.value = "${song.title} indiriliyor..."

            try {
                val file = downloadMp3ToPhone(url, song.title)
                val updatedSong = song.copy(audioFilePath = file.absolutePath)
                db.songDao().updateSong(updatedSong)

                if (_activePlayingSong.value?.id == song.id) {
                    _activePlayingSong.value = updatedSong
                }

                _downloadProgress.value = 100
                _downloadMessage.value = "${song.title} telefona indirildi."
            } catch (e: Exception) {
                _downloadMessage.value =
                    "İndirme hatası: ${e.message ?: "Bilinmeyen hata"}"
            } finally {
                _downloadingSongId.value = null
            }
        }
    }

    private suspend fun downloadMp3ToPhone(
        url: String,
        title: String
    ): File = withContext(Dispatchers.IO) {
        val musicFolder = File(app.filesDir, "Music/MuseCraft")

        if (!musicFolder.exists() && !musicFolder.mkdirs()) {
            throw IllegalStateException("MuseCraft müzik klasörü oluşturulamadı.")
        }

        val safeTitle = title
            .replace(Regex("[^a-zA-Z0-9ğüşöçıİĞÜŞÖÇ _-]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(70)
            .ifBlank { "MuseCraft_Sarki" }

        var outputFile = File(musicFolder, "$safeTitle.mp3")
        if (outputFile.exists()) {
            outputFile = File(
                musicFolder,
                "${safeTitle}_${System.currentTimeMillis()}.mp3"
            )
        }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30000
            readTimeout = 60000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "MuseCraft Android")
        }

        try {
            connection.connect()
            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                throw IllegalStateException("Sunucu indirme hatası: HTTP $responseCode")
            }

            val contentType = connection.contentType.orEmpty()
            if (contentType.contains("text/html", true) ||
                contentType.contains("application/json", true)
            ) {
                throw IllegalStateException("Sunucu MP3 yerine farklı içerik gönderdi.")
            }

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                }
            }

            if (!outputFile.exists() || outputFile.length() < 1024) {
                outputFile.delete()
                throw IllegalStateException("İndirilen MP3 dosyası boş veya geçersiz.")
            }

            outputFile
        } catch (e: Exception) {
            outputFile.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    // COVER: GEMINI ONLY - EROQ KULLANILMAZ
    fun generateCover() {
        if (_isGeneratingCover.value) return

        if (coverTargetVocal.value.id == "user_own_voice") {
            _coverMessage.value =
                "Kendi sesin seçili. Bu bölüm şu anda yalnızca söz ve aranje metni oluşturur."
            return
        }

        viewModelScope.launch {
            _isGeneratingCover.value = true
            _coverMessage.value =
                "Gemini şarkı sözlerini ve aranje notunu hazırlıyor..."

            _generatedCover.value = null
            _generatedCoverAudioUrl.value = null

            try {
                val selectedVocal = coverTargetVocal.value
                val selectedGenre = coverTargetGenre.value
                val originalTitle = coverOriginalTitle.value.trim()

                require(originalTitle.isNotBlank()) {
                    "Lütfen cover yapılacak şarkının adını veya temasını yaz."
                }

                val cover = geminiService.generateCover(
                    originalSongTitle = originalTitle,
                    targetGenre = selectedGenre.displayName,
                    targetVocalGender = selectedVocal.gender.displayName,
                    targetVocalTone = selectedVocal.name,
                    coverStyleNotes = coverNotes.value
                )

                _generatedCover.value = cover
                _coverMessage.value =
                    "Cover sözleri ve aranje notu hazır. Eroq kullanılmadı; kredi harcanmadı. Bu bölüm MP3 üretmez."

            } catch (e: Exception) {
                _coverMessage.value =
                    "Cover sözleri oluşturma hatası: ${e.message ?: "Bilinmeyen hata"}"
            } finally {
                _isGeneratingCover.value = false
            }
        }
    }

    fun playGeneratedCover() {
        _coverMessage.value =
            "Gemini Cover bölümü MP3 üretmez veya oynatmaz."
    }

    fun saveGeneratedCover() {
        val cover = _generatedCover.value ?: run {
            _coverMessage.value =
                "Kaydedilecek cover sözleri bulunamadı."
            return
        }

        viewModelScope.launch {
            try {
                val item = SongItem(
                    title = cover.title,
                    lyrics = cover.lyrics,
                    genre = coverTargetGenre.value.displayName,
                    mood = "Cover Sözleri",
                    tempoBpm = 126,
                    musicalKey = "E Minör",
                    chordProgression = cover.chordProgression,
                    vocalToneName = coverTargetVocal.value.name,
                    vocalGender = coverTargetVocal.value.gender.name,
                    coverSourceSong = coverOriginalTitle.value,
                    isCover = true,
                    audioUrl = null,
                    audioFilePath = null,
                    durationSeconds = null
                )

                db.songDao().insertSong(item)
                _activePlayingSong.value = item

                _coverMessage.value =
                    "Cover sözleri kütüphaneye kaydedildi. MP3 oluşturulmadı."

            } catch (e: Exception) {
                _coverMessage.value =
                    "Cover kaydetme hatası: ${e.message ?: "Bilinmeyen hata"}"
            }
        }
    }

    // ARTIST
    fun generateArtist() {
        if (_isGeneratingArtist.value) return

        viewModelScope.launch {
            _isGeneratingArtist.value = true

            try {
                _generatedArtist.value = geminiService.generateArtistPersona(
                    name = artistName.value,
                    genre = artistGenre.value.displayName,
                    gender = artistGender.value.displayName,
                    visualStyle = artistVisualStyle.value
                )
            } catch (e: Exception) {
                _generationMessage.value =
                    "Sanatçı oluşturma hatası: ${e.message}"
            } finally {
                _isGeneratingArtist.value = false
            }
        }
    }

    fun saveGeneratedArtist() {
        val artist = _generatedArtist.value ?: return

        viewModelScope.launch {
            val female = artistGender.value == VocalGender.FEMALE
            val item = VirtualArtist(
                name = artistName.value,
                stageTitle = artist.stageTitle,
                genre = artistGenre.value.displayName,
                gender = artistGender.value.name,
                bio = artist.bio,
                visualStyle = artistVisualStyle.value,
                signatureLook = artist.signatureLook,
                vocalToneId = if (female) "female_elif" else "male_kaan",
                avatarDrawableRes = if (female) {
                    "img_virtual_female_artist"
                } else {
                    "img_virtual_male_artist"
                }
            )

            db.virtualArtistDao().insertArtist(item)
        }
    }

    // VIDEO STORYBOARD
    fun generateVideoStoryboard(song: SongItem) {
        if (_isGeneratingVideo.value) return

        viewModelScope.launch {
            _isGeneratingVideo.value = true
            videoSelectedSong.value = song

            try {
                val result = geminiService.generateVideoStoryboard(
                    songTitle = song.title,
                    genre = song.genre,
                    lyrics = song.lyrics,
                    visualStyle = videoVisualStyle.value
                )

                _generatedStoryboard.value = result

                val scenesJson = JSONArray().apply {
                    result.scenes.forEach { scene ->
                        put(JSONObject().apply {
                            put("sceneNumber", scene.sceneNumber)
                            put("timeCode", scene.timeCode)
                            put("sectionName", scene.sectionName)
                            put("cameraAngle", scene.cameraAngle)
                            put("visualDescription", scene.visualDescription)
                            put("lyricSnippet", scene.lyricSnippet)
                            put("lightingVibe", scene.lightingVibe)
                        })
                    }
                }.toString()

                db.videoStoryboardDao().insertStoryboard(
                    VideoStoryboard(
                        songId = song.id,
                        songTitle = song.title,
                        visualStyle = videoVisualStyle.value,
                        conceptTitle = result.conceptTitle,
                        scenesJson = scenesJson
                    )
                )
            } catch (e: Exception) {
                _generationMessage.value =
                    "Video senaryosu hatası: ${e.message}"
            } finally {
                _isGeneratingVideo.value = false
            }
        }
    }

    // PLAYBACK
    fun playSongAudio(song: SongItem) {
        _activePlayingSong.value = song

        val localPath = song.audioFilePath
        val remoteUrl = song.audioUrl

        when {
            !localPath.isNullOrBlank() && File(localPath).exists() -> {
                audioEngine.playFile(localPath, song.tempoBpm)
            }
            !remoteUrl.isNullOrBlank() -> {
                audioEngine.playUrl(remoteUrl, song.tempoBpm)
            }
            else -> playLegacySyntheticSong(song)
        }
    }

    private fun playLegacySyntheticSong(song: SongItem) {
        val isCustomUserVoice =
            song.vocalToneName.contains("Kendi Sesim", true) ||
            song.vocalToneName.contains("user_own_voice", true)

        val isFemale =
            song.vocalGender == "FEMALE" ||
            song.vocalToneName.contains("Kadın", true) ||
            song.vocalToneName.contains("Elif", true) ||
            song.vocalToneName.contains("Selin", true) ||
            song.vocalToneName.contains("Derya", true) ||
            song.vocalToneName.contains("Zeynep", true)

        audioEngine.playTrack(
            genre = song.genre,
            musicalKey = song.musicalKey,
            isFemaleVocal = isFemale,
            isCustomUserVoice = isCustomUserVoice,
            bpm = song.tempoBpm
        )
    }

    fun togglePlayback() {
        if (audioEngine.isPreparing.value) return

        if (audioEngine.isPlaying.value) {
            audioEngine.pauseTrack()
            return
        }

        val song = _activePlayingSong.value
        if (song != null) {
            playSongAudio(song)
        } else {
            val customVoice = composeVocalTone.value.id == "user_own_voice"
            audioEngine.playTrack(
                genre = composeGenre.value.displayName,
                musicalKey = composeKey.value,
                isFemaleVocal = composeVocalTone.value.gender == VocalGender.FEMALE,
                isCustomUserVoice = customVoice,
                bpm = composeBpm.value
            )
        }
    }

    fun resumePlayback() {
        audioEngine.resumeTrack()
    }

    fun pausePlayback() {
        audioEngine.pauseTrack()
    }

    fun stopPlayback() {
        audioEngine.stopTrack()
    }

    fun deleteSong(song: SongItem) {
        viewModelScope.launch {
            db.songDao().deleteSong(song)
            if (_activePlayingSong.value?.id == song.id) {
                audioEngine.stopTrack()
                _activePlayingSong.value = null
            }
        }
    }

    fun deleteArtist(artist: VirtualArtist) {
        viewModelScope.launch {
            db.virtualArtistDao().deleteArtist(artist)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopTrack()
        voiceRecorder.stopRecording()
        voiceRecorder.stopPlayingSample()
    }
}
