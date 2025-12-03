package com.example.rememberworlds.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT * FROM word_table WHERE bookType = :type")
    suspend fun getAllWordsByBook(type: String): List<WordEntity>

    // 获取未学过的单词
    @Query("SELECT * FROM word_table WHERE bookType = :type AND isLearned = 0")
    suspend fun getUnlearnedWordsList(type: String): List<WordEntity>

    // 获取已学过的单词 (用于复习列表)
    @Query("SELECT * FROM word_table WHERE bookType = :type AND isLearned = 1 ORDER BY id DESC")
    fun getLearnedWords(type: String): Flow<List<WordEntity>>

    // 随机获取测试单词
    @Query("SELECT * FROM word_table WHERE bookType = :type ORDER BY RANDOM() LIMIT 20")
    suspend fun getQuizWords(type: String): List<WordEntity>

    @Update
    suspend fun updateWord(word: WordEntity)

    // 撤销斩杀
    @Query("UPDATE word_table SET isLearned = 0 WHERE id = :wordId")
    suspend fun markAsUnlearned(wordId: Int)

    // 批量标记已学
    @Query("UPDATE word_table SET isLearned = 1 WHERE bookType = :bookType AND id IN (:ids)")
    suspend fun markWordsAsLearned(bookType: String, ids: List<Int>)

    @Query("DELETE FROM word_table WHERE bookType = :type")
    suspend fun clearBook(type: String)

    @Query("DELETE FROM word_table")
    suspend fun deleteAll()

    // --- 新增：统计数据 ---
    @Query("SELECT COUNT(*) FROM word_table WHERE isLearned = 1")
    fun getLearnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_table")
    fun getTotalCount(): Flow<Int>
}