
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

        private const val POLL_INTERVAL_MS = 5000L
        private const val MAX_POLL_ATTEMPTS = 60
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

        val request = Request.Builder()
            .url("$BASE_URL/api/music/status/$jobId")
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

        val jobId = extractJobId(generation)
            ?: throw IOException(
                "Eroq üretim isteğini kabul etti ancak iş kimliği bulunamadı. Yanıt: $generation"
            )

        repeat(MAX_POLL_ATTEMPTS) {

            delay(POLL_INTERVAL_MS)

            val statusResult = getMusicStatus(jobId)
            val status = statusResult.optString("status", "")
                .lowercase()

            val audioUrl = findAudioUrl(statusResult)

            if (!audioUrl.isNullOrBlank()) {
                return JSONObject().apply {
                    put("ok", true)
                    put("id", jobId)
                    put("status", status)
                    put("title", statusResult.optString("title", title))
                    put("audioUrl", audioUrl)
                    put("seconds", statusResult.optInt("seconds", seconds))
                    put("rawResponse", statusResult)
                }
            }

            if (
                status.contains("fail") ||
                status.contains("error") ||
                status.contains("cancel")
            ) {
                throw IOException(
                    "Eroq müzik üretimi başarısız: $status"
                )
            }
        }

        throw IOException(
            "Müzik üretimi zaman aşımına uğradı. Daha sonra durum tekrar kontrol edilmeli."
        )
    }

    private fun extractJobId(response: JSONObject): String? {

        val jobs = response.optJSONArray("jobs")

        if (jobs != null) {
            for (i in 0 until jobs.length()) {
                val item = jobs.optJSONObject(i)

                if (item != null) {
                    val id = item.optString("id", "")
                        .ifBlank { item.optString("job_id", "") }
                        .ifBlank { item.optString("jobId", "") }

                    if (id.isNotBlank()) return id
                }

                val directValue = jobs.optString(i, "")
                if (directValue.isNotBlank() && directValue != "null") {
                    return directValue
                }
            }
        }

        val directId = response.optString("id", "")
            .ifBlank { response.optString("job_id", "") }
            .ifBlank { response.optString("jobId", "") }

        return directId.takeIf { it.isNotBlank() }
    }

    private fun findAudioUrl(json: JSONObject): String? {

        val directKeys = listOf(
            "audioUrl",
            "audio_url",
            "url",
            "mp3",
            "download_url",
            "downloadUrl"
        )

        for (key in directKeys) {
            val value = json.optString(key, "")
            if (isAudioUrl(value)) return value
        }

        val data = json.opt("data")

        when (data) {
            is JSONObject -> {
                findAudioUrl(data)?.let { return it }
            }

            is JSONArray -> {
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i)
                    if (item != null) {
                        findAudioUrl(item)?.let { return it }
                    }
                }
            }
        }

        val jobs = json.optJSONArray("jobs")
        if (jobs != null) {
            for (i in 0 until jobs.length()) {
                val item = jobs.optJSONObject(i)
                if (item != null) {
                    findAudioUrl(item)?.let { return it }
                }
            }
        }

        val result = json.optJSONObject("result")
        if (result != null) {
            findAudioUrl(result)?.let { return it }
        }

        return null
    }

    private fun isAudioUrl(value: String): Boolean {
        return value.startsWith("https://") ||
            value.startsWith("http://")
    }

    private fun executeRequest(request: Request): JSONObject {

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

                throw IOException("HTTP ${response.code}: $error")
            }

            return json
        }
    }
}
