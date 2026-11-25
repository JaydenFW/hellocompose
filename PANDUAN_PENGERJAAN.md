# Praktikum Pertemuan 12 — CatatanKu Foto

| | |
|---|---|
| **Nama** | Jayden FW |
| **NIM** | 01082240004 |
| **Email** | 01082240004@student.uph.edu |
| **Repo** | https://github.com/JaydenFW/hellocompose (branch `pertemuan-12`) |

## Ringkasan pekerjaan

Semua TODO sudah dikerjakan, dan jendela TODO di Android Studio kosong.

| ID | File | Isi |
|---|---|---|
| T2.1 | `ui/profile/ProfileRoute.kt` | Photo Picker `PickVisualMedia(ImageOnly)` tanpa izin |
| T2.2 | `ui/profile/ProfileViewModel.kt` | `onPhotoSelected`: abaikan jika sibuk, simpan ke `SavedStateHandle`, tampilkan preview |
| T3.1 | `AndroidManifest.xml` | `CAMERA` + `camera.any required=false` |
| T3.2 | `util/CameraPermission.kt` | `cameraClickAction` dan `permissionResultAction` |
| T3.3 | `ui/profile/ProfileRoute.kt` | launcher `RequestPermission()`; izin diminta saat tombol Kamera diketuk |
| T4.1 | `util/ImageMath.kt` | `calculateInSampleSize` dengan pangkat 2 |
| T4.2 | `data/photo/AndroidImageCompressor.kt` | dekode 2 tahap, crop, skala 400 px, rotasi EXIF, JPEG 80, `recycle()` |
| T4.3 | `data/photo/FirebasePhotoStorage.kt` | `callbackFlow` atas `putBytes` → `Progress`/`Done`, `awaitClose { cancel }` |
| T4.4 | `ui/profile/ProfileViewModel.kt` | `upload()`: kompres → upload dengan progres → simpan URL |
| T4.5 | `data/profile/FirestoreProfileRepository.kt` | `set(..., SetOptions.merge())` tanpa `await()` |
| T4.6 | `storage.rules`, `firestore.rules` | Storage: pemilik, < 1 MB, `image/jpeg`. Firestore: `users/{userId}` + `isValidProfile()` |
| T5 | `README.md` → bagian Laporan | alur izin, alasan galeri tanpa izin, hasil kompresi |
| T6.1–6.2 | `ImageMath.kt`, `AndroidImageCompressor.kt` | `centerSquare` → hasil 400×400 |

---

## Tugas 1 — Firebase (pakai proyek Ch11: `catatanku-jayden`)

### T1.1 dan T1.2: daftarkan app baru dan unduh JSON
1. Buka Firebase Console → **catatanku-jayden** → ⚙️ **Project settings** → **Your apps** → **Add app** → **Android**.
2. Isi **package name `com.example.ch12`**. Salin persis, jangan sampai salah ketik seperti di Ch11.
3. **Register app** → **Download google-services.json** → timpa `app/google-services.json`.
4. Buka file itu dan pastikan baris `"storage_bucket"` terisi, misalnya `catatanku-jayden.firebasestorage.app`.
   Kalau JSON diunduh **sebelum** Storage diaktifkan (T1.3), unduh ulang setelah Storage aktif.

### T1.3: aktifkan Cloud Storage
Cloud Storage untuk proyek baru membutuhkan paket **Blaze**. Pilih salah satu cara berikut.

**Pilihan A: paket Blaze (ada kartu kredit/debit)**
1. Klik **Upgrade** di kiri bawah console → pilih **Blaze** → hubungkan akun billing.
2. Pasang budget alert: Google Cloud Console → **Billing → Budgets & alerts** → budget kecil (misalnya Rp10.000) dengan notifikasi email.
   Pemakaian tugas ini jauh di bawah kuota gratis.
3. Buka **Build → Storage → Get started** → **Start in production mode** → pilih lokasi yang **sama dengan Firestore** (`asia-southeast2`).
4. Unduh ulang `google-services.json` (lihat T1.2 langkah 4).

