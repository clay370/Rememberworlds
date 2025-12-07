package com.example.rememberworlds

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * 应用程序自定义Application类
 * 作为应用程序的入口点，用于初始化全局资源、配置和依赖项
 * 继承自Android系统的Application类
 */
class MyApplication : Application() {
    /**
     * 应用程序创建时调用的方法
     * 在此方法中可以进行全局初始化操作
     * 例如：
     * - 初始化数据库
     * - 初始化网络库
     * - 初始化第三方SDK
     * - 设置全局配置
     */
    override fun onCreate() {
        super.onCreate()
        
        // 1. 初始化 Firebase
        FirebaseApp.initializeApp(this)

        // 2. 配置 App Check (暂时禁用以排除上传错误)
        /*
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        
        // 判断是调试版还是发布版
        val providerFactory = if (BuildConfig.DEBUG) {
            try {
                // 强制指定一个固定的 Token
                val myFixedToken = "D0C26F37-3C59-47AE-86BB-EE9C9B6684C0"
                System.setProperty("com.google.firebase.appcheck.debug.testing.token", myFixedToken)
                
                // 使用反射加载 DebugAppCheckProviderFactory
                val debugFactoryClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                val getInstanceMethod = debugFactoryClass.getMethod("getInstance")
                getInstanceMethod.invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
            } catch (e: Exception) {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }

        firebaseAppCheck.installAppCheckProviderFactory(providerFactory)
        */
    }
}