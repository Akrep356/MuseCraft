package com.example.data.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiMusicService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelName = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

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
            Aşağıdaki parametrelere göre Türkçe (veya uygun tarzda) tam ve profesyonel bir şarkı bestele ve sözlerini yaz:
            
            - Konu / Tema: $theme
            - Müzik Türü / Stili: $genre
            - Ruh Hali / Mood: $mood
            - Vokal Tipi: $vocalGender ($vocalTone)
            ${if (vocalTone.contains("Kendi Sesim", true)) "- Özel Vokal Direktifi: Kullanıcının kendi ses modeli icra edecektir; sözler ve vokal yönlendirmesi kişisel, samimi ve doğal bir şarkı söyleme üslubuna göre hazırlanmalıdır." else ""}
            - Müzik Tonu (Key): $musicalKey
            - Tempo (BPM): $tempoBpm
            
            Lütfen yanıtı SADECE aşağıdaki JSON formatında ver:
            {
                "title": "Şarkı Adı",
                "chordProgression": "Am - F - C - G",
                "instrumentation": "Synthesizer, 808 Bass, Elektro Gitar, Akustik Davul",
                "vocalDirection": "Duygusal, nefesli ve nakaratta patlayan güçlü vokal",
                "lyrics": "[Giriş / Intro]\\n(Akustik gitar ve synth pad)...\\n\\n[Bölüm 1 / Verse 1]\\n[Am] Gecenin karanlığı çökerken şehre\\n[F] Bir melodi yankılanır içimde...\\n\\n[Nakarat / Chorus]\\n[C] Kalbimin ritmi sana vurur\\n[G] Bu şarkı sonsuzluğa yürür...\\n\\n[Bölüm 2 / Verse 2]\\n...\\n\\n[Köprü / Bridge]\\n...\\n\\n[Çıkış / Outro]\\n..."
            }
        """.trimIndent()

        try {
            val jsonResponse = callGeminiApi(prompt)
            val jsonText = extractResponseText(jsonResponse)
            val cleaned = cleanJsonString(jsonText)
            val json = JSONObject(cleaned)

            SongGenerationResult(
                title = json.optString("title", "AI Bestesi"),
                chordProgression = json.optString("chordProgression", "Am - F - C - G"),
                instrumentation = json.optString("instrumentation", "Synthesizer, Davul, Bas"),
                vocalDirection = json.optString("vocalDirection", "Dinamik stüdyo vokali"),
                lyrics = json.optString("lyrics", createDefaultLyrics(theme, genre))
            )
        } catch (e: Exception) {
            SongGenerationResult(
                title = if (theme.isNotBlank()) theme else "Gece Işıkları",
                chordProgression = "Am - F - C - G",
                instrumentation = "$genre Enstrümanları, Bas, Davul",
                vocalDirection = "$vocalTone ile $mood icra",
                lyrics = createDefaultLyrics(theme, genre)
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
            Sen dahi bir prodüktör ve aranjörsün. "$originalSongTitle" adlı eseri veya temasını tamamen YENİ BİR TARZA ($targetGenre) ve yeni bir vokal yorumuna ($targetVocalGender - $targetVocalTone) COVER olarak uyarla.
            Ek İstekler: $coverStyleNotes
            
            Lütfen yanıtı SADECE aşağıdaki JSON formatında ver:
            {
                "title": "$originalSongTitle (AI $targetGenre Cover)",
                "chordProgression": "Em - C - G - D",
                "instrumentation": "Tarza özel aranje, yeni ritim ve harmoniler",
                "vocalDirection": "Cover yorumuna uygun vokal tonlaması",
                "lyrics": "[Cover Giriş / Intro]\\n...\\n\\n[1. Bölüm]\\n...\\n\\n[Nakarat]\\n...\\n\\n[Outro]\\n..."
            }
        """.trimIndent()

        try {
            val jsonResponse = callGeminiApi(prompt)
            val jsonText = extractResponseText(jsonResponse)
            val cleaned = cleanJsonString(jsonText)
            val json = JSONObject(cleaned)

            SongGenerationResult(
                title = json.optString("title", "$originalSongTitle ($targetGenre Cover)"),
                chordProgression = json.optString("chordProgression", "Em - C - G - D"),
                instrumentation = json.optString("instrumentation", "Yeniden aranje edilmiş enstrümanlar"),
                vocalDirection = json.optString("vocalDirection", "$targetVocalTone stili"),
                lyrics = json.optString("lyrics", createDefaultCoverLyrics(originalSongTitle, targetGenre))
            )
        } catch (e: Exception) {
            SongGenerationResult(
                title = "$originalSongTitle ($targetGenre Cover)",
                chordProgression = "Em - C - G - D",
                instrumentation = "$targetGenre aranjmanı",
                vocalDirection = "$targetVocalTone için özel yorum",
                lyrics = createDefaultCoverLyrics(originalSongTitle, targetGenre)
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
            
            Lütfen yanıtı SADECE aşağıdaki JSON formatında ver:
            {
                "stageTitle": "Karakterin Sahne Ünvanı (örn: Neon Queen, Cyber Soul)",
                "bio": "3-4 cümlelik etkileyici sanatçı geçmişi, müzikal vizyonu ve tarzı",
                "signatureLook": "Giyim stili, saç, aksesuarlar ve sahne aurası",
                "vocalDescription": "Vokal karakteristiği ve sahne performansı enerjisi"
            }
        """.trimIndent()

        try {
            val jsonResponse = callGeminiApi(prompt)
            val jsonText = extractResponseText(jsonResponse)
            val cleaned = cleanJsonString(jsonText)
            val json = JSONObject(cleaned)

            ArtistPersonaResult(
                stageTitle = json.optString("stageTitle", "AI Müzik İdolü"),
                bio = json.optString("bio", "Yapay zeka ve insan duygularını birleştiren yenilikçi $genre sanatçısı."),
                signatureLook = json.optString("signatureLook", "$visualStyle temalı fütüristik sahne kostümleri."),
                vocalDescription = json.optString("vocalDescription", "Büyüleyici ve yüksek frekanslı modern tını.")
            )
        } catch (e: Exception) {
            ArtistPersonaResult(
                stageTitle = "$name • $genre Vision",
                bio = "$name, $genre türünde dijital dünyayla gerçek duyguları harmanlayan fütüristik bir sanal sanatçıdır.",
                signatureLook = "$visualStyle estetiği, parlak neon aksesuarlar ve modern sahne stili.",
                vocalDescription = "Güçlü dinamiklere sahip $gender vokali."
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
            "$songTitle" ($genre) şarkısı için 4-5 sahnelik müzik video klibi storyboard'u ve sahne yönetimi oluştur.
            Görsel Stil: $visualStyle
            Şarkı Sözü / Teması: ${lyrics.take(200)}
            
            Lütfen yanıtı SADECE aşağıdaki JSON formatında ver:
            {
                "conceptTitle": "Klip Konsept Başlığı",
                "scenes": [
                    {
                        "sceneNumber": 1,
                        "timeCode": "00:00 - 00:15",
                        "sectionName": "Giriş / Intro",
                        "cameraAngle": "Geniş Açı & Yavaş Yakınlaşma",
                        "visualDescription": "Karanlık neon ışıklı stüdyoda sanatçının silüeti belirir, synthesizer beat'i ile ışıklar yanıp söner.",
                        "lyricSnippet": "Giriş ritmi...",
                        "lightingVibe": "Derin mor ve neon mavi atmosferik ışıklar"
                    },
                    {
                        "sceneNumber": 2,
                        "timeCode": "00:15 - 00:35",
                        "sectionName": "1. Bölüm / Verse",
                        "cameraAngle": "Orta Plan & Dinamik Omuz Çekimi",
                        "visualDescription": "Sanatçı mikrofona duygusal vokal icra ederken arka planda holografik ses dalgaları yükselir.",
                        "lyricSnippet": "Gecenin karanlığı çökerken şehre...",
                        "lightingVibe": "Parlak neon cyan ve sıcak amber kontur ışığı"
                    },
                    {
                        "sceneNumber": 3,
                        "timeCode": "00:35 - 01:05",
                        "sectionName": "Nakarat / Chorus",
                        "cameraAngle": "360 Derece Dönen Hızlı Kamera & Drone Çekimi",
                        "visualDescription": "Görkemli sahnede patlayan lazerler, ritimle zıplayan synth efektleri ve etkileyici vokal performansı.",
                        "lyricSnippet": "Kalbimin ritmi sana vurur...",
                        "lightingVibe": "Stroboskopik neon magenta ve altın sarısı patlamalar"
                    },
                    {
                        "sceneNumber": 4,
                        "timeCode": "01:05 - 01:25",
                        "sectionName": "Çıkış / Outro",
                        "cameraAngle": "Yukarıdan Yavaşça Uzaklaşan Vinç Açısı",
                        "visualDescription": "Müzik sönümlenirken sanatçı gökyüzündeki dijital yıldızlara bakar ve logo parlar.",
                        "lyricSnippet": "Sonsuzluğa uzanan melodi...",
                        "lightingVibe": "Yumuşak gece mavisi ve kaybolan ışıklar"
                    }
                ]
            }
        """.trimIndent()

        try {
            val jsonResponse = callGeminiApi(prompt)
            val jsonText = extractResponseText(jsonResponse)
            val cleaned = cleanJsonString(jsonText)
            val json = JSONObject(cleaned)

            val concept = json.optString("conceptTitle", "$songTitle - Görsel Müzik Deneyimi")
            val scenesArray = json.optJSONArray("scenes") ?: JSONArray()
            val scenesList = mutableListOf<SceneData>()

            for (i in 0 until scenesArray.length()) {
                val sObj = scenesArray.getJSONObject(i)
                scenesList.add(
                    SceneData(
                        sceneNumber = sObj.optInt("sceneNumber", i + 1),
                        timeCode = sObj.optString("timeCode", "00:${i * 20}"),
                        sectionName = sObj.optString("sectionName", "Sahne ${i + 1}"),
                        cameraAngle = sObj.optString("cameraAngle", "Sinematik Açı"),
                        visualDescription = sObj.optString("visualDescription", "Sanatçı sahnede performans sergiler."),
                        lyricSnippet = sObj.optString("lyricSnippet", "Müzik ritmi..."),
                        lightingVibe = sObj.optString("lightingVibe", "Neon Stüdyo Işıkları")
                    )
                )
            }

            if (scenesList.isEmpty()) {
                scenesList.addAll(createDefaultScenes(songTitle, visualStyle))
            }

            VideoStoryboardResult(conceptTitle = concept, scenes = scenesList)
        } catch (e: Exception) {
            VideoStoryboardResult(
                conceptTitle = "$songTitle - $visualStyle Video Klip",
                scenes = createDefaultScenes(songTitle, visualStyle)
            )
        }
    }

    private fun callGeminiApi(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API key not configured")
        }

        val requestBodyJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
            }
            put("generationConfig", generationConfig)
        }

        val body = requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = "$baseUrl?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Gemini API Error: ${response.code} ${response.message}")
            }
            return response.body?.string() ?: ""
        }
    }

    private fun extractResponseText(jsonResponse: String): String {
        val root = JSONObject(jsonResponse)
        val candidates = root.optJSONArray("candidates") ?: return ""
        val firstCandidate = candidates.optJSONObject(0) ?: return ""
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        val firstPart = parts.optJSONObject(0) ?: return ""
        return firstPart.optString("text", "")
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

    private fun createDefaultLyrics(theme: String, genre: String): String {
        return """
[Giriş / Intro]
(Synthesizer & Ritim girişi)

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

    private fun createDefaultCoverLyrics(originalTitle: String, targetGenre: String): String {
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

    private fun createDefaultScenes(songTitle: String, visualStyle: String): List<SceneData> {
        return listOf(
            SceneData(
                1,
                "00:00 - 00:15",
                "Giriş / Intro",
                "Geniş Açı & Sis Efekti",
                "Karanlık $visualStyle sahnesinde neon lambalar açılır ve atmosferik ritim başlar.",
                "Giriş beat'i yükselir...",
                "Derin mor ve neon mavi ambians"
            ),
            SceneData(
                2,
                "00:15 - 00:35",
                "1. Bölüm / Verse",
                "Orta Plan & Sanatçı Vokal Performansı",
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
