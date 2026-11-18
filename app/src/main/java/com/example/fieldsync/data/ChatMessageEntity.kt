
package com.example.fieldsync.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(

    @PrimaryKey
    val id: String,

    val sender: String,

    val deviceId: String,

    val text: String,

    // Server timestamps. Null until the relay accepts the message.
    val sentAt: Long?,

    val expiresAt: Long?,

    // CREATED, WAITING, SENT, DELIVERED, DISPLAYED, EXPIRED, DROPPED
    val status: String,

    // Local time when this message was first created on this phone.
    val createdAtMs: Long
)
