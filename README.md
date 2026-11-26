# SIG_Mapdroid 🗺️📱
**Aplikasi Android Sistem Informasi Geografis (SIG) & Peta Interaktif Kampus ULBI**

Aplikasi mobile Android dan website landing page interaktif untuk Sistem Informasi Geografis (SIG) kawasan kampus Universitas Logistik dan Bisnis Internasional (ULBI) Bandung, ditenagai oleh engine **[MapLibre.org](https://maplibre.org)**, **[CrootJS](https://github.com/jscroot)**, dan **GitHub Actions CI/CD**.

---

## 📌 Rincian Penyelesaian Tugas (Sesuai Perintah)

### 1. GitHub Workflow Auto Tag & Release APK Begitu di-Push
- **File Workflow:** [`.github/workflows/auto-release.yml`](.github/workflows/auto-release.yml)
- **Mekanisme Otomatis:**
  - Terpicu secara otomatis setiap kali ada **push** ke branch `main` atau `master` (serta mendukung trigger manual `workflow_dispatch`).
  - Mengompilasi aplikasi Android secara otomatis menggunakan Gradle (`gradle assembleDebug`).
  - Mendeteksi tag semantik terakhir (`vX.Y.Z`) dari repositori, kemudian menaikkan patch version secara otomatis (contoh: `v1.0.0` ➔ `v1.0.1`).
  - Membuat **GitHub Release** baru dengan tag yang baru dibuat dan menyematkan berkas `SIG_Mapdroid-${NEW_TAG}.apk` sebagai aset rilis.
  - Pengguna dapat langsung mengunduh berkas `.apk` pada menu [Releases](https://github.com/zahraramanayshilla/SIG_Mapdroid/releases).

### 2. Landing Page (`index.html`) Menggunakan CrootJS
- **File Utama:** [`index.html`](index.html), didukung oleh skrip modular [`js/main.js`](js/main.js) dan [`css/style.css`](css/style.css).
- **Integrasi CrootJS (`jscroot`):**
  - Mengimpor pustaka mikro CrootJS langsung melalui CDN jsDelivr:
    ```javascript
    import { getJSON } from "https://cdn.jsdelivr.net/gh/jscroot/lib@0.2.8/api.min.js";
    import { setInner } from "https://cdn.jsdelivr.net/gh/jscroot/lib@0.2.8/element.min.js";
    ```
  - Mengambil data rilis terakhir secara dinamis melalui GitHub API (`https://api.github.com/repos/zahraramanayshilla/SIG_Mapdroid/releases/latest`).
  - Secara otomatis memperbarui tombol unduh langsung dengan tautan unduhan berkas APK rilis terakhir (`browser_download_url`), menampilkan ukuran berkas, versi tag rilis, serta tanggal rilis.
  - Dilengkapi *graceful fallback* jika rilis baru sedang diproses.
- **Isi Panduan Lengkap:**
  - **Panduan Instalasi APK Android:** Langkah demi langkah mengunduh, mengizinkan instalasi dari *Sumber Tidak Dikenal (Unknown Sources)*, dan memasang APK.
  - **Panduan Fitur Aplikasi Mobile:** Navigasi gestur peta, pelacakan GPS, pengalihan basemap, dan pemeriksaan atribut spasial.

### 3. Aplikasi Peta Menggunakan MapLibre.org
Proyek ini mengimplementasikan MapLibre pada dua platform:
1. **Aplikasi Mobile Android (MapLibre Native Android SDK):**
   - Menggunakan dependensi resmi `org.maplibre.gl:android-sdk-opengl:11.5.1`.
   - Kode utama: [`app/src/main/java/com/zahra/sigmapdroid/MainActivity.kt`](app/src/main/java/com/zahra/sigmapdroid/MainActivity.kt).
   - Memuat data spasial GeoJSON kawasan kampus ULBI ([`app/src/main/assets/kampus_ulbi.geojson`](app/src/main/assets/kampus_ulbi.geojson)) yang mencakup poligon gedung (Gedung Pos, Vokasi, FLTB, Rektorat, Student Centre), rute jalan, dan titik fasilitas/gerbang.
   - Fitur interaktif:
     - Pelacakan posisi pengguna secara realtime via sensor GPS (`LocationComponent`).
     - Multi-Basemap Switcher (OpenStreetMap, Carto Voyager, MapLibre Demo).
     - Deteksi klik objek spasial untuk memunculkan detail atribut nama dan koordinat.
     - Tombol re-center kamera ke koordinat kampus ULBI Bandung (`-6.8732° LS, 107.5758° BT`).
2. **Peta Web Interaktif (MapLibre GL JS):**
   - Disematkan langsung di dalam [`index.html`](index.html) menggunakan `maplibre-gl.js v4.7.1`.
   - Menampilkan visualisasi data spasial kampus ULBI dengan popup interaktif, toggle layer gedung/jalan/titik, dan pilihan basemap.

---

## 📁 Struktur Repositori

```text
SIG_Mapdroid/
├── .github/
│   └── workflows/
│       └── auto-release.yml         # GitHub Actions Auto Tag & Release APK
├── app/                             # Modul Aplikasi Android MapLibre Native
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── kampus_ulbi.geojson  # Data spasial GeoJSON kampus ULBI
│   │   ├── java/com/zahra/sigmapdroid/
│   │   │   └── MainActivity.kt      # Logika utama MapLibre Android
│   │   ├── res/                     # Layout XML, tema, drawable, ikon
│   │   └── AndroidManifest.xml      # Izin Internet, Jaringan, dan GPS
│   ├── build.gradle                 # Konfigurasi dependensi MapLibre SDK
│   └── proguard-rules.pro
├── css/
│   └── style.css                    # Gaya visual kustom & glassmorphic
├── data/
│   └── kampus_ulbi.geojson          # Salinan data GeoJSON untuk peta web
├── js/
│   └── main.js                      # CrootJS GitHub API fetch & MapLibre Web
├── build.gradle                     # Top-level Gradle build file
├── settings.gradle                  # Gradle project settings
├── gradle.properties                # Properti JVM & AndroidX
├── gradlew                          # Wrapper skrip Linux/macOS
├── gradlew.bat                      # Wrapper skrip Windows
├── index.html                       # Landing page utama & peta web
└── README.md                        # Dokumentasi proyek
```

---

## 🚀 Cara Menjalankan & Mengembangkan

### 1. Menjalankan Landing Page & Peta Web Secara Lokal
Jalankan server HTTP lokal di folder root:
```bash
python -m http.server 8000
```
Buka browser pada alamat:
`http://localhost:8000`

### 2. Mengaktifkan GitHub Pages (Opsional untuk Hosting Gratis)
1. Buka repositori Anda di GitHub: **Settings** ➔ **Pages**.
2. Pada bagian **Build and deployment**, pilih **Deploy from a branch**.
3. Pilih branch `main` dan folder `/(root)`, lalu klik **Save**.
4. Website landing page Anda akan aktif di `https://zahraramanayshilla.github.io/SIG_Mapdroid/`.

### 3. Menguji Build APK di Komputer Lokal
Jika Anda memiliki Android SDK dan Java terpasang:
```bash
./gradlew assembleDebug
```
Berkas APK akan dihasilkan di direktori:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Rilis Otomatis ke GitHub Releases
Cukup lakukan `git commit` dan `git push` ke branch `main`:
```bash
git add .
git commit -m "feat: complete SIG Mapdroid with MapLibre, CrootJS, and GitHub Actions Auto Release"
git push origin main
```
GitHub Actions akan secara otomatis menjalankan workflow, mengompilasi APK, membuat tag semantik baru, dan mempublikasikan APK ke tab [Releases](https://github.com/zahraramanayshilla/SIG_Mapdroid/releases).

---

## 👤 Informasi Pengembang

- **Nama:** Zahra Ramanayshilla
- **NPM:** 714240003
- **Mata Kuliah:** Sistem Informasi Geografis (SIG) — Semester 5
- **Institusi:** Universitas Logistik dan Bisnis Internasional (ULBI) Bandung
