package com.example.rememberworlds.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单词实体类，用于Room数据库存储
 * 表示应用程序中的一个单词记录
 */
// Entity注解：指定数据库表名
@Entity(
    tableName = "word_table",
    indices = [androidx.room.Index(value = ["bookType", "id"], unique = true)]
)
data class WordEntity(
    /**
     * 本地自增主键
     * 使用autoGenerate = true自动生成唯一ID
     */
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,

    /**
     * 单词的原始ID，对应JSON数据中的id字段
     */
    val id: Int,
    
    /**
     * 单词本身的英文拼写
     */
    val word: String,
    
    /**
     * 单词的中文释义
     */
    val cn: String,
    
    /**
     * 单词的音频文件路径或URL
     */
    val audio: String,

    /**
     * 书籍类型标识符，用于区分不同单词书
     * 例如："cet4"表示大学英语四级单词
     */
    val bookType: String,

    /**
     * 是否已经学习过该单词
     * 默认值为false
     */
    var isLearned: Boolean = false,
    
    /**
     * 该单词是否为错题
     * 默认值为false
     */
    var isWrong: Boolean = false,
    
    /**
     * 该单词的错误次数统计
     * 默认值为0
     */
    var wrongCount: Int = 0,

    // --- 新增字段 (Rich Data) ---

    /**
     * 单词的全局唯一ID (例如: "PEPChuZhong7_2_18")
     */
    val wordId: String = "",

    /**
     * 单词的详细富文本内容 (JSON对象)
     * Room将使用Converters将其存储为JSON字符串
     */
    val detailedContent: com.example.rememberworlds.data.model.WordDetailContent? = null
)