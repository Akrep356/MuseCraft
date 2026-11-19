const express = require("express");
const cors = require("cors");
const crypto = require("crypto");
const { Readable } = require("stream");
const { pipeline } = require("stream/promises");

const app = express();
const PORT = process.env.PORT || 10000;

const EROQ_API_KEY = process.env.EROQ_API_KEY;
const EROQ_API_URL = "https://eroq.ai/v1";

const BLUEMETAL_API_KEY = process.env.BLUEMETAL_API_KEY;
const BLUEMETAL_API_URL = "https://api.bluemetal.ai/v1";

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

const GEMINI_MODELS = [
    "gemini-3.8-flash",
    "gemini-3.7-flash",
    "gemini-3.6-flash",
    "gemini-3.5-flash-lite"
];

const GEMINI_MODEL = GEMINI_MODELS[0];

const GEMINI_API_BASE =
    "https://generativelanguage.googleapis.com/v1beta/models";

const BLUEMETAL_MAX_PROMPT_LENGTH = 2000;

app.use(cors());
app.use(express.json({ limit: "10mb" }));

// ========================================
// GENERAL HELPERS
// ========================================

async function readJsonResponse(response) {
    const text = await response.text();

    try {
        return text ? JSON.parse(text) : {};
    } catch {
        return {
            error: "Sunucu geçerli JSON yanıtı döndürmedi.",
            raw: text.slice(0, 2000)
        };
    }
}

function getErrorMessage(result, fallback) {
    if (!result) return fallback;

    if (typeof result.error === "string") {
        return result.error;
    }

    if (result.error && typeof result.error.message === "string") {
        return result.error.message;
    }

    if (typeof result.message === "string") {
        return result.message;
    }

    return fallback;
}

function isHttpUrl(value) {
    if (typeof value !== "string") return false;

    try {
        const parsed = new URL(value);

        return parsed.protocol === "https:" ||
            parsed.protocol === "http:";
    } catch {
        return false;
    }
}

function findAudioUrl(root) {
    const preferredKeys = [
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
    ];

    const visited = new Set();
    let checked = 0;

    function search(value, depth) {
        if (
            value === null ||
            value === undefined ||
            depth > 10 ||
            checked > 300
        ) {
            return null;
        }

        if (typeof value !== "object") return null;
        if (visited.has(value)) return null;

        visited.add(value);
        checked++;

        if (Array.isArray(value)) {
            for (const item of value) {
                const found = search(item, depth + 1);
                if (found) return found;
            }

            return null;
        }

        for (const key of preferredKeys) {
            if (
                Object.prototype.hasOwnProperty.call(value, key) &&
                isHttpUrl(value[key])
            ) {
                return value[key];
            }
        }

        const nestedKeys = [
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
        ];

        for (const key of nestedKeys) {
            if (Object.prototype.hasOwnProperty.call(value, key)) {
                const found = search(value[key], depth + 1);

                if (found) return found;
            }
        }

        for (const key of Object.keys(value)) {
            if (nestedKeys.includes(key)) continue;

            const child = value[key];

            if (child && typeof child === "object") {
                const found = search(child, depth + 1);

                if (found) return found;
            }
        }

        return null;
    }

    return search(root, 0);
}

function findJobId(result) {
    if (!result || typeof result !== "object") {
        return null;
    }

    if (Array.isArray(result.jobs) && result.jobs.length > 0) {
        const job = result.jobs[0];

        if (job && typeof job === "object") {
            return job.id ||
                job.job_id ||
                job.jobId ||
                null;
        }
    }

    return result.id ||
        result.job_id ||
        result.jobId ||
        null;
}

function normalizeStatus(value) {
    return String(value || "unknown").toLowerCase();
}

