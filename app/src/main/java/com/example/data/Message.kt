package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "default"
)
