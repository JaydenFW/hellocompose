package com.example.ch07.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test fun `daftar tag menjadi JSON dan kembali`() {
        val tags = listOf("kuliah", "ide")
        val json = converters.fromTagList(tags)
        assertEquals("[\"kuliah\",\"ide\"]", json)
        assertEquals(tags, converters.toTagList(json))
    }

    // '[]' adalah nilai DEFAULT kolom tags hasil MIGRATION_1_2
    @Test fun `nilai default migration terbaca sebagai daftar kosong`() =
        assertEquals(emptyList<String>(), converters.toTagList("[]"))
}
