package com.example.rememberworlds.data.repository

import android.content.Context
import android.util.Log
import cn.leancloud.LCObject
import cn.leancloud.LCQuery
import cn.leancloud.LCUser
import com.example.rememberworlds.data.db.WordDao
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.network.NetworkModule
import com.example.rememberworlds.data.network.SearchResponseItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.reactivex.Observable

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
                results?.forEach { it.deleteInBackground().subscribe() }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- 4. 云端删除用户账户 ---
    fun deleteCurrentUser(): Observable<cn.leancloud.types.LCNull> {
        val user = LCUser.currentUser()
        // 如果未登录，返回一个错误 Observable
        return user?.deleteInBackground() ?: Observable.error(Exception("用户未登录或Session过期"))
    }

    // --- 5. 辅助方法 ---
    suspend fun searchWordOnline(word: String): SearchResponseItem? = withContext(Dispatchers.IO) { /* ... 保持不变 ... */
        try {
            val url = "https://api.dictionaryapi.dev/api/v2/entries/en/$word"
            val response = NetworkModule.api.searchWordOnline(url)
            if (response.isNotEmpty()) return@withContext response[0]
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext null
    }

    suspend fun checkUpdate(bookType: String): Boolean = withContext(Dispatchers.IO) { /* ... 保持不变 ... */
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