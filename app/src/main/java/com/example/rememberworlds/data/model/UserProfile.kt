package com.example.rememberworlds.data.model

/**
 * 用户资料数据模型
 * 存储用户的个人信息
 */
data class UserProfile(
    /**
     * 用户唯一标识符
     * 默认值为空字符串
     */
    val uid: String = "",
    
    /**
     * 用户昵称
     * 默认值为"未填写"
     */
    val nickname: String = "未填写",
    
    /**
     * 用户性别
     * 默认值为"保密"
     */
    val gender: String = "保密",
    
    /**
     * 用户出生日期
     * 默认值为"未填写"
     */
    val birthDate: String = "未填写",
    
    /**
     * 用户所在地
     * 默认值为"未填写"
     */
    val location: String = "未填写",
    
    /**
     * 用户学校
     * 默认值为"未填写"
     */
    val school: String = "未填写",
    
    /**
     * 用户年级
     * 默认值为"未填写"
     */
    val grade: String = "未填写",
    
    /**
     * 用户IP所在地
     * 默认值为"未知"
     */
    val ipLocation: String = "未知",
    
    /**
     * 用户头像URL
     * 默认值为空字符串
     */
    val avatarUrl: String = "" 
)
