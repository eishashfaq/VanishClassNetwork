
package com.example.fieldsync.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RecordEntity::class,
        ChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Existing record-sync functionality
    abstract fun recordDao(): RecordDao

    // New chat-message functionality
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {

        /**
         * Safely upgrades an existing version 1 database
         * by creating the new chat_messages table.
         *
         * Existing records are preserved.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        sender TEXT NOT NULL,
                        deviceId TEXT NOT NULL,
                        text TEXT NOT NULL,
                        sentAt INTEGER,
                        expiresAt INTEGER,
                        status TEXT NOT NULL,
                        createdAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
