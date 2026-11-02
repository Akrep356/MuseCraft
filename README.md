# MuseCraft AI Music Studio - Android Projesi

Bu proje; AI destekli beste yapma, şarkı sözü yazma, kadın & erkek vokal seçimi, kullanıcı vokal kaydı klonlama, sanal müzisyen yaratma ve video klip storyboard stüdyosu sunan tam teşekküllü bir modern Android uygulamasıdır.

## 🚀 Proje İçeriği ve Özellikler

1. **Beste & Şarkı Sözü Stüdyosu (`Beste & Söz`)**:
   - Tema, müzik türü (Pop, Rock, Rap, EDM, Anadolu Rock, Arabesk Pop, vb.), mod ve BPM ayarları.
   - Yapay zeka ile profesyonel akor dizisi (chord progression) ve bölümlendirilmiş sözler (Intro, Verse, Chorus, Bridge, Outro).

2. **Cover & Vokal Stüdyosu (`Cover & Vokal`)**:
   - Kadın (*Elif*, *Selin*, *Derya*, *Zeynep*) ve Erkek (*Emre*, *Kaan*, *Burak*, *Cem*) vokal modelleri.
   - Herhangi bir parçayı yeni bir türe ve vokal yorumuna dönüştürme; Autotune ve Reverb efektleri.

3. **Kendi Sesinle Şarkı Söyletme (Ses Klonlama)**:
   - Dahili mikrofon ses kaydedici (`UserVoiceRecorder`) ile 5-15 saniyelik ses örneği kaydetme.
   - Canlı frekans/genlik dalgası görselleştiricisi ve kayıt dinleme/önizleme.
   - Kaydedilen sesi özel vokal modeli (*"🌟 Kendi AI Sesin"*) olarak tüm bestelerde ve coverlarda kullanma.

4. **Sanal Sanatçı & Avatar Yaratıcı (`Sanal Sanatçı`)**:
   - Cyberpunk, 3D Anime Idol, Futuristic Rocker sahne tarzlarında avatar ve sahne personası üretimi.
   - Biyografi, sahne kostümü/imajı ve vokal profili oluşturup kütüphaneye kaydetme.

5. **Müzik Video Klip & Storyboard (`Video Klip`)**:
   - 16:9 oranında sinematik klip sahnesi, timecode'lu kamera açıları, ışıklandırma ve görsel sahne yönetmenliği.
   - Dinamik ses dalgası görselleştiricisi ve şarkı sözü altyazıları.

6. **Canlı Sentezleyici & Kütüphane (`Room DB`)**:
   - Dahili PCM ses motoru (`AudioSynthesisEngine`) ile canlı melodi ve ritim çalma.
   - Room SQLite veritabanı ile offline depolama, silme ve tek dokunuşla panoya kopyalama.

---

## 🛠️ Nasıl Derlenir ve APK Alınır?

### 1. Android Studio ile Derleme
1. Bu ZIP dosyasını çıkartın.
2. Android Studio'yu açıp **Open** seçeneğiyle bu proje klasörünü seçin.
3. Gradle senkronizasyonu tamamlandıktan sonra **Build > Build Bundle(s) / APK(s) > Build APK(s)** yolunu izleyin.
4. Çıkan `app-debug.apk` dosyasını telefonunuza yükleyin.

### 2. GitHub Actions ile Otomatik APK Oluşturma
Projede `.github/workflows/build-apk.yml` hazır olarak bulunmaktadır:
1. Bu projeyi bir GitHub deposuna yükleyin (Push).
2. GitHub'da **Actions** sekmesine gidin.
3. **Build Android APK** iş akışı otomatik çalışacak ve derlenen `.apk` dosyasını **Artifacts** bölümünden indirmenizi sağlayacaktır.

### 3. Komut Satırı ile Derleme
Terminalde proje kök dizinindeyken:
```bash
gradle assembleDebug
```
Üretilen APK konumu: `app/build/outputs/apk/debug/app-debug.apk`