function wait(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function isRetryableStatus(status) {
    return [
        408,
        429,
        500,
        502,
        503,
        504
    ].includes(status);
}

// ========================================
// GEMINI HELPERS
// ========================================

function extractGeminiText(result) {
    const candidates = result.candidates || [];

    if (!candidates.length) {
        throw new Error(
            "Gemini yanıtında içerik bulunamadı."
        );
    }

    const firstCandidate = candidates[0];
    const parts = firstCandidate.content?.parts || [];

    const generatedText = parts
        .filter(part => typeof part.text === "string")
        .map(part => part.text)
        .join("\n")
        .trim();

    if (!generatedText) {
        const reason = firstCandidate.finishReason || "Bilinmiyor";

        throw new Error(
            "Gemini boş yanıt döndürdü. Bitiş nedeni: " + reason
        );
    }

    return generatedText;
}

async function requestGeminiModel(prompt, model) {
    const endpoint =
        `${GEMINI_API_BASE}/${model}:generateContent` +
        `?key=${encodeURIComponent(GEMINI_API_KEY)}`;

    const response = await fetch(endpoint, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            contents: [
                {
                    role: "user",
                    parts: [
                        {
                            text: prompt
                        }
                    ]
                }
            ],
            generationConfig: {
                temperature: 0.7,
                topP: 0.95,
                maxOutputTokens: 8192
            }
        }),
        signal: AbortSignal.timeout(35000)
    });

    const result = await readJsonResponse(response);

    if (!response.ok) {
        const error = new Error(
            getErrorMessage(
                result,
                `Gemini API hatası: HTTP ${response.status}`
            )
        );

        error.status = response.status;
        throw error;
    }

    return extractGeminiText(result);
}

async function callGemini(prompt) {
    if (!GEMINI_API_KEY) {
        throw new Error(
            "GEMINI_API_KEY Render ortamında tanımlanmamış."
        );
    }

    let lastError = null;

    for (let index = 0; index < GEMINI_MODELS.length; index++) {
        const model = GEMINI_MODELS[index];

        console.log("GEMINI ATTEMPT:", {
            attempt: index + 1,
            totalModels: GEMINI_MODELS.length,
            model: model
        });

        try {
            const generatedText =
                await requestGeminiModel(prompt, model);

            console.log("GEMINI SUCCESS:", {
                model: model,
                responseLength: generatedText.length
            });

            return {
                text: generatedText,
                model: model
            };

        } catch (error) {
            lastError = error;

            console.error("GEMINI MODEL ERROR:", {
                model: model,
                status: error.status || null,
                name: error.name,
                message: error.message
            });

            const retryable =
                isRetryableStatus(error.status) ||
                error.name === "TimeoutError" ||
                error.name === "AbortError" ||
                error.name === "TypeError";

            if (!retryable) {
                throw error;
            }

            if (index < GEMINI_MODELS.length - 1) {
                console.log("GEMINI FALLBACK:", {
                    failedModel: model,
                    nextModel: GEMINI_MODELS[index + 1]
                });

                await wait(4000);
            }
        }
    }

    const finalError = new Error(
        "Gemini modellerinin tamamı yanıt veremedi. Son hata: " +
        (lastError?.message || "Bilinmeyen hata")
    );

    finalError.status = lastError?.status || 503;

    throw finalError;
}

// ========================================
// BLUEMETAL PROMPT BUILDER
// ========================================

