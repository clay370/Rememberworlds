package com.example.rememberworlds.data.repository


import android.content.Context
import com.example.rememberworlds.data.db.WordDao
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.network.NetworkModule
import com.example.rememberworlds.data.network.SearchResponseItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


/**
 * 单词仓库类
 * 负责协调本地数据库和远程Firebase数据
 * 实现数据的获取、存储、同步和更新等功能
 */
class WordRepository(
    private val wordDao: WordDao,
    private val context: Context
) {

    /**
     * Firebase Firestore数据库实例
     */
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Firebase Auth认证实例
     */
    private val auth = FirebaseAuth.getInstance()

    /**
     * 同步用户学习进度
     * 从Firestore的users/{uid}/progress集合中拉取数据
     * 将云端标记的已学单词同步到本地数据库
     *
     * @param bookType 书籍类型标识符
     */
    suspend fun syncUserProgress(bookType: String) = 
        withContext(Dispatchers.IO) {
            // 检查用户是否已登录，未登录则直接返回
            val user = auth.currentUser 
                ?: return@withContext
            
            try {
                // 查询当前用户指定书籍类型的学习进度
                val snapshot = db.collection("users")
                    .document(user.uid) // 当前用户ID
                    .collection("progress") // 进度集合
                    .whereEqualTo("book_type", bookType) // 筛选指定书籍类型
                    .get()
                    .await() // 使用协程等待异步结果

                // 如果查询结果不为空
                if (!snapshot.isEmpty) {
                    // 提取所有已学单词的ID列表
                    val learnedIds = snapshot.documents.mapNotNull {
                        document ->
                        // Firestore中数字默认是Long类型，需要转换为Int
                        (document.getLong("word_id"))?.toInt()
                    }
                    
                    // 如果有已学单词ID，批量标记到本地数据库
                    if (learnedIds.isNotEmpty()) {
                        wordDao.markWordsAsLearned(bookType, learnedIds)
                    }
                }
            } catch (e: Exception) {
                // 打印异常信息
                e.printStackTrace()
            }
        }

    /**
     * 保存单词学习进度
     * 将学习记录写入Firestore的users/{uid}/progress集合
     *
     * @param bookType 书籍类型标识符
     * @param wordId 学习的单词ID
     */
    suspend fun saveWordProgress(
        bookType: String,
        wordId: Int
    ) = 
        withContext(Dispatchers.IO) {
            // 检查用户是否已登录，未登录则直接返回
            val user = auth.currentUser 
                ?: return@withContext
            
            try {
                // 准备要保存的数据
                val data = hashMapOf(
                    "book_type" to bookType, // 书籍类型
                    "word_id" to wordId, // 单词ID
                    "timestamp" to System.currentTimeMillis() // 学习时间戳
                )
                
                // 添加文档到Firestore
                db.collection("users")
                    .document(user.uid)
                    .collection("progress")
                    .add(data) // 自动生成文档ID
                    .await()
            } catch (e: Exception) {
                // 打印异常信息
                e.printStackTrace()
            }
        }

    /**
     * 撤销单词的学习状态
     * 将本地数据库中单词标记为未学，并删除Firestore中的相应记录
     *
     * @param bookType 书籍类型标识符
     * @param wordId 要撤销的单词ID
     */
    suspend fun revertWordStatus(
        bookType: String,
        wordId: Int
    ) = 
        withContext(Dispatchers.IO) {
            // 1. 本地回滚：将单词标记为未学
            wordDao.markAsUnlearned(wordId)
            
            // 检查用户是否已登录，未登录则无需同步到云端
            val user = auth.currentUser 
                ?: return@withContext
            
            try {
                // 2. 查询Firestore中对应的记录
                val snapshot = db.collection("users")
                    .document(user.uid)
                    .collection("progress")
                    .whereEqualTo("book_type", bookType)
                    .whereEqualTo("word_id", wordId)
                    .get()
                    .await()

                // 3. 删除所有找到的文档
                for (document in snapshot.documents) {
                    document.reference.delete().await()
                }
            } catch (e: Exception) {
                // 打印异常信息
                e.printStackTrace()
            }
        }

    /**
     * 注销账户并删除所有相关数据
     * 1. 删除Firestore中该用户的所有进度数据
     * 2. 删除Firebase Auth中的用户账户
     *
     * @throws Exception 如果用户未登录或操作失败
     */
    suspend fun deleteCurrentUserAndProgress() = 
        withContext(Dispatchers.IO) {
            // 检查用户是否已登录，未登录则抛出异常
            val user = auth.currentUser 
                ?: throw Exception("未登录")

            try {
                // 1. 删除用户的所有进度数据
                // Firestore删除集合需要分批处理，这里每次处理100个文档
                val progressCollection = db.collection("users")
                    .document(user.uid)
                    .collection("progress")
                
                var snapshot = progressCollection
                    .limit(100)
                    .get()
                    .await()
                
                // 循环处理直到所有文档都被删除
                while (!snapshot.isEmpty) {
                    // 使用批量写入操作提高效率
                    val batch = db.batch()
                    
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    
                    // 提交批量操作
                    batch.commit().await()
                    
                    // 继续查询下一批文档
                    snapshot = progressCollection
                        .limit(100)
                        .get()
                        .await()
                }

                // 2. 删除Firebase Auth用户账户
                user.delete().await()
            } catch (e: Exception) {
                // 重新抛出异常，让调用者处理
                throw e
            }
        }

    /**
     * 检查单词书更新
     * 从Firestore的VersionControl集合查询最新版本信息
     * 如果云端版本高于本地版本，则下载并更新本地数据库
     *
     * @param bookType 书籍类型标识符
     * @return 是否成功更新
     */
    suspend fun checkUpdate(bookType: String): Boolean = 
        withContext(Dispatchers.IO) {
            var isUpdated = false
            
            try {
                // 查询VersionControl集合中指定书籍的版本信息
                val snapshot = db.collection("VersionControl")
                    .whereEqualTo("book_type", bookType)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    // 获取第一个文档（应该只有一个）
                    val doc = snapshot.documents[0]
                    
                    // 云端版本号
                    val cloudVersion = doc.getLong("version_code")?.toInt() ?: 0
                    
                    // 单词数据JSON下载URL
                    val downloadUrl = doc.getString("json_url") ?: ""

                    // 获取本地存储的版本号
                    val prefs = context.getSharedPreferences(
                        "app_config", 
                        Context.MODE_PRIVATE
                    )
                    
                    val localVersion = prefs.getInt("version_$bookType", 0)

                    // 如果云端版本高于本地版本且下载URL有效
                    if (cloudVersion > localVersion && downloadUrl.isNotEmpty()) {
                        // 下载最新的单词数据
                        val jsonList = NetworkModule.api.downloadWords(downloadUrl)
                        
                        // 转换为本地数据库实体
                        val entityList = jsonList.map { 
                            json ->
                            WordEntity(
                                id = json.id, 
                                word = json.word, 
                                cn = json.cn, 
                                audio = json.audio, 
                                bookType = bookType
                            )
                        }
                        
                        // 清空本地该书籍的所有数据
                        wordDao.clearBook(bookType)
                        
                        // 插入新下载的数据
                        wordDao.insertAll(entityList)
                        
                        // 更新本地版本号
                        prefs.edit()
                            .putInt("version_$bookType", cloudVersion)
                            .apply()
                        
                        // 同步学习进度
                        syncUserProgress(bookType)
                        
                        // 更新成功标志
                        isUpdated = true
                    }
                }
            } catch (e: Exception) {
                // 打印异常信息
                e.printStackTrace() 
            }
            
            // 返回更新结果
            return@withContext isUpdated
        }
    
    /**
     * 在线查词功能
     * 使用Free Dictionary API查询单词的详细信息
     *
     * @param word 要查询的单词
     * @return 包含单词详细信息的SearchResponseItem对象，查询失败返回null
     */
    suspend fun searchWordOnline(word: String): SearchResponseItem? = 
        withContext(Dispatchers.IO) {
            try {
                // 构建查词API URL
                val url = "https://api.dictionaryapi.dev/api/v2/entries/en/$word"
                
                // 发起网络请求
                val response = NetworkModule.api.searchWordOnline(url)
                
                // 如果响应不为空，返回第一个结果
                if (response.isNotEmpty()) {
                    return@withContext response[0]
                }
            } catch (e: Exception) {
                // 打印异常信息
                e.printStackTrace() 
            }
            
            // 查询失败返回null
            return@withContext null
        }

    /**
     * 删除指定书籍类型的所有单词
     *
     * @param bookType 书籍类型标识符
     */
    suspend fun deleteBook(bookType: String) = 
        withContext(Dispatchers.IO) {
            wordDao.clearBook(bookType)
        }

    /**
     * 删除所有单词数据
     */
    suspend fun clearAllData() = 
        withContext(Dispatchers.IO) {
            wordDao.deleteAll()
        }

    /**
     * 获取指定书籍类型下已学过的单词
     * 返回Flow以便实时监听数据变化
     *
     * @param bookType 书籍类型标识符
     * @return 包含已学单词列表的Flow
     */
    fun getLearnedWords(bookType: String) = 
        wordDao.getLearnedWords(bookType)
}