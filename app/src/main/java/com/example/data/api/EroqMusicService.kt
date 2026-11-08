
package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class EroqMusicService {

    companion object {
        private const val BASE_URL =
            "https://musecraft-08ec.onrender.com"

        private const val GENERATE_ENDPOINT =
            "$BASE_URL/api/music/generate"

        private const val JSON_MEDIA_TYPE =
            "application/json; charset=utf-8"

        private const val POLL_INTERVAL_MS = 4000L
        private const val MAX_POLL_ATTEMPTS = 120
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateMusic(
        prompt: String,
        style: String,
        lyrics: String,
        title: String,
        seconds: Int = 60,
        instrumental: Boolean = false
    ): JSONObject = withContext(Dispatchers.IO) {

        require(seconds in 30..240 && seconds % 30 == 0) {
            "Şarkı süresi 30 ile 240 saniye arasında ve 30'un katı olmalı."
        }

        val requestJson = JSONObject().apply {
            put("prompt", prompt)
            put("style", style)
            put("lyrics", lyrics)
            put("title", title)
            put("seconds", seconds)
            put("instrumental", instrumental)
        }

        val body = requestJson.toString()
            .toRequestBody(JSON_MEDIA_TYPE.toMediaType())

        val request = Request.Builder()
            .url(GENERATE_ENDPOINT)
            .post(body)
            .header("Accept", "application/json")
            .build()

        executeRequest(request)
    }

    suspend fun getMusicStatus(
        jobId: String
    ): JSONObject = withContext(Dispatchers.IO) {

        require(jobId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            "Geçersiz müzik iş kimliği."
        }

        val url =
            "$BASE_URL/api/music/status/${jobId}"

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        executeRequest(request)
    }

    suspend fun generateMusicAndWait(
        prompt: String,
        style: String,
        lyrics: String,
        title: String,
        seconds: Int = 60,
        instrumental: Boolean = false
    ): JSONObject {

        val generation = generateMusic(
            prompt = prompt,
            style = style,
            lyrics = lyrics,
            title = title,
            seconds = seconds,
            instrumental = instrumental
        )

        val jobs = generation.optJSONArray("jobs")
            ?: throw IOException(
                "Eroq sunucusu müzik iş listesini döndürmedi."
            )

        if (jobs.length() == 0) {
            throw IOException(
                "Eroq sunucusundan müzik iş kimliği alınamadı."
            )
        }

        val firstJob = jobs.optJSONObject(0)
            ?: throw IOException("Müzik iş bilgisi okunamadı.")

        val jobId = firstJob.optString("id", "")

        if (jobId.isBlank()) {
            throw IOException("Müzik iş kimliği boş geldi.")
        }

        repeat(MAX_POLL_ATTEMPTS) {

            delay(POLL_INTERVAL_MS)

            val statusResult = getMusicStatus(jobId)
            val status = statusResult
                .optString("status", "")
                .lowercase()

            when (status) {

                "succeeded", "completed", "success" -> {
                    val audioUrl = extractAudioUrl(
                        statusResult.optJSONArray("data")
                    )

                    if (audioUrl.isBlank()) {
                        throw IOException(
                            "Şarkı hazırlandı ancak MP3 bağlantısı bulunamadı."
                        )
                    }

                    return statusResult.put("audioUrl", audioUrl)
                }

                "failed", "error", "cancelled", "canceled" -> {
                    val error = statusResult.optJSONObject("error")
                        ?.optString("message")
                        ?: statusResult.optString(
                            "error",
                            "Eroq müzik üretimi başarısız oldu."
                        )

                    throw IOException(error)
                }
            }
        }

        throw IOException(
            "Müzik üretimi beklenen sürede tamamlanmadı. Daha sonra tekrar kontrol et."
        )
    }

    private fun extractAudioUrl(data: JSONArray?): String {

        if (data == null || data.length() == 0) {
            return ""
        }

        for (index in 0 until data.length()) {

            val item = data.optJSONObject(index) ?: continue

            val possibleUrlKeys = listOf(
                "url",
                "audio_url",
                "audioUrl",
                "mp3_url",
                "download_url",
                "downloadUrl"
            )

            for (key in possibleUrlKeys) {
                val url = item.optString(key, "")

                if (
                    url.startsWith("https://") ||
                    url.startsWith("http://")
                ) {
                    return url
                }
            }
        }

        return ""
    }

    private fun executeRequest(
        request: Request
    ): JSONObject {

        client.newCall(request).execute().use { response ->

            val responseText = response.body?.string().orEmpty()

            val json = try {
                JSONObject(responseText)
            } catch (exception: Exception) {
                throw IOException(
                    "Sunucudan geçerli JSON yanıtı alınamadı.",
                    exception
                )
            }

            if (!response.isSuccessful || !json.optBoolean("ok", false)) {

                val error = json.optString(
                    "error",
                    "Müzik sunucusu isteği başarısız oldu."
                )

                throw IOException(
                    "HTTP ${response.code}: $error"
                )
            }

            return json
        }
    }
}
