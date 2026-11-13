package com.example.ch07.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Satu objek = satu baris tabel "notes".
// Skema berevolusi lewat migration: v1 (id, title, content, createdAt,
// isPinned) -> v2 (+ tags) -> v3 (+ updatedAt). Lihat NoteDatabase.
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    // Tugas 4: List<String> disimpan sebagai JSON lewat TypeConverter
    val tags: List<String> = emptyList(),
    // Tugas 3: kapan catatan terakhir diubah
    val updatedAt: Long = createdAt
)
