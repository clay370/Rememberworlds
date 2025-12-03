package com.example.rememberworlds.data.network

import retrofit2.http.GET
import retrofit2.http.Url

interface WordApiService {
    // 1. 原有的：下载书籍
    @GET
    suspend fun downloadWords(@Url url: String): List<WordJson>

    // 2. 新增：在线查词
    // 因为 base_url 是 gitee，所以这里我们用 @Url 传入完整的 API 地址来覆盖它
    @GET
    suspend fun searchWordOnline(@Url url: String): List<SearchResponseItem>
}