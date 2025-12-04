package com.example.rememberworlds

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
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rememberworlds.data.db.AppDatabase
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.network.SearchResponseItem
import com.example.rememberworlds.data.repository.WordRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.rememberworlds.data.model.UserProfile
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- 数据模型 --- 
/**
 * 书籍模型数据类
 * 表示应用程序中的一个单词书
 *
 * @param type 书籍类型标识符
 * @param name 书籍名称
 * @param isDownloaded 是否已下载
 */
data class BookModel(
    val type: String,
    val name: String,
    val isDownloaded: Boolean = false
)

/**
 * 测验题目数据类
 * 表示一个测验题目
 *
 * @param targetWord 目标单词
 * @param options 选项列表
 * @param type 测验类型
 */
data class Question(
    val targetWord: WordEntity,
    val options: List<String>,
    val type: QuizType
)

/**
 * 测验类型枚举
 * 定义支持的测验类型
 */
enum class QuizType {
    /** 英转中 - 从英文单词选择中文释义 */
    EN_TO_CN,
    /** 中转英 - 从中文释义选择英文单词 */
    CN_TO_EN,
    /** 听音选义 - 听音频选择中文释义 */
    AUDIO_TO_CN,
    /** 拼写题 - 根据中文释义拼写英文单词 */
    SPELLING     
}

/**
 * 连击状态数据类
 * 表示用户当前的连击状态
 *
 * @param count 连击次数
 * @param multiplier 得分倍数
 * @param showAnimation 是否显示动画
 */
data class ComboState(
    val count: Int = 0,
    val multiplier: Float = 1.0f,
    val showAnimation: Boolean = false
)

/**
 * 拼写题状态数据类
 * 表示拼写题的当前状态
 *
 * @param input 用户输入的文本
 * @param hintText 提示文本，如 "a _ _ l _"
 * @param isError 是否输入错误
 * @param hintCount 已使用的提示次数
 * @param correctAnswer 正确答案，用于错误时显示
 */
data class SpellingState(
    val input: String = "",
    val hintText: String = "", 
    val isError: Boolean = false,
    val hintCount: Int = 0,
    val correctAnswer: String = ""
)

// --- ViewModel --- 
/**
 * 应用程序主视图模型
 * 管理应用程序的所有业务逻辑和状态
 * 继承自AndroidViewModel，持有Application上下文
 * 实现TextToSpeech.OnInitListener接口，处理TTS初始化
 *
 * @param application 应用程序上下文
 */
