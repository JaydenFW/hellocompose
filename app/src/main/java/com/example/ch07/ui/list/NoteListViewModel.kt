package com.example.ch07.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ch07.data.NoteEntity
import com.example.ch07.data.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteListUiState(
    val isLoading: Boolean = false,
    val notes: List<NoteEntity> = emptyList(),
    val query: String = "",
    // Tugas 4: semua tag yang dipakai catatan, untuk filter chip
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    // Tugas 2: catatan yang baru dihapus (untuk Snackbar "Batalkan")
    val recentlyDeleted: NoteEntity? = null,
    val errorMessage: String? = null
) {
    // Turunan: sedang mencari/memfilter atau tidak (menentukan pesan kosong)
    val isFiltering: Boolean
        get() = query.isNotBlank() || selectedTag != null
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NoteListViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    // Teks yang sedang diketik (langsung tampil di TextField)
    private val queryText = MutableStateFlow("")
    private val selectedTag = MutableStateFlow<String?>(null)
    private val recentlyDeleted = MutableStateFlow<NoteEntity?>(null)
    private val loadError = MutableStateFlow<String?>(null)

    // Query ke database ditunda 300 ms setelah pengguna berhenti mengetik
    // (debounce), dan hanya query TERBARU yang diamati (flatMapLatest).
    // Query kosong tidak ditunda: daftar penuh tampil segera.
    private val searchResults: Flow<List<NoteEntity>> = queryText
        .debounce { query -> if (query.isBlank()) 0L else 300L }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllNotes()
            else repository.searchNotes(query)
        }
        .catch { e ->
            loadError.value = e.message ?: "Gagal memuat catatan"
            emit(emptyList())
        }

    private val filters = combine(queryText, selectedTag) { query, tag -> query to tag }

    // UiState DITURUNKAN dari beberapa sumber (pola Chapter 6): hasil Room,
    // seluruh catatan (untuk daftar tag), filter, catatan terhapus, error.
    val uiState: StateFlow<NoteListUiState> = combine(
        searchResults,
        repository.getAllNotes(),
        filters,
        recentlyDeleted,
        loadError
    ) { results, all, (query, tag), deleted, error ->
        val availableTags = all.flatMap { it.tags }.distinct().sorted()
        // Tag yang sudah tidak dipakai catatan mana pun otomatis dilepas
        val activeTag = tag?.takeIf { it in availableTags }
        NoteListUiState(
            notes = if (activeTag == null) results else results.filter { activeTag in it.tags },
            query = query,
            availableTags = availableTags,
            selectedTag = activeTag,
            recentlyDeleted = deleted,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteListUiState(isLoading = true)
    )

    fun onQueryChange(newQuery: String) {
        queryText.value = newQuery
    }

    // Ketuk tag yang sedang aktif = lepas filter; null = "Semua"
    fun onTagSelected(tag: String?) {
        selectedTag.value = if (tag == selectedTag.value) null else tag
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            recentlyDeleted.value = note
        }
    }

    // Snackbar "Batalkan": masukkan kembali catatan (id yang sama, REPLACE)
    fun undoDelete() {
        val note = recentlyDeleted.value ?: return
        recentlyDeleted.value = null
        viewModelScope.launch { repository.insertNote(note) }
    }

    // Snackbar hilang tanpa dibatalkan
    fun onUndoDismissed() {
        recentlyDeleted.value = null
    }

    // Menyematkan tidak mengubah updatedAt (bukan perubahan isi catatan)
    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }
}
