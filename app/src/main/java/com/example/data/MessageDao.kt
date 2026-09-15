package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun getMessagesForUser(userId: String): Flow<List<Message>>

    @Insert
    suspend fun insert(message: Message)
    
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}
