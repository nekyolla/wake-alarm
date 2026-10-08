#  WakeAlarm (WhatsApp Keyword Alarm)

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" alt="Ikon WakeAlarm" />
  <br />
  <b>Aplikasi Android yang membangunkan Anda saat pesan WhatsApp darurat masuk, berdasarkan kombinasi kata kunci.</b>
  <br />
  <i>"May this journey lead us starward" — Kindness</i>
</p>

---

##  Fitur Utama

- **Deteksi per pesan, tanpa alarm berulang**: setiap bubble WhatsApp / WhatsApp Business dievaluasi tepat satu kali. Balasan Anda sendiri dari notifikasi tidak ikut dihitung.
- **Pencocokan kata utuh**: kata, frasa, dan kata bertanda hubung dicocokkan utuh (`co-ass` tidak cocok dengan `co-assistant`); tanda baca di tepi kata kunci tetap bekerja (`dr.`, `#igd`).
- **Ambang 1–5 kata kunci** per pesan, ditampilkan seperti rarity ★★☆☆☆, plus **uji pesan** langsung di layar Kata kunci.
- **Pengirim yang tepat di grup**: layar alarm dan riwayat menampilkan orang yang benar-benar menulis pesan darurat.
- **Tidak ada pesan darurat yang hilang**: pesan darurat yang masuk saat alarm sedang bunyi dicatat di Riwayat dan ditampilkan sebagai "+N pesan darurat lain".
- **Menembus Hening & DND**: volume alarm dipaksa 100% lalu dikembalikan seperti semula.
- **Layar alarm full-screen**: jam besar, pendamping 3D, *geser untuk mematikan* dengan getaran per seperempat jalan, dan **Matikan & buka chat**.
- **Jeda satu ketukan**: Quick Settings tile, tombol di notifikasi status, atau tombol besar di Beranda.
- **Bahasa Indonesia & English**: ikut sistem atau pilih sendiri di Pengaturan / langkah pertama onboarding.
- **Riwayat**: 30 pesan darurat terakhir, hanya di perangkat, tidak ikut di-backup.

---

##  Desain: "Astral Express · Crimson"

Tampilan bernuansa Honkai: Star Rail, diambil dari ikon aplikasi (jam merah, bintang emas, mata ungu).

| Elemen | Keputusan |
|---|---|
| **Palet** | Latar luar angkasa gelap + nebula, **emas** untuk aksi utama (teks gelap di atas emas), **crimson** untuk merek/alarm, **ungu** sebagai aksen sekunder. Semua teks lolos kontras WCAG AA. Tema gelap saja. |
| **Tipografi** | **Rajdhani** (OFL) untuk judul, angka, dan label kecil berhuruf kapital; font sistem untuk isi agar mudah dibaca. |
| **Bentuk** | Sudut terpotong miring (*cut corner*) seperti panel menu HSR, panel kaca dengan garis emas tipis dan ornamen sudut, ornamen ◆ dari motif kartu di ikon. |
| **Token** | `ui/theme/Tokens.kt` (spasi 4/8/12/16/20/24/32, target sentuh 48/56/64dp, durasi animasi). Semua komponen di `ui/components/`. |
| **Gerak** | Starfield berkelip, pendamping 3D (miring mengikuti HP, orbit ◆, kilau hologram). Semua berhenti jika animasi sistem dimatikan. |

**UX**
- **Thumb zone**: navigasi bawah (Beranda · Kata kunci · Riwayat · Pengaturan), tombol siaga besar di area jempol, kolom tambah kata kunci menempel di bawah layar.
- **Status sistem**: strip kesiapan (Izin · Pantau · Kata kunci) di kartu utama; tiap item yang bermasalah bisa diketuk untuk diperbaiki. Hanya satu banner, yaitu masalah yang paling penting.
- **Feedback**: snackbar + getar saat siaga diubah, kata kunci ditambah/dihapus, atau nada gagal diputar.
- **Pencegahan error**: dialog konfirmasi saat mengaktifkan siaga padahal belum siap; peringatan + Urungkan saat kata kunci aktif turun di bawah ambang; peringatan jika file nada tidak bisa dibuka; validasi kata kunci (kosong, terlalu pendek/panjang, tanpa huruf/angka, duplikat); alarm tetap bergetar kalau tidak ada nada yang bisa diputar.
- **Privasi di layar kunci**: saat HP terkunci dengan PIN, notifikasi dan layar alarm hanya menampilkan pengirim dan kata kunci; isi pesan baru terlihat setelah "Buka kunci untuk membaca". Mematikan siaga dari layar kunci (tile Quick Settings atau tombol Jeda) meminta PIN.
- **Responsif**: tombol geser-untuk-mematikan selalu terlihat (HP pendek & landscape), lebar konten maksimum 600dp di tablet, aman untuk font besar dan layar 320–360dp.

