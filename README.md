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
│   ├── Kartun.jpg              -> foto profil (avatar) kanal "Kartun"
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
- **Foto profil kanal (opsional)**: letakkan satu gambar di dalam folder kanal dengan
  nama sama seperti nama foldernya, mis. `Kartun/Kartun.jpg`. Nama umum seperti
  `avatar.jpg`, `kanal.png`, atau `logo.webp` juga diterima. Kalau tidak ada, avatar
  memakai inisial huruf seperti biasa.

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
| **Pengaturan** | Ganti folder, pindai ulang, tema, mode aman, cek codec |
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

- Di layar 2K head unit (2000x1200, sekitar 1000dp seperti YouTube di tablet)
  grid menampilkan **3 kolom** video dengan ukuran kartu dan jarak yang sama
  dengan YouTube. Layar sangat lebar (≥1800dp) memakai 4 kolom, layar kecil
  2 kolom.
- Teks di bawah thumbnail (nama kanal, status, tanggal, sisa menit) digabung
  dalam satu baris seperti YouTube. Bila ruangnya kurang, teks otomatis turun
  ke baris berikutnya. Kartu **Lanjutkan menonton** memakai susunan yang sama:
  avatar kanal di kiri, lalu judul dan subjudul.
- Urutan video di beranda **diacak setiap aplikasi dibuka** atau tombol
  **Pindai ulang** ditekan, jadi anak tidak bosan dengan susunan yang sama
  terus-menerus. Rekomendasi di halaman tonton juga diacak dengan aturan yang
  sama. Favorit, riwayat, dan **Lanjutkan menonton** tidak ikut diacak.
- Kartu di beranda dimuat **bertahap 15 per batch**. Gulir sampai bawah untuk
  memuat batch berikutnya, sehingga koleksi 100+ video tetap ringan.
- Header atas selalu tampil dengan logo dan judul selebar aplikasi YouTube.
- Menu **Beranda / Favorit / Riwayat / Pengaturan** berada di tengah bawah
  layar, sama seperti aplikasi YouTube Android. Di halaman tonton menu ini
  disembunyikan agar pemutar penuh dari sudut kiri atas.
- **Status bar dan tombol navigasi Android** (Back/Home/Recent atau gesture
  pill) tetap terlihat seperti YouTube di head unit. Konten diberi jarak aman
  supaya tidak tertutup tombol navigasi.

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
- **Kompatibilitas codec** — memeriksa codec setiap video dan memberi tahu jika ada
  video yang hanya bisa didekode lewat CPU (penyebab pemutaran patah-patah).
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
| **Video patah-patah** | Lihat bagian di bawah 👇 |

### Video patah-patah (codec AV1 / H.265)

Penyebab paling umum pemutaran tersendat **bukan** aplikasi, melainkan **codec video**
yang tidak didukung hardware head unit. Head unit kelas Allwinner T527 misalnya
memutar MP4 **H.264** langsung dengan chip khusus (lancar), tetapi file **AV1** besar
(apalagi 4K) hanya bisa didekode lewat CPU sehingga gambar patah-patah, walaupun
audio tetap jalan.

Cara mengetahui dan mengatasinya:

1. Buka **Pengaturan → Kompatibilitas codec**. Bagian ini menunjukkan jumlah video
   yang memakai codec tanpa dukungan hardware beserta judulnya.
2. Di daftar video, video seperti ini diberi **penanda peringatan ⚠** di pojok
   thumbnail (mis. `AV1`). Penjelasan juga muncul di halaman tonton.
3. Ubah file tersebut ke **MP4 H.264** (720p atau 1080p) memakai aplikasi konversi
   video gratis di komputer (mis. HandBrake), lalu **Pindai ulang**.

Head unit 2000×1200 juga tidak butuh video 4K — 1080p sudah lebih dari cukup dan
lebih hemat memori, sehingga jauh lebih lancar.


---

## Membangun dari Sumber

Prasyarat: JDK 17 dan Android SDK (compileSdk 37).

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio2\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Hasil: `app/build/outputs/apk/debug/app-debug.apk`.

---

## Merilis Versi Baru

APK **tidak disimpan di dalam repo** (folder `dist/` diabaikan git). Setiap rilis
diunggah manual ke halaman
[Releases](https://github.com/prabowoadhy/lala-tube/releases).

1. Naikkan versi di `app/build.gradle.kts` — `versionCode` wajib selalu bertambah,
   `versionName` adalah nomor yang dilihat pengguna:
   ```kotlin
   versionCode = 6
   versionName = "1.0.5"
   ```
2. Bangun dan uji:
   ```powershell
   .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
   ```
3. Salin APK ke `dist/` sebagai arsip lokal (opsional):
   ```powershell
   Copy-Item app\build\outputs\apk\debug\app-debug.apk dist\LalaTube-1.0.5-debug.apk
   ```
4. Commit lalu push:
   ```powershell
   git add -A
   git commit -m "LalaTube 1.0.5: ..."
   git push origin main
   ```
5. Publish rilis beserta APK-nya:
   ```powershell
   gh release create v1.0.5 dist\LalaTube-1.0.5-debug.apk `
     --title "LalaTube 1.0.5" --notes "Perubahan: ..."
   ```
   Bisa juga lewat web: buka
   <https://github.com/prabowoadhy/lala-tube/releases/new>, pilih tag `v1.0.5`
   (buat baru), lampirkan APK, lalu **Publish release**.

> **Penting:** rilis baru hanya muncul di sidebar *Releases* setelah langkah 5
> selesai. Push tag saja tidak cukup — tag tanpa rilis hanya tercatat di daftar
> Tags. Selalu build di komputer yang sama agar tanda tangan APK konsisten dan
> pembaruan bisa menimpa versi lama di head unit.

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
