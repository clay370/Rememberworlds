package com.example.rememberworlds.data.network

import retrofit2.http.GET
import retrofit2.http.Url

/**
 * 单词API服务接口
 * 定义了应用程序的网络请求方法
 */
interface WordApiService {
    /**
     * 下载单词书籍
     * 从指定URL获取单词列表
     *
     * @param url 完整的API地址，用于覆盖默认baseUrl
     * @return 包含WordJson对象的列表
     */
    @GET
    suspend fun downloadWords(@Url url: String): List<WordJson>

    /**
     * 在线查词功能
     * 从指定URL搜索单词信息
     * 因为base_url是gitee，所以使用@Url传入完整API地址来覆盖
     *
     * @param url 完整的查词API地址
     * @return 包含SearchResponseItem对象的列表
     */
    @GET
    suspend fun searchWordOnline(@Url url: String): List<SearchResponseItem>
}