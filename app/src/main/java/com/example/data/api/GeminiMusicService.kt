
package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiMusicService {

    companion object {
        private const val GEMINI_ENDPOINT =
            "https://musecraft-08ec.onrender.com/api/gemini/generate"

        private const val JSON_MEDIA_TYPE =
            "application/json; charset=utf-8"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateSong(
        theme: String,
        genre: String,
        mood: String,
        vocalGender: String,
        vocalTone: String,
        musicalKey: String,
        tempoBpm: Int
    ): SongGenerationResult = withContext(Dispatchers.IO) {

        val prompt = """
            Sen profesyonel bir müzik yapımcısı, besteci ve şarkı yazarısın.
            Aşağıdaki parametrelere göre Türkçe, tam ve profesyonel bir şarkı bestele ve sözlerini yaz:

            - Konu / Tema: $theme
            - Müzik Türü / Stili: $genre
            - Ruh Hali / Mood: $mood
            - Vokal Tipi: $vocalGender ($vocalTone)
            ${if (vocalTone.contains("Kendi Sesim", true)) "- Özel Vokal Direktifi: Kullanıcının kendi ses modeli icra edecektir; sözler ve vokal yönlendirmesi kişisel, samimi ve doğal bir şarkı söyleme üslubuna göre hazırlanmalıdır." else ""}
            - Müzik Tonu (Key): $musicalKey
            - Tempo (BPM): $tempoBpm

            Şarkı sözleri tamamen Türkçe olmalı.
            Türkçe karakterleri doğru kullan.
            Şarkı doğal, anlamlı, kafiyeli ve profesyonel olsun.

            Yanıtı SADECE aşağıdaki JSON formatında ver:
            {
                "title": "Şarkı Adı",
                "chordProgression": "Am - F - C - G",
                "instrumentation": "Synthesizer, 808 Bass, Elektro Gitar, Akustik Davul",
                "vocalDirection": "Duygusal, nefesli ve nakaratta patlayan güçlü vokal",
                "lyrics": "[Giriş / Intro]\n...\n\n[Bölüm 1 / Verse 1]\n...\n\n[Nakarat / Chorus]\n...\n\n[Bölüm 2 / Verse 2]\n...\n\n[Köprü / Bridge]\n...\n\n[Çıkış / Outro]\n..."
            }
        """.trimIndent()

        try {
            val jsonText = cleanJsonString(callGeminiApi(prompt))
            val json = JSONObject(jsonText)

            SongGenerationResult(
                title = json.optString("title", "AI Bestesi"),
                chordProgression = json.optString(
                    "chordProgression",
                    "Am - F - C - G"
                ),
                instrumentation = json.optString(
                    "instrumentation",
                    "Synthesizer, Davul, Bas"
                ),
                vocalDirection = json.optString(
                    "vocalDirection",
                    "Dinamik stüdyo vokali"
                ),
                lyrics = json.optString(
                    "lyrics",
                    createDefaultLyrics(theme, genre)
                )
            )
        } catch (e: Exception) {
            throw IOException(
                "Şarkı sözleri Gemini üzerinden alınamadı: ${e.message}",
                e
            )
        }
    }

    suspend fun generateCover(
        originalSongTitle: String,
        targetGenre: String,
        targetVocalGender: String,
        targetVocalTone: String,
        coverStyleNotes: String
    ): SongGenerationResult = withContext(Dispatchers.IO) {

        val prompt = """
            Sen profesyonel bir prodüktör ve aranjörsün.
            "$originalSongTitle" adlı eseri veya temasını tamamen yeni bir tarza
            ($targetGenre) ve yeni bir vokal yorumuna
            ($targetVocalGender - $targetVocalTone) COVER olarak uyarla.

            Ek İstekler: $coverStyleNotes

            Yanıtı SADECE aşağıdaki JSON formatında ver:
            {
                "title": "$originalSongTitle (AI $targetGenre Cover)",
                "chordProgression": "Em - C - G - D",
                "instrumentation": "Tarza özel aranje, yeni ritim ve harmoniler",
                "vocalDirection": "Cover yorumuna uygun vokal tonlaması",
                "lyrics": "[Cover Giriş / Intro]\n...\n\n[1. Bölüm]\n...\n\n[Nakarat]\n...\n\n[Outro]\n..."
            }
        """.trimIndent()

        try {
            val jsonText = cleanJsonString(callGeminiApi(prompt))
            val json = JSONObject(jsonText)

            SongGenerationResult(
                title = json.optString(
                    "title",
                    "$originalSongTitle ($targetGenre Cover)"
                ),
                chordProgression = json.optString(
                    "chordProgression",
                    "Em - C - G - D"
                ),
                instrumentation = json.optString(
                    "instrumentation",
                    "Yeniden aranje edilmiş enstrümanlar"
                ),
                vocalDirection = json.optString(
                    "vocalDirection",
                    "$targetVocalTone stili"
                ),
                lyrics = json.optString(
                    "lyrics",
                    createDefaultCoverLyrics(
                        originalSongTitle,
                        targetGenre
                    )
                )
            )
        } catch (e: Exception) {
            throw IOException(
                "Cover hazırlama hatası: ${e.message}",
                e
            )
        }
    }

    suspend fun generateArtistPersona(
        name: String,
        genre: String,
        gender: String,
        visualStyle: String
    ): ArtistPersonaResult = withContext(Dispatchers.IO) {

        val prompt = """
            Yeni nesil bir Sanal AI Müzik Sanatçısı / Pop İdolü yarat.

            İsim: $name
            Müzik Türü: $genre
            Cinsiyet: $gender
            Görsel Tarz: $visualStyle

            Yanıtı SADECE aşağıdaki JSON formatında ver:
            {
                "stageTitle": "Karakterin Sahne Ünvanı",
                "bio": "3-4 cümlelik etkileyici sanatçı geçmişi, müzikal vizyonu ve tarzı",
                "signatureLook": "Giyim stili, saç, aksesuarlar ve sahne aurası",
                "vocalDescription": "Vokal karakteristiği ve sahne performansı enerjisi"
            }
        """.trimIndent()

        try {
            val jsonText = cleanJsonString(callGeminiApi(prompt))
            val json = JSONObject(jsonText)

            ArtistPersonaResult(
                stageTitle = json.optString(
                    "stageTitle",
                    "AI Müzik İdolü"
                ),
                bio = json.optString(
                    "bio",
                    "Yapay zeka ve insan duygularını birleştiren yenilikçi $genre sanatçısı."
                ),
                signatureLook = json.optString(
                    "signatureLook",
                    "$visualStyle temalı fütüristik sahne kostümleri."
                ),
                vocalDescription = json.optString(
                    "vocalDescription",
                    "Büyüleyici ve yüksek frekanslı modern tını."
                )
            )
        } catch (e: Exception) {
            throw IOException(
                "Sanatçı oluşturma hatası: ${e.message}",
                e
            )
        }
    }

    suspend fun generateVideoStoryboard(
        songTitle: String,
        genre: String,
        lyrics: String,
        visualStyle: String
    ): VideoStoryboardResult = withContext(Dispatchers.IO) {

        val prompt = """
            "$songTitle" ($genre) şarkısı için 4-5 sahnelik
            profesyonel müzik video klibi storyboard'u oluştur.

            Görsel Stil: $visualStyle
            Şarkı Sözü / Teması: ${lyrics.take(200)}

            Yanıtı SADECE aşağıdaki JSON formatında ver:
            {
                "conceptTitle": "Klip Konsept Başlığı",
                "scenes": [
                    {
                        "sceneNumber": 1,
                        "timeCode": "00:00 - 00:15",
                        "sectionName": "Giriş / Intro",
                        "cameraAngle": "Geniş Açı ve Yavaş Yakınlaşma",
                        "visualDescription": "Karanlık neon ışıklı stüdyoda sanatçının silüeti belirir.",
                        "lyricSnippet": "Giriş ritmi...",
                        "lightingVibe": "Derin mor ve neon mavi atmosferik ışıklar"
                    },
                    {
                        "sceneNumber": 2,
                        "timeCode": "00:15 - 00:35",
                        "sectionName": "1. Bölüm / Verse",
                        "cameraAngle": "Orta Plan",
                        "visualDescription": "Sanatçı mikrofona duygusal vokal icra eder.",
                        "lyricSnippet": "Şarkının ilk sözleri...",
                        "lightingVibe": "Neon cyan ve sıcak amber ışık"
                    },
                    {
                        "sceneNumber": 3,
                        "timeCode": "00:35 - 01:05",
                        "sectionName": "Nakarat / Chorus",
                        "cameraAngle": "360 Derece Dönen Kamera",
                        "visualDescription": "Görkemli sahnede lazerler ve etkileyici vokal performansı.",
                        "lyricSnippet": "Nakarat sözleri...",
                        "lightingVibe": "Neon magenta ve altın sarısı ışıklar"
                    },
                    {
                        "sceneNumber": 4,
                        "timeCode": "01:05 - 01:25",
                        "sectionName": "Çıkış / Outro",
                        "cameraAngle": "Yavaşça Uzaklaşan Kamera",
                        "visualDescription": "Sanatçı gökyüzündeki dijital yıldızlara bakar.",
                        "lyricSnippet": "Son sözler...",
                        "lightingVibe": "Gece mavisi ve yumuşak ışıklar"
                    }
                ]
            }
        """.trimIndent()

        try {
            val jsonText = cleanJsonString(callGeminiApi(prompt))
            val json = JSONObject(jsonText)

            val concept = json.optString(
                "conceptTitle",
                "$songTitle - Görsel Müzik Deneyimi"
            )

            val scenesArray = json.optJSONArray("scenes")
                ?: JSONArray()

            val scenesList = mutableListOf<SceneData>()

            for (i in 0 until scenesArray.length()) {
                val scene = scenesArray.optJSONObject(i) ?: continue

                scenesList.add(
                    SceneData(
                        sceneNumber = scene.optInt("sceneNumber", i + 1),
                        timeCode = scene.optString(
                            "timeCode",
                            "00:${i * 20}"
                        ),
                        sectionName = scene.optString(
                            "sectionName",
                            "Sahne ${i + 1}"
                        ),
                        cameraAngle = scene.optString(
                            "cameraAngle",
                            "Sinematik Açı"
                        ),
                        visualDescription = scene.optString(
                            "visualDescription",
                            "Sanatçı sahnede performans sergiler."
                        ),
                        lyricSnippet = scene.optString(
                            "lyricSnippet",
                            "Müzik ritmi..."
                        ),
                        lightingVibe = scene.optString(
                            "lightingVibe",
                            "Neon Stüdyo Işıkları"
                        )
                    )
                )
            }

            if (scenesList.isEmpty()) {
                scenesList.addAll(
                    createDefaultScenes(songTitle, visualStyle)
                )
            }

            VideoStoryboardResult(
                conceptTitle = concept,
                scenes = scenesList
            )
        } catch (e: Exception) {
            throw IOException(
                "Video senaryosu oluşturulamadı: ${e.message}",
                e
            )
        }
    }

    private fun callGeminiApi(prompt: String): String {

        val requestJson = JSONObject().apply {
            put("prompt", prompt)
        }

        val body = requestJson.toString()
            .toRequestBody(JSON_MEDIA_TYPE.toMediaType())

        val request = Request.Builder()
            .url(GEMINI_ENDPOINT)
            .post(body)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->

            val responseText = response.body?.string().orEmpty()

            val json = try {
                JSONObject(responseText)
            } catch (e: Exception) {
                throw IOException(
                    "Render sunucusundan geçerli JSON yanıtı alınamadı.",
                    e
                )
            }

            if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                throw IOException(
                    "Gemini / Render hatası: HTTP ${response.code}: " +
                        json.optString(
                            "error",
                            "Bilinmeyen sunucu hatası."
                        )
                )
            }

            val generatedText = json.optString("text", "").trim()

            if (generatedText.isBlank()) {
                throw IOException(
                    "Render üzerinden Gemini boş yanıt döndürdü."
                )
            }

            return generatedText
        }
    }

    private fun cleanJsonString(raw: String): String {
        var text = raw.trim()

        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        }

        if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }

        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }

        return text.trim()
    }

    private fun createDefaultLyrics(
        theme: String,
        genre: String
    ): String {
        return """
[Giriş / Intro]
(Synthesizer ve ritim girişi)

[Bölüm 1 / Verse 1]
[Am] Gece yarısı sokaklar sessiz
[F] İçimde bir ses durmaksızın söyler
[C] $theme rüzgarı eserken bu şehre
[G] Adımlarım sana doğru yönelir

[Ön Nakarat / Pre-Chorus]
[Dm] Beklemek zor, zaman akıp giderken
[G] Bir ışık belirir karanlık çökerken

[Nakarat / Chorus]
[Am] $genre ritmiyle yanar bu yürek
[F] Sana ulaşmak için bir umut gerek
[C] Gökyüzüne yankılanır sesimiz
[G] Sonsuza kadar sürecek bu iz!

[Bölüm 2 / Verse 2]
[Am] Işıklar parıldar gözlerinde yine
[F] Bir melodi dokunur kalbime
[C] Her notada saklı büyük bir gizem
[G] Bu şarkıyla yeniden başlarım ben

[Köprü / Bridge]
[F] Ne fırtınalar kopsa da durmayız
[G] Biz bu sahnede birlikte parlarız!

[Çıkış / Outro]
[Am] $theme fısıldar rüzgara...
[F] Melodi yavaşça sönümlenir...
[C] [G] [Am]
        """.trimIndent()
    }

    private fun createDefaultCoverLyrics(
        originalTitle: String,
        targetGenre: String
    ): String {
        return """
[Cover Giriş / Intro - $targetGenre Tarzı]
(Yeniden yorumlanmış modern ritim ve bas hatları)

[1. Bölüm / Verse]
[Em] Bilindik bir şarkının yeni nefesi
[C] $originalTitle esintisi yankılanır
[G] $targetGenre harmonileriyle donatılmış
[D] Her nota yeniden can bulur

[Nakarat / Chorus]
[Em] Bu cover senin için baştan yazıldı
[C] Vokaller sahneye bambaşka yayıldı
[G] Ritim hızlandı, duygular derinleşti
[D] Eski hatıralar yeni sound'la birleşti!

[Çıkış / Outro]
[Em] [C] [G] [D]
($targetGenre tarzında solo ile sonlanır)
        """.trimIndent()
    }

    private fun createDefaultScenes(
        songTitle: String,
        visualStyle: String
    ): List<SceneData> {
        return listOf(
            SceneData(
                1,
                "00:00 - 00:15",
                "Giriş / Intro",
                "Geniş Açı ve Sis Efekti",
                "Karanlık $visualStyle sahnesinde neon lambalar açılır ve atmosferik ritim başlar.",
                "Giriş beat'i yükselir...",
                "Derin mor ve neon mavi ambiyans"
            ),
            SceneData(
                2,
                "00:15 - 00:35",
                "1. Bölüm / Verse",
                "Orta Plan ve Sanatçı Vokal Performansı",
                "Sanatçı mikrofonda duygusal performans sergilerken arkada holografik ses dalgaları belirir.",
                "Sözler ekranda parlar...",
                "Cyan ve amber kontur aydınlatması"
            ),
            SceneData(
                3,
                "00:35 - 01:05",
                "Nakarat / Chorus",
                "360 Derece Dinamik Kamera",
                "Patlayan lazer gösterileri, yüksek enerjili vokal performansı ve ritmik ışık senkronu.",
                "Nakarat doruk noktasına ulaşır!",
                "Stroboskopik neon magenta ve altın ışıklar"
            ),
            SceneData(
                4,
                "01:05 - 01:25",
                "Çıkış / Outro",
                "Sinematik Yavaş Kapanış",
                "Müzik ve sahne ışıkları yavaşça sönerken logonun parıltısı sahnede kalır.",
                "Melodi usulca sona erer...",
                "Sıcak gece mavisi tonları"
            )
        )
    }
}

data class SongGenerationResult(
    val title: String,
    val chordProgression: String,
    val instrumentation: String,
    val vocalDirection: String,
    val lyrics: String
)

data class ArtistPersonaResult(
    val stageTitle: String,
    val bio: String,
    val signatureLook: String,
    val vocalDescription: String
)

data class VideoStoryboardResult(
    val conceptTitle: String,
    val scenes: List<SceneData>
)

data class SceneData(
    val sceneNumber: Int,
    val timeCode: String,
    val sectionName: String,
    val cameraAngle: String,
    val visualDescription: String,
    val lyricSnippet: String,
    val lightingVibe: String
)
