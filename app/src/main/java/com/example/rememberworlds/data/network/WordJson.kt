package com.example.rememberworlds.data.network

/**
 * 单词JSON数据模型
 * 用于从Gitee下载单词书籍时解析JSON数据
 */
data class WordJson(
    /**
     * 单词ID
     */
    val id: Int,
    
    /**
     * 英文单词
     */
    val word: String,
    
    /**
     * 中文释义
     */
    val cn: String,
    
    /**
     * 音频文件路径或URL
     */
    val audio: String
)

/**
 * 在线查词响应项数据模型
 * 对应Free Dictionary API的响应格式
 */
data class SearchResponseItem(
    /**
     * 搜索的单词
     */
    val word: String?,
    
    /**
     * 单词音标
     */
    val phonetic: String?, 
    
    /**
     * 单词释义列表
     */
    val meanings: List<Meaning>?, 
    
    /**
     * 单词音频列表
     */
    val phonetics: List<Phonetic>? 
)

/**
 * 单词含义数据模型
 * 包含单词的词性和具体释义
 */
data class Meaning(
    /**
     * 词性 (例如：n. 名词, v. 动词, adj. 形容词)
     */
    val partOfSpeech: String?, 
    
    /**
     * 具体释义列表
     */
    val definitions: List<Definition>?
)

/**
 * 单词定义数据模型
 * 包含英文释义和例句
 */
data class Definition(
    /**
     * 英文释义
     */
    val definition: String?, 
    
    /**
     * 例句
     */
    val example: String? 
)

/**
 * 单词语音数据模型
 * 包含音标文本和音频链接
 */
data class Phonetic(
    /**
     * 音标文本
     */
    val text: String?,
    
    /**
     * 发音音频链接
     */
    val audio: String? 
)