---

##  Pendamping animasi (GIF)

Aplikasi memutar animasi dari `app/src/main/assets/` di Beranda, onboarding, dan layar alarm (Android 9+):

- **`companion.webp`** (WebP animasi **berlatar transparan**) → karakter tampil melayang tanpa bingkai. Ini yang dipakai sekarang: hasil konversi dari `art/companion_source.gif` (latar putih dihapus, bagian bawah diberi fade).
- **`companion.gif`** → tampil di dalam bingkai hologram emas (cocok untuk GIF berlatar solid).

Jika keduanya ada, `companion.webp` yang dipakai. Tanpa file tersebut (atau di Android 8), aplikasi menampilkan maskot dari ikon. File besar otomatis diperkecil saat didekode.

> ⚠️ GIF karakter HSR (mis. Evernight) adalah hak cipta HoYoverse. Bundel hanya untuk pemakaian pribadi; jangan distribusikan APK-nya ke publik.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3 (tema kustom)
- **Architecture**: MVVM + Repository Pattern
- **Storage**: Jetpack DataStore Preferences
- **Concurrency**: Kotlin Coroutines & Flow
- **Background Service**: Android `NotificationListenerService` & `ForegroundService` (Media Playback)
- **Min SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15 / Xiaomi HyperOS 2)

Logika inti tanpa ketergantungan Android (bisa di-unit-test): `util/KeywordMatcher`, `util/KeywordValidator`, `util/MessageDeduplicator`, `util/WhatsAppMessageParser`, `util/TriggerPlanner`, `data/AlarmHistoryCodec`.

---

## 🚀 Cara Build & Menjalankan

### Persyaratan:
- JDK 17+
- Android SDK (API 35)

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

### Checklist uji di HP
1. Onboarding: ganti bahasa ID/EN di langkah pertama, berikan semua izin.
2. Beranda: aktifkan siaga saat izin belum lengkap → muncul dialog "Belum siap".
3. Tes alarm dengan HP terkunci → layar alarm muncul, geser untuk mematikan terasa bergetar tiap seperempat.
4. Kirim 2 pesan darurat beruntun → alarm bunyi sekali, layar menampilkan "+1 pesan darurat lain", dan keduanya ada di Riwayat.
5. Balas pesan dari notifikasi dengan kata kunci → alarm tidak bunyi.
6. Pengaturan: pilih file audio, hapus filenya, buka Pengaturan lagi → muncul peringatan nada.
7. Matikan animasi di Opsi Developer → starfield & pendamping diam.
8. HP dikunci dengan PIN, kirim pesan darurat → pengirim & isi pesan tersembunyi di notifikasi & layar alarm; "Buka kunci untuk membaca" menampilkannya.
9. HP terkunci → tile Quick Settings / tombol "Jeda" meminta PIN sebelum siaga dimatikan.
10. Jangan Ganggu "Hening total" (Android 8–14, izin DND diberikan) → alarm tetap bunyi, DND kembali seperti semula setelah dimatikan.
11. HP layar kecil (360×640) dan landscape → tombol geser-untuk-mematikan terlihat tanpa scroll.

---

## 🔒 Izin Khusus yang Diperlukan

**Wajib:** Akses Notifikasi, Tampilkan Notifikasi, Alarm Layar Penuh (Android 14+), Tanpa Batasan Baterai.
**Disarankan:** Tampil di Atas Aplikasi Lain, Akses Jangan Ganggu, dan (Xiaomi/HyperOS) Autostart.

Semua bisa diberikan dari layar **Izin** di aplikasi.

---

## 📄 Lisensi aset

- Font **Rajdhani** © Indian Type Foundry, SIL Open Font License 1.1 — lihat `licenses/OFL-Rajdhani.txt` (di-subset ke huruf Latin).
- Ikon & maskot: karya pemilik proyek (`art/app_icon_source.webp`).

---

## 👨‍💻 Author
Made with ❤️ by **Kindness**