**Pilihan B: tanpa Blaze, pakai Firebase Emulator Suite**
1. Pasang Node.js, lalu jalankan `npm install -g firebase-tools`. Java 11+ sudah tersedia dari Android Studio/JDK.
2. Di folder project, jalankan `firebase emulators:start --project demo-catatanku`.
3. Di `gradle.properties`, tambahkan `catatanku.useFirebaseEmulator=true` → Sync → Run di **emulator Android**.
4. Buka UI emulator di http://localhost:4000. Rules dibaca dari `firestore.rules` dan `storage.rules` di folder ini.

### Publish rules (Pilihan A)
- **Storage → Rules**: tempel isi `storage.rules` → **Publish**.
- **Firestore → Rules**: tempel isi `firestore.rules` → **Publish**. Ini termasuk aturan catatan dari Ch11 dan aturan baru `users/{userId}`.

Cara lain lewat CLI: `firebase deploy --only storage,firestore:rules`.

---

## Uji dan screenshot yang dikumpulkan

```bash
./gradlew testDebugUnitTest     # targetnya 81 tes hijau
```

Uji kamera sebaiknya di HP fisik. Kalau pakai emulator, buka **Extended controls → Camera** dan pilih `VirtualScene`.

Checklist (✅ = **ambil screenshot**):
- [ ] Avatar di kanan atas daftar catatan → layar **Foto Profil**.
- [ ] **Galeri** → pilih foto **tanpa dialog izin** → muncul preview → **Unggah** → ✅ **progres upload**.
- [ ] Avatar di daftar catatan berubah.
- [ ] Teks kompresi menunjukkan ukuran **< 100 KB**. Salin angkanya ke tabel "Hasil kompresi" di README.
- [ ] **Kamera** pertama kali → ✅ **dialog izin sistem** muncul setelah tombol diketuk.
- [ ] Tolak sekali → Snackbar galeri. Ketuk Kamera lagi → dialog alasan.
- [ ] Tolak lagi → ketuk Kamera → dialog **Buka Pengaturan**.
- [ ] Firestore → Data: `users/{uid}` hanya berisi `photoUrl` dan `photoUpdatedAt`.
- [ ] ✅ **Rules Playground Storage** (lihat bawah) → **Denied**.

Untuk mengulang dari awal pengujian izin: **Pengaturan HP → Apps → CatatanKu → Permissions → Camera → Don't allow / Ask every time**, atau jalankan `adb shell pm reset-permissions`.

### Rules Playground Storage
1. Siapkan dua akun, A dan B. Akun A sudah pernah mengunggah foto. Salin UID A dan UID B dari **Authentication → Users**.
2. **Storage → Rules → Rules Playground**:
   - Simulation type: **create**
   - Location: `users/<UID_A>/profile.jpg`
   - Authenticated: **ON**, Firebase UID: `<UID_B>`
   - Kalau ada isian content type/size, pakai `image/jpeg` dengan ukuran kecil, misalnya 40000.
3. **Run** → hasilnya **Denied** → screenshot.
4. Opsional: dengan UID **A**, hasilnya harus **Allowed**.

Perkecil screenshot sebelum dilampirkan atau di-commit (< 1 MB, jangan commit gambar besar).

---

## Pengumpulan (branch `pertemuan-12`)

ZIP ini sudah berisi repo git dengan branch `pertemuan-12` yang sudah di-commit.

```bash
cd ch12_starter
# setelah mengganti app/google-services.json dengan file asli:
git add app/google-services.json README.md
git commit -m "Pertemuan 12: google-services.json + hasil kompresi"

git remote add origin https://github.com/JaydenFW/hellocompose.git
git push -u origin pertemuan-12
```

Kumpulkan ke LMS: `https://github.com/JaydenFW/hellocompose/tree/pertemuan-12`,
ditambah screenshot dialog izin, progres upload, dan Rules Playground.
