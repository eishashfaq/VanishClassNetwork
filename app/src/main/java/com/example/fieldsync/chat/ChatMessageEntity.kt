
package com.example.fieldsync.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(

    @PrimaryKey
    val id: String,

    val sender: String,

    val deviceId: String,

    val text: String,

    val sentAt: Long?,

    val expiresAt: Long?,

    val status: String,

    val createdAtMs: Long
)
