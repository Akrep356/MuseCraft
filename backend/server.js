
const express = require("express");
const cors = require("cors");

const app = express();
const PORT = process.env.PORT || 10000;

const EROQ_API_KEY = process.env.EROQ_API_KEY;
const EROQ_API_URL = "https://eroq.ai/v1";

app.use(cors());
app.use(express.json({ limit: "10mb" }));

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
        } = req.body;

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

        if (
            !prompt &&
            !style &&
            !lyrics
        ) {
            return res.status(400).json({
                ok: false,
                error: "Müzik açıklaması veya müzik stili gerekli."
            });
        }

        const musicRequest = {
            model: "eroq-music-one",
            seconds: duration,
            n: 1,
            instrumental: Boolean(instrumental)
        };

        if (prompt) musicRequest.prompt = String(prompt).slice(0, 600);
        if (style) musicRequest.style = String(style).slice(0, 600);
        if (lyrics) musicRequest.lyrics = String(lyrics).slice(0, 3000);
        if (title) musicRequest.title = String(title).slice(0, 80);

        const response = await fetch(
            `${EROQ_API_URL}/music/generations`,
            {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${EROQ_API_KEY}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(musicRequest)
            }
        );

        const result = await response.json();

        if (!response.ok) {
            return res.status(response.status).json({
                ok: false,
                error: result.error || result.message || "Eroq müzik üretim hatası.",
                details: result
            });
        }

        return res.status(202).json({
            ok: true,
            status: result.status,
            title: result.title,
            lyrics: result.lyrics,
            style: result.style,
            seconds: result.seconds,
            jobs: result.jobs,
            usage: result.usage
        });

    } catch (error) {
        console.error("MUSIC GENERATION ERROR:", error);

        res.status(500).json({
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
                    "Authorization": `Bearer ${EROQ_API_KEY}`
                }
            }
        );

        const result = await response.json();

        if (!response.ok) {
            return res.status(response.status).json({
                ok: false,
                error: "Müzik durumu alınamadı.",
                details: result
            });
        }

        return res.json({
            ok: true,
            id: result.id,
            status: result.status,
            title: result.title,
            seconds: result.seconds,
            data: result.data,
            error: result.error
        });

    } catch (error) {
        console.error("MUSIC STATUS ERROR:", error);

        res.status(500).json({
            ok: false,
            error: "Müzik durumu kontrol edilemedi."
        });
    }
});

// SUNUCUYU BAŞLAT
app.listen(PORT, "0.0.0.0", () => {
    console.log("MuseCraft Music Server listening on port " + PORT);
    console.log("Eroq Music One integration ready.");
});
