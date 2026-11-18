package com.example.data.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class BlueMetalGenerationResult(
    val id: String,
    val status: String,
    val audioUrl: String,
    val title: String,
    val seconds: Int
)

class BlueMetalMusicService {

    companion object {
        private const val BASE_URL =
            "https://musecraft-08ec.onrender.com"

        private const val POLL_INTERVAL_MS = 5000L
        private const val MAX_WAIT_MS = 900000L
    }

    private suspend fun requestJson(
        endpoint: String,
        method: String,
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {

        val connection = URL(
            BASE_URL + endpoint
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 30000
            connection.readTimeout = 65000
            connection.instanceFollowRedirects = true

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (body != null) {
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val responseCode = connection.responseCode

            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use {
                    it.readText()
                }
            } else {
                connection.errorStream?.bufferedReader()?.use {
                    it.readText()
                }.orEmpty()
            }

            val json = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                JSONObject()
            }

            if (responseCode !in 200..299) {
                throw IOException(
                    json.optString(
                        "error",
                        "BlueMetal HTTP hatası: $responseCode"
                    )
                )
            }

            if (!json.optBoolean("ok", false)) {
                throw IOException(
                    json.optString(
                        "error",
                        "BlueMetal isteği başarısız."
                    )
                )
            }

            json

        } finally {
            connection.disconnect()
        }
    }

    suspend fun generateMusicAndWait(
        prompt: String,
        style: String,
        lyrics: String,
        title: String,
        instrumental: Boolean = false
    ): BlueMetalGenerationResult {

        require(prompt.isNotBlank() || lyrics.isNotBlank()) {
            "Müzik açıklaması veya şarkı sözleri gerekli."
        }

        // Aynı üretim isteğinin tekrar gönderilmesini önle.
        val idempotencyKey = UUID.randomUUID().toString()

        val requestBody = JSONObject().apply {
            put("prompt", prompt)
            put("style", style)
            put("lyrics", lyrics)
            put("title", title)
            put("instrumental", instrumental)
            put("idempotencyKey", idempotencyKey)
        }

        // 1. Üretimi başlat.
        // Bu fonksiyon yalnızca kullanıcı ikinci düğmeye
        // bastığında ViewModel tarafından çağrılacak.

        val generation = requestJson(
            endpoint = "/api/bluemetal/generate",
            method = "POST",
            body = requestBody
        )

        val generationId = generation.optString("id", "")

        if (!generationId.startsWith("gen_")) {
            throw IOException(
                "BlueMetal geçerli üretim kimliği döndürmedi."
            )
        }

        // 2. Üretim durumunu kontrol et.

        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < MAX_WAIT_MS) {

            delay(POLL_INTERVAL_MS)

            val statusResponse = requestJson(
                endpoint = "/api/bluemetal/status/$generationId",
                method = "GET"
            )

            val status = statusResponse
                .optString("status", "unknown")
                .lowercase()

            when (status) {

                "succeeded", "completed", "complete" -> {

                    val audio = statusResponse.optJSONObject("audio")

                    val duration = audio
                        ?.optDouble("duration", 0.0)
                        ?.toInt()
                        ?: 0

                    // İmzalı BlueMetal bağlantısı yerine
                    // kendi güvenli Render indirme adresimizi kullan.
                    val downloadUrl =
                        "$BASE_URL/api/bluemetal/download/$generationId"

                    return BlueMetalGenerationResult(
                        id = generationId,
                        status = status,
                        audioUrl = downloadUrl,
                        title = title,
                        seconds = duration
                    )
                }

                "failed", "error", "cancelled", "canceled" -> {
                    throw IOException(
                        statusResponse.optString(
                            "error",
                            "BlueMetal müzik üretimini tamamlayamadı."
                        )
                    )
                }

                else -> {
                    // queued / processing durumlarında beklemeye devam et.
                }
            }
        }

        throw IOException(
            "BlueMetal üretimi zaman aşımına uğradı. " +
            "Üretim kimliği: $generationId"
        )
    }
}
