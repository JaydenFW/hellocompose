# Ch07 — Local Database dengan Room (CatatanKu)

Praktikum ini membangun aplikasi catatan **CatatanKu**: dua layar (daftar dan
form tambah/edit), data tersimpan ke **Room**, dan tetap ada setelah aplikasi
ditutup. Arsitekturnya melanjutkan Ch06: `Entity → DAO → Database →
Repository → ViewModel → Route/Screen`, dirakit dengan manual DI.

---

## Hasil Akhir

| Layer | Isi |
|---|---|
| Data | `NoteEntity`, `Converters`, `NoteDao`, `NoteDatabase` (+ migration), `NoteRepository` |
| DI | `AppContainer`, `DefaultAppContainer`, `MainApplication` |
| Presentation | `NoteListViewModel`, `NoteEditViewModel` (+ `UiState`-nya) |
| View | `NoteListRoute/Screen`, `NoteCard`, `NoteEditRoute/Screen`, `CatatanKuApp` (navigasi) |
| Util | `formatRelativeTime()` ("3 jam lalu"), `parseTags()` |

Fitur: tambah/ubah/hapus catatan, **sematkan** (pin) ke atas daftar, **pencarian**
real-time (judul atau isi), **undo hapus** lewat Snackbar, waktu **"Diubah 3 jam
lalu"**, dan **tag** dengan filter chip.

---

## Prasyarat

- Sudah menyelesaikan Ch06 (ViewModel, StateFlow, Repository, manual DI)
- Android Studio Hedgehog (2023.1.1) atau lebih baru
- Android SDK API 34, JDK 17

> **Versi compiler sama dengan Ch03–Ch06** — Gradle 8.6, AGP 8.3.2, Kotlin 1.9.23.
> Ditambah plugin **KSP** `1.9.23-1.0.20` dan Room `2.6.1`.

> **Titik awal untuk mahasiswa:** `praktikum/ch07_starter` (skema v1 + daftar TODO Tugas 2–4).
> Folder ini adalah **jawaban lengkapnya**.

---

## Cara Membuka di Android Studio

1. **File → Open** → pilih folder `praktikum/ch07/`
2. Tunggu Gradle sync — dependency baru: Room (`room-runtime`, `room-ktx`,
   `room-compiler` via KSP) dan Gson.
3. Pilih device/emulator → klik **Run ▶**

Paket dan `applicationId` adalah `com.example.ch07` (mengikuti `ch05`/`ch06`);
nama aplikasinya **CatatanKu**.

---

## Langkah 1 — Tambahkan Room + KSP

Root `build.gradle.kts` mendaftarkan plugin KSP; `app/build.gradle.kts`
memakainya:

```kotlin
plugins {
    id("com.google.devtools.ksp")            // KSP untuk code generation
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }   // ekspor skema JSON

dependencies {
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")    // coroutines & Flow
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("com.google.code.gson:gson:2.10.1")       // TypeConverter (Tugas 4)
}
```

Room memakai **KSP**, bukan `kapt`, karena lebih cepat.

---

## Langkah 2 — `NoteEntity` (`data/`)

