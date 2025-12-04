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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- 数据模型 ---
data class BookModel(
    val type: String,
    val name: String,
    val isDownloaded: Boolean = false
)

data class Question(
    val targetWord: WordEntity,
    val options: List<String>,
    val type: QuizType
)

enum class QuizType {
    EN_TO_CN,
    CN_TO_EN,
    AUDIO_TO_CN
}

// --- ViewModel ---
class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val repository = WordRepository(db.wordDao(), application)

    // --- 1. 状态变量 ---
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()
    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _downloadingBookType = MutableStateFlow<String?>(null)
    val downloadingBookType = _downloadingBookType.asStateFlow()
    // [修改] 用户类型变为 FirebaseUser
    private val _currentUser = MutableStateFlow<FirebaseUser?>(FirebaseAuth.getInstance().currentUser)
    val currentUser = _currentUser.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance() // [新增] Auth 实例
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _statusMsg = MutableStateFlow("")
    val statusMsg = _statusMsg.asStateFlow()
    private val _bookList = MutableStateFlow<List<BookModel>>(emptyList())
    val bookList = _bookList.asStateFlow()
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
    private val _quizQuestions = MutableStateFlow<List<Question>>(emptyList())
    val quizQuestions = _quizQuestions.asStateFlow()
    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex = _currentQuizIndex.asStateFlow()
    private val _quizScore = MutableStateFlow(0)
    val quizScore = _quizScore.asStateFlow()
    private val _isQuizFinished = MutableStateFlow(false)
    val isQuizFinished = _isQuizFinished.asStateFlow()
    private val _answerState = MutableStateFlow(0)
    val answerState = _answerState.asStateFlow()
    private val _quizStep = MutableStateFlow(1)
    val quizStep = _quizStep.asStateFlow()
    private val _quizSelectedBookType = MutableStateFlow("")
    val quizSelectedBookType = _quizSelectedBookType.asStateFlow()
    private val _userSelectedOption = MutableStateFlow("")
    val userSelectedOption = _userSelectedOption.asStateFlow()

    // 统计数据
    val learnedCount: Flow<Int> = db.wordDao().getLearnedCount()
    val totalCount: Flow<Int> = db.wordDao().getTotalCount()
    private val _streakDays = MutableStateFlow(0)
    val streakDays = _streakDays.asStateFlow()
    private val _dailyCount = MutableStateFlow(0)
    val dailyCount = _dailyCount.asStateFlow()

    // 【关键变量】每日目标
    private val _dailyGoal = MutableStateFlow(20)
    val dailyGoal = _dailyGoal.asStateFlow()

    private var learningList: List<WordEntity> = emptyList()
    private var mediaPlayer: MediaPlayer? = null

    init {
        initTheme()
        refreshBookshelf()
        initDailyStats()
        initNetworkMonitor()
        tts = TextToSpeech(application, this)
    }

    // --- 主题逻辑 ---
    private fun initTheme() {
        val prefs = getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE)
        _isDarkTheme.value = prefs.getBoolean("is_dark_theme", false)
    }

    fun toggleTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE)
            .edit().putBoolean("is_dark_theme", isDark).apply()
    }

    // --- TTS 初始化 ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    // --- 网络监听 ---
    private fun initNetworkMonitor() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _isOnline.value = true }
            override fun onLost(network: Network) { _isOnline.value = false }
        })
    }

    // --- 播放音频 ---
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

    override fun onCleared() {
        super.onCleared()
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

    // ================= 核心业务逻辑 =================

    fun selectQuizBook(bt: String) {
        _quizSelectedBookType.value = bt
        _quizStep.value = 2
    }

    fun backToBookSelection() {
        _quizStep.value = 1
        _quizSelectedBookType.value = ""
    }

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
            val questions = quizWords.map { target ->
                val qType = when(mode) {
                    1 -> QuizType.EN_TO_CN
                    2 -> QuizType.CN_TO_EN
                    3 -> QuizType.AUDIO_TO_CN
                    else -> QuizType.values().random()
                }

                val distractors = allWords.filter { it.id != target.id }.shuffled().take(3)

                val options = if (qType == QuizType.CN_TO_EN) {
                    (distractors + target).map { it.word }.shuffled()
                } else {
                    (distractors + target).map { it.cn }.shuffled()
                }

                Question(target, options, qType)
            }

            _quizQuestions.value = questions
            _currentQuizIndex.value = 0
            _quizScore.value = 0
            _answerState.value = 0
            _userSelectedOption.value = ""
            _isQuizFinished.value = false

            if (questions.isNotEmpty() && questions[0].type == QuizType.AUDIO_TO_CN) {
                delay(500)
                playAudio(questions[0].targetWord.audio, questions[0].targetWord.word)
            }
        }
    }

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
            _quizScore.value += 10
            _answerState.value = 1
            triggerVibration(true)
        } else {
            _answerState.value = 2
            triggerVibration(false)
        }
    }

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
        } else {
            _isQuizFinished.value = true
        }
    }

    fun quitQuiz() {
        _quizQuestions.value = emptyList()
        mediaPlayer?.release()
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

        // 读取目标
        _dailyGoal.value = prefs.getInt("daily_goal", 20)
    }

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

    fun setDailyGoal(newGoal: Int) {
        _dailyGoal.value = newGoal
        getApplication<Application>().getSharedPreferences("user_stats", Context.MODE_PRIVATE)
            .edit().putInt("daily_goal", newGoal).apply()
    }

    // ================= 书架与学习逻辑 =================

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
            }
        }
    }

    fun deleteBook(book: BookModel) {
        viewModelScope.launch {
            repository.deleteBook(book.type)
            getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE)
                .edit().remove("version_${book.type}").apply()
            refreshBookshelf()
        }
    }

    // 【已修复/优化】: 使用 Coroutine 替代 RxJava，并调用 Repository 中新的完整删除逻辑
    fun deleteAccount() {
        _isLoading.value = true
        _statusMsg.value = "正在注销..."
        viewModelScope.launch {
            try {
                // 1. 删除云端用户和进度 (调用新的 suspend 函数)
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
                auth.signOut() // [修改] Firebase 退出
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

    fun markKnown() {
        val w = _currentWord.value ?: return
        viewModelScope.launch {
            db.wordDao().updateWord(w.copy(isLearned = true))
            if (_isOnline.value) repository.saveWordProgress(w.bookType, w.id)
            incrementDailyProgress()
            nextWord()
        }
    }

    fun markUnknown() { nextWord() }

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

    fun quitLearning() {
        _isLearningMode.value = false
        _currentWord.value = null
        mediaPlayer?.release()
    }

    fun openReviewList(bt: String) {
        viewModelScope.launch {
            db.wordDao().getLearnedWords(bt).collect { _reviewedWords.value = it }
        }
        _isReviewingMode.value = true
    }

    fun closeReviewList() {
        _isReviewingMode.value = false
    }

    fun unlearnWord(w: WordEntity) {
        viewModelScope.launch {
            repository.revertWordStatus(w.bookType, w.id)
        }
    }

    // --- 查词 ---
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

    fun closeSearchDialog() {
        _showSearchDialog.value = false
        _searchResult.value = null
    }

    // --- 用户系统 ---
    // [修改] 登录逻辑
    fun login(u: String, p: String) {
        if (!_isOnline.value) {
            Toast.makeText(getApplication(), "无网络", Toast.LENGTH_SHORT).show()
            return
        }
        _isLoading.value = true
        // 这里的 u 必须是 email。LeanCloud 可以是用户名，Firebase 默认是 Email。
        // 如果你之前的用户名不是 Email，可能需要调整 UI 提示让用户输入 Email。
        auth.signInWithEmailAndPassword(u, p)
            .addOnSuccessListener { result ->
                _currentUser.value = result.user
                _isLoading.value = false
                _statusMsg.value = "登录成功"
                refreshBookshelf()
                initDailyStats()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _statusMsg.value = "登录失败: ${e.message}"
            }
    }

    // [修改] 注册逻辑
    fun register(u: String, p: String) {
        if (!_isOnline.value) {
            Toast.makeText(getApplication(), "无网络", Toast.LENGTH_SHORT).show()
            return
        }
        _isLoading.value = true
        auth.createUserWithEmailAndPassword(u, p)
            .addOnSuccessListener { result ->
                _currentUser.value = result.user
                _isLoading.value = false
                _statusMsg.value = "注册成功"
                refreshBookshelf()
                initDailyStats()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _statusMsg.value = "注册失败: ${e.message}"
            }
    }

    // [修改] 退出登录
    fun logout() {
        viewModelScope.launch {
            repository.clearAllData()
            // ... 清除 SP ...
            getApplication<Application>().getSharedPreferences("app_config", Context.MODE_PRIVATE).edit().clear().apply()
            getApplication<Application>().getSharedPreferences("user_stats", Context.MODE_PRIVATE).edit().clear().apply()
            
            _bookList.value = emptyList()
            _isLearningMode.value = false
            _currentWord.value = null
            _streakDays.value = 0
            _dailyCount.value = 0
            
            auth.signOut() // [修改] Firebase 退出
            _currentUser.value = null
            _statusMsg.value = "已安全退出"
            refreshBookshelf()
        }
    }

    private fun translateError(e: Throwable) = e.message ?: "Error"
}