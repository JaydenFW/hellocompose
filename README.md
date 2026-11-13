# Ch05 — Navigation & Multi-Screen App

Praktikum membangun aplikasi **multi-layar** dengan Navigation Compose:
`NavController`, `NavHost`, argument antar layar, dan Bottom Navigation.

---

## Hasil Akhir

Aplikasi dengan tiga tab di Bottom Navigation:

| Beranda | Eksplorasi | Profil |
|---|---|---|
| Daftar artikel (`LazyColumn`) | Grid foto (`LazyVerticalGrid`) | Info profil sederhana |
| Klik artikel → `DetailScreen` dengan argument `itemId` | | |
| Tombol back mengembalikan ke Beranda | | |

> Kode di repo ini sudah termasuk jawaban **Tugas Pertemuan 5** (grid
> Eksplorasi, animasi transisi, dan deep link) — lihat bagian
> [Tugas Pertemuan 5 — Jawaban Lengkap](#tugas-pertemuan-5--jawaban-lengkap)
> di bawah untuk penjelasan tiap bagiannya. Langkah 1–8 tetap menjelaskan
> struktur dasar praktikum sebelum fitur-fitur itu ditambahkan.

---

## Prasyarat

- Sudah menyelesaikan Ch04 (state management)
- Android Studio Hedgehog (2023.1.1) atau lebih baru
- Android SDK API 34, JDK 17

> **Versi compiler sama dengan Ch03/Ch04** — Gradle 8.6, AGP 8.3.2, Kotlin 1.9.23.

---

## Cara Membuka di Android Studio

1. **File → Open** → pilih folder `ch05/` hasil ekstraksi (berisi `settings.gradle.kts`)
2. Tunggu Gradle sync — kali ini akan mengunduh dependency baru:
   `androidx.navigation:navigation-compose`
3. Pilih device/emulator → klik **Run ▶**

---

## Langkah 1 — Tambahkan Dependency Navigation Compose

Buka `app/build.gradle.kts`, pastikan baris berikut ada di blok `dependencies`:

```kotlin
implementation("androidx.navigation:navigation-compose:2.8.5")
```

Sync project. Dependency ini menyediakan `NavController`, `NavHost`, dan
Composable `composable { }` yang jadi inti dari Navigation Compose.

---

## Langkah 2 — Definisikan Routes.kt

Praktik terbaik: definisikan semua route sebagai `sealed class` agar
terhindar dari typo string.

```kotlin
sealed class Routes(val route: String) {
    data object Home    : Routes("home")
    data object Explore : Routes("explore")
    data object Profile : Routes("profile")
    data object Detail  : Routes("detail/{itemId}") {
        fun createRoute(itemId: Int) = "detail/$itemId"
    }
}
```

`Detail` butuh helper `createRoute()` karena route-nya mengandung
placeholder `{itemId}` yang harus diisi nilai sebenarnya saat navigasi.

---

## Langkah 3 — Pahami HomeScreen.kt

`HomeScreen.kt` sudah tersedia — berisi data dummy `Article` dan
`LazyColumn` yang menampilkannya (konsep list sudah dipelajari di bab
sebelumnya). Perhatikan bagian navigasinya:

```kotlin
@Composable
fun HomeScreen(navController: NavController) {
    // ...
    items(dummyArticles, key = { it.id }) { article ->
        ArticleCard(
            article = article,
            onClick = {
                navController.navigate(Routes.Detail.createRoute(article.id))
            }
        )
    }
}
```

`HomeScreen` menerima `NavController` langsung agar bisa memanggil
`navigate()` saat sebuah artikel diklik.

---

## Langkah 4 — Buat DetailScreen.kt

Layar ini menerima `itemId: Int` sebagai parameter biasa — nilainya
nanti diambil dari argument route oleh `NavHost` (lihat Langkah 7):

```kotlin
@Composable
fun DetailScreen(itemId: Int, onBack: () -> Unit) {
    val article = dummyArticles.firstOrNull { it.id == itemId }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Detail Artikel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        if (article == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Artikel tidak ditemukan")
            }
        } else {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text(article.title, style = MaterialTheme.typography.headlineSmall)
                // ...isi selengkapnya
            }
        }
    }
}
```

`onBack` adalah lambda — `DetailScreen` tidak tahu apa-apa soal
`NavController`, ia hanya memberi tahu "tombol back ditekan". Pemanggil
(`MainScreen`) yang memutuskan itu berarti `navController.popBackStack()`.
Pola ini membuat Composable lebih mudah di-preview dan di-test.

---

## Langkah 5 — Buat ProfileScreen.kt dan ExploreScreen.kt

Dua layar sederhana untuk melengkapi tiga tab. `ProfileScreen.kt`
menampilkan info statis, `ExploreScreen.kt` masih berupa placeholder
(akan dilengkapi jadi grid foto di Tugas):

```kotlin
@Composable
fun ProfileScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Profil") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.AccountCircle, null, Modifier.size(96.dp))
            Text("Mahasiswa Android", style = MaterialTheme.typography.titleLarge)
        }
    }
}
```

---

## Langkah 6 — Buat BottomNavItem.kt

Data class untuk merepresentasikan satu item Bottom Navigation, plus
daftar berisi tiga tab yang akan ditampilkan:

```kotlin
data class BottomNavItem(
    val label:          String,
    val route:          String,
    val iconSelected:   ImageVector,
    val iconUnselected: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Beranda",    Routes.Home.route,    Icons.Filled.Home,   Icons.Outlined.Home),
    BottomNavItem("Eksplorasi", Routes.Explore.route, Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem("Profil",     Routes.Profile.route, Icons.Filled.Person, Icons.Outlined.Person),
)
```

Icon berbeda dipakai untuk state selected vs unselected — pola umum di
Material Design 3 (filled saat aktif, outlined saat tidak).

---

## Langkah 7 — Rangkai MainScreen.kt

Ini bagian intinya: `Scaffold` dengan `NavigationBar` di `bottomBar`,
membungkus satu `NavHost` yang mendaftarkan semua layar — termasuk
`Detail` yang butuh `navArgument`:

```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick  = {
                            navController.navigate(item.route) {
                                popUpTo(Routes.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = {
                            Icon(
                                if (currentRoute == item.route) item.iconSelected
                                else item.iconUnselected,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Routes.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Home.route)    { HomeScreen(navController) }
            composable(Routes.Explore.route) { ExploreScreen() }
            composable(Routes.Profile.route) { ProfileScreen() }

            composable(
                route     = Routes.Detail.route,
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
                DetailScreen(itemId = itemId, onBack = { navController.popBackStack() })
            }
        }
    }
}
```

> **Kenapa `popUpTo` + `launchSingleTop` + `restoreState`?**
> Tanpa ketiganya, menekan tab yang sama berulang kali akan menumpuk
> banyak instance layar di back stack. Kombinasi ini adalah pola standar
> untuk Bottom Navigation: hindari duplikasi, tapi tetap simpan/pulihkan
> state layar yang ditinggalkan (misal posisi scroll).

Jalankan aplikasi → klik artikel di Beranda → masuk ke Detail → tombol
back kembali ke Beranda. Pindah tab → Bottom Navigation berfungsi.

---

## Langkah 8 — Wire di MainActivity

`MainActivity.kt` sudah tersedia. Tidak perlu diubah — cukup memanggil
`Ch05Theme { MainScreen() }`.

---

## Tugas Pertemuan 5 — Jawaban Lengkap

1. **Grid foto di `ExploreScreen.kt`** — selesai.
   `LazyVerticalGrid(columns = GridCells.Fixed(3))` menampilkan 21 item
   dummy (`Photo(id, color)`). Gambarnya memakai `painterResource` yang
   menunjuk ke `res/drawable/ic_photo_placeholder.xml` (vector drawable),
   bukan `Icons` bawaan Compose — supaya polanya sama dengan cara memuat
   foto sungguhan nanti (`Image(painter = painterResource(...))`). Warna
   latar tiap sel dibedakan lewat sebuah palet warna yang di-cycle per
   index, sekadar supaya grid terlihat berbeda-beda tanpa aset foto asli.

2. **Animasi transisi di `MainScreen.kt`** — selesai, dengan dua level:
   - **Default di `NavHost`** (`enterTransition`/`exitTransition` = fade
     200ms) dipakai saat pindah tab Bottom Navigation — Home/Explore/Profile
     sederajat, jadi transisinya halus tanpa arah tertentu.
   - **Override khusus di `composable(Routes.Detail.route, ...)`** — geser
     horizontal (`slideIntoContainer`/`slideOutOfContainer` 300ms, arah
     `Left` saat masuk, `Right` saat `popBackStack()`), karena Detail
     adalah navigasi "masuk lebih dalam", bukan sekadar berganti tab.

3. **Tantangan — deep link `myapp://article/{id}`** — selesai:
   - `AndroidManifest.xml`: `<activity>` mendapat `<intent-filter>` kedua
     (terpisah dari filter `LAUNCHER`) dengan
     `action.VIEW` + kategori `DEFAULT`/`BROWSABLE` +
     `<data android:scheme="myapp" android:host="article" />`.
   - `MainScreen.kt`: `composable(Routes.Detail.route, ...)` mendapat
     `deepLinks = listOf(navDeepLink { uriPattern = "myapp://article/{itemId}" })`
     — placeholder `{itemId}` sama persis dengan route biasa, jadi
     `DetailScreen` menerima `itemId` dengan cara yang identik baik dibuka
     lewat `navigate()` di dalam app maupun dari luar.

   Uji lewat ADB:
   ```bash
   adb shell am start -W -a android.intent.action.VIEW \
       -d "myapp://article/3" com.example.ch05
   ```
   Harus langsung membuka `DetailScreen` untuk artikel id 3, melewati
   Beranda sama sekali.

**Pengumpulan:** push ke branch `pertemuan-5` → kumpulkan link di LMS.

## Menjalankan dari terminal Windows

Buka terminal PowerShell di folder `ch05` dan pastikan emulator sudah berjalan.

```powershell
.\gradlew.bat installDebug
adb shell am start -n com.example.ch05/.MainActivity
adb shell am start -W -a android.intent.action.VIEW -d "myapp://article/3" com.example.ch05
```
