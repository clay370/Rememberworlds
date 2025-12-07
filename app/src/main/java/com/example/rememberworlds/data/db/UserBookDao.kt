package com.example.rememberworlds.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserBookDao {
    @Query("SELECT * FROM user_books ORDER BY createdAt DESC")
    fun getAllUserBooks(): Flow<List<UserBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: UserBookEntity)

    @Query("DELETE FROM user_books WHERE id = :id")
    suspend fun deleteById(id: String)
}
