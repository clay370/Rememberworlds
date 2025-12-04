package com.example.rememberworlds.data.repository

import android.content.Context
import android.util.Log
import com.example.rememberworlds.data.db.WordDao
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.network.NetworkModule
import com.example.rememberworlds.data.network.SearchResponseItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WordRepository(private val wordDao: WordDao, private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- 1. 同步进度 ---
    // 从 Firestore: users/{uid}/progress 集合中拉取数据
    suspend fun syncUserProgress(bookType: String) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        try {
            // 查询: users -> {uid} -> progress 集合，筛选 book_type
            val snapshot = db.collection("users")
                .document(user.uid)
                .collection("progress")
                .whereEqualTo("book_type", bookType)
                .get()
                .await() // 使用协程等待结果

            if (!snapshot.isEmpty) {
                // 提取 word_id 列表
                val learnedIds = snapshot.documents.mapNotNull {
                    // Firestore 中数字默认可能是 Long，需要转 Int
                    (it.getLong("word_id"))?.toInt()
                }
                if (learnedIds.isNotEmpty()) {
                    wordDao.markWordsAsLearned(bookType, learnedIds)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 2. 保存进度 ---
    // 写入 Firestore: users/{uid}/progress
    suspend fun saveWordProgress(bookType: String, wordId: Int) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        try {
            val data = hashMapOf(
                "book_type" to bookType,
                "word_id" to wordId,
                "timestamp" to System.currentTimeMillis()
            )
            // 添加文档
            db.collection("users")
                .document(user.uid)
                .collection("progress")
                .add(data)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 3. 撤销斩杀 ---
    suspend fun revertWordStatus(bookType: String, wordId: Int) = withContext(Dispatchers.IO) {
        wordDao.markAsUnlearned(wordId) // 本地回滚
        
        val user = auth.currentUser ?: return@withContext
        try {
            // 先查询找到那个文档
            val snapshot = db.collection("users")
                .document(user.uid)
                .collection("progress")
                .whereEqualTo("book_type", bookType)
                .whereEqualTo("word_id", wordId)
                .get()
                .await()

            // 删除找到的文档
            for (document in snapshot.documents) {
                document.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 4. 注销账户 (包含删除数据) ---
    suspend fun deleteCurrentUserAndProgress() = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: throw Exception("未登录")

        try {
            // 1. 删除该用户下所有的 progress 数据 (Firestore 删除集合比较麻烦，需要遍历删除)
            // 简单做法：这里演示删除 progress 子集合中的所有文档
            val progressCollection = db.collection("users").document(user.uid).collection("progress")
            var snapshot = progressCollection.limit(100).get().await()
            
            while (!snapshot.isEmpty) {
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                snapshot = progressCollection.limit(100).get().await()
            }

            // 2. 删除用户 User (Auth)
            user.delete().await()
        } catch (e: Exception) {
            throw e
        }
    }

    // --- 5. 检查更新 ---
    // 查询 Firestore: VersionControl 集合
    suspend fun checkUpdate(bookType: String): Boolean = withContext(Dispatchers.IO) {
        var isUpdated = false
        try {
            // 假设 Firestore 中有一个集合叫 VersionControl，里面存了各本书的配置
            val snapshot = db.collection("VersionControl")
                .whereEqualTo("book_type", bookType)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val doc = snapshot.documents[0]
                val cloudVersion = doc.getLong("version_code")?.toInt() ?: 0
                val downloadUrl = doc.getString("json_url") ?: ""

                val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
                val localVersion = prefs.getInt("version_$bookType", 0)

                if (cloudVersion > localVersion && downloadUrl.isNotEmpty()) {
                    // 下载逻辑保持不变
                    val jsonList = NetworkModule.api.downloadWords(downloadUrl)
                    val entityList = jsonList.map { json ->
                        WordEntity(id = json.id, word = json.word, cn = json.cn, audio = json.audio, bookType = bookType)
                    }
                    wordDao.clearBook(bookType)
                    wordDao.insertAll(entityList)
                    
                    prefs.edit().putInt("version_$bookType", cloudVersion).apply()
                    syncUserProgress(bookType)
                    isUpdated = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // throw e // 可选：是否抛出异常给 ViewModel 处理
        }
        return@withContext isUpdated
    }
    
    // ... 其他保持不变 ...
    suspend fun searchWordOnline(word: String): SearchResponseItem? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.dictionaryapi.dev/api/v2/entries/en/$word"
            val response = NetworkModule.api.searchWordOnline(url)
            if (response.isNotEmpty()) return@withContext response[0]
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext null
    }

    suspend fun deleteBook(bookType: String) = withContext(Dispatchers.IO) { wordDao.clearBook(bookType) }
    suspend fun clearAllData() = withContext(Dispatchers.IO) { wordDao.deleteAll() }
    fun getLearnedWords(bookType: String) = wordDao.getLearnedWords(bookType)
}