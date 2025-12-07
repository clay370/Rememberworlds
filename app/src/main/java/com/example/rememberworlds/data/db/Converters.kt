package com.example.rememberworlds.data.db

import androidx.room.TypeConverter
import com.example.rememberworlds.data.model.WordDetailContent
import com.google.gson.Gson

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromWordDetailContent(value: WordDetailContent?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWordDetailContent(value: String?): WordDetailContent? {
        if (value.isNullOrEmpty()) return null
        return gson.fromJson(value, WordDetailContent::class.java)
    }
}
