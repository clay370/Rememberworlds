package com.example.rememberworlds

import android.app.Application

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
        // 目前为空，可根据需要添加初始化代码
    }
}