function buildBlueMetalPrompt({
    prompt,
    style,
    lyrics,
    title,
    instrumental
}) {
    const cleanPrompt = String(prompt || "").trim();
    const cleanStyle = String(style || "").trim();
    const cleanLyrics = String(lyrics || "").trim();
    const cleanTitle = String(title || "").trim();

    const instruction = instrumental
        ? "Instrumental music. No vocals or singing."
        : "Sing in Turkish. Use the supplied lyrics exactly as written.";

    const lyricsBlock = !instrumental && cleanLyrics
        ? "Lyrics:\n" + cleanLyrics
        : "";

    const baseParts = [
        instruction,
        lyricsBlock
    ].filter(Boolean);

    const basePrompt = baseParts.join("\n\n");

    if (basePrompt.length > BLUEMETAL_MAX_PROMPT_LENGTH) {
        const availableLyricsLength =
            BLUEMETAL_MAX_PROMPT_LENGTH -
            instruction.length -
            "\n\nLyrics:\n".length;

        const error = new Error(
            "Şarkı sözleri BlueMetal sınırına sığmıyor. " +
            "Sözlerin mevcut uzunluğu " +
            cleanLyrics.length +
            " karakter. Açıklama ve başlık eklenmeden " +
            "kullanılabilecek yaklaşık söz alanı " +
            Math.max(0, availableLyricsLength) +
            " karakter."
        );

        error.code = "BLUEMETAL_LYRICS_TOO_LONG";
        error.status = 400;

        throw error;
    }

    let remaining =
        BLUEMETAL_MAX_PROMPT_LENGTH - basePrompt.length;

    const metadata = [];

    function addMetadata(label, value) {
        if (!value || remaining <= 0) return;

        const fullLine = `${label}: ${value}`;
        const separator = metadata.length > 0 ? "\n" : "";
        const available = remaining - separator.length - label.length - 2;

        if (available <= 0) return;

        const shortenedValue = value.slice(0, available);

        if (!shortenedValue) return;

        const line = `${label}: ${shortenedValue}`;

        metadata.push(line);
        remaining -= separator.length + line.length;
    }

    // Öncelik sırası: müzik tarzı, açıklama, başlık.
    // Şarkı sözleri kesilmez.

    addMetadata("Style", cleanStyle);
    addMetadata("Direction", cleanPrompt);
    addMetadata("Title", cleanTitle);

    const metadataText = metadata.join("\n");

    const finalPrompt = metadataText
        ? `${metadataText}\n\n${basePrompt}`
        : basePrompt;

    if (finalPrompt.length > BLUEMETAL_MAX_PROMPT_LENGTH) {
        throw new Error(
            "BlueMetal prompt uzunluğu güvenli sınırı aştı."
        );
    }

    return finalPrompt;
}

// ========================================
// BLUEMETAL API REQUEST
// ========================================

async function blueMetalRequest(path, options = {}) {
    if (!BLUEMETAL_API_KEY) {
        throw new Error(
            "BLUEMETAL_API_KEY Render ortamında tanımlanmamış."
        );
    }

    const response = await fetch(
        `${BLUEMETAL_API_URL}${path}`,
        {
            ...options,
            headers: {
                Authorization: `Bearer ${BLUEMETAL_API_KEY}`,
                ...(options.headers || {})
            },
            signal: AbortSignal.timeout(60000)
        }
    );

    const result = await readJsonResponse(response);

    if (!response.ok) {
        const error = new Error(
            getErrorMessage(
                result,
                `BlueMetal API hatası: HTTP ${response.status}`
            )
        );

        error.status = response.status;
        error.requestId =
            response.headers.get("x-request-id");

        error.retryAfter =
            response.headers.get("retry-after");

        throw error;
    }

    return {
        result,
        requestId: response.headers.get("x-request-id")
    };
}

// ========================================
// SERVER INFORMATION
// ========================================

app.get("/", (req, res) => {
    res.json({
        app: "MuseCraft Music Server",
        status: "online",

        engines: {
            music: [
                "eroq-music-one",
                "bluemetal"
            ],
            textAI: GEMINI_MODEL,
            fallbackModels: GEMINI_MODELS
        },

        endpoints: [
            "/health",
            "/api/music/generate",
            "/api/music/status/:id",
            "/api/bluemetal/generate",
            "/api/bluemetal/status/:id",
            "/api/bluemetal/download/:id",
            "/api/gemini/generate"
        ]
    });
});

// ========================================
// HEALTH CHECK
// ========================================

app.get("/health", (req, res) => {
    res.json({
        ok: true,
        server: "MuseCraft",
        eroqConfigured: Boolean(EROQ_API_KEY),
        blueMetalConfigured: Boolean(BLUEMETAL_API_KEY),
        geminiConfigured: Boolean(GEMINI_API_KEY),
        geminiModels: GEMINI_MODELS
    });
});

// ========================================
// GEMINI GENERATION
// ========================================

