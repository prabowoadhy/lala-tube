# LalaTube — Video Offline untuk Anak

Aplikasi pemutar video bergaya YouTube untuk **head unit Android** (dan HP Android),
dirancang agar anak bisa menonton video langsung dari **flashdisk tanpa internet**.

- Tampilan landscape, ramah layar sentuh dan tombol D-pad.
- Seluruh antarmuka **berbahasa Indonesia**.
- **Tanpa izin INTERNET** — aplikasi benar-benar offline.
- Video diputar langsung dari flashdisk, **tidak disalin** ke memori internal.

---

## Cara Pasang

1. Unduh APK rilis terbaru dari halaman **Releases**
   (<https://github.com/prabowoadhy/lala-tube/releases>), lalu salin ke flashdisk.
2. Di head unit, buka aplikasi File Manager, ketuk APK tersebut, lalu pilih **Instal**.
   Jika muncul peringatan, aktifkan **Instal dari sumber tidak dikenal** untuk File Manager.
3. Buka **LalaTube**.

> APK tidak disimpan di dalam repo ini. Folder `dist/` hanya dipakai di komputer
> pengembang untuk menaruh hasil build sebelum diunggah ke halaman Releases.

> Saat pertama dibuka, aplikasi akan meminta izin membaca foto/video. Pilih **Allow**
> (Izinkan). Tanpa izin ini daftar folder akan terlihat kosong.

---

## Menyiapkan Flashdisk

Buat folder di akar flashdisk, misalnya `LalaTube`. Setiap **subfolder** menjadi satu
"kanal" (seperti channel YouTube), dan file video yang diletakkan langsung di dalam
folder utama masuk ke kanal **Video Offline**.

```
LalaTube/
├── video_akar.mp4              -> kanal "Video Offline"
├── Lagu Anak/
│   ├── balonku.mp4             -> kanal "Lagu Anak"
│   └── cicak.mp4
├── Kartun/
│   ├── upin.mp4                -> kanal "Kartun"
│   ├── upin.jpg                -> poster/thumbnail untuk upin.mp4
│   └── Ipin/
│       └── ipin.mp4            -> tetap masuk kanal "Kartun"
└── Belajar/
    └── angka.mkv               -> kanal "Belajar"
```

Aturan penamaan:

- **Nama kanal** = nama subfolder tingkat pertama di dalam folder yang dipilih.
- **Judul video** = nama file tanpa ekstensi, dengan `_` diganti spasi.
  Contoh `balonku_ada_lima.mp4` → "balonku ada lima".
- **Poster (opsional)**: letakkan file gambar dengan nama sama di samping video,
  mis. `upin.mp4` + `upin.jpg`. Kalau tidak ada, thumbnail dibuat otomatis dari
  detik pertama video.

Format video yang didukung: **MP4, MKV, WEBM, AVI, MOV, M4V, TS, 3GP, FLV, WMV**
(dan format lain yang dikenali pemutar Android). Poster: **JPG, PNG, WEBP**.

> **Catatan:** di Android 11 ke atas, akar penyimpanan tidak bisa dipilih langsung.
> Pilih subfolder seperti `LalaTube`, bukan seluruh flashdisk.

---

## Memilih Folder di Head Unit

Head unit biasanya **tidak punya aplikasi pemilih folder Android (DocumentsUI)**.
Karena itu LalaTube memakai **browser folder bawaan sendiri**:

1. Di Beranda, ketuk **Pilih folder di flashdisk**.
2. Izinkan akses file bila diminta.
3. Pilih penyimpanan: **Penyimpanan internal** atau **Flashdisk**.
4. Telusuri folder, lalu ketuk **Pilih** pada folder yang diinginkan
   (mis. `LalaTube`).

Folder yang dipilih **diingat** sampai flashdisk dicabut, jadi tidak perlu memilih
ulang setiap menyalakan head unit.

### Kalau folder tidak bisa dibuka

Gunakan tombol **Cari otomatis semua video**. Mode ini mencari seluruh video yang
sudah terdaftar di Android (MediaStore), jadi tidak perlu memilih folder sama sekali.
Cocok untuk head unit yang membatasi akses folder.

---

## Cara Pakai

| Bagian | Fungsi |
| --- | --- |
| **Beranda** | Video terbaru, filter per kanal |
| **Favorit** | Video yang ditandai bintang |
| **Riwayat** | Video yang pernah/sedang ditonton |
| **Pengaturan** | Ganti folder, pindai ulang, tema, mode aman |
| **Cari** | Mencari judul video dan kanal |

Di halaman tonton:

- Pemutar di kiri, informasi video di bawahnya, rekomendasi di samping kanan.
- Header aplikasi disembunyikan di halaman tonton, seperti YouTube. Tombol
  **Kembali** dan **Layar penuh** muncul di overlay pemutar saat layar disentuh.
- **Layar penuh** untuk mode bioskop.
- Video berikutnya diputar otomatis.
- Posisi tontonan disimpan; menonton lagi melanjutkan dari titik terakhir.
- Tombol **Favorit** menandai video.

### Tampilan mengikuti ukuran layar

- Di layar 2K (sekitar 1280dp, seperti YouTube di tablet) grid menampilkan
  **3 kolom** video. Layar sangat lebar (≥1800dp) memakai 4 kolom, layar kecil
  2 kolom.
- Header atas otomatis tersembunyi saat daftar digulir ke bawah dan muncul lagi
  saat digulir ke atas, sama seperti aplikasi YouTube.

### Tombol D-pad / remot

Arahkan fokus dengan tombol panah, tekan **OK/Enter** untuk memilih,
**Back** untuk kembali.

---

## Pengaturan Penting

- **Ganti folder** — memilih folder video lain.
- **Pindai ulang** — membaca ulang isi flashdisk setelah menambah video.
  Jalankan ini setelah menyalin video baru.
- **Cari otomatis semua video** — mode cadangan tanpa memilih folder.
- **Lupakan folder ini** — melepas folder dari aplikasi. File di flashdisk **tidak
  dihapus**; favorit dan riwayat tetap tersimpan dan akan kembali jika folder yang
  sama dipilih lagi.
- **Mode aman** — melewati pembuatan thumbnail. Aktifkan jika aplikasi terasa berat
  atau sering berhenti saat memindai banyak video.
- **Tema** — Terang, Gelap, atau Ikuti sistem.

---

## Jika Terjadi Masalah

Aplikasi ini tidak akan menutup sendiri saat ada video rusak. Jika pernah berhenti,
laporan singkat tersimpan otomatis dan bisa dilihat di **Pengaturan → Laporan
masalah terakhir**, lengkap dengan tombol untuk menghapusnya.

| Gejala | Solusi |
| --- | --- |
| Daftar folder kosong | Beri izin baca file, atau pakai **Cari otomatis semua video** |
| Video tidak muncul | Pastikan format didukung, lalu **Pindai ulang** |
| Flashdisk tidak terdeteksi | Cabut dan colok ulang, lalu buka pemilih folder lagi |
| Aplikasi berat saat memindai | Aktifkan **Mode aman**, lalu pindai ulang |
| Thumbnail hitam | Normal untuk sebagian video; video tetap bisa diputar |

---

## Membangun dari Sumber

Prasyarat: JDK 17 dan Android SDK (compileSdk 37).

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio2\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Hasil: `app/build/outputs/apk/debug/app-debug.apk`.

---

## Struktur Singkat

```
app/src/main/java/com/lalatube/offline/
├── MainActivity.kt          Titik masuk
├── LalaTubeApp.kt           Kerangka navigasi & pemilih folder
├── MainViewModel.kt         Status aplikasi, pemindaian, izin
├── LalaTubeApplication.kt   Pencatat crash
├── data/
│   ├── LibraryRepository.kt Pemindaian folder, metadata, thumbnail
│   ├── StorageAccess.kt     Browser penyimpanan tanpa DocumentsUI
│   └── Video.kt             Model video & tema
└── ui/
    ├── screens/             Beranda, tonton, favorit, riwayat, cari,
    │                        pengaturan, pemilih folder
    └── components/          Komponen bersama
```

Video disimpan sebagai URI `file://` (browser bawaan) atau `content://`
(mode MediaStore), sehingga tidak ada file yang perlu disalin.