class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    // 数据库和仓库实例
    private val db = AppDatabase.getDatabase(application)
    private val repository = WordRepository(db.wordDao(), application)

    // --- 1. 状态变量 --- 
    /** 深色主题状态 */
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()
    
    /** 网络连接状态 */
    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()
    
    /** 文本转语音实例 */
    private var tts: TextToSpeech? = null
    /** TTS是否准备就绪 */
    private var isTtsReady = false

    /** 当前下载的书籍类型 */
    private val _downloadingBookType = MutableStateFlow<String?>(null)
    val downloadingBookType = _downloadingBookType.asStateFlow()
    
    /** 当前登录用户 */
    private val _currentUser = MutableStateFlow<FirebaseUser?>(FirebaseAuth.getInstance().currentUser)
    val currentUser = _currentUser.asStateFlow()
    
    /** Firebase Auth实例 */
    private val auth = FirebaseAuth.getInstance()
    
    /** 用户详细资料 */
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()
    
    /** 加载状态 */
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    /** 状态消息 */
    private val _statusMsg = MutableStateFlow("")
    val statusMsg = _statusMsg.asStateFlow()
    
    /** 书架书籍列表 */
    private val _bookList = MutableStateFlow<List<BookModel>>(emptyList())
    val bookList = _bookList.asStateFlow()
    
    /** 是否处于学习模式 */
    private val _isLearningMode = MutableStateFlow(false)
    val isLearningMode = _isLearningMode.asStateFlow()
    
    /** 当前学习的单词 */
    private val _currentWord = MutableStateFlow<WordEntity?>(null)
    val currentWord = _currentWord.asStateFlow()
    
    /** 是否处于复习模式 */
    private val _isReviewingMode = MutableStateFlow(false)
    val isReviewingMode = _isReviewingMode.asStateFlow()
    
    /** 已复习的单词列表 */
    private val _reviewedWords = MutableStateFlow<List<WordEntity>>(emptyList())
    val reviewedWords = _reviewedWords.asStateFlow()
    
    /** 查词结果 */
    private val _searchResult = MutableStateFlow<SearchResponseItem?>(null)
    val searchResult = _searchResult.asStateFlow()
    
    /** 是否正在查词 */
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    
    /** 是否显示查词对话框 */
    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog = _showSearchDialog.asStateFlow()
    
    /** 测验题目列表 */
    private val _quizQuestions = MutableStateFlow<List<Question>>(emptyList())
    val quizQuestions = _quizQuestions.asStateFlow()
    
    /** 当前测验题目索引 */
    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex = _currentQuizIndex.asStateFlow()
    
    /** 测验得分 */
    private val _quizScore = MutableStateFlow(0)
    val quizScore = _quizScore.asStateFlow()
    
    /** 测验是否结束 */
    private val _isQuizFinished = MutableStateFlow(false)
    val isQuizFinished = _isQuizFinished.asStateFlow()
    
    /** 答案状态：0未回答, 1正确, 2错误 */
    private val _answerState = MutableStateFlow(0)
    val answerState = _answerState.asStateFlow()
    
    /** 测验步骤：1选择题库, 2选择模式 */
    private val _quizStep = MutableStateFlow(1)
    val quizStep = _quizStep.asStateFlow()
    
    /** 选中的测验书籍类型 */
    private val _quizSelectedBookType = MutableStateFlow("")
    val quizSelectedBookType = _quizSelectedBookType.asStateFlow()
    
    /** 用户选择的选项 */
    private val _userSelectedOption = MutableStateFlow("")
    val userSelectedOption = _userSelectedOption.asStateFlow()

    /** 连击状态 */
    private val _comboState = MutableStateFlow(ComboState())
    val comboState = _comboState.asStateFlow()

    /** 拼写题状态 */
    private val _spellingState = MutableStateFlow(SpellingState())
    val spellingState = _spellingState.asStateFlow()

    /** 剩余时间 */
    private val _timeLeft = MutableStateFlow(15.0f) 
    val timeLeft = _timeLeft.asStateFlow()
    
    /** 总时间 */
    private val _totalTime = MutableStateFlow(15.0f) 

    /** 计时器任务 */
    private var timerJob: Job? = null
    /** 本次测验的错题列表 */
    private val _wrongWords = mutableListOf<WordEntity>() 

    /** 统计数据 */
    /** 已学单词数量 */
    val learnedCount: Flow<Int> = db.wordDao().getLearnedCount()
    /** 总单词数量 */
    val totalCount: Flow<Int> = db.wordDao().getTotalCount()
    /** 连续打卡天数 */
    private val _streakDays = MutableStateFlow(0)
    val streakDays = _streakDays.asStateFlow()
    /** 今日学习单词数 */
    private val _dailyCount = MutableStateFlow(0)
    val dailyCount = _dailyCount.asStateFlow()

    /** 每日目标 */
    private val _dailyGoal = MutableStateFlow(20)
    val dailyGoal = _dailyGoal.asStateFlow()

    /** 学习列表 */
    private var learningList: List<WordEntity> = emptyList()
    /** 媒体播放器 */
    private var mediaPlayer: MediaPlayer? = null

    /**
     * 初始化方法
     * 初始化主题、书架、每日统计、网络监听和TTS
     */
    init {
        initTheme()
        refreshBookshelf()
        initDailyStats()
        initNetworkMonitor()
        tts = TextToSpeech(application, this)
        
        // 监听 currentUser 变化，登录成功后拉取详细资料
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    fetchUserProfile(user.uid)
                } else {
                    _userProfile.value = UserProfile() // 重置
                }
            }
        }
    }

    // --- 主题逻辑 --- 
    /**
     * 初始化主题
     * 从SharedPreferences读取主题设置
     */
    private fun initTheme() {
        val prefs = getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE)
        _isDarkTheme.value = prefs.getBoolean("is_dark_theme", false)
    }

    /**
     * 切换主题
     *
     * @param isDark 是否为深色主题
     */
    fun toggleTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE)
            .edit().putBoolean("is_dark_theme", isDark).apply()
    }

    /**
     * 初始化网络监听
     * 监听设备网络连接状态变化
     */
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
     * 优先使用网络音频，失败则使用TTS
     *
     * @param url 音频URL
     * @param wordText 单词文本，用于TTS
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

    /**
     * 播放TTS
     *
     * @param text 要朗读的文本
     */
    private fun playTTS(text: String?) {
        if (isTtsReady && !text.isNullOrBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    /**
     * TTS初始化回调
     *
     * @param status 初始化状态
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    /**
     * 组件销毁时调用
     * 释放资源
     */
    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        tts?.stop()
        tts?.shutdown()
    }

    /**
     * 触发振动
     * 正确时短振动，错误时长振动
     *
     * @param isCorrect 答案是否正确
     */
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
     * 选择测验书籍
     *
     * @param bt 书籍类型
     */
    fun selectQuizBook(bt: String) {
        _quizSelectedBookType.value = bt
        _quizStep.value = 2
    }

    /**
     * 返回书架选择
     */
    fun backToBookSelection() {
        _quizStep.value = 1
        _quizSelectedBookType.value = ""
    }

    /**
     * 开始测验
     * 生成测验题目，重置状态
     *
     * @param mode 测验模式
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
                    4 -> QuizType.SPELLING // 模式4为拼写
                    1 -> QuizType.EN_TO_CN
                    2 -> QuizType.CN_TO_EN
                    3 -> QuizType.AUDIO_TO_CN
                    else -> QuizType.values().random()
                }
                
                // 如果是拼写题，options 留空
                val options = if (qType == QuizType.SPELLING) emptyList() else {
                    val distractors = allWords.filter { it.id != target.id }.shuffled().take(3)
                    if (qType == QuizType.CN_TO_EN) {
                        (distractors + target).map { it.word }.shuffled()
                    } else {
                        (distractors + target).map { it.cn }.shuffled()
                    }
                }
                Question(target, options, qType)
            }

            // 重置测验状态
            _quizQuestions.value = questions
            _currentQuizIndex.value = 0
            _quizScore.value = 0
            _answerState.value = 0
            _userSelectedOption.value = ""
            _comboState.value = ComboState() // 重置连击
            _wrongWords.clear()
            _isQuizFinished.value = false
            
            startTimer() // 开始倒计时
            initSpellingState(questions[0]) // 初始化拼写

            // 如果是听音选义题，自动播放音频
            if (questions.isNotEmpty() && questions[0].type == QuizType.AUDIO_TO_CN) {
                delay(500)
                playAudio(questions[0].targetWord.audio, questions[0].targetWord.word)
            }
        }
    }

    /**
     * 回答选择题
     *
     * @param opt 用户选择的选项
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

        // 处理答案
        if (correct) {
            processCorrectAnswer()
        } else {
            _answerState.value = 2
            _comboState.value = ComboState(0, 1.0f) // 连击断裂
            triggerVibration(false)
            _wrongWords.add(_quizQuestions.value[_currentQuizIndex.value].targetWord)
        }
        timerJob?.cancel() // 停止计时
    }

    /**
     * 进入下一题
     */
    fun nextQuestion() {
        val index = _currentQuizIndex.value
        val qs = _quizQuestions.value

        if (index < qs.size - 1) {
            // 还有下一题
            _currentQuizIndex.value += 1
            _answerState.value = 0
            _userSelectedOption.value = ""
            
            val nextQ = qs[_currentQuizIndex.value]
            if (nextQ.type == QuizType.AUDIO_TO_CN) {
                playAudio(nextQ.targetWord.audio, nextQ.targetWord.word)
            }
            
            // 重置拼写和计时
            startTimer()
            initSpellingState(nextQ)
        } else {
            // 测验结束
            _isQuizFinished.value = true
        }
    }

    /**
     * 退出测验
     */
    fun quitQuiz() {
        _quizQuestions.value = emptyList()
        mediaPlayer?.release()
        timerJob?.cancel() // 取消计时
    }

    /**
     * 开始倒计时
     */
    private fun startTimer() {
        timerJob?.cancel()
        _timeLeft.value = 15.0f // 每题15秒
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0 && isActive) {
                delay(100) // 0.1秒刷新一次
                _timeLeft.value -= 0.1f
            }
            if (_timeLeft.value <= 0) {
                handleTimeout()
            }
        }
    }

    /**
     * 处理超时
     * 超时视为错误
     */
    private fun handleTimeout() {
        _answerState.value = 2 // 视为错误
        _comboState.value = ComboState(0, 1.0f) // 连击断裂
        // 记录错题
        val currentQ = _quizQuestions.value.getOrNull(_currentQuizIndex.value)
        currentQ?.let { _wrongWords.add(it.targetWord) }
    }

    /**
     * 初始化拼写题状态
     *
     * @param q 题目
     */
    private fun initSpellingState(q: Question) {
        if (q.type == QuizType.SPELLING) {
            // 初始化提示，全部显示为 _
            val length = q.targetWord.word.length
            val mask = "_ ".repeat(length).trim()
            _spellingState.value = SpellingState(
                input = "", 
                hintText = mask,
                correctAnswer = q.targetWord.word // 设置正确答案
            )
        }
    }

    /**
     * 更新拼写输入
     *
     * @param input 用户输入
     */
    fun updateSpellingInput(input: String) {
        _spellingState.value = _spellingState.value.copy(input = input, isError = false)
    }

    /**
     * 提交拼写答案
     */
    fun submitSpelling() {
        val currentQ = _quizQuestions.value[_currentQuizIndex.value]
        val input = _spellingState.value.input.trim()
        val target = currentQ.targetWord.word.trim()

        if (input.equals(target, ignoreCase = true)) {
            // 答对
            processCorrectAnswer()
        } else {
            // 答错
            _spellingState.value = _spellingState.value.copy(isError = true)
            _comboState.value = ComboState(0, 1.0f) // 连击清零
            _answerState.value = 2 // 设置为错误状态
            triggerVibration(false)
            _wrongWords.add(currentQ.targetWord) // 记录错题
            timerJob?.cancel() // 停止计时
        }
    }

    /**
     * 使用提示
     */
    fun useHint() {
        val currentQ = _quizQuestions.value[_currentQuizIndex.value]
        val word = currentQ.targetWord.word
        val currentInput = _spellingState.value.input
        
        // 简单的提示：自动填充下一个正确的字母
        if (currentInput.length < word.length) {
            val nextChar = word[currentInput.length]
            val newInput = currentInput + nextChar
            _spellingState.value = _spellingState.value.copy(
                input = newInput,
                hintCount = _spellingState.value.hintCount + 1
            )
        }
    }

    /**
     * 处理正确答案
     */
    private fun processCorrectAnswer() {
        val currentCombo = _comboState.value.count + 1
        // 连击加分公式：基础分10 * (1 + 连击数 * 0.1)
        val multiplier = 1.0f + (currentCombo * 0.1f)
        val points = (10 * multiplier).toInt()
        
        _quizScore.value += points
        _answerState.value = 1
        _comboState.value = ComboState(currentCombo, multiplier, true)
        
        triggerVibration(true)
        timerJob?.cancel()
    }

    // ================= 每日打卡逻辑 =================

    /**
     * 初始化每日统计
     * 从SharedPreferences读取每日统计数据
     */
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

        // 读取目标
        _dailyGoal.value = prefs.getInt("daily_goal", 20)
    }

    /**
     * 增加每日学习进度
     */
    private fun incrementDailyProgress() {
        val prefs = getApplication<Application>().getSharedPreferences("user_stats", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val count = _dailyCount.value + 1
        _dailyCount.value = count
        prefs.edit().putInt("today_count", count).apply()

        // 使用动态目标
        if (count == _dailyGoal.value) {
            updateStreak(prefs, today)
        }
    }

    /**
     * 更新连续打卡天数
     *
     * @param prefs SharedPreferences实例
     * @param today 今天日期
     */
    private fun updateStreak(prefs: android.content.SharedPreferences, today: String) {
        val last = prefs.getString("last_streak_date", "") ?: ""
        var streak = prefs.getInt("streak_days", 0)

        if (last == today) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            if (last.isNotEmpty()) {
                val d1 = sdf.parse(last)
                val d2 = sdf.parse(today)
                if (d1 != null && d2 != null) {
                    val diff = TimeUnit.DAYS.convert(d2.time - d1.time, TimeUnit.MILLISECONDS)
                    if (diff == 1L) streak++ else streak = 1
                }
            } else {
                streak = 1
            }
        } catch (e: Exception) {
            streak = 1
        }

        prefs.edit().putString("last_streak_date", today).putInt("streak_days", streak).apply()
        _streakDays.value = streak
        Toast.makeText(getApplication(), "🎉 打卡成功！坚持 $streak 天！", Toast.LENGTH_LONG).show()
    }

    /**
     * 设置每日目标
     *
     * @param newGoal 新的每日目标
     */
    fun setDailyGoal(newGoal: Int) {
        _dailyGoal.value = newGoal
        getApplication<Application>().getSharedPreferences("user_stats", Context.MODE_PRIVATE)
            .edit().putInt("daily_goal", newGoal).apply()
    }

    // ================= 书架与学习逻辑 =================

    /**
     * 刷新书架
     * 从SharedPreferences读取书籍下载状态
     */
    private fun refreshBookshelf() {
        val prefs = getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE)
        _bookList.value = listOf(
            BookModel("cet4", "四级词汇 (CET4)"),
            BookModel("cet6", "六级词汇 (CET6)"),
            BookModel("kaoyan", "考研核心词汇"),
            BookModel("tem8", "专业八级 (TEM8)")
        ).map {
            it.copy(isDownloaded = prefs.getInt("version_${it.type}", 0) > 0)
        }
    }

    /**
     * 下载书籍
     *
     * @param book 要下载的书籍
     */
    fun downloadBook(book: BookModel) {
        if (!_isOnline.value) {
            Toast.makeText(getApplication(), "无网络连接", Toast.LENGTH_SHORT).show()
            return
        }
        if (_downloadingBookType.value != null) return

        viewModelScope.launch {
            _downloadingBookType.value = book.type
            _statusMsg.value = "下载中..."
            try {
                if (repository.checkUpdate(book.type)) {
                    Toast.makeText(getApplication(), "成功", Toast.LENGTH_SHORT).show()
                }
                refreshBookshelf()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "失败", Toast.LENGTH_SHORT).show()
            } finally {
                _downloadingBookType.value = null
                _statusMsg.value = "" // 操作结束后，清空状态
            }
        }
    }

    /**
     * 删除书籍
     *
     * @param book 要删除的书籍
     */
    fun deleteBook(book: BookModel) {
        viewModelScope.launch {
            repository.deleteBook(book.type)
            getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE)
                .edit().remove("version_${book.type}").apply()
            refreshBookshelf()
        }
    }

    /**
     * 删除账户
     * 清除本地数据和云端数据
     */
    fun deleteAccount() {
        _isLoading.value = true
        _statusMsg.value = "正在注销..."
        viewModelScope.launch {
            try {
                // 1. 删除云端用户和进度
                repository.deleteCurrentUserAndProgress()

                // 2. 清除本地数据和状态
                repository.clearAllData()
                getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE).edit().clear().apply()
                getApplication<Application>().getSharedPreferences("user_stats", Context.MODE_PRIVATE).edit().clear().apply()

                // 3. 更新 UI 状态
                _bookList.value = emptyList()
                _isLearningMode.value = false
                _currentWord.value = null
                _streakDays.value = 0
                _dailyCount.value = 0
                auth.signOut() // Firebase 退出
                _currentUser.value = null
                _statusMsg.value = "已注销"
                refreshBookshelf()

            } catch (e: Exception) {
                // 捕获错误，例如网络错误或Session过期
                _statusMsg.value = translateError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 开始学习
     *
     * @param bookType 书籍类型
     */
    fun startLearning(bookType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            if (_isOnline.value) repository.syncUserProgress(bookType)
            val unlearned = db.wordDao().getUnlearnedWordsList(bookType)
            if (unlearned.isNotEmpty()) {
                learningList = unlearned.shuffled()
                nextWord()
                _isLearningMode.value = true
            } else {
                Toast.makeText(getApplication(), "已背完", Toast.LENGTH_SHORT).show()
            }
            _isLoading.value = false
        }
    }

    /**
     * 标记为已知
     */
    fun markKnown() {
        val w = _currentWord.value ?: return
        viewModelScope.launch {
            db.wordDao().updateWord(w.copy(isLearned = true))
            if (_isOnline.value) repository.saveWordProgress(w.bookType, w.id)
            incrementDailyProgress()
            nextWord()
        }
    }

    /**
     * 标记为未知
     */
    fun markUnknown() { nextWord() }

    /**
     * 进入下一个单词
     */
    private fun nextWord() {
        val list = learningList.toMutableList()
        val cur = _currentWord.value
        if (cur != null) list.remove(cur)
        if (list.isNotEmpty()) {
            learningList = list
            _currentWord.value = list.first()
        } else {
            _currentWord.value = null
        }
    }

    /**
     * 退出学习模式
     */
    fun quitLearning() {
        _isLearningMode.value = false
        _currentWord.value = null
        mediaPlayer?.release()
    }

    /**
     * 打开复习列表
     *
     * @param bt 书籍类型
     */
    fun openReviewList(bt: String) {
        viewModelScope.launch {
            db.wordDao().getLearnedWords(bt).collect { _reviewedWords.value = it }
        }
        _isReviewingMode.value = true
    }

    /**
     * 关闭复习列表
     */
    fun closeReviewList() {
        _isReviewingMode.value = false
    }

    /**
     * 标记单词为未学
     *
     * @param w 要标记的单词
     */
    fun unlearnWord(w: WordEntity) {
        viewModelScope.launch {
            repository.revertWordStatus(w.bookType, w.id)
        }
    }

    // --- 查词 ---
    /**
     * 搜索单词
     *
     * @param q 要搜索的单词
     */
    fun searchWord(q: String) {
        if (q.isBlank()) return
        if (!_isOnline.value) { Toast.makeText(getApplication(), "需要网络", Toast.LENGTH_SHORT).show(); return }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val res = repository.searchWordOnline(q.trim())
                if (res != null) { _searchResult.value = res; _showSearchDialog.value = true } else { Toast.makeText(getApplication(), "未找到", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
            } finally { _isSearching.value = false }
        }
    }

    /**
     * 关闭查词对话框
     */
    fun closeSearchDialog() {
        _showSearchDialog.value = false
        _searchResult.value = null
    }

    // --- 用户系统 ---
    /**
     * 登录
     *
     * @param u 用户名或邮箱
     * @param p 密码
     */
    fun login(u: String, p: String) {
        if (!_isOnline.value) {
            _statusMsg.value = "当前无网络连接"
            return
        }
        
        // 处理用户名：如果用户没输 @，自动加上假后缀
        val email = if (u.contains("@")) u else "$u@rememberworlds.com"
        
        _isLoading.value = true
        _statusMsg.value = "正在连接服务器..."

        auth.signInWithEmailAndPassword(email, p)
            .addOnSuccessListener { result ->
                _currentUser.value = result.user
                _isLoading.value = false
                _statusMsg.value = "登录成功"
                refreshBookshelf()
                initDailyStats()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                val errorMsg = when {
                    e.message?.contains("network") == true -> "网络连接失败，请确保开启了VPN"
                    e.message?.contains("password") == true -> "密码错误"
                    e.message?.contains("no user") == true -> "账号不存在"
                    else -> "登录失败: ${e.message}"
                }
                _statusMsg.value = errorMsg
                Log.e("AuthError", "Login failed", e)
            }
    }

    /**
     * 注册
     *
     * @param u 用户名或邮箱
     * @param p 密码
     */
    fun register(u: String, p: String) {
        if (!_isOnline.value) {
            _statusMsg.value = "当前无网络连接"
            return
        }

        // 处理注册时的邮箱
        val email = if (u.contains("@")) u else "$u@rememberworlds.com"

        _isLoading.value = true
        _statusMsg.value = "正在注册..."

        auth.createUserWithEmailAndPassword(email, p)
            .addOnSuccessListener { result ->
                _currentUser.value = result.user
                _isLoading.value = false
                _statusMsg.value = "注册成功"
                refreshBookshelf()
                initDailyStats()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                val errorMsg = when {
                    e.message?.contains("network") == true -> "网络连接失败，请确保开启了VPN"
                    e.message?.contains("email") == true -> "账号格式错误或已被占用"
                    e.message?.contains("password") == true -> "密码长度需大于6位"
                    else -> "注册失败: ${e.message}"
                }
                _statusMsg.value = errorMsg
                Log.e("AuthError", "Register failed", e)
            }
    }

    /**
     * 退出登录
     */
    fun logout() {
        viewModelScope.launch {
            repository.clearAllData()
            // 清除 SharedPreferences
            getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE).edit().clear().apply()
            getApplication<Application>().getSharedPreferences("user_stats", Context.MODE_PRIVATE).edit().clear().apply()
            
            _bookList.value = emptyList()
            _isLearningMode.value = false
            _currentWord.value = null
            _streakDays.value = 0
            _dailyCount.value = 0
            
            auth.signOut() // Firebase 退出
            _currentUser.value = null
            _statusMsg.value = "已安全退出"
            refreshBookshelf()
        }
    }

    /**
     * 从 Firestore 拉取用户资料
     *
     * @param uid 用户ID
     */
    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val snapshot = db.collection("user_profiles").document(uid).get().await()
                
                if (snapshot.exists()) {
                    // 将 Firestore 数据转为 UserProfile 对象
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        _userProfile.value = profile
                    }
                } else {
                    // 如果还没有资料，初始化一份
                    val newProfile = UserProfile(uid = uid, nickname = _currentUser.value?.email?.split("@")?.get(0) ?: "用户")
                    _userProfile.value = newProfile
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 更新用户资料字段
     *
     * @param field 要更新的字段名
     * @param value 新的值
     */
    fun updateProfileField(field: String, value: String) {
        val uid = _currentUser.value?.uid ?: return
        
        // 1. 更新本地状态
        val current = _userProfile.value
        val updated = when(field) {
            "nickname" -> current.copy(nickname = value)
            "gender" -> current.copy(gender = value)
            "birthDate" -> current.copy(birthDate = value)
            "location" -> current.copy(location = value)
            "school" -> current.copy(school = value)
            "grade" -> current.copy(grade = value)
            "avatarUrl" -> current.copy(avatarUrl = value)
            else -> current
        }
        _userProfile.value = updated

        // 2. 同步到 Firestore
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("user_profiles").document(uid).set(updated).await()
                _statusMsg.value = "资料已更新"
                delay(1000)
                _statusMsg.value = "" // 清除提示
            } catch (e: Exception) {
                _statusMsg.value = "更新失败: ${e.message}"
            }
        }
    }
    
    /**
     * 上传头像
     *
     * @param uri 头像文件URI
     */
    fun uploadAvatar(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        
        _isLoading.value = true
        _statusMsg.value = "正在上传头像..."

        // 1. 获取 Firebase Storage 引用
        // 路径：avatars/{用户ID}.jpg
        val storageRef = FirebaseStorage.getInstance().reference
        val avatarRef = storageRef.child("avatars/$uid.jpg")

        // 2. 上传文件
        avatarRef.putFile(uri)
            .addOnSuccessListener {
                // 3. 上传成功后，获取下载链接
                avatarRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // 4. 将下载链接更新到 Firestore 用户资料中
                    updateProfileField("avatarUrl", downloadUri.toString())
                    _isLoading.value = false
                    _statusMsg.value = "头像更新成功"
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _statusMsg.value = "上传失败: ${e.message}"
                e.printStackTrace()
            }
    }
    
    /**
     * 翻译错误信息
     *
     * @param e 异常
     * @return 翻译后的错误信息
     */
    private fun translateError(e: Throwable) = e.message ?: "Error"
    
    /**
     * 清除状态消息
     */
    fun clearStatusMsg() {
        _statusMsg.value = ""
    }
}