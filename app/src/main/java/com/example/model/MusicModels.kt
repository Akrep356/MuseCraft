package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MusicGenre(val displayName: String, val emoji: String, val bpmRange: IntRange, val defaultScale: String) {
    POP("Pop & Dance", "✨", 115..128, "C Majör"),
    ROCK("Rock & Anadolu Rock", "🎸", 120..145, "E Minör"),
    RAP_HIPHOP("Rap & Hip-Hop", "🎤", 85..100, "D Minör"),
    ANATOLIAN_ROCK("Anadolu Rock", "🪕", 110..130, "A Minör"),
    RNB_SOUL("R&B & Soul", "🎷", 75..95, "F Minör"),
    EDM_ELECTRONIC("EDM & Synthwave", "⚡", 124..132, "A Minör"),
    ARABESK_POP("Arabesk Pop", "🎻", 80..105, "Hicaz / Dm"),
    ACOUSTIC_FOLK("Akustik & Folk", "🪗", 70..95, "G Majör"),
    LOFI_CHILL("Lofi & Chill", "☕", 70..85, "C# Minör"),
    METAL("Modern Metal", "⚡", 130..170, "Drop D / Dm")
}

enum class VocalGender(val displayName: String) {
    MALE("Erkek Vokal"),
    FEMALE("Kadın Vokal"),
    DUET("Erkek & Kadın Düet")
}

data class VocalTone(
    val id: String,
    val name: String,
    val gender: VocalGender,
    val description: String,
    val tag: String,
    val recommendedGenres: List<MusicGenre>
)

object PresetVocalTones {
    val userCustomTone = VocalTone(
        id = "user_own_voice",
        name = "Kendi Sesim (AI Klonu)",
        gender = VocalGender.DUET,
        description = "Mikrofondan kaydettiğin ses örneğinle eğitilen özel AI vokal",
        tag = "Kendi Sesin 🎙️",
        recommendedGenres = MusicGenre.values().toList()
    )

    val tones = listOf(
        userCustomTone,
        VocalTone(
            id = "male_emre",
            name = "Emre - Kadife R&B",
            gender = VocalGender.MALE,
            description = "Pürüzsüz, yumuşak tizler, duygusal modern Pop ve R&B tınısı",
            tag = "Kadife & Akıcı",
            recommendedGenres = listOf(MusicGenre.POP, MusicGenre.RNB_SOUL, MusicGenre.LOFI_CHILL)
        ),
        VocalTone(
            id = "male_kaan",
            name = "Kaan - Rock & Anadolu",
            gender = VocalGender.MALE,
            description = "Güçlü göğüs sesi, yırtıcı tını ve Anadolu rock tınılı vokaller",
            tag = "Güçlü & Yırtıcı",
            recommendedGenres = listOf(MusicGenre.ROCK, MusicGenre.ANATOLIAN_ROCK, MusicGenre.METAL)
        ),
        VocalTone(
            id = "male_burak",
            name = "Burak - Trap & Flow",
            gender = VocalGender.MALE,
            description = "Hızlı artikülasyon, ritmik punchline ve modern autotune",
            tag = "Seri Flow & Autotune",
            recommendedGenres = listOf(MusicGenre.RAP_HIPHOP, MusicGenre.EDM_ELECTRONIC)
        ),
        VocalTone(
            id = "male_cem",
            name = "Cem - Akustik Bariton",
            gender = VocalGender.MALE,
            description = "Sıcak, tok bariton tonlar, samimi akustik ve folk hissi",
            tag = "Tok Bariton",
            recommendedGenres = listOf(MusicGenre.ACOUSTIC_FOLK, MusicGenre.ARABESK_POP)
        ),
        VocalTone(
            id = "female_elif",
            name = "Elif - Cyber Pop Diva",
            gender = VocalGender.FEMALE,
            description = "Geniş oktav aralığı, parlak tizler ve enerjik sahne vokali",
            tag = "Parlak Diva",
            recommendedGenres = listOf(MusicGenre.POP, MusicGenre.EDM_ELECTRONIC)
        ),
        VocalTone(
            id = "female_selin",
            name = "Selin - Indie Dream & Whisper",
            gender = VocalGender.FEMALE,
            description = "Buğulu, nefesli, atmosferik ve derin duygusal tınılar",
            tag = "Ethereal & Nefesli",
            recommendedGenres = listOf(MusicGenre.LOFI_CHILL, MusicGenre.ACOUSTIC_FOLK, MusicGenre.RNB_SOUL)
        ),
        VocalTone(
            id = "female_derya",
            name = "Derya - Güçlü Soul & Belt",
            gender = VocalGender.FEMALE,
            description = "Dramatik dinamikler, kuvvetli vibrato ve soul gücü",
            tag = "Dramatik Soul",
            recommendedGenres = listOf(MusicGenre.RNB_SOUL, MusicGenre.ROCK, MusicGenre.ARABESK_POP)
        ),
        VocalTone(
            id = "female_zeynep",
            name = "Zeynep - Duru Akustik",
            gender = VocalGender.FEMALE,
            description = "Duru, berrak, naif ve melodik halk müziği/akustik vokal",
            tag = "Duru & Naif",
            recommendedGenres = listOf(MusicGenre.ACOUSTIC_FOLK, MusicGenre.POP)
        )
    )
}

@Entity(tableName = "songs")
data class SongItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val lyrics: String,
    val genre: String,
    val mood: String,
    val tempoBpm: Int = 120,
    val musicalKey: String = "A Minör",
    val chordProgression: String = "Am - F - C - G",
    val vocalToneName: String = "Elif - Cyber Pop Diva",
    val vocalGender: String = "FEMALE",
    val virtualArtistId: String? = null,
    val virtualArtistName: String? = null,
    val coverSourceSong: String? = null,
    val isCover: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "virtual_artists")
data class VirtualArtist(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val stageTitle: String,
    val genre: String,
    val gender: String, // MALE, FEMALE
    val bio: String,
    val visualStyle: String, // Cyberpunk, Anime Pop, Vintage Rock, Aesthetic Street
    val signatureLook: String,
    val vocalToneId: String,
    val avatarDrawableRes: String = "img_virtual_female_artist", // resource name
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "video_storyboards")
data class VideoStoryboard(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val songId: String,
    val songTitle: String,
    val visualStyle: String,
    val conceptTitle: String,
    val scenesJson: String, // JSON list of scenes
    val createdAt: Long = System.currentTimeMillis()
)

data class VideoScene(
    val sceneNumber: Int,
    val timeCode: String,
    val sectionName: String, // "Giriş / Intro", "Nakarat / Chorus", etc.
    val cameraAngle: String,
    val visualDescription: String,
    val lyricSnippet: String,
    val lightingVibe: String
)