Satu data class = satu tabel; satu objek = satu baris:

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList(),   // Tugas 4
    val updatedAt: Long = createdAt         // Tugas 3
)
```

`tags` bertipe `List<String>` yang tidak dikenal Room, sehingga butuh
`TypeConverter` (Langkah 3).

---

## Langkah 3 — `Converters` (`data/`)

```kotlin
class Converters {
    @TypeConverter fun fromTagList(tags: List<String>): String = gson.toJson(tags)
    @TypeConverter fun toTagList(json: String): List<String> = /* JSON -> List */
}
```

`["kuliah","ide"]` disimpan sebagai teks JSON di satu kolom.

---

## Langkah 4 — `NoteDao` (`data/`)

```kotlin
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>            // reaktif

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Int): NoteEntity?    // sekali ambil

    @Query("... WHERE title LIKE '%' || :query || '%' OR content LIKE ...")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNote(note: NoteEntity): Long
    @Update suspend fun updateNote(note: NoteEntity)
    @Delete suspend fun deleteNote(note: NoteEntity)
}
```

- Fungsi yang mengembalikan **`Flow`** bersifat reaktif: tabel berubah → Room
  emit daftar baru → layar ter-update **tanpa refresh manual**.
- Operasi satu kali (`insert`, `update`, `delete`, ambil satu baris) memakai **`suspend`**.
- `REPLACE` menimpa baris ber-id sama — dipakai juga untuk **Batalkan hapus**.

---

## Langkah 5 — `NoteDatabase` dan Migration (`data/`)

```kotlin
@Database(entities = [NoteEntity::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    companion object { /* singleton: @Volatile + synchronized */ }
}
```

Database dibangun dengan `.addMigrations(MIGRATION_1_2, MIGRATION_2_3)` —
**bukan** `fallbackToDestructiveMigration()`, karena itu menghapus data
pengguna saat versi naik.

| Versi | Perubahan skema | Migration |
|---|---|---|
| 1 | `id, title, content, createdAt, isPinned` | — (skema awal di buku) |
| 2 | + `tags` | `ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'` |
| 3 | + `updatedAt` | `ALTER TABLE ... ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0` lalu `UPDATE notes SET updatedAt = createdAt` |

Langkah `UPDATE` pada v2→v3 penting: tanpa itu catatan lama punya `updatedAt = 0`
dan tampil sebagai "56 tahun lalu".

> **Aturan:** setiap kali skema berubah, **wajib** menaikkan `version` dan
> menyediakan `Migration`. Jika tidak, Room crash dengan
> `IllegalStateException` di perangkat yang sudah punya database lama.

Skema tiap versi diekspor Room ke `app/schemas/.../<versi>.json` (diaktifkan oleh
`exportSchema = true` + argumen KSP di Langkah 1). Berkas ini berguna untuk
memverifikasi migration dan untuk CI.

---

## Langkah 6 — `NoteRepository` (`data/`)

ViewModel **tidak boleh** berbicara langsung ke DAO. Repository membungkus DAO
sehingga ViewModel tidak tahu detail penyimpanan, dan bisa diuji dengan fake:

```kotlin
interface NoteRepository {
    fun getAllNotes(): Flow<List<NoteEntity>>
    fun searchNotes(query: String): Flow<List<NoteEntity>>
    suspend fun getNoteById(id: Int): NoteEntity?
    suspend fun insertNote(note: NoteEntity): Long
    suspend fun updateNote(note: NoteEntity)
    suspend fun deleteNote(note: NoteEntity)
}
class NoteRepositoryImpl(private val dao: NoteDao) : NoteRepository { /* meneruskan ke DAO */ }
```

---

## Langkah 7 — Manual DI (`di/`)

```kotlin
interface AppContainer { val noteRepository: NoteRepository }

class DefaultAppContainer(context: Context) : AppContainer {
    private val database by lazy { NoteDatabase.getInstance(context) }
    override val noteRepository by lazy { NoteRepositoryImpl(database.noteDao()) }
}
```

`MainApplication` menyimpan container-nya dan didaftarkan di `AndroidManifest.xml`
lewat `android:name=".MainApplication"` — sama seperti Ch06.

---

## Langkah 8 — ViewModel

### `NoteListViewModel` (`ui/list/`)

UiState **diturunkan** dari beberapa sumber dengan `combine` (pola Ch06):

```kotlin
private val searchResults = queryText
    .debounce { query -> if (query.isBlank()) 0L else 300L }   // tunda 300 ms
    .distinctUntilChanged()
    .flatMapLatest { q ->                                      // hanya query TERBARU
        if (q.isBlank()) repository.getAllNotes() else repository.searchNotes(q)
    }

val uiState = combine(searchResults, repository.getAllNotes(), filters,
                      recentlyDeleted, loadError) { ... }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteListUiState(isLoading = true))
```

- **`debounce`** menunda query sampai pengguna berhenti mengetik, supaya database
  tidak di-query setiap huruf. Query **kosong tidak ditunda**, jadi daftar penuh
  langsung tampil saat aplikasi dibuka atau pencarian dihapus.
- **`flatMapLatest`** membatalkan langganan ke query lama saat query berubah.
- `TextField` menampilkan teks **langsung** (`queryText`), sementara hasilnya
  mengikuti versi yang sudah di-debounce.

### `NoteEditViewModel` (`ui/edit/`)

`noteId = 0` berarti catatan baru; selain itu, mengedit catatan tersebut.
Saat mengedit, catatan asli disimpan di `original` lalu di-`copy(...)`, sehingga
`id`, `createdAt`, dan `isPinned` **tidak ikut tereset** (lihat "Perbedaan dengan
listing di buku").

---

## Langkah 9 — Route, Screen, dan Navigasi

Pola yang sama seperti Ch06: `NoteListRoute` / `NoteEditRoute` mengenal ViewModel
dan `Context`; `NoteListScreen` / `NoteEditScreen` murni menerima state + callback.

```kotlin
NavHost(navController, startDestination = "notes") {
    composable("notes")          { NoteListRoute(onAddNote = { navigate("edit/0") },
                                                 onEditNote = { id -> navigate("edit/$id") }) }
    composable("edit/{noteId}")  { NoteEditRoute(noteId = ..., onNavigateBack = { popBackStack() }) }
}
```

`NavController` hanya ada di `CatatanKuApp`; layar lain menerima lambda.

Alur data lengkap:

```
NoteDao.getAllNotes(): Flow  →  NoteRepositoryImpl  →  NoteListViewModel (combine)
   →  NoteListUiState (StateFlow)  →  collectAsStateWithLifecycle  →  NoteListScreen
```

---

## Checklist Praktikum

- Aplikasi membuka daftar catatan (kosong saat pertama kali).
- Ketuk FAB **+** membuka layar tambah catatan.
- Catatan tersimpan ke Room dan muncul di daftar **tanpa refresh manual**.
- Data tetap ada setelah aplikasi ditutup total dan dibuka kembali.
- Tombol pin memindahkan catatan ke bagian atas daftar.
- Tombol hapus menghapus catatan dari database.
- Pencarian memfilter catatan secara real-time saat mengetik.
- Ketuk kartu catatan membuka form edit dengan data yang sudah terisi.

---

## Tugas Pertemuan 7 — Jawaban Lengkap

1. **Aplikasi CatatanKu** — dua layar (daftar dan edit), data tersimpan ke Room —
   **sudah dikerjakan** (Langkah 1–9).

2. **Undo hapus dengan Snackbar "Batalkan"** — selesai. `deleteNote()` menghapus
   dari database lalu mengisi `recentlyDeleted` di UiState. `NoteListScreen`
   menampilkan `Snackbar` lewat `LaunchedEffect(uiState.recentlyDeleted)`:
   - **Batalkan** → `undoDelete()` memasukkan kembali **catatan yang sama**
     (id, tag, pin ikut kembali, karena `insertNote` memakai `REPLACE`).
   - Snackbar hilang sendiri → `onUndoDismissed()`; hapus menjadi permanen.

3. **Kolom `updatedAt` dan "Diubah 3 jam lalu"** — selesai. Kolom baru + `MIGRATION_2_3`
   (catatan lama diisi `updatedAt = createdAt`). `NoteCard` menampilkan
   `formatRelativeTime(now, updatedAt)`: *baru saja*, *N menit lalu*, *N jam lalu*,
   *N hari lalu*, *N bulan lalu*, *N tahun lalu*. Menyematkan catatan **tidak**
   mengubah `updatedAt` karena bukan perubahan isi.

4. **Tantangan — tag/label** — selesai. `tags: List<String>` disimpan sebagai JSON
   lewat `Converters` (`@TypeConverters` di `NoteDatabase`) + `MIGRATION_1_2`.
   Form edit menerima tag dipisah koma (`"Kuliah, #Ide"` → `["kuliah","ide"]`).
   Di atas daftar ada **filter chip** (`Semua`, `#ide`, `#kuliah`, ...): ketuk chip
   untuk memfilter, ketuk lagi untuk melepas. Filter tag bekerja bersama pencarian,
   dan tag yang sudah tidak dipakai catatan mana pun otomatis dilepas.

**Pengumpulan:** push ke branch `pertemuan-7` → kumpulkan link di LMS sebelum
pertemuan berikutnya.

---

## Perbedaan dengan Listing di Buku

| Hal | Listing buku | Praktikum ini | Alasan |
|---|---|---|---|
| Simpan hasil edit | `NoteEntity(id, title, content)` baru | `original.copy(...)` | Listing buku mereset `createdAt` dan `isPinned` saat catatan diedit |
| `debounce` | `debounce(300)` | `debounce { if blank 0 else 300 }` | Daftar penuh tidak menunggu 300 ms saat dibuka |
| UiState `NoteListViewModel` | diperbarui manual di `collect` | diturunkan dengan `combine` | Konsisten dengan pola Ch06 |
| `searchNotes` | tanpa `ORDER BY` | `ORDER BY isPinned DESC, createdAt DESC` | Urutan hasil pencarian sama dengan daftar |
| `SearchBar` | dipanggil tanpa definisi | `NoteSearchField` (`OutlinedTextField`) | Composable `SearchBar` Material 3 punya tanda tangan lain |
| Database | `fallbackToDestructiveMigration()` | `addMigrations(...)` | Data pengguna tidak boleh hilang |
| Package | `com.example.catatanku` | `com.example.ch07` | Mengikuti konvensi folder praktikum |

---

## Pengujian

```bash
./gradlew testDebugUnitTest      # 23 unit test JVM, tanpa emulator
```

| Berkas | Yang diuji |
|---|---|
| `NoteListViewModelTest` | pin di atas, debounce 300 ms, filter tag, undo/dismiss hapus, tag yatim dilepas, pin tidak mengubah `updatedAt` |
| `NoteEditViewModelTest` | judul kosong ditolak, catatan baru, edit mempertahankan `createdAt`/`isPinned`, catatan tidak ditemukan |
| `ConvertersTest` | `List<String>` ⇄ JSON, nilai default `'[]'` |
| `RelativeTimeTest`, `TagsTest` | format waktu dan parsing tag |

ViewModel diuji dengan `FakeNoteRepository` (bukan Room) — bukti bahwa ViewModel
hanya bergantung pada interface `NoteRepository`.

**Migration** diverifikasi terpisah: SQL `MIGRATION_1_2` dan `MIGRATION_2_3` dijalankan
pada database v1 berisi data, lalu skemanya dibandingkan dengan `schemas/.../3.json`
buatan Room — sama, dan catatan lama tetap ada dengan `tags = '[]'` serta
`updatedAt = createdAt`. Uji instrumented (`MigrationTestHelper`) belum ada.

---

## Struktur Proyek

```
com.example.ch07
 ├── data/     NoteEntity, Converters, NoteDao, NoteDatabase, NoteRepository
 ├── di/       AppContainer, DefaultAppContainer
 ├── ui/
 │   ├── list/ NoteListViewModel, NoteListRoute, NoteListScreen, NoteCard
 │   ├── edit/ NoteEditViewModel, NoteEditRoute, NoteEditScreen
 │   └── theme/
 ├── util/     RelativeTime, Tags
 ├── MainApplication.kt, MainActivity.kt
 └── Routes.kt, CatatanKuApp.kt         (navigasi)
```

---

## Catatan dan Batasan

- Menghapus dua catatan berturut-turut sebelum Snackbar pertama hilang: Snackbar
  diganti, sehingga hanya catatan **terakhir** yang bisa dibatalkan (rancangan
  sederhana; belum diuji di perangkat).
- Karakter `%` dan `_` pada kolom pencarian bertindak sebagai wildcard `LIKE`
  (tidak di-escape).
- Pencarian mencocokkan judul dan isi, **bukan** tag; tag difilter lewat chip.

---

## Run the code

```bash
cd /Users/dhareva/Books/2026_MP/codes/praktikum/ch07
./gradlew installDebug
adb shell am start -n com.example.ch07/.MainActivity
```
