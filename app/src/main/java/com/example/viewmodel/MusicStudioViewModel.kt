
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class StudioTab(val title: String) {
    COMPOSE("Beste & Söz"),
    COVER("Cover & Vokal"),
    VIRTUAL_ARTIST("Sanal Sanatçı"),
    VIDEO_CLIP("Video Klip"),
    LIBRARY("Kütüphane")
}

class MusicStudioViewModel(application: Application) :
    AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val geminiService = GeminiMusicService()
    private val eroqService = EroqMusicService()

    val audioEngine = AudioSynthesisEngine()
    val voiceRecorder = UserVoiceRecorder(application)
    val showVoiceRecordingDialog = MutableStateFlow(false)

    // NAVIGATION
    private val _currentTab = MutableStateFlow(StudioTab.COMPOSE)
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(tab: StudioTab) {
        _currentTab.value = tab
    }

    // DATABASE
    val allSongs = db.songDao().getAllSongs()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allArtists = db.virtualArtistDao().getAllArtists()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allStoryboards = db.videoStoryboardDao().getAllStoryboards()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // COMPOSE STATE
    val composeTheme =
        MutableStateFlow("Gece sürüşü, neon ışıklar ve özlem")

    val composeGenre = MutableStateFlow(MusicGenre.POP)
    val composeMood = MutableStateFlow("Enerjik & Melankolik")
    val composeVocalTone =
        MutableStateFlow(PresetVocalTones.tones[4])

    val composeKey = MutableStateFlow("A Minör")
    val composeBpm = MutableStateFlow(124)

    private val _isGeneratingSong = MutableStateFlow(false)
    val isGeneratingSong = _isGeneratingSong.asStateFlow()

    private val _generatedSong =
        MutableStateFlow<SongGenerationResult?>(null)

    val generatedSong = _generatedSong.asStateFlow()

    private val _generatedSongAudioUrl =
        MutableStateFlow<String?>(null)

    val generatedSongAudioUrl =
        _generatedSongAudioUrl.asStateFlow()

    private val _generationMessage = MutableStateFlow("")
    val generationMessage = _generationMessage.asStateFlow()

    // REAL AUDIO STATE
    val isAudioPlaying = audioEngine.isPlaying
    val isAudioPreparing = audioEngine.isPreparing
    val audioPlaybackError = audioEngine.playbackError
    val currentPlaybackTime = audioEngine.currentPlaybackTimeSec
    val totalPlaybackDuration = audioEngine.totalDurationSec
    val visualizerAmplitudes = audioEngine.visualizerAmplitudes

    // COVER STATE
    val coverOriginalTitle =
        MutableStateFlow("Unutabilsem / Gece Masalı")

    val coverTargetGenre =
        MutableStateFlow(MusicGenre.EDM_ELECTRONIC)

    val coverTargetVocal =
        MutableStateFlow(PresetVocalTones.tones[0])

    val coverNotes = MutableStateFlow(
        "Daha yüksek BPM, synthwave basları ve duygusal tiz vokaller"
    )

    private val _isGeneratingCover = MutableStateFlow(false)
    val isGeneratingCover = _isGeneratingCover.asStateFlow()

    private val _generatedCover =
        MutableStateFlow<SongGenerationResult?>(null)

    val generatedCover = _generatedCover.asStateFlow()

    // VIRTUAL ARTIST
    val artistName = MutableStateFlow("AURA-7")
    val artistGenre =
        MutableStateFlow(MusicGenre.EDM_ELECTRONIC)

    val artistGender =
        MutableStateFlow(VocalGender.FEMALE)

    val artistVisualStyle =
        MutableStateFlow("Cyberpunk Hologram")

    private val _isGeneratingArtist = MutableStateFlow(false)
    val isGeneratingArtist = _isGeneratingArtist.asStateFlow()

    private val _generatedArtist =
        MutableStateFlow<ArtistPersonaResult?>(null)

    val generatedArtist = _generatedArtist.asStateFlow()

    // VIDEO
    val videoSelectedSong =
        MutableStateFlow<SongItem?>(null)

    val videoVisualStyle =
        MutableStateFlow("Cyberpunk Neon & Hologram")

    private val _isGeneratingVideo = MutableStateFlow(false)
    val isGeneratingVideo = _isGeneratingVideo.asStateFlow()

    private val _generatedStoryboard =
        MutableStateFlow<VideoStoryboardResult?>(null)

    val generatedStoryboard = _generatedStoryboard.asStateFlow()

    // ACTIVE SONG
    private val _activePlayingSong =
        MutableStateFlow<SongItem?>(null)

    val activePlayingSong =
        _activePlayingSong.asStateFlow()

    init {
        viewModelScope.launch {
            val starterArtist = VirtualArtist(
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

            db.virtualArtistDao().insertArtist(starterArtist)

            val starterArtistMale = VirtualArtist(
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

            db.virtualArtistDao().insertArtist(starterArtistMale)
        }
    }

    // GEMINI LYRICS + EROQ REAL MUSIC
    fun generateSong() {
        if (_isGeneratingSong.value) return

        viewModelScope.launch {
            _isGeneratingSong.value = true
            _generationMessage.value =
                "Şarkı sözleri hazırlanıyor..."

            _generatedSongAudioUrl.value = null
            _generatedSong.value = null

            try {
                val lyricsResult = geminiService.generateSong(
                    theme = composeTheme.value,
                    genre = composeGenre.value.displayName,
                    mood = composeMood.value,
                    vocalGender =
                        composeVocalTone.value.gender.displayName,
                    vocalTone = composeVocalTone.value.name,
                    musicalKey = composeKey.value,
                    tempoBpm = composeBpm.value
                )

                _generatedSong.value = lyricsResult

                _generationMessage.value =
                    "Eroq gerçek müzik ve vokal üretiyor..."

                val selectedVocal = composeVocalTone.value

                val style = buildString {
                    append("Turkish language, ")
                    append(selectedVocal.gender.displayName)
                    append(", ")
                    append(selectedVocal.name)
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

                val prompt = buildString {
                    append("Türkçe ")
                    append(composeGenre.value.displayName)
                    append(" şarkısı. ")
                    append(composeTheme.value)
                    append(". Ruh hali: ")
                    append(composeMood.value)
                    append(". ")
                    append(selectedVocal.gender.displayName)
                    append(" vokal, doğal Türkçe telaffuz, ")
                    append(composeBpm.value)
                    append(" BPM.")
                }

                // Bu çağrı kullanıcı uygulamada üretim başlattığında
                // Eroq kredisi kullanabilir.
                val result = eroqService.generateMusicAndWait(
                    prompt = prompt,
                    style = style,
                    lyrics = lyricsResult.lyrics,
                    title = lyricsResult.title,
                    seconds = 60,
                    instrumental = false
                )

                val audioUrl =
                    result.optString("audioUrl", "")

                if (
                    audioUrl.isBlank() ||
                    !audioUrl.startsWith("http")
                ) {
                    throw IllegalStateException(
                        "Eroq MP3 bağlantısı döndürmedi."
                    )
                }

                _generatedSongAudioUrl.value = audioUrl

                val finalTitle = result.optString(
                    "title",
                    lyricsResult.title
                )

                _generatedSong.value =
                    lyricsResult.copy(title = finalTitle)

                _generationMessage.value =
                    "Gerçek şarkı hazır. Dinleyebilir veya kaydedebilirsin."

            } catch (e: Exception) {
                _generationMessage.value =
                    "Üretim hatası: ${e.message ?: "Bilinmeyen hata"}"
            } finally {
                _isGeneratingSong.value = false
            }
        }
    }

    // GENERATED SONG PLAYBACK
    fun playGeneratedSong() {
        val url = _generatedSongAudioUrl.value

        if (url.isNullOrBlank()) {
            _generationMessage.value =
                "Önce gerçek şarkı üretmelisin."
            return
        }

        val song = _generatedSong.value

        if (song == null) {
            _generationMessage.value =
                "Şarkı bilgileri bulunamadı."
            return
        }

        audioEngine.playUrl(
            url = url,
            bpm = composeBpm.value
        )

        _generationMessage.value =
            "Gerçek MP3 oynatılıyor."
    }

    // SAVE GENERATED SONG
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
                durationSeconds =
                    if (_generatedSongAudioUrl.value != null) {
                        60
                    } else {
                        null
                    }
            )

            db.songDao().insertSong(item)

            _activePlayingSong.value = item

            _generationMessage.value =
                "Şarkı kütüphaneye kaydedildi."
        }
    }

    // COVER GENERATION
    fun generateCover() {
        if (_isGeneratingCover.value) return

        viewModelScope.launch {
            _isGeneratingCover.value = true

            try {
                val result = geminiService.generateCover(
                    originalSongTitle = coverOriginalTitle.value,
                    targetGenre = coverTargetGenre.value.displayName,
                    targetVocalGender =
                        coverTargetVocal.value.gender.displayName,
                    targetVocalTone = coverTargetVocal.value.name,
                    coverStyleNotes = coverNotes.value
                )

                _generatedCover.value = result

            } catch (e: Exception) {
                _generationMessage.value =
                    "Cover hazırlama hatası: ${e.message}"
            } finally {
                _isGeneratingCover.value = false
            }
        }
    }

    fun saveGeneratedCover() {
        val cover = _generatedCover.value ?: return

        viewModelScope.launch {
            val item = SongItem(
                title = cover.title,
                lyrics = cover.lyrics,
                genre = coverTargetGenre.value.displayName,
                mood = "Cover Yeniden Yorumu",
                tempoBpm = 126,
                musicalKey = "E Minör",
                chordProgression = cover.chordProgression,
                vocalToneName = coverTargetVocal.value.name,
                vocalGender = coverTargetVocal.value.gender.name,
                coverSourceSong = coverOriginalTitle.value,
                isCover = true
            )

            db.songDao().insertSong(item)
            _activePlayingSong.value = item
        }
    }

    // ARTIST GENERATION
    fun generateArtist() {
        if (_isGeneratingArtist.value) return

        viewModelScope.launch {
            _isGeneratingArtist.value = true

            try {
                val result = geminiService.generateArtistPersona(
                    name = artistName.value,
                    genre = artistGenre.value.displayName,
                    gender = artistGender.value.displayName,
                    visualStyle = artistVisualStyle.value
                )

                _generatedArtist.value = result

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
            val avatarRes =
                if (artistGender.value == VocalGender.FEMALE) {
                    "img_virtual_female_artist"
                } else {
                    "img_virtual_male_artist"
                }

            val item = VirtualArtist(
                name = artistName.value,
                stageTitle = artist.stageTitle,
                genre = artistGenre.value.displayName,
                gender = artistGender.value.name,
                bio = artist.bio,
                visualStyle = artistVisualStyle.value,
                signatureLook = artist.signatureLook,
                vocalToneId =
                    if (artistGender.value == VocalGender.FEMALE) {
                        "female_elif"
                    } else {
                        "male_kaan"
                    },
                avatarDrawableRes = avatarRes
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
                val result =
                    geminiService.generateVideoStoryboard(
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
                            put(
                                "visualDescription",
                                scene.visualDescription
                            )
                            put("lyricSnippet", scene.lyricSnippet)
                            put("lightingVibe", scene.lightingVibe)
                        })
                    }
                }.toString()

                val storyboard = VideoStoryboard(
                    songId = song.id,
                    songTitle = song.title,
                    visualStyle = videoVisualStyle.value,
                    conceptTitle = result.conceptTitle,
                    scenesJson = scenesJson
                )

                db.videoStoryboardDao()
                    .insertStoryboard(storyboard)

            } catch (e: Exception) {
                _generationMessage.value =
                    "Video senaryosu hatası: ${e.message}"
            } finally {
                _isGeneratingVideo.value = false
            }
        }
    }

    // REAL MP3 PLAYBACK FROM LIBRARY
    fun playSongAudio(song: SongItem) {
        _activePlayingSong.value = song

        val localPath = song.audioFilePath
        val remoteUrl = song.audioUrl

        when {
            !localPath.isNullOrBlank() -> {
                audioEngine.playFile(
                    filePath = localPath,
                    bpm = song.tempoBpm
                )
            }

            !remoteUrl.isNullOrBlank() -> {
                audioEngine.playUrl(
                    url = remoteUrl,
                    bpm = song.tempoBpm
                )
            }

            else -> {
                playLegacySyntheticSong(song)
            }
        }
    }

    private fun playLegacySyntheticSong(song: SongItem) {
        val isCustomUserVoice =
            song.vocalToneName.contains(
                "Kendi Sesim",
                true
            ) ||
            song.vocalToneName.contains(
                "user_own_voice",
                true
            )

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

    // PLAY / PAUSE
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
            val isCustomUserVoice =
                composeVocalTone.value.id == "user_own_voice"

            audioEngine.playTrack(
                genre = composeGenre.value.displayName,
                musicalKey = composeKey.value,
                isFemaleVocal =
                    composeVocalTone.value.gender ==
                        VocalGender.FEMALE,
                isCustomUserVoice = isCustomUserVoice,
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

    // DELETE SONG
    fun deleteSong(song: SongItem) {
        viewModelScope.launch {
            db.songDao().deleteSong(song)

            if (_activePlayingSong.value?.id == song.id) {
                audioEngine.stopTrack()
                _activePlayingSong.value = null
            }
        }
    }

    // DELETE ARTIST
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
