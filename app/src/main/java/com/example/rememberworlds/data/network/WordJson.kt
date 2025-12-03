package com.example.rememberworlds.data.network

// 1. 原有的：用于下载 Gitee 书籍的结构
data class WordJson(
    val id: Int,
    val word: String,
    val cn: String,
    val audio: String
)

// 2. 新增：用于在线查词的结构 (对应 Free Dictionary API)
data class SearchResponseItem(
    val word: String?,
    val phonetic: String?, // 音标
    val meanings: List<Meaning>?, // 释义列表
    val phonetics: List<Phonetic>? // 音频列表
)

data class Meaning(
    val partOfSpeech: String?, // 词性 (n. v. adj.)
    val definitions: List<Definition>?
)

data class Definition(
    val definition: String?, // 英文释义
    val example: String? // 例句
)

data class Phonetic(
    val text: String?,
    val audio: String? // 发音链接
)