app.post("/api/gemini/generate", async (req, res) => {
    try {
        if (!GEMINI_API_KEY) {
            return res.status(500).json({
                ok: false,
                error: "GEMINI_API_KEY Render ortamında tanımlanmamış."
            });
        }

        const prompt = String(
            req.body?.prompt || ""
        ).trim();

        if (!prompt) {
            return res.status(400).json({
                ok: false,
                error: "Gemini için prompt gerekli."
            });
        }

        if (prompt.length > 20000) {
            return res.status(400).json({
                ok: false,
                error: "Prompt en fazla 20000 karakter olabilir."
            });
        }

        console.log("GEMINI REQUEST:", {
            primaryModel: GEMINI_MODEL,
            promptLength: prompt.length
        });

        const result = await callGemini(prompt);

        console.log("GEMINI RESPONSE:", {
            model: result.model,
            responseLength: result.text.length
        });

        return res.json({
            ok: true,
            model: result.model,
            text: result.text
        });

    } catch (error) {
        console.error("GEMINI GENERATION ERROR:", error);

        return res.status(
            error.status || 500
        ).json({
            ok: false,
            error: error.message ||
                "Gemini üretiminde beklenmeyen hata."
        });
    }
});

// ========================================
// EROQ MUSIC GENERATION
// ========================================

app.post("/api/music/generate", async (req, res) => {
    try {
        if (!EROQ_API_KEY) {
            return res.status(500).json({
                ok: false,
                error: "EROQ_API_KEY Render ortamında tanımlanmamış."
            });
        }

        const {
            prompt,
            style,
            lyrics,
            title,
            seconds = 60,
            instrumental = false
        } = req.body || {};

        const duration = Number(seconds);

        if (
            !Number.isInteger(duration) ||
            duration < 30 ||
            duration > 240 ||
            duration % 30 !== 0
        ) {
            return res.status(400).json({
                ok: false,
                error: "Süre 30 ile 240 saniye arasında ve 30'un katı olmalı."
            });
        }

        const cleanPrompt = String(prompt || "").trim();
        const cleanStyle = String(style || "").trim();
        const cleanLyrics = String(lyrics || "").trim();
        const cleanTitle = String(title || "").trim();

        if (!cleanPrompt && !cleanStyle && !cleanLyrics) {
            return res.status(400).json({
                ok: false,
                error: "Müzik açıklaması, stil veya şarkı sözleri gerekli."
            });
        }

        if (
            cleanPrompt.length > 600 ||
            cleanStyle.length > 600 ||
            cleanLyrics.length > 3000 ||
            cleanTitle.length > 80
        ) {
            return res.status(400).json({
                ok: false,
                error: "Gönderilen metinlerden biri izin verilen uzunluğu aşıyor."
            });
        }

        const musicRequest = {
            model: "eroq-music-one",
            seconds: duration,
            n: 1,
            instrumental: Boolean(instrumental)
        };

        if (cleanPrompt) musicRequest.prompt = cleanPrompt;
        if (cleanStyle) musicRequest.style = cleanStyle;
        if (cleanLyrics) musicRequest.lyrics = cleanLyrics;
        if (cleanTitle) musicRequest.title = cleanTitle;

        console.log("EROQ MUSIC REQUEST:", {
            seconds: duration,
            takes: 1,
            instrumental: Boolean(instrumental)
        });

        const response = await fetch(
            `${EROQ_API_URL}/music/generations`,
            {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${EROQ_API_KEY}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(musicRequest),
                signal: AbortSignal.timeout(120000)
            }
        );

        const result = await readJsonResponse(response);

        if (!response.ok) {
            console.error("EROQ GENERATION ERROR:", result);

            return res.status(response.status).json({
                ok: false,
                error: getErrorMessage(
                    result,
                    "Eroq müzik üretim hatası."
                ),
                details: result
            });
        }

        const jobId = findJobId(result);

        if (!jobId) {
            console.error("EROQ JOB ID MISSING:", result);

            return res.status(502).json({
                ok: false,
                error: "Eroq üretim yanıtında iş kimliği bulunamadı.",
                details: result
            });
        }

        const audioUrl = findAudioUrl(result);

        console.log("EROQ JOB CREATED:", jobId);

        return res.status(202).json({
            ok: true,
            status: normalizeStatus(result.status || "processing"),
            id: String(jobId),
            jobs: Array.isArray(result.jobs) ? result.jobs : [],
            audioUrl: audioUrl,
            title: result.title || cleanTitle || null,
            lyrics: result.lyrics || cleanLyrics || null,
            style: result.style || cleanStyle || null,
            seconds: result.seconds || duration,
            usage: result.usage || null
        });

    } catch (error) {
        console.error("MUSIC GENERATION ERROR:", error);

        return res.status(error.status || 500).json({
            ok: false,
            error: error.message ||
                "Müzik sunucusunda beklenmeyen hata."
        });
    }
});

