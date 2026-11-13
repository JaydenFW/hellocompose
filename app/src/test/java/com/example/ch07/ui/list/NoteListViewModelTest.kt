package com.example.ch07.ui.list

import com.example.ch07.data.NoteEntity
import com.example.ch07.testutil.FakeNoteRepository
import com.example.ch07.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private fun note(
        id: Int, title: String, content: String = "",
        pinned: Boolean = false, tags: List<String> = emptyList()
    ) = NoteEntity(id, title, content, createdAt = id.toLong(), isPinned = pinned,
        tags = tags, updatedAt = id.toLong())

    // uiState memakai stateIn(WhileSubscribed): harus ada yang mengumpulkan
    private fun TestScope.start(vm: NoteListViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
    }

    @Test fun `catatan yang disematkan tampil paling atas`() = runTest {
        val vm = NoteListViewModel(FakeNoteRepository(listOf(note(1, "A"), note(2, "B", pinned = true))))
        start(vm)

        assertEquals(listOf("B", "A"), vm.uiState.value.notes.map { it.title })
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `pencarian ditunda 300 ms lalu memfilter judul atau isi`() = runTest {
        val vm = NoteListViewModel(FakeNoteRepository(listOf(
            note(1, "Belanja", "susu"), note(2, "Kuliah", "bab Room"), note(3, "Ide", "aplikasi")
        )))
        start(vm)

        vm.onQueryChange("room")
        runCurrent()
        assertEquals("teks langsung tampil di TextField", "room", vm.uiState.value.query)
        advanceTimeBy(299); runCurrent()
        assertEquals("belum difilter sebelum 300 ms", 3, vm.uiState.value.notes.size)

        advanceTimeBy(2); runCurrent()
        assertEquals(listOf("Kuliah"), vm.uiState.value.notes.map { it.title })
        assertTrue(vm.uiState.value.isFiltering)

        vm.onQueryChange("")           // kosong = tanpa jeda, semua tampil lagi
        runCurrent()
        assertEquals(3, vm.uiState.value.notes.size)
    }

    @Test fun `filter tag dan ketuk tag yang sama melepas filter`() = runTest {
        val vm = NoteListViewModel(FakeNoteRepository(listOf(
            note(1, "A", tags = listOf("kuliah")),
            note(2, "B", tags = listOf("ide", "kuliah")),
            note(3, "C", tags = listOf("ide")),
            note(4, "D")
        )))
        start(vm)
        assertEquals(listOf("ide", "kuliah"), vm.uiState.value.availableTags)

        vm.onTagSelected("kuliah"); runCurrent()
        assertEquals(setOf("A", "B"), vm.uiState.value.notes.map { it.title }.toSet())
        assertEquals("kuliah", vm.uiState.value.selectedTag)

        vm.onTagSelected("kuliah"); runCurrent()
        assertNull(vm.uiState.value.selectedTag)
        assertEquals(4, vm.uiState.value.notes.size)
    }

    @Test fun `tag yang tidak dipakai lagi otomatis dilepas`() = runTest {
        val only = note(1, "A", tags = listOf("langka"))
        val vm = NoteListViewModel(FakeNoteRepository(listOf(only, note(2, "B"))))
        start(vm)
        vm.onTagSelected("langka"); runCurrent()
        assertEquals("langka", vm.uiState.value.selectedTag)

        vm.deleteNote(only); runCurrent()
        assertNull(vm.uiState.value.selectedTag)
        assertEquals(listOf("B"), vm.uiState.value.notes.map { it.title })
    }

    @Test fun `hapus lalu batalkan mengembalikan catatan yang sama`() = runTest {
        val target = note(7, "Penting", "jangan hilang", pinned = true, tags = listOf("x"))
        val repo = FakeNoteRepository(listOf(target, note(8, "Lain")))
        val vm = NoteListViewModel(repo)
        start(vm)

        vm.deleteNote(target); runCurrent()
        assertEquals(listOf(8), repo.current.map { it.id })
        assertEquals(target, vm.uiState.value.recentlyDeleted)

        vm.undoDelete(); runCurrent()
        assertTrue(target in repo.current)               // id, tags, pin ikut kembali
        assertNull(vm.uiState.value.recentlyDeleted)
        assertEquals(listOf("Penting", "Lain"), vm.uiState.value.notes.map { it.title })
    }

    @Test fun `snackbar hilang tanpa dibatalkan menghapus permanen`() = runTest {
        val target = note(1, "A")
        val repo = FakeNoteRepository(listOf(target))
        val vm = NoteListViewModel(repo)
        start(vm)

        vm.deleteNote(target); runCurrent()
        vm.onUndoDismissed(); runCurrent()

        assertNull(vm.uiState.value.recentlyDeleted)
        assertTrue(repo.current.isEmpty())
    }

    @Test fun `menyematkan tidak mengubah updatedAt`() = runTest {
        val n = note(1, "A")
        val repo = FakeNoteRepository(listOf(n))
        val vm = NoteListViewModel(repo)
        start(vm)

        vm.togglePin(n); runCurrent()

        val after = repo.current.single()
        assertTrue(after.isPinned)
        assertEquals(n.updatedAt, after.updatedAt)
    }
}
