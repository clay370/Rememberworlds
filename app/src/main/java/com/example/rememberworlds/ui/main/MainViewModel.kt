package com.example.rememberworlds.ui.main

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.google.firebase.storage.FirebaseStorage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rememberworlds.data.db.AppDatabase
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.model.BookModel
import com.example.rememberworlds.data.network.SearchResponseItem
import com.example.rememberworlds.data.repository.WordRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.rememberworlds.data.model.UserProfile
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit


/**
 * 测验题目
 */
data class Question(
    val type: QuizType,
    val targetWord: WordEntity,
    val options: List<String>
)

/**
 * 测验类型
 */
enum class QuizType {
    EN_TO_CN,
    CN_TO_EN,
    AUDIO_TO_CN,
    SPELLING     
}

/**
 * 连击状态
 */
data class ComboState(
    val count: Int = 0,
    val multiplier: Float = 1.0f,
    val showAnimation: Boolean = false
)

/**
 * 拼写状态
 */
data class SpellingState(
    val input: String = "",
    val hintText: String = "", 
    val isError: Boolean = false,
    val hintCount: Int = 0,
    val correctAnswer: String = ""
)

/**
 * 测验结果项 (新增)
 */
data class QuizResultItem(
    val question: Question,
    val isCorrect: Boolean
)

