package com.example.ch07.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Room hanya mengenal tipe primitif dan String. List<String> disimpan
// sebagai teks JSON, misalnya ["kuliah","ide"], lalu dibaca kembali.
class Converters {

    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromTagList(tags: List<String>): String = gson.toJson(tags)

    @TypeConverter
    fun toTagList(json: String): List<String> =
        gson.fromJson<List<String>>(json, listType) ?: emptyList()
}
