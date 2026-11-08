
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

        // İlk yanıtta MP3 bağlantısı varsa beklemeden kullan.
        val initialAudioUrl = findAudioUrl(generation)

        if (!initialAudioUrl.isNullOrBlank()) {
            return createSuccessResult(
                jobId = jobId,
                status = generation.optString("status", "completed"),
                title = generation.optString("title", title),
                audioUrl = initialAudioUrl,
                seconds = generation.optInt("seconds", seconds),
                rawResponse = generation
            )
        }

        // İlk yanıtta bağlantı yoksa üretim durumunu kontrol et.
        repeat(MAX_POLL_ATTEMPTS) {

            delay(POLL_INTERVAL_MS)

            val statusResult = getMusicStatus(jobId)

            val status = statusResult
                .optString("status", "unknown")
                .lowercase()

            val audioUrl = findAudioUrl(statusResult)

            if (!audioUrl.isNullOrBlank()) {
                return createSuccessResult(
                    jobId = jobId,
                    status = status,
                    title = statusResult.optString("title", title),
                    audioUrl = audioUrl,
                    seconds = statusResult.optInt("seconds", seconds),
                    rawResponse = statusResult
                )
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

    private fun createSuccessResult(
        jobId: String,
        status: String,
        title: String,
        audioUrl: String,
        seconds: Int,
        rawResponse: JSONObject
    ): JSONObject {

        return JSONObject().apply {
            put("ok", true)
            put("id", jobId)
            put("status", status)
            put("title", title)
            put("audioUrl", audioUrl)
            put("seconds", seconds)
            put("rawResponse", rawResponse)
        }
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

        val preferredKeys = listOf(
            "audioUrl",
            "audio_url",
            "downloadUrl",
            "download_url",
            "mp3Url",
            "mp3_url",
            "audioFileUrl",
            "audio_file_url",
            "fileUrl",
            "file_url",
            "mp3",
            "url"
        )

        val nestedKeys = listOf(
            "data",
            "result",
            "results",
            "output",
            "outputs",
            "audio",
            "track",
            "tracks",
            "file",
            "files",
            "job",
            "jobs"
        )

        val visited = HashSet<Int>()
        var checked = 0

        fun search(value: Any?, depth: Int): String? {

            if (
                value == null ||
                depth > 10 ||
                checked > 300
            ) {
                return null
            }

            if (value is JSONObject || value is JSONArray) {
                val identity = System.identityHashCode(value)

                if (!visited.add(identity)) {
                    return null
                }

                checked++
            }

            when (value) {
                is JSONObject -> {

                    for (key in preferredKeys) {
                        val candidate = value.optString(key, "")

                        if (isAudioUrl(candidate)) {
                            return candidate
                        }
                    }

                    for (key in nestedKeys) {
                        val child = value.opt(key)

                        val found = search(child, depth + 1)

                        if (found != null) return found
                    }

                    val keys = value.keys()

                    while (keys.hasNext()) {
                        val key = keys.next()

                        if (nestedKeys.contains(key)) continue

                        val child = value.opt(key)

                        if (
                            child is JSONObject ||
                            child is JSONArray
                        ) {
                            val found = search(child, depth + 1)

                            if (found != null) return found
                        }
                    }
                }

                is JSONArray -> {

                    for (i in 0 until value.length()) {
                        val found = search(
                            value.opt(i),
                            depth + 1
                        )

                        if (found != null) return found
                    }
                }
            }

            return null
        }

        return search(json, 0)
    }

    private fun isAudioUrl(value: String): Boolean {

        val cleanValue = value.trim()

        return cleanValue.startsWith("https://") ||
            cleanValue.startsWith("http://")
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