// --- ViewModel --- 
class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val repository = WordRepository(db.wordDao(), application)

    // --- 状态变量 --- 
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()
    
    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var lastActionTime = 0L // 防止快速点击

    private val _downloadingBookType = MutableStateFlow<String?>(null)
    val downloadingBookType = _downloadingBookType.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(FirebaseAuth.getInstance().currentUser)
    val currentUser = _currentUser.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _statusMsg = MutableStateFlow("")
    val statusMsg = _statusMsg.asStateFlow()
    
    private val _bookList = MutableStateFlow<List<BookModel>>(emptyList())
    val bookList = _bookList.asStateFlow()

    // 我的单词本列表 (目前仅包含收藏单词本)
    private val _myBooksList = MutableStateFlow<List<BookModel>>(
        listOf(
            BookModel(
                bookId = "favorite",
                name = "收藏单词本",
                category = "我的单词本",
                isDownloaded = true // 默认为已存在
            ),
            BookModel(
                bookId = "mistake",
                name = "错词本",
                category = "我的单词本",
                isDownloaded = true // 默认为已存在
            )
        )
    )
    val myBooksList = _myBooksList.asStateFlow()
    
    private val _isLearningMode = MutableStateFlow(false)
    val isLearningMode = _isLearningMode.asStateFlow()
    
    private val _currentWord = MutableStateFlow<WordEntity?>(null)
    val currentWord = _currentWord.asStateFlow()
    
    private val _isReviewingMode = MutableStateFlow(false)
    val isReviewingMode = _isReviewingMode.asStateFlow()
    
    private val _reviewedWords = MutableStateFlow<List<WordEntity>>(emptyList())
    val reviewedWords = _reviewedWords.asStateFlow()
    
    private val _searchResult = MutableStateFlow<SearchResponseItem?>(null)
    val searchResult = _searchResult.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    
    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog = _showSearchDialog.asStateFlow()
    
    // --- 测验状态 ---
    private val _quizQuestions = MutableStateFlow<List<Question>>(emptyList())
    val quizQuestions = _quizQuestions.asStateFlow()
    
    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex = _currentQuizIndex.asStateFlow()
    
    private val _quizScore = MutableStateFlow(0)
    val quizScore = _quizScore.asStateFlow()
    
    private val _isQuizFinished = MutableStateFlow(false)
    val isQuizFinished = _isQuizFinished.asStateFlow()
    
    private val _answerState = MutableStateFlow(0) // 0:未回答, 1:正确, 2:错误
    val answerState = _answerState.asStateFlow()
    
    private val _quizStep = MutableStateFlow(2) 
    val quizStep = _quizStep.asStateFlow()
    
    private val _quizSelectedBookType = MutableStateFlow("cet4")
    val quizSelectedBookType = _quizSelectedBookType.asStateFlow()
    
    private val _userSelectedOption = MutableStateFlow("")
    val userSelectedOption = _userSelectedOption.asStateFlow()

    private val _comboState = MutableStateFlow(ComboState())
    val comboState = _comboState.asStateFlow()

    private val _spellingState = MutableStateFlow(SpellingState())
    val spellingState = _spellingState.asStateFlow()
    
    // 测验历史记录 (新增)
    private val _quizHistory = MutableStateFlow<List<QuizResultItem>>(emptyList())
    val quizHistory = _quizHistory.asStateFlow()

    private val _timeLeft = MutableStateFlow(15.0f) 
    val timeLeft = _timeLeft.asStateFlow()
    
    private var timerJob: Job? = null
    private val _wrongWords = mutableListOf<WordEntity>() 
    
    // 学习模式当前列表
    private var learningList: List<WordEntity> = emptyList() 

    // 统计数据
    val learnedCount: Flow<Int> = db.wordDao().getLearnedCount()
    val totalCount: Flow<Int> = db.wordDao().getTotalCount()
    private val _streakDays = MutableStateFlow(0)
    val streakDays = _streakDays.asStateFlow()
    private val _dailyCount = MutableStateFlow(0)
    val dailyCount = _dailyCount.asStateFlow()
    private val _dailyGoal = MutableStateFlow(20)
    val dailyGoal = _dailyGoal.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser
    }
    
    init {
        auth.addAuthStateListener(authStateListener)

        initTheme()
        refreshBookshelf()
        initDailyStats()
        initNetworkMonitor()
        tts = TextToSpeech(application, this)
        
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    fetchUserProfile(user.uid)
                    // 登录即同步所有已下载书籍的进度
                    val types = listOf("cet4", "cet6", "tem4", "tem8", "kaoyan", "toefl", "ielts", "gre")
                    types.forEach { type ->
                        repository.syncUserProgress(type)
                    }
                } else {
                    _userProfile.value = UserProfile()
                }
            }
        }
    }

    private fun initTheme() {
        val prefs = getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE)
        _isDarkTheme.value = prefs.getBoolean("is_dark_theme", false)
    }

    /**
     * 刷新书架数据
     * 优先加载本地缓存以快速显示，随后从网络获取最新数据更新
     */
    fun refreshBookshelf() {
        viewModelScope.launch {
            // 1. Load cache first for instant UI
            val cachedBooks = repository.getCachedBooks()
            if (cachedBooks.isNotEmpty()) {
                val cachedWithStatus = cachedBooks.map { book ->
                    val count = db.wordDao().getAllWordsByBook(book.bookId).size
                    book.copy(isDownloaded = count > 0)
                }
                _bookList.value = cachedWithStatus
                autoSelectQuizBook()
            }
            
            // 2. Fetch from network for updates
            _isLoading.value = true
            try {
                // Fetch from Firestore
                val remoteBooks = repository.fetchAvailableBooks()
                
                // Update local status check
                val updatedList = remoteBooks.map { book ->
                    // Check if we have words for this bookId
                    val count = db.wordDao().getAllWordsByBook(book.bookId).size
                    book.copy(isDownloaded = count > 0)
                }
                
                _bookList.value = updatedList
                autoSelectQuizBook()
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMsg.value = "获取书架失败，请检查网络"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun autoSelectQuizBook() {
        val currentType = _quizSelectedBookType.value
        val books = _bookList.value
        
        // Check if current selection is valid (exists and downloaded)
        val currentValid = books.any { it.bookId == currentType && it.isDownloaded }
        
        if (!currentValid) {
            // Find first downloaded book
            val firstDownloaded = books.find { it.isDownloaded }
            if (firstDownloaded != null) {
                _quizSelectedBookType.value = firstDownloaded.bookId
            }
        }
    }

    /**
     * 加载书籍
     * 如果书籍未下载则开始下载，已下载则切换到该书籍并刷新视图
     *
     * @param book 目标书籍对象
     */
    fun loadBook(book: BookModel) {
        if (!book.isDownloaded) {
            downloadBook(book.bookId)
        } else {
             // 切换书籍
            viewModelScope.launch {
                _isLoading.value = true
                delay(500)
                // 模拟刷新做点什么，实际可能需要更新 UI 状态
                refreshBookshelf()
                _isLoading.value = false
            }
        }
    }

    /**
     * 切换应用主题 (日间/夜间模式)
     *
     * @param isDark 是否为夜间模式
     */
    fun toggleTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE)
            .edit().putBoolean("is_dark_theme", isDark).apply()
    }

    private fun initNetworkMonitor() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _isOnline.value = true }
            override fun onLost(network: Network) { _isOnline.value = false }
        })
    }

    /**
     * 播放音频
     * 如果网络不可用或URL为空，则降级使用TTS播放
     *
     * @param url 音频文件URL
     * @param wordText 单词文本 (用于TTS备选)
     */
    fun playAudio(url: String, wordText: String? = null) {
        if (!_isOnline.value || url.isBlank()) {
            playTTS(wordText)
            return
        }
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnErrorListener { _, _, _ ->
                    playTTS(wordText)
                    true
                }
            }
        } catch (e: Exception) {
            playTTS(wordText)
        }
    }

    private fun playTTS(text: String?) {
        if (isTtsReady && !text.isNullOrBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        mediaPlayer?.release()
        tts?.stop()
        tts?.shutdown()
    }

    private fun triggerVibration(isCorrect: Boolean) {
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (isCorrect) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }

    // ================= 测验逻辑 =================

    /**
     * 选择用于测验的书籍类型
     *
     * @param bt 书籍ID (类型)
     */
    fun selectQuizBook(bt: String) {
        _quizSelectedBookType.value = bt
        _quizStep.value = 2
    }

    fun backToBookSelection() {
        _quizStep.value = 1
        _quizSelectedBookType.value = ""
    }

    /**
     * 开始测验
     * 根据选择的模式生成题目
     *
     * @param mode 测验模式ID
     */
    fun startQuiz(mode: Int) {
        val bookType = _quizSelectedBookType.value
        if (bookType.isEmpty()) return

        viewModelScope.launch {
            val allWords = db.wordDao().getAllWordsByBook(bookType)
            if (allWords.size < 4) {
                Toast.makeText(getApplication(), "单词不足4个，无法生成题目", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val quizWords = allWords.shuffled().take(10)
            // 生成题目
            val questions = quizWords.map { target ->
                val qType = when(mode) {
                    4 -> QuizType.SPELLING
                    1 -> QuizType.EN_TO_CN
                    2 -> QuizType.CN_TO_EN
                    3 -> QuizType.AUDIO_TO_CN
                    else -> QuizType.values().random()
                }
                
                val options = if (qType == QuizType.SPELLING) emptyList() else {
                    val distractors = allWords.filter { it.id != target.id }.shuffled().take(3)
                    if (qType == QuizType.CN_TO_EN) {
                        (distractors + target).map { it.word }.shuffled()
                    } else {
                        (distractors + target).map { it.cn }.shuffled()
                    }
                }
                Question(qType, target, options)
            }

            // 重置测验状态
            _quizQuestions.value = questions
            _currentQuizIndex.value = 0
            _quizScore.value = 0
            _answerState.value = 0
            _userSelectedOption.value = ""
            _comboState.value = ComboState()
            _quizHistory.value = emptyList() // 重置历史
            _wrongWords.clear()
            _isQuizFinished.value = false
            
            startTimer()
            initSpellingState(questions[0])

            if (questions.isNotEmpty() && questions[0].type == QuizType.AUDIO_TO_CN) {
                delay(500)
                playAudio(questions[0].targetWord.audio, questions[0].targetWord.word)
            }
        }
    }

    /**
     * 提交答案
     * 验证用户选择的选项是否正确，并处理计分和连击逻辑
     *
     * @param opt 用户选择的选项文本
     */
    fun answerQuestion(opt: String) {
        if (_answerState.value != 0) return

        _userSelectedOption.value = opt
        val index = _currentQuizIndex.value
        val qs = _quizQuestions.value
        if (index >= qs.size) return

        val q = qs[index]
        val correct = if (q.type == QuizType.CN_TO_EN) {
            opt == q.targetWord.word
        } else {
            opt == q.targetWord.cn
        }

        if (correct) {
            processCorrectAnswer()
        } else {
            _answerState.value = 2
            _comboState.value = ComboState(0, 1.0f)
            triggerVibration(false)
            _wrongWords.add(_quizQuestions.value[_currentQuizIndex.value].targetWord)
            recordCurrentResult(false) // 记录错误
        }
        timerJob?.cancel()
    }

    /**
     * 进入下一题
     * 重置相关状态并启动计时器
     */
    fun nextQuestion() {
        val index = _currentQuizIndex.value
        val qs = _quizQuestions.value

        if (index < qs.size - 1) {
            _currentQuizIndex.value += 1
            _answerState.value = 0
            _userSelectedOption.value = ""
            
            val nextQ = qs[_currentQuizIndex.value]
            if (nextQ.type == QuizType.AUDIO_TO_CN) {
                playAudio(nextQ.targetWord.audio, nextQ.targetWord.word)
            }
            
            startTimer()
            initSpellingState(nextQ)
        } else {
            _isQuizFinished.value = true
        }
    }

    /**
     * 退出测验
     * 清理资源和状态
     */
    fun quitQuiz() {
        _quizQuestions.value = emptyList()
        mediaPlayer?.release()
        timerJob?.cancel()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _timeLeft.value = 15.0f
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0 && isActive) {
                delay(100)
                _timeLeft.value -= 0.1f
            }
            if (_timeLeft.value <= 0) {
                handleTimeout()
            }
        }
    }

    private fun handleTimeout() {
        _answerState.value = 2
        _comboState.value = ComboState(0, 1.0f)
        triggerVibration(false)
        
        val currentQ = _quizQuestions.value.getOrNull(_currentQuizIndex.value)
        currentQ?.let { 
            _wrongWords.add(it.targetWord) 
            recordCurrentResult(false) // 记录超时错误
        }
    }

    private fun initSpellingState(q: Question) {
        if (q.type == QuizType.SPELLING) {
            val length = q.targetWord.word.length
            val mask = "-".repeat(length)
            _spellingState.value = SpellingState(
                input = "", 
                hintText = mask,
                correctAnswer = q.targetWord.word
            )
        }
    }

    fun updateSpellingInput(input: String) {
        if (input.length <= _spellingState.value.correctAnswer.length) {
            _spellingState.value = _spellingState.value.copy(input = input, isError = false)
        }
    }

    fun submitSpelling() {
        val currentQ = _quizQuestions.value[_currentQuizIndex.value]
        val input = _spellingState.value.input.trim()
        val target = currentQ.targetWord.word.trim()

        if (input.equals(target, ignoreCase = true)) {
            processCorrectAnswer()
        } else {
            _spellingState.value = _spellingState.value.copy(isError = true)
            _comboState.value = ComboState(0, 1.0f)
            _answerState.value = 2
            triggerVibration(false)
            _wrongWords.add(currentQ.targetWord)
            recordCurrentResult(false) // 记录错误
            timerJob?.cancel()
        }
    }

    fun useHint() {
        val currentQ = _quizQuestions.value[_currentQuizIndex.value]
        val word = currentQ.targetWord.word
        val currentInput = _spellingState.value.input
        
        if (currentInput.length < word.length) {
            val nextChar = word[currentInput.length]
            val newInput = currentInput + nextChar
            _spellingState.value = _spellingState.value.copy(
                input = newInput,
                hintCount = _spellingState.value.hintCount + 1
            )
        }
    }

    private fun processCorrectAnswer() {
        val currentCombo = _comboState.value.count + 1
        val multiplier = 1.0f + (currentCombo * 0.1f)
        val points = (10 * multiplier).toInt()
        
        _quizScore.value += points
        _answerState.value = 1
        _comboState.value = ComboState(currentCombo, multiplier, true)
        
        triggerVibration(true)
        timerJob?.cancel()
        
        recordCurrentResult(true) // 记录正确
    }
    
    // 记录历史
    private fun recordCurrentResult(isCorrect: Boolean) {
        val list = _quizHistory.value.toMutableList()
        val currentQ = _quizQuestions.value.getOrNull(_currentQuizIndex.value)
        if (currentQ != null) {
            // 简单追加，不重复判断（假设流程严谨）
            // 实际上如果用户多次点击可能重复，加个判断
            if (list.none { it.question == currentQ }) {
                list.add(QuizResultItem(currentQ, isCorrect))
                _quizHistory.value = list
            }
        }
    }

    // ================= 每日打卡逻辑 =================

    private fun initDailyStats() {
        val prefs = getApplication<Application>().getSharedPreferences("user_stats", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        if (prefs.getString("record_date", "") != today) {
            prefs.edit().putString("record_date", today).putInt("today_count", 0).apply()
            _dailyCount.value = 0
        } else {
            _dailyCount.value = prefs.getInt("today_count", 0)
        }
        _streakDays.value = prefs.getInt("streak_days", 0)
        _dailyGoal.value = prefs.getInt("daily_goal", 20)
    }

    private fun incrementDailyProgress() {
        val prefs = getApplication<Application>().getSharedPreferences("user_stats", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val count = _dailyCount.value + 1
        _dailyCount.value = count
        prefs.edit().putInt("today_count", count).apply()

        if (count == _dailyGoal.value) {
            updateStreak(prefs, today)
        }
    }

    private fun updateStreak(prefs: android.content.SharedPreferences, today: String) {
        val last = prefs.getString("last_streak_date", "") ?: ""
        var streak = prefs.getInt("streak_days", 0)

        if (last == today) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val lastDate = if (last.isNotEmpty()) sdf.parse(last) else null
            val todayDate = sdf.parse(today)
            
            if (lastDate != null) {
                val diff = todayDate!!.time - lastDate.time
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                if (days == 1L) {
                    streak++
                } else if (days > 1L) {
                    streak = 1
                }
            } else {
                streak = 1
            }
        } catch (e: Exception) {
            streak = 1
        }
        
        prefs.edit()
            .putInt("streak_days", streak)
            .putString("last_streak_date", today)
            .apply()
        _streakDays.value = streak
    }

    // New download method supporting BookModel lookup
    fun downloadBook(bookId: String) {
         viewModelScope.launch {
            val book = _bookList.value.find { it.bookId == bookId } ?: return@launch
            
            _downloadingBookType.value = book.bookId
            _isLoading.value = true
            _statusMsg.value = "开始下载..."
            
            try {
                val success = repository.downloadBook(book) { progressMsg ->
                    _statusMsg.value = progressMsg
                }
                
                if (success) {
                    _statusMsg.value = "下载完成"
                    refreshBookshelf()
                } else {
                    _statusMsg.value = "下载失败"
                }
            } catch (e: Exception) {
                _statusMsg.value = "出错: ${e.message}"
            } finally {
                _isLoading.value = false
                _downloadingBookType.value = null
            }
        }
    }

    // 获取用户资料
    // 获取用户资料
    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                // 从 Firestore 获取真实用户资料
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()
                
                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        _userProfile.value = profile
                        Log.d("MainViewModel", "Loaded profile for $uid: ${profile.nickname}")
                    }
                } else {
                    // 如果是新用户，创建并通过 updateProfileField 保存初始状态
                    val newProfile = UserProfile(uid = uid, nickname = "User_${uid.take(4)}")
                     _userProfile.value = newProfile
                     // 保存到云端
                     com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(newProfile)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Fetch profile error", e)
                // 出错时使用本地默认
                val fallback = UserProfile(uid = uid, nickname = "User_${uid.take(4)}")
                _userProfile.value = fallback
            }
        }
    }

    // 更新用户名
    fun updateUsername(name: String) {
        updateProfileField("nickname", name)
    }
    
    // 验证手机号
    fun isValidPhoneNumber(phone: String): Boolean {
        // 简单验证: 11位数字
        return phone.length == 11 && phone.all { it.isDigit() }
    }
    
    // 发送验证码
    fun sendVerificationCode(phone: String, activity: android.app.Activity) {
        // 模拟发送
        viewModelScope.launch {
            _isLoading.value = true
            delay(1000)
            _isLoading.value = false
            _statusMsg.value = "验证码已发送"
        }
    }
    
    // 验证验证码
    fun verifyCode(code: String) {
        // 模拟验证
        viewModelScope.launch {
            _isLoading.value = true
            delay(1000)
            _isLoading.value = false
            // 登录成功逻辑...
        }
    }

    // ================= 遗漏方法的修复与恢复 =================

    // --- 查词对话框 ---
    fun closeSearchDialog() { _showSearchDialog.value = false }
    fun openSearchDialog() { _showSearchDialog.value = true }

    fun searchWord(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val res = repository.searchWordOnline(query)
            _searchResult.value = res
            _isSearching.value = false
            
            if (res != null) {
                _showSearchDialog.value = true
            } else {
                 _statusMsg.value = "未找到单词: $query"
            }
        }
    }

    /**
     * 开始学习模式
     * 加载指定书籍的单词，优先加载未学单词，全部学完则进入复习模式
     *
     * @param bookType 书籍类型ID
     */
    fun startLearning(bookType: String = _quizSelectedBookType.value) {
        if (bookType.isBlank()) return
        
        _learningBookType.value = bookType // Track current book for progress bar

        viewModelScope.launch {
            _isLoading.value = true
            _isLearningMode.value = true
            _currentWord.value = null // 清除旧数据，避免显示错误的单词
            
            try {

                // 1. 尝试获取未学过的单词
                val unlearnedWords = when (bookType) {
                    "favorite" -> repository.getFavoriteWords()
                    "mistake" -> repository.getMistakeWords()
                    else -> db.wordDao().getUnlearnedWordsList(bookType)
                }
                
                if (unlearnedWords.isNotEmpty()) {
                    // 有未学单词：从第一个未学单词开始
                     _currentWord.value = unlearnedWords[0]
                     learningList = unlearnedWords
                } else {
                    if (bookType == "favorite") {
                        Toast.makeText(getApplication(), "暂无收藏单词", Toast.LENGTH_SHORT).show()
                        quitLearning()
                    } else if (bookType == "mistake") {
                        Toast.makeText(getApplication(), "暂无错词", Toast.LENGTH_SHORT).show()
                        quitLearning()
                    } else {
                        // 全部学完：进入复习模式 (加载所有单词)
                        val allWords = db.wordDao().getAllWordsByBook(bookType)
                        if (allWords.isNotEmpty()) {
                            _currentWord.value = allWords[0]
                            learningList = allWords
                            Toast.makeText(getApplication(), "已学完所有单词，进入复习模式", Toast.LENGTH_SHORT).show()
                        } else {
                            // 书本为空
                            Toast.makeText(getApplication(), "词书为空，请先下载", Toast.LENGTH_SHORT).show()
                            quitLearning()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(getApplication(), "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                quitLearning()
            } finally {
                // 只有在没有退出学习模式的情况下才取消加载状态
                // 如果已经 quitLearning，isLearningMode 为 false，UI 会切换回书架，isLoading 无所谓
                // 但为了保险，还是还原 isLoading
                _isLoading.value = false
            }
        }
    }
    
    fun quitLearning() { 
        _isLearningMode.value = false 
        _currentWord.value = null
    }

    /**
     * 标记当前单词为"认识" (斩)
     * 保存进度并自动切换到下一个单词
     */
    fun markKnown() {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 500) return // 500ms 防抖
        lastActionTime = now

        val word = _currentWord.value ?: return
        viewModelScope.launch {
            repository.saveWordProgress(word.bookType, word.id)
            // 移动到下一个
            val index = learningList.indexOf(word)
            if (index < learningList.size - 1) {
                _currentWord.value = learningList[index + 1]
            } else {
                Toast.makeText(getApplication(), "本组单词已学完", Toast.LENGTH_SHORT).show()
                quitLearning()
            }
        }
    }
    
    /**
     * 标记当前单词为"不认识"
     * 标记为错题并保存到数据库，暂时跳过该单词
     */
    fun markUnknown() {
        val word = _currentWord.value ?: return
        
        viewModelScope.launch {
            // 标记为错题
            repository.markAsWrong(word.id)
            
            // 移动到下一个
            val index = learningList.indexOf(word)
            if (index < learningList.size - 1) {
                _currentWord.value = learningList[index + 1]
            } else {
                Toast.makeText(getApplication(), "本组单词已学完", Toast.LENGTH_SHORT).show()
                quitLearning()
            }
        }
    }
    
    fun unlearnWord(word: WordEntity) {
        viewModelScope.launch {
            repository.revertWordStatus(word.bookType, word.id)
            // 刷新列表?
            _reviewedWords.value = _reviewedWords.value.filter { it.id != word.id }
        }
    }

    /**
     * 切换当前单词的收藏状态
     */
    fun toggleFavorite() {
        val word = _currentWord.value ?: return
        val newStatus = !word.isFavorite
        
        viewModelScope.launch {
            // Update DB
            repository.toggleFavorite(word.id, newStatus)
            
            // Update current word state locally
            _currentWord.value = word.copy(isFavorite = newStatus)
            
            // Also need to update it in the learningList so if we come back to it (unlikely in linear flow but good practice)
            // Or if we are in "Review List" mode
            learningList = learningList.map { 
                if (it.id == word.id) it.copy(isFavorite = newStatus) else it 
            }

            // Show feedback
            val msg = if (newStatus) "已加入收藏" else "已取消收藏"
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 删除已下载的书籍
     * 移除本地数据库中的相关单词数据
     * @param type 书籍类型ID
     */
    fun deleteBook(type: String) {
        viewModelScope.launch { 
            repository.deleteBook(type)
            refreshBookshelf()
        }
    }
    
    // 兼容旧调用的别名

    // --- 用户与认证 ---
    fun clearStatusMsg() { _statusMsg.value = "" }
    
    /**
     * 用户注册
     * 使用邮箱和密码创建新账户
     *
     * @param email 邮箱地址
     * @param pass 密码
     */
    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                _statusMsg.value = "注册成功"
            } catch (e: Exception) {
                _statusMsg.value = "注册失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 用户登录
     * 使用邮箱和密码登录
     *
     * @param email 邮箱地址
     * @param pass 密码
     */
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _statusMsg.value = "登录成功"
            } catch (e: Exception) {
                _statusMsg.value = "登录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 退出登录
     * 清除本地进度缓存和用户状态
     */
    fun logout() {
        viewModelScope.launch {
            repository.resetLocalProgress()
            auth.signOut()
            _currentUser.value = null
            _userProfile.value = UserProfile()
        }
    }
    
    fun deleteAccount() {
        viewModelScope.launch {
            try {
                repository.deleteCurrentUserAndProgress()
                repository.resetLocalProgress() // 清除本地进度
                _currentUser.value = null
                _statusMsg.value = "账号已注销"
                _userProfile.value = UserProfile()
            } catch (e: Exception) {
                _statusMsg.value = "注销失败: ${e.message}"
            }
        }
    }

    /**
     * 上传用户头像
     * 将图片上传至Firebase Storage并更新用户资料
     *
     * @param uri 图片文件的本地URI
     */
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                 val user = currentUser.value ?: return@launch
                 val ref = FirebaseStorage.getInstance().reference.child("avatars/${user.uid}.jpg")
                 ref.putFile(uri).await()
                 val downloadUrl = ref.downloadUrl.await().toString()
                 
                 // 更新本地和云端
                 updateProfileField("avatarUrl", downloadUrl)
                 
                 _statusMsg.value = "上传成功"
            } catch (e: Exception) {
                _statusMsg.value = "上传失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateProfileField(field: String, value: String) {
        viewModelScope.launch {
             val p = _userProfile.value
             val newProfile = when(field) {
                 "nickname" -> p.copy(nickname = value)
                 "gender" -> p.copy(gender = value)
                 "birthDate" -> p.copy(birthDate = value)
                 "location" -> p.copy(location = value)
                 "school" -> p.copy(school = value)
                 "grade" -> p.copy(grade = value)
                 "avatarUrl" -> p.copy(avatarUrl = value)
                 else -> p
             }
             _userProfile.value = newProfile
             
             // 同步到服务器
             val user = currentUser.value
             if (user != null) {
                 try {
                     com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.uid)
                        .set(newProfile)
                 } catch (e: Exception) {
                     Log.e("MainViewModel", "Sync profile error", e)
                 }
             }
        }
    }

    fun setDailyGoal(goal: Int) { 
        _dailyGoal.value = goal 
        getApplication<Application>().getSharedPreferences("user_stats", Context.MODE_PRIVATE)
            .edit().putInt("daily_goal", goal).apply()
    }

    // --- 学习进度条 ---
    private val _learningBookType = MutableStateFlow("")
    val learningBookType = _learningBookType.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentBookProgress = _learningBookType.flatMapLatest { type ->
        if (type.isBlank()) flowOf(Pair(0, 0))
        else combine(
            db.wordDao().getBookLearnedCount(type),
            db.wordDao().getBookTotalCount(type)
        ) { learned, total ->
            Pair(learned, total)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Pair(0, 0))

    // --- 短信验证别名 ---
    private val _isCodeSent = MutableStateFlow(false)
    val isCodeSent = _isCodeSent.asStateFlow()

    fun sendSmsCode(phone: String, activity: android.app.Activity) {
        sendVerificationCode(phone, activity)
        _isCodeSent.value = true
    }
    fun verifySmsCode(code: String) = verifyCode(code)
    fun resetPhoneAuthState() { 
        _statusMsg.value = "" 
        _isCodeSent.value = false
    }

    // --- 复习列表 ---
    /**
     * 打开复习列表
     * 加载指定书籍中已学会的单词列表
     *
     * @param bookType 书籍类型ID
     */
    fun openReviewList(bookType: String) { 
        viewModelScope.launch {
            try {
                _isReviewingMode.value = true
                // 加载指定书籍的已学单词
                if (bookType.isNotBlank()) {
                     val list = db.wordDao().getAllWordsByBook(bookType).filter { it.isLearned }
                     _reviewedWords.value = list
                } else {
                     _reviewedWords.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMsg.value = "加载复习列表失败: ${e.localizedMessage}"
                _isReviewingMode.value = false
            }
        }
    }
    
    fun closeReviewList() { _isReviewingMode.value = false }

}