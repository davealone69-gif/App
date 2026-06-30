package com.example.appmaker.data

import androidx.room.*
import java.util.UUID

/**
 * Settings for app preferences and user configuration
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: String = "default",
    val isDarkMode: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val autoBackupEnabled: Boolean = true,
    val apiRateLimit: Int = 60, // requests per minute
    val encryptionEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Represents an API error for error tracking
 */
@Entity(tableName = "api_errors")
data class ApiError(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val errorCode: String,
    val errorMessage: String,
    val endpoint: String,
    val retryCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSolved: Boolean = false
)

/**
 * Conversation backup/export metadata
 */
@Entity(tableName = "conversation_backups")
data class ConversationBackup(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val backupPath: String, // Local file path or cloud path
    val backupFormat: String, // "json", "csv", etc.
    val messageCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0
)
