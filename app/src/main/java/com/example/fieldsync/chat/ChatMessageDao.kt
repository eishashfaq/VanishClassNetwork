
package com.example.fieldsync.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    @Query("""
        SELECT * FROM chat_messages
        WHERE expiresAt IS NULL OR expiresAt > :now
        ORDER BY createdAtMs ASC
    """)
    suspend fun getActiveMessages(now: Long): List<ChatMessageEntity>

    // Messages waiting to be sent.
    @Query("""
        SELECT * FROM chat_messages
        WHERE status = 'WAITING'
        ORDER BY createdAtMs ASC
    """)
    suspend fun getWaitingMessages(): List<ChatMessageEntity>

    // Messages sent locally but not yet acknowledged by the relay.
    @Query("""
        SELECT * FROM chat_messages
        WHERE status IN ('WAITING', 'SENT')
        ORDER BY createdAtMs ASC
    """)
    suspend fun getPendingMessages(): List<ChatMessageEntity>

    @Query("""
        SELECT * FROM chat_messages
        WHERE id = :messageId
        LIMIT 1
    """)
    suspend fun getById(messageId: String): ChatMessageEntity?

    @Query("""
        UPDATE chat_messages
        SET status = :status,
            sentAt = :sentAt,
            expiresAt = :expiresAt
        WHERE id = :messageId
    """)
    suspend fun updateDelivery(
        messageId: String,
        status: String,
        sentAt: Long?,
        expiresAt: Long?
    )

    @Query("""
        UPDATE chat_messages
        SET status = :status
        WHERE id = :messageId
    """)
    suspend fun updateStatus(
        messageId: String,
        status: String
    )

    @Query("""
        DELETE FROM chat_messages
        WHERE expiresAt IS NOT NULL AND expiresAt <= :now
    """)
    suspend fun deleteExpired(now: Long)

    @Query("""
        DELETE FROM chat_messages
        WHERE id = :messageId
    """)
    suspend fun deleteById(messageId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
