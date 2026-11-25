# Praktikum Pertemuan 11 — CatatanKu Cloud

| | |
|---|---|
| **Nama** | Jayden FW |
| **NIM** | 01082240004 |
| **Email** | 01082240004@student.uph.edu |
| **Repo** | https://github.com/JaydenFW/hellocompose (branch `pertemuan-11`) |

## Ringkasan pekerjaan

Semua TODO di kode sudah dikerjakan dan komentar `TODO` sudah dihapus (jendela TODO kosong).

| ID | File | Isi |
|---|---|---|
| T2.1 | `data/auth/FirebaseAuthRepository.kt` | `authState` = `callbackFlow` + `AuthStateListener` + `awaitClose`, `distinctUntilChanged()`; `login`/`register` pakai `.await()` |
| T2.2 | `ui/auth/LoginViewModel.kt` | cegah klik ganda → `validateLogin` → `authRepository.login(email.trim(), password)` |
| T2.3 | `ui/auth/RegisterViewModel.kt` | cegah klik ganda → `validateRegistration` → `authRepository.register(...)` |
| T2.4 | `ui/session/SessionViewModel.kt` | `authState.stateIn(viewModelScope, Eagerly, initialValue = currentUser)` |
| T2.5 | `CatatanKuApp.kt` | `LaunchedEffect(isSignedIn)` → navigasi + back stack dikosongkan |
| T3.1 | `data/note/NoteMapper.kt` | `toFirestoreMap()` (6 field) dan `noteFromMap()` aman (`as?` + default) |
| T3.2 | `data/note/FirestoreNoteRepository.kt` | `callbackFlow` + `addSnapshotListener(MetadataChanges.INCLUDE)` + `awaitClose { remove() }` |
| T3.3 | file yang sama | `getNote` pakai `.get().await()`; `add/update/delete` **tanpa** `await()` |
| T4.1 | `firestore.rules` | fungsi `isOwner(userId)`, hanya pemilik yang bisa baca/tulis |
| T4.2 | file yang sama | `isValidNote()`: 6 field wajib (tidak boleh ada field lain), tipe benar, judul 1–200 karakter |
| T5.1 | `ui/auth/GoogleSignIn.kt` | Credential Manager + `GetGoogleIdOption` |
| T5.2 | `data/auth/FirebaseAuthRepository.kt` | `GoogleAuthProvider.getCredential(idToken, null)` → `signInWithCredential` |
| T5.3 | `ui/auth/LoginViewModel.kt` | `onGoogleIdToken()` → `authRepository.signInWithGoogle(idToken)` |

Aturan tugas tetap dipatuhi: ViewModel tidak meng-import `com.google.firebase`, password tidak
disimpan, tidak ada `navigate()` di LoginScreen/RegisterScreen, penulisan Firestore tidak
di-`await()`, dan rules tidak memakai `if true` atau hanya `request.auth != null`.

---

## Tugas 1 — Membuat proyek Firebase (WAJIB dikerjakan sendiri di console)

> `app/google-services.json` di repo ini **masih placeholder**. Selama belum diganti,
> login akan menampilkan *"Konfigurasi Firebase tidak valid…"*.

1. **Buat proyek (T1.1)**
   - Buka https://console.firebase.google.com → **Create a project** (Tambah proyek).
   - Nama bebas, misalnya `catatanku-jayden`. Google Analytics boleh dimatikan.
   - Setelah proyek jadi, klik ikon **Android** (Add app).
   - **Android package name: `com.example.ch11`** (harus sama persis).
   - App nickname bebas. SHA-1 boleh dikosongkan dulu (dibutuhkan untuk Tugas 5).
   - Klik **Register app**.
2. **Unduh `google-services.json` (T1.2)**
   - Klik **Download google-services.json**.
   - Timpa file `app/google-services.json` di project ini dengan file tersebut.
   - Langkah "Add Firebase SDK" di console dilewati saja (Gradle sudah siap) → **Next → Continue to console**.
3. **Aktifkan Email/Password (T1.3)**
   - Menu **Build → Authentication → Get started**.
   - Tab **Sign-in method → Email/Password → Enable → Save**.
4. **Buat Firestore (T1.4)**
   - Menu **Build → Firestore Database → Create database**.
   - Lokasi: `asia-southeast2 (Jakarta)`. Pilih **Start in production mode** → **Create**.
5. **Publish Security Rules (Tugas 4)**
   - **Firestore Database → tab Rules**. Untuk bukti "sebelum", tempel dulu rules starter:
     ```
     rules_version = '2';
     service cloud.firestore {
       match /databases/{database}/documents {
         match /users/{userId}/notes/{noteId} {
           allow read, write: if request.auth != null;
         }
       }
     }
     ```
     lalu **Publish** dan ambil screenshot Playground (lihat bagian di bawah).
   - Setelah itu tempel **seluruh isi `firestore.rules`** dari repo ini → **Publish**.

### Tugas 5 (Google Sign-In) — opsional/tantangan
1. **Authentication → Sign-in method → Add new provider → Google → Enable**, isi support email → Save.
2. Ambil SHA-1 debug di terminal Android Studio:
   ```bash
   ./gradlew signingReport
   ```
   Salin nilai `SHA1` dari varian `debug`.
3. **Project settings (ikon gear) → Your apps → Android app → Add fingerprint** → tempel SHA-1.
4. **Unduh ulang** `google-services.json` dan timpa lagi `app/google-services.json`.
5. Uji di emulator dengan image **Google Play** yang sudah login akun Google.

---

## Menjalankan dan menguji

```bash
./gradlew testDebugUnitTest      # harus 48 tes hijau
```
Lalu **Run ▶** di Android Studio.

Checklist uji manual:
- [ ] Registrasi → langsung masuk daftar catatan; logout → layar login, tombol **Back** tidak kembali ke daftar.
- [ ] Tutup lalu buka aplikasi → masih login.
- [ ] Catatan muncul di **Firestore → Data** pada `users/{uid}/notes`.
- [ ] Ubah data di console → aplikasi berubah dalam beberapa detik.
- [ ] Mode pesawat → tambah catatan → muncul seketika dengan ikon *Menunggu sinkron* → online → ikon hilang.

### Bukti Tugas 4 (Rules Playground) — screenshot WAJIB
1. Daftar akun **A**, buat 2 catatan. Di **Firestore → Data** salin UID A dan ID salah satu catatan.
2. Logout → daftar akun **B** (daftar catatan B harus kosong). Salin UID B dari **Authentication → Users**.
3. **Firestore → Rules → Rules Playground**:
   - Simulation type: **get**
   - Location: `/users/<UID_A>/notes/<ID_CATATAN>`
   - **Authenticated** = ON, Firebase UID = `<UID_B>` → **Run**.
4. Dengan rules starter → **Allowed** (screenshot #1). Dengan rules repo ini → **Denied** (screenshot #2).

---

## Pengumpulan (push ke branch `pertemuan-11`)

ZIP ini sudah berisi repo git dengan branch `pertemuan-11` yang sudah di-commit.

```bash
cd ch11_starter
# (setelah mengganti app/google-services.json dengan file asli)
git add app/google-services.json
git commit -m "Pertemuan 11: google-services.json dari Firebase Console"

git remote add origin https://github.com/JaydenFW/hellocompose.git
git push -u origin pertemuan-11
```

Link yang dikumpulkan ke LMS:
`https://github.com/JaydenFW/hellocompose/tree/pertemuan-11`
(+ lampirkan dua screenshot Rules Playground).
