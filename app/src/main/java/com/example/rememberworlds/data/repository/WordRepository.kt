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
    private val userBookDao: com.example.rememberworlds.data.db.UserBookDao?,
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
    private val storage = com.google.firebase.storage.FirebaseStorage.getInstance()

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
            // 1. 本地立即更新状态
            wordDao.markWordsAsLearned(bookType, listOf(wordId))

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
     * 获取所有可用书籍
     * 从Firestore获取书籍列表，并尝试从本地缓存加载以作为备选
     *
     * @return 书籍模型列表
     */
    suspend fun fetchAvailableBooks(): List<com.example.rememberworlds.data.model.BookModel> =
        withContext(Dispatchers.IO) {
            try {
                val snapshot = db.collection("books").get().await()
                val books = snapshot.toObjects(com.example.rememberworlds.data.model.BookModel::class.java)
                
                // Cache the results
                saveBooksToCache(books)
                
                books
            } catch (e: Exception) {
                e.printStackTrace()
                // Return cached books if network fails, or empty list
                getCachedBooks()
            }
        }
        
    private fun saveBooksToCache(books: List<com.example.rememberworlds.data.model.BookModel>) {
        try {
            val json = com.google.gson.Gson().toJson(books)
            context.getSharedPreferences("app_cache", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("cached_books", json)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getCachedBooks(): List<com.example.rememberworlds.data.model.BookModel> {
        try {
            val json = context.getSharedPreferences("app_cache", android.content.Context.MODE_PRIVATE)
                .getString("cached_books", null)
            if (json != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<com.example.rememberworlds.data.model.BookModel>>() {}.type
                return com.google.gson.Gson().fromJson(json, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Return default list for first run if cache is empty
        return listOf(
            com.example.rememberworlds.data.model.BookModel("cet4", "四级词汇", "大学英语", parts = listOf("cet4")),
            com.example.rememberworlds.data.model.BookModel("cet6", "六级词汇", "大学英语", parts = listOf("cet6")),
            com.example.rememberworlds.data.model.BookModel("kaoyan", "考研词汇", "研究生入学", parts = listOf("kaoyan")),
            com.example.rememberworlds.data.model.BookModel("toefl", "托福词汇", "出国留学", parts = listOf("toefl")),
            com.example.rememberworlds.data.model.BookModel("ielts", "雅思词汇", "出国留学", parts = listOf("ielts")),
            com.example.rememberworlds.data.model.BookModel("gre", "GRE词汇", "出国留学", parts = listOf("gre")),
            com.example.rememberworlds.data.model.BookModel("tem4", "专四词汇", "英语专业", parts = listOf("tem4")),
            com.example.rememberworlds.data.model.BookModel("tem8", "专八词汇", "英语专业", parts = listOf("tem8"))
        )
    }

    /**
     * 下载并导入书籍
     * 支持分卷下载 (Multi-part)，下载ZIP后自动解压并导入JSON数据
     *
     * @param book 书籍对象
     * @param onProgress 进度回调函数
     * @return 下载是否成功
     */
    suspend fun downloadBook(
        book: com.example.rememberworlds.data.model.BookModel,
        onProgress: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val tempDir = java.io.File(context.cacheDir, "book_temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        
        try {
            // Clear previous data for this book to avoid duplication if re-downloading
            // We use book.bookId as the identifier.
            wordDao.clearBook(book.bookId)

            val parts = if (book.parts.isNotEmpty()) book.parts else listOf(book.type) // Fallback if parts empty? No, checking parts is enough.
            
            // If parts is empty but we have a storagePath (legacy), handle that? 
            // The new guide says stick to 'parts'. But let's handle if parts is used.
            val downloadTargets = if (book.parts.isNotEmpty()) book.parts else emptyList()

            downloadTargets.forEachIndexed { index, path ->
                onProgress("正在下载第 ${index + 1}/${downloadTargets.size} 部分...")
                
                val targetFile = java.io.File(tempDir, "temp_${System.currentTimeMillis()}.zip")
                val storageRef = storage.reference.child(path)
                
                // Download to file
                storageRef.getFile(targetFile).await()
                
                onProgress("正在解压第 ${index + 1} 部分...")
                val unzippedFiles = com.example.rememberworlds.utils.ZipUtils.unzip(targetFile, tempDir)
                
                onProgress("正在导入数据...")
                // Find JSON files
                for (file in unzippedFiles) {
                    if (file.extension.equals("json", ignoreCase = true)) {
                        java.io.FileInputStream(file).use { stream ->
                           com.example.rememberworlds.utils.JsonImporter.importJsonData(stream, wordDao, book.bookId)
                        }
                    }
                    file.delete() // Clean up extracted file
                }
                targetFile.delete() // Clean up zip
            }
            
            // Mark as learned? No, default is false.
            // Sync progress if user logged in
            syncUserProgress(book.bookId)
            
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            tempDir.deleteRecursively()
        }
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
     * 重置本地所有学习进度
     * 将所有单词的 isLearned 设为 false
     */
    suspend fun resetLocalProgress() = 
        withContext(Dispatchers.IO) {
            wordDao.resetAllProgress()
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

    /**
     * 获取所有收藏的单词
     */
    suspend fun getFavoriteWords() = 
        withContext(Dispatchers.IO) {
            wordDao.getFavoriteWords()
        }

    /**
     * 切换单词的收藏状态
     */
    suspend fun toggleFavorite(wordId: Int, isFavorite: Boolean) = 
        withContext(Dispatchers.IO) {
            wordDao.updateIsFavorite(wordId, isFavorite)
        }

    /**
     * 获取所有错题
     */
    suspend fun getMistakeWords() = 
        withContext(Dispatchers.IO) {
            wordDao.getMistakeWords()
        }

    /**
     * 标记单词为错题
     */
    suspend fun markAsWrong(wordId: Int) = 
        withContext(Dispatchers.IO) {
            wordDao.updateIsWrong(wordId, true)
        }

    // --- User Books ---
    fun getAllUserBooks() = userBookDao?.getAllUserBooks() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun createUserBook(name: String) {
        val book = com.example.rememberworlds.data.db.UserBookEntity(name = name)
        userBookDao?.insert(book)
    }

    suspend fun deleteUserBook(id: String) {
        userBookDao?.deleteById(id)
    }

    suspend fun addUserWord(bookId: String, word: String, meaning: String) {
        val newWord = WordEntity(
            id = (word + System.currentTimeMillis()).hashCode(), // Simple unique ID generation
            word = word,
            cn = meaning,
            audio = "", // No audio for manual words initially
            bookType = bookId,
            isLearned = false
        )
        withContext(Dispatchers.IO) {
            wordDao.insertAll(listOf(newWord))
        }
    }
}