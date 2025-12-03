package com.example.rememberworlds.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    // 基础地址（虽然我们会用 @Url 覆盖它，但 Retrofit 要求必须填一个）
    private const val BASE_URL = "https://gitee.com/"

    // 创建 API 实例
    val api: WordApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // 使用 Gson 解析 JSON
            .build()
            .create(WordApiService::class.java)
    }
}