
const express = require("express");
const cors = require("cors");

const app = express();
const PORT = process.env.PORT || 10000;

const EROQ_API_KEY = process.env.EROQ_API_KEY;
const EROQ_API_URL = "https://eroq.ai/v1";

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

const GEMINI_MODELS = [
    "gemini-3.8-flash",
    "gemini-3.7-flash",
    "gemini-3.5-flash-lite"
];

const GEMINI_MODEL = GEMINI_MODELS[0];
const GEMINI_API_BASE =
    "https://generativelanguage.googleapis.com/v1beta/models";

app.use(cors());
app.use(express.json({ limit: "10mb" }));

// JSON RESPONSE READER
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

// ERROR MESSAGE READER
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

// HTTP URL CHECK
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

// FIND AUDIO URL
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
            if (
                Object.prototype.hasOwnProperty.call(value, key)
            ) {
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

// FIND EROQ JOB ID
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

// NORMALIZE STATUS
function normalizeStatus(value) {
    return String(value || "unknown").toLowerCase();
}

// WAIT HELPER
function wait(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}

// GEMINI ERROR STATUS CHECK
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

// GEMINI TEXT EXTRACTION
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

// GEMINI REQUEST TO ONE MODEL
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

// GEMINI API WITH MODEL FALLBACK
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

                await wait(800);
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

// SERVER INFORMATION
app.get("/", (req, res) => {
    res.json({
        app: "MuseCraft Music Server",
        status: "online",
        engines: {
            music: "eroq-music-one",
            textAI: GEMINI_MODEL,
            fallbackModels: GEMINI_MODELS
        },
        endpoints: [
            "/health",
            "/api/music/generate",
            "/api/music/status/:id",
            "/api/gemini/generate"
        ]
    });
});

// HEALTH CHECK
app.get("/health", (req, res) => {
    res.json({
        ok: true,
        server: "MuseCraft",
        eroqConfigured: Boolean(EROQ_API_KEY),
        geminiConfigured: Boolean(GEMINI_API_KEY),
        geminiModels: GEMINI_MODELS
    });
});

// GEMINI GENERATION
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

// EROQ MUSIC GENERATION
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
                body: JSON.stringify(musicRequest)
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
        console.log(
            "EROQ INITIAL AUDIO URL:",
            audioUrl ? "FOUND" : "NOT YET AVAILABLE"
        );

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

        return res.status(500).json({
            ok: false,
            error: "Müzik sunucusunda beklenmeyen hata."
        });
    }
});

// EROQ MUSIC STATUS
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
                }
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

        return res.status(500).json({
            ok: false,
            error: "Müzik durumu kontrol edilemedi."
        });
    }
});

// START SERVER
app.listen(PORT, "0.0.0.0", () => {
    console.log(
        "MuseCraft Music Server listening on port " + PORT
    );

    console.log("Eroq Music One integration ready.");
    console.log("Gemini primary model:", GEMINI_MODEL);
    console.log("Gemini fallback models:", GEMINI_MODELS.join(", "));
    console.log("Gemini integration ready:", Boolean(GEMINI_API_KEY));
});
