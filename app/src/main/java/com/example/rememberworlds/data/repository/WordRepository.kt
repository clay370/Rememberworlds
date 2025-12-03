package com.example.rememberworlds.data.repository

import android.content.Context
import cn.leancloud.LCObject
import cn.leancloud.LCQuery
import cn.leancloud.LCUser
import com.example.rememberworlds.data.db.WordDao
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.network.NetworkModule
import com.example.rememberworlds.data.network.SearchResponseItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordRepository(private val wordDao: WordDao, private val context: Context) {

    // --- 1. 同步进度 ---
    suspend fun syncUserProgress(bookType: String) = withContext(Dispatchers.IO) {
        val currentUser = LCUser.currentUser() ?: return@withContext
        try {
            val query = LCQuery<LCObject>("UserProgress")
            query.whereEqualTo("user", currentUser)
            query.whereEqualTo("book_type", bookType)
            query.limit(1000)
            val resultList = query.find()
            if (resultList != null && resultList.isNotEmpty()) {
                val learnedIds = resultList.map { it.getInt("word_id") }
                wordDao.markWordsAsLearned(bookType, learnedIds)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- 2. 保存进度 ---
    fun saveWordProgress(bookType: String, wordId: Int) {
        val currentUser = LCUser.currentUser() ?: return
        val progress = LCObject("UserProgress")
        progress.put("user", currentUser)
        progress.put("book_type", bookType)
        progress.put("word_id", wordId)
        progress.saveInBackground().subscribe()
    }

    // --- 3. 撤销斩杀 ---
    suspend fun revertWordStatus(bookType: String, wordId: Int) = withContext(Dispatchers.IO) {
        wordDao.markAsUnlearned(wordId)
        try {
            val currentUser = LCUser.currentUser()
            if (currentUser != null) {
                val query = LCQuery<LCObject>("UserProgress")
                query.whereEqualTo("user", currentUser)
                query.whereEqualTo("book_type", bookType)
                query.whereEqualTo("word_id", wordId)
                val results = query.find()
                // 这里量很少，用 forEach delete 也没问题，或者用 deleteAll
                if (results != null && results.isNotEmpty()) {
                    LCObject.deleteAll(results)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- 4. 云端删除用户账户 ---
    // 【已修复】: 支持删除超过1000条数据，并确保数据删完后才删用户
    suspend fun deleteCurrentUserAndProgress() = withContext(Dispatchers.IO) {
        val currentUser = LCUser.currentUser() ?: throw Exception("用户未登录或Session过期")

        // 步骤 1: 循环删除所有 UserProgress 记录 (处理数据量 > 1000 的情况)
        // 这一步必须完全成功，否则不进行下一步
        try {
            var hasMoreData = true
            while (hasMoreData) {
                val query = LCQuery<LCObject>("UserProgress")
                query.whereEqualTo("user", currentUser)
                query.limit(1000) // LeanCloud 单次查询上限
                val resultList = query.find()

                if (resultList != null && resultList.isNotEmpty()) {
                    // 同步批量删除，阻塞直到完成
                    LCObject.deleteAll(resultList)
                    
                    // 如果取出的数量少于 1000，说明是最后一批，或者已经删完了
                    if (resultList.size < 1000) {
                        hasMoreData = false
                    }
                } else {
                    hasMoreData = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果清理数据这一步就挂了，抛出异常阻止删除用户，提示用户重试
            throw Exception("数据清理中断，请检查网络后重试: ${e.message}")
        }

        // 步骤 2: 删除 LCUser 账户
        try {
            currentUser.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果数据删完了，但人没删掉 (比如网络波动)，抛出异常让 ViewModel 提示用户
            throw Exception("数据已清除，但账户注销失败: ${e.message}")
        }
    }

    // --- 5. 辅助方法 ---
    suspend fun searchWordOnline(word: String): SearchResponseItem? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.dictionaryapi.dev/api/v2/entries/en/$word"
            val response = NetworkModule.api.searchWordOnline(url)
            if (response.isNotEmpty()) return@withContext response[0]
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext null
    }

    suspend fun checkUpdate(bookType: String): Boolean = withContext(Dispatchers.IO) {
        var isUpdated = false
        try {
            val query = LCQuery<LCObject>("VersionControl")
            query.whereEqualTo("book_type", bookType)
            val resultList = query.find()

            if (resultList != null && resultList.isNotEmpty()) {
                val cloudData = resultList[0]
                val cloudVersion = cloudData.getInt("version_code")
                val downloadUrl = cloudData.getString("json_url")

                val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
                val localVersion = prefs.getInt("version_$bookType", 0)

                if (cloudVersion > localVersion) {
                    val jsonList = NetworkModule.api.downloadWords(downloadUrl)
                    val entityList = jsonList.map { json -> WordEntity(id = json.id, word = json.word, cn = json.cn, audio = json.audio, bookType = bookType) }
                    wordDao.clearBook(bookType)
                    wordDao.insertAll(entityList)
                    prefs.edit().putInt("version_$bookType", cloudVersion).apply()
                    syncUserProgress(bookType)
                    isUpdated = true
                }
            }
        } catch (e: Exception) { throw e }
        return@withContext isUpdated
    }

    suspend fun deleteBook(bookType: String) = withContext(Dispatchers.IO) { wordDao.clearBook(bookType) }
    suspend fun clearAllData() = withContext(Dispatchers.IO) { wordDao.deleteAll() }
    fun getLearnedWords(bookType: String) = wordDao.getLearnedWords(bookType)
}