// ========================================
// EROQ MUSIC STATUS
// ========================================

app.get("/api/music/status/:id", async (req, res) => {
    try {
        if (!EROQ_API_KEY) {
            return res.status(500).json({
                ok: false,
                error: "EROQ_API_KEY Render ortamında tanımlanmamış."
            });
        }

        const id = req.params.id;

        if (!/^[a-zA-Z0-9_-]+$/.test(id)) {
            return res.status(400).json({
                ok: false,
                error: "Geçersiz müzik ID."
            });
        }

        const response = await fetch(
            `${EROQ_API_URL}/music/generations/${encodeURIComponent(id)}`,
            {
                method: "GET",
                headers: {
                    Authorization: `Bearer ${EROQ_API_KEY}`
                },
                signal: AbortSignal.timeout(60000)
            }
        );

        const result = await readJsonResponse(response);

        if (!response.ok) {
            console.error("EROQ STATUS ERROR:", result);

            return res.status(response.status).json({
                ok: false,
                error: getErrorMessage(
                    result,
                    "Müzik durumu alınamadı."
                ),
                details: result
            });
        }

        const audioUrl = findAudioUrl(result);

        const status = normalizeStatus(
            result.status ||
            result.data?.status ||
            "unknown"
        );

        const responseId = findJobId(result) || id;

        console.log("EROQ STATUS:", {
            id: responseId,
            status: status,
            audioUrlFound: Boolean(audioUrl)
        });

        return res.json({
            ok: true,
            id: String(responseId),
            status: status,
            title: result.title || result.data?.title || null,
            style: result.style || result.data?.style || null,
            lyrics: result.lyrics || result.data?.lyrics || null,
            seconds: result.seconds || result.data?.seconds || null,
            audioUrl: audioUrl,
            data: result.data || null,
            error: result.error || null
        });

    } catch (error) {
        console.error("MUSIC STATUS ERROR:", error);

        return res.status(error.status || 500).json({
            ok: false,
            error: error.message ||
                "Müzik durumu kontrol edilemedi."
        });
    }
});

// ========================================
// BLUEMETAL MUSIC GENERATION
// ========================================

