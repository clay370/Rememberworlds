package com.example.rememberworlds.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 应用程序的主数据库类，使用Room框架实现
 * 管理单词实体的存储和访问
 */
// 数据库注解：指定实体类、版本号和是否导出Schema
@Database(entities = [WordEntity::class], version = 3, exportSchema = false)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * 获取单词数据访问对象(DAO)
     * 用于执行数据库操作
     */
    abstract fun wordDao(): WordDao

    /**
     * 伴生对象，用于实现单例模式
     * 确保整个应用程序中只有一个数据库实例
     */
    companion object {
        // 使用@Volatile注解确保INSTANCE变量的可见性，防止指令重排
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库实例的静态方法
         * 使用双重检查锁定模式实现线程安全的单例
         *
         * @param context 应用程序上下文
         * @return AppDatabase实例
         */
        fun getDatabase(context: Context): AppDatabase {
            // 如果INSTANCE不为null，直接返回
            // 否则进入同步块创建实例
            return INSTANCE ?: synchronized(this) {
                // 再次检查INSTANCE，防止多线程竞争
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,  // 使用应用程序上下文，避免内存泄漏
                    AppDatabase::class.java,       // 数据库类
                    "word_app_database"           // 数据库名称
                )
                .fallbackToDestructiveMigration() // 允许破坏性迁移（开发阶段）
                .build()
                // 更新INSTANCE引用
                INSTANCE = instance
                // 返回新创建的实例
                instance
            }
        }
    }
}