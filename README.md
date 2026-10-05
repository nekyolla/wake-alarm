#  WakeAlarm (WhatsApp Keyword Alarm)

<p align="center">
  <b>Aplikasi Android untuk membangunkan Anda saat pesan darurat/krusial WhatsApp masuk menggunakan sistem pencocokan multi-kata kunci.</b>
  <br />
  <i>"May this journey lead us starward - Kindness"</i>
</p>

---

##  Fitur Utama

- **Deteksi per pesan, tanpa alarm berulang**: Setiap bubble pesan WhatsApp / WhatsApp Business dievaluasi tepat satu kali, jadi pesan lama yang belum dibaca tidak membunyikan alarm lagi.
- **Ambang kata kunci 1–5**: Atur berapa kata kunci berbeda yang harus muncul dalam satu pesan.
- **Uji pesan**: Ketik contoh pesan di layar Kata Kunci untuk melihat langsung apakah alarm akan berbunyi.
- **Menembus Hening & DND**: Volume alarm dipaksa 100% saat berbunyi, lalu **dikembalikan** ke volume semula setelah dimatikan.
- **Volume bertahap & getar** yang bisa diatur, plus nada kustom (MP3/file lokal atau nada sistem) dengan preview.
- **Layar alarm full-screen**: Jam besar, nama pengirim, *geser untuk mematikan* (anti tersentuh tak sengaja), dan **Matikan & buka chat** langsung ke percakapan WhatsApp.
- **Jeda satu ketukan**: Quick Settings tile, tombol di notifikasi status, atau kartu Mode Siaga di beranda.
- **Riwayat alarm**: 30 alarm terakhir, disimpan hanya di perangkat dan tidak ikut di-backup.
- **Onboarding & izin**: Panduan awal dengan pengungkapan privasi, checklist izin Wajib/Disarankan, dan panduan khusus Xiaomi/HyperOS.
- **Pemantauan kesehatan**: Peringatan jika izin kurang, listener terputus (dengan tombol hubungkan ulang), atau kata kunci aktif lebih sedikit dari ambang.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVVM + Repository Pattern
- **Storage**: Jetpack DataStore Preferences
- **Concurrency**: Kotlin Coroutines & Flow
- **Background Service**: Android `NotificationListenerService` & `ForegroundService` (Media Playback)
- **Min SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 34/35 (Android 14/15 / Xiaomi HyperOS 2)

---

## 🚀 Cara Build & Menjalankan

### Persyaratan:
- JDK 17+
- Android SDK (API 34+)

Gradle akan mencari JDK 21 secara otomatis (`gradle/gradle-daemon-jvm.properties`). Jika JDK 21 tidak terdeteksi, daftarkan lokasinya di `~/.gradle/gradle.properties`:
```properties
org.gradle.java.installations.paths=/path/ke/jdk-21
```

### Build APK Debug:
```bash
./gradlew testDebugUnitTest assembleDebug
```
File APK akan berada di: `app/build/outputs/apk/debug/app-debug.apk`

### Build APK Release:
Salin `keystore.properties.example` menjadi `keystore.properties` (sudah di-.gitignore) dan isi kredensial keystore, atau set env var `WAKEALARM_STORE_FILE`, `WAKEALARM_STORE_PASSWORD`, `WAKEALARM_KEY_ALIAS`, `WAKEALARM_KEY_PASSWORD`. Tanpa itu, APK release tetap dibuat tapi tidak ditandatangani.
```bash
./gradlew assembleRelease
```

---

## 🔒 Izin Khusus yang Diperlukan

**Wajib:** Akses Notifikasi, Tampilkan Notifikasi, Alarm Layar Penuh (Android 14+), Tanpa Batasan Baterai.
**Disarankan:** Tampil di Atas Aplikasi Lain, Akses Jangan Ganggu, dan (Xiaomi/HyperOS) Autostart.

Semua bisa diberikan dari layar **Izin dan keandalan** di aplikasi.

---

## 👨‍💻 Author
Made with ❤️ by **Kindness**
