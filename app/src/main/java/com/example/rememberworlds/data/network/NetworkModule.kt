package com.example.rememberworlds.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 网络模块单例对象
 * 负责配置和提供Retrofit API服务实例
 */
object NetworkModule {
    /**
     * 基础URL
     * 虽然实际请求会使用@Url注解覆盖，但Retrofit要求必须提供一个基础URL
     */
    private const val BASE_URL = "https://gitee.com/"

    /**
     * 单词API服务实例
     * 使用lazy委托实现延迟初始化，只有在首次访问时才会创建
     * 确保整个应用程序中只有一个API服务实例
     */
    val api: WordApiService by lazy {
        // 创建Retrofit构建器
        Retrofit.Builder()
            .baseUrl(BASE_URL) // 设置基础URL
            .addConverterFactory(GsonConverterFactory.create()) // 添加Gson转换器，用于JSON解析
            .build() // 构建Retrofit实例
            .create(WordApiService::class.java) // 创建API服务接口实例
    }
}