app.post("/api/bluemetal/generate", async (req, res) => {
    try {
        if (!BLUEMETAL_API_KEY) {
            return res.status(500).json({
                ok: false,
                error: "BLUEMETAL_API_KEY Render ortamında tanımlanmamış."
            });
        }

        const {
            prompt,
            style,
            lyrics,
            title,
            instrumental = false,
            idempotencyKey
        } = req.body || {};

        const cleanPrompt = String(prompt || "").trim();
        const cleanStyle = String(style || "").trim();
        const cleanLyrics = String(lyrics || "").trim();
        const cleanTitle = String(title || "").trim();
        const isInstrumental = Boolean(instrumental);

        if (
            !cleanPrompt &&
            !cleanStyle &&
            !cleanLyrics
        ) {
            return res.status(400).json({
                ok: false,
                error: "BlueMetal için müzik açıklaması veya şarkı sözleri gerekli."
            });
        }

        if (
            cleanPrompt.length > 1500 ||
            cleanStyle.length > 600 ||
            cleanLyrics.length > 3000 ||
            cleanTitle.length > 100
        ) {
            return res.status(400).json({
                ok: false,
                error: "Gönderilen metinlerden biri izin verilen uzunluğu aşıyor."
            });
        }

        let finalPrompt;

        try {
            finalPrompt = buildBlueMetalPrompt({
                prompt: cleanPrompt,
                style: cleanStyle,
                lyrics: cleanLyrics,
                title: cleanTitle,
                instrumental: isInstrumental
            });
        } catch (promptError) {
            return res.status(promptError.status || 400).json({
                ok: false,
                error: promptError.message,
                code: promptError.code || "BLUEMETAL_PROMPT_ERROR",
                lyricsLength: cleanLyrics.length,
                maximumPromptLength: BLUEMETAL_MAX_PROMPT_LENGTH
            });
        }

        const requestKey =
            String(idempotencyKey || "").trim() ||
            crypto.randomUUID();

        if (
            requestKey.length > 200 ||
            !/^[a-zA-Z0-9_-]+$/.test(requestKey)
        ) {
            return res.status(400).json({
                ok: false,
                error: "Geçersiz Idempotency-Key."
            });
        }

        const musicRequest = {
            prompt: finalPrompt,
            format: "mp3",
            mode: "async"
        };

        console.log("BLUEMETAL MUSIC REQUEST:", {
            promptLength: finalPrompt.length,
            maximumPromptLength: BLUEMETAL_MAX_PROMPT_LENGTH,
            lyricsLength: cleanLyrics.length,
            instrumental: isInstrumental
        });

        const { result, requestId } = await blueMetalRequest(
            "/music/generations",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Idempotency-Key": requestKey
                },
                body: JSON.stringify(musicRequest)
            }
        );

        const generationId = result.id;

        if (!generationId) {
            console.error("BLUEMETAL ID MISSING:", result);

            return res.status(502).json({
                ok: false,
                error: "BlueMetal yanıtında üretim kimliği bulunamadı."
            });
        }

        const audioUrl = result.audio?.url || null;

        console.log("BLUEMETAL JOB CREATED:", {
            id: generationId,
            status: result.status,
            requestId: requestId
        });

        return res.status(202).json({
            ok: true,
            provider: "bluemetal",
            id: String(generationId),
            status: normalizeStatus(result.status),
            queuePosition: result.queue_position || null,
            audioUrl: audioUrl,
            audio: result.audio || null,
            usage: result.usage || null,
            title: cleanTitle || null,
            promptLength: finalPrompt.length,
            idempotencyKey: requestKey,
            requestId: requestId
        });

    } catch (error) {
        console.error("BLUEMETAL GENERATION ERROR:", {
            status: error.status || null,
            message: error.message,
            requestId: error.requestId || null
        });

        return res.status(error.status || 500).json({
            ok: false,
            provider: "bluemetal",
            error: error.message ||
                "BlueMetal müzik üretiminde beklenmeyen hata.",
            requestId: error.requestId || null,
            retryAfter: error.retryAfter || null
        });
    }
});

// ========================================
// BLUEMETAL MUSIC STATUS
// ========================================

app.get("/api/bluemetal/status/:id", async (req, res) => {
    try {
        if (!BLUEMETAL_API_KEY) {
            return res.status(500).json({
                ok: false,
                error: "BLUEMETAL_API_KEY Render ortamında tanımlanmamış."
            });
        }

        const id = req.params.id;

        if (!/^gen_[a-zA-Z0-9_-]+$/.test(id)) {
            return res.status(400).json({
                ok: false,
                error: "Geçersiz BlueMetal üretim ID."
            });
        }

        const { result, requestId } = await blueMetalRequest(
            `/music/generations/${encodeURIComponent(id)}`,
            {
                method: "GET"
            }
        );

        const status = normalizeStatus(result.status);
        const audioUrl = result.audio?.url || null;

        console.log("BLUEMETAL STATUS:", {
            id: id,
            status: status,
            audioUrlFound: Boolean(audioUrl),
            requestId: requestId
        });

        return res.json({
            ok: true,
            provider: "bluemetal",
            id: String(result.id || id),
            status: status,
            queuePosition: result.queue_position || null,
            audioUrl: audioUrl,
            audio: result.audio || null,
            usage: result.usage || null,
            error: result.error || null,
            requestId: requestId
        });

    } catch (error) {
        console.error("BLUEMETAL STATUS ERROR:", {
            status: error.status || null,
            message: error.message,
            requestId: error.requestId || null
        });

        return res.status(error.status || 500).json({
            ok: false,
            provider: "bluemetal",
            error: error.message ||
                "BlueMetal üretim durumu alınamadı.",
            requestId: error.requestId || null,
            retryAfter: error.retryAfter || null
        });
    }
});

// ========================================
// BLUEMETAL MP3 DOWNLOAD PROXY
// ========================================

app.get("/api/bluemetal/download/:id", async (req, res) => {
    try {
        if (!BLUEMETAL_API_KEY) {
            return res.status(500).json({
                ok: false,
                error: "BLUEMETAL_API_KEY tanımlanmamış."
            });
        }

        const id = req.params.id;

        if (!/^gen_[a-zA-Z0-9_-]+$/.test(id)) {
            return res.status(400).json({
                ok: false,
                error: "Geçersiz BlueMetal üretim ID."
            });
        }

        const { result } = await blueMetalRequest(
            `/music/generations/${encodeURIComponent(id)}`,
            {
                method: "GET"
            }
        );

        const status = normalizeStatus(result.status);

        if (!["succeeded", "completed", "complete"].includes(status)) {
            return res.status(409).json({
                ok: false,
                status: status,
                error: "MP3 henüz indirilmeye hazır değil."
            });
        }

        const audioUrl = result.audio?.url;

        if (!isHttpUrl(audioUrl)) {
            return res.status(502).json({
                ok: false,
                error: "BlueMetal geçerli MP3 bağlantısı döndürmedi."
            });
        }

        const parsedUrl = new URL(audioUrl);

        if (
            parsedUrl.protocol !== "https:" ||
            !parsedUrl.hostname.endsWith(".r2.cloudflarestorage.com")
        ) {
            return res.status(502).json({
                ok: false,
                error: "Beklenmeyen MP3 depolama adresi."
            });
        }

        console.log("BLUEMETAL DOWNLOAD START:", id);

        const audioResponse = await fetch(audioUrl, {
            method: "GET",
            signal: AbortSignal.timeout(120000)
        });

        if (!audioResponse.ok || !audioResponse.body) {
            console.error("AUDIO DOWNLOAD ERROR:", {
                id,
                status: audioResponse.status
            });

            return res.status(502).json({
                ok: false,
                error: "MP3 dosyası BlueMetal'den alınamadı."
            });
        }

        res.setHeader("Content-Type", "audio/mpeg");

        res.setHeader(
            "Content-Disposition",
            `attachment; filename="MuseCraft_${id}.mp3"`
        );

        res.setHeader("Cache-Control", "no-store");

        const contentLength =
            audioResponse.headers.get("content-length");

        if (contentLength) {
            res.setHeader("Content-Length", contentLength);
        }

        await pipeline(
            Readable.fromWeb(audioResponse.body),
            res
        );

        console.log("BLUEMETAL DOWNLOAD SUCCESS:", id);

    } catch (error) {
        console.error("BLUEMETAL DOWNLOAD ERROR:", error);

        if (!res.headersSent) {
            res.status(500).json({
                ok: false,
                error: error.message ||
                    "MP3 indirme işlemi başarısız."
            });
        } else {
            res.destroy(error);
        }
    }
});

// ========================================
// START SERVER
// ========================================

app.listen(PORT, "0.0.0.0", () => {
    console.log(
        "MuseCraft Music Server listening on port " + PORT
    );

    console.log("Eroq Music One integration:", Boolean(EROQ_API_KEY));
    console.log("BlueMetal integration:", Boolean(BLUEMETAL_API_KEY));

    console.log("Gemini primary model:", GEMINI_MODEL);

    console.log(
        "Gemini fallback models:",
        GEMINI_MODELS.join(", ")
    );

    console.log(
        "Gemini integration ready:",
        Boolean(GEMINI_API_KEY)
    );
});
