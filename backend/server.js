
const express = require("express");
const cors = require("cors");

const app = express();
const PORT = process.env.PORT || 10000;

const EROQ_API_KEY = process.env.EROQ_API_KEY;
const EROQ_API_URL = "https://eroq.ai/v1";

app.use(cors());
app.use(express.json({ limit: "10mb" }));

// JSON yanıtını güvenli şekilde oku
async function readJsonResponse(response) {
    const text = await response.text();

    try {
        return text ? JSON.parse(text) : {};
    } catch {
        return {
            error: "Eroq geçerli JSON yanıtı döndürmedi.",
            raw: text.slice(0, 2000)
        };
    }
}

// SUNUCU KONTROLÜ
app.get("/", (req, res) => {
    res.json({
        app: "MuseCraft Music Server",
        status: "online",
        engine: "eroq-music-one",
        endpoints: [
            "/health",
            "/api/music/generate",
            "/api/music/status/:id"
        ]
    });
});

// SAĞLIK KONTROLÜ
app.get("/health", (req, res) => {
    res.json({
        ok: true,
        server: "MuseCraft",
        eroqConfigured: Boolean(EROQ_API_KEY)
    });
});

// MÜZİK ÜRET
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

        if (cleanPrompt.length > 600 ||
            cleanStyle.length > 600 ||
            cleanLyrics.length > 3000 ||
            cleanTitle.length > 80) {
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
                error:
                    result.error?.message ||
                    result.error ||
                    result.message ||
                    "Eroq müzik üretim hatası.",
                details: result
            });
        }

        const jobs = Array.isArray(result.jobs)
            ? result.jobs
            : [];

        if (jobs.length === 0 || !jobs[0].id) {
            console.error("EROQ JOB ID MISSING:", result);

            return res.status(502).json({
                ok: false,
                error: "Eroq üretim yanıtında iş kimliği bulunamadı.",
                details: result
            });
        }

        return res.status(202).json({
            ok: true,
            status: result.status || "processing",
            id: jobs[0].id,
            jobs: jobs,
            title: result.title || cleanTitle,
            lyrics: result.lyrics || cleanLyrics,
            style: result.style || cleanStyle,
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

// MÜZİK DURUMUNU KONTROL ET
app.get("/api/music/status/:id", async (req, res) => {
    try {
        if (!EROQ_API_KEY) {
            return res.status(500).json({
                ok: false,
                error: "EROQ_API_KEY tanımlanmamış."
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
                error:
                    result.error?.message ||
                    result.error ||
                    "Müzik durumu alınamadı.",
                details: result
            });
        }

        const status = String(result.status || "unknown").toLowerCase();

        return res.json({
            ok: true,
            id: result.id || id,
            status: status,
            title: result.title || null,
            style: result.style || null,
            lyrics: result.lyrics || null,
            seconds: result.seconds || null,
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

// SUNUCUYU BAŞLAT
app.listen(PORT, "0.0.0.0", () => {
    console.log(
        "MuseCraft Music Server listening on port " + PORT
    );
    console.log("Eroq Music One integration ready.");
});
