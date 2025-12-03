package com.example.rememberworlds

import android.app.Application
import cn.leancloud.LeanCloud

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ==========================================
        // 【重要】请去 LeanCloud 后台 -> 设置 -> 应用凭证 复制你的信息
        // ==========================================
        val APP_ID = "bsINfsxxiHcajZa7aX7NqA4G-gzGzoHsz"
        val APP_KEY = "NtNk1nVUVBhly4fYhuRdIVTg"
        val SERVER_URL = "https://bsinfsxx.lc-cn-n1-shared.com"

        // 初始化 LeanCloud
        LeanCloud.initialize(this, APP_ID, APP_KEY, SERVER_URL)
    }
}