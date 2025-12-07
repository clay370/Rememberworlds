package com.example.rememberworlds.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 单词数据访问对象(DAO)
 * 定义了所有数据库操作方法
 * 使用Room注解简化数据库访问
 */
@Dao
interface WordDao {
    /**
     * 插入或替换多个单词实体
     * 当存在冲突时，使用替换策略
     *
     * @param words 要插入的单词列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    /**
     * 根据书籍类型获取所有单词
     *
     * @param type 书籍类型标识符
     * @return 该类型下的所有单词列表
     */
    @Query("SELECT * FROM word_table WHERE bookType = :type")
    suspend fun getAllWordsByBook(type: String): List<WordEntity>

    /**
     * 获取指定书籍类型下未学过的单词列表
     *
     * @param type 书籍类型标识符
     * @return 未学过的单词列表
     */
    @Query("SELECT * FROM word_table WHERE bookType = :type AND isLearned = 0")
    suspend fun getUnlearnedWordsList(type: String): List<WordEntity>

    /**
     * 获取指定书籍类型下已学过的单词
     * 返回Flow以便实时监听数据变化
     * 按ID降序排列，最新学习的单词在前
     *
     * @param type 书籍类型标识符
     * @return 包含已学单词列表的Flow
     */
    @Query("SELECT * FROM word_table WHERE bookType = :type AND isLearned = 1 ORDER BY id DESC")
    fun getLearnedWords(type: String): Flow<List<WordEntity>>

    /**
     * 随机获取指定书籍类型下的20个单词用于测试
     *
     * @param type 书籍类型标识符
     * @return 随机选择的20个单词列表
     */
    @Query("SELECT * FROM word_table WHERE bookType = :type ORDER BY RANDOM() LIMIT 20")
    suspend fun getQuizWords(type: String): List<WordEntity>

    /**
     * 更新单个单词实体
     *
     * @param word 要更新的单词实体
     */
    @Update
    suspend fun updateWord(word: WordEntity)

    /**
     * 将指定ID的单词标记为未学过
     * 用于撤销之前的学习标记
     *
     * @param wordId 要标记的单词ID
     */
    @Query("UPDATE word_table SET isLearned = 0 WHERE id = :wordId")
    suspend fun markAsUnlearned(wordId: Int)

    /**
     * 批量标记指定书籍类型下的多个单词为已学过
     *
     * @param bookType 书籍类型标识符
     * @param ids 要标记的单词ID列表
     */
    @Query("UPDATE word_table SET isLearned = 1 WHERE bookType = :bookType AND id IN (:ids)")
    suspend fun markWordsAsLearned(bookType: String, ids: List<Int>)

    /**
     * 清空指定书籍类型下的所有单词
     *
     * @param type 书籍类型标识符
     */
    @Query("DELETE FROM word_table WHERE bookType = :type")
    suspend fun clearBook(type: String)

    /**
     * 删除数据库中的所有单词
     */
    @Query("DELETE FROM word_table")
    suspend fun deleteAll()

    /**
     * 获取已学单词的数量
     * 返回Flow以便实时监听统计数据变化
     *
     * @return 包含已学单词数量的Flow
     */
    @Query("SELECT COUNT(*) FROM word_table WHERE isLearned = 1")
    fun getLearnedCount(): Flow<Int>

    /**
     * 重置所有单词的学习状态
     * 将所有单词的 isLearned 标记设为 false
     * 用于切换用户时清除上一用户的进度
     */
    @Query("UPDATE word_table SET isLearned = 0")
    suspend fun resetAllProgress()

    /**
     * 获取指定书籍下的单词总数
     */
    @Query("SELECT COUNT(*) FROM word_table WHERE bookType = :type")
    fun getBookTotalCount(type: String): Flow<Int>

    /**
     * 获取指定书籍下已学单词数量
     */
    @Query("SELECT COUNT(*) FROM word_table WHERE bookType = :type AND isLearned = 1")
    fun getBookLearnedCount(type: String): Flow<Int>

    /**
     * 获取所有已学单词（跨书籍）
     */
    @Query("SELECT * FROM word_table WHERE isLearned = 1 ORDER BY id DESC")
    suspend fun getAllLearnedWords(): List<WordEntity>

    /**
     * 获取数据库中所有单词的总数
     * 返回Flow以便实时监听统计数据变化
     *
     * @return 包含总单词数量的Flow
     */
    @Query("SELECT COUNT(*) FROM word_table")
    fun getTotalCount(): Flow<Int>

    /**
     * 获取所有收藏的单词
     * 按ID降序排列
     */
    @Query("SELECT * FROM word_table WHERE isFavorite = 1 ORDER BY id DESC")
    suspend fun getFavoriteWords(): List<WordEntity>

    /**
     * 更新单词的收藏状态
     */
    @Query("UPDATE word_table SET isFavorite = :isFavorite WHERE id = :wordId")
    suspend fun updateIsFavorite(wordId: Int, isFavorite: Boolean)

    /**
     * 获取收藏单词的数量
     */
    @Query("SELECT COUNT(*) FROM word_table WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    /**
     * 获取所有错题 (Mistake Notebook)
     * 按ID降序排列
     */
    @Query("SELECT * FROM word_table WHERE isWrong = 1 ORDER BY id DESC")
    suspend fun getMistakeWords(): List<WordEntity>

    /**
     * 更新单词的错题状态
     */
    @Query("UPDATE word_table SET isWrong = :isWrong WHERE id = :wordId")
    suspend fun updateIsWrong(wordId: Int, isWrong: Boolean)
}