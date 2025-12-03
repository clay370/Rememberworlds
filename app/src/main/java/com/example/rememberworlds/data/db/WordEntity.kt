package com.example.rememberworlds.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_table")
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0, // 本地自增主键

    val id: Int,          // 对应 JSON 里的 id
    val word: String,     // 单词
    val cn: String,       // 中文
    val audio: String,    // 音频

    val bookType: String, // 标记属于哪本书 (例如 "cet4")

    var isLearned: Boolean = false, // 是否已背
    var isWrong: Boolean = false,   // 是否错题
    var wrongCount: Int = 0         // 错误次数
)