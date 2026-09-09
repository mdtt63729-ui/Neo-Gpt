package com.altrex.mobile.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ════════════════════════════════════════════════════════════════════════════
//  ENTITIES
// ════════════════════════════════════════════════════════════════════════════

/**
 * Stores provider connection metadata (the API key is kept separately in
 * EncryptedSharedPreferences via [ProviderRepository]).
 */
@Entity(tableName = "provider_connections")
data class ProviderConnectionEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val displayName: String,
    val baseUrl: String?,
    val model: String?,
    val isConnected: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * A conversation (chat session).
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val providerId: String?,
    val model: String?,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * A single message within a conversation.
 *
 * Complex fields (images, toolCalls) are stored as JSON strings and decoded
 * via [Converters].
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val images: List<String>?,
    val toolCallsJson: String?,
    val toolCallId: String?,
    val name: String?,
    val timestamp: Long
)

/**
 * A multi-AI director run, persisted for crash recovery.
 */
@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val status: String,
    val phase: String,
    val planJson: String?,
    val tasksJson: String?,
    val result: String?,
    val startedAt: Long,
    val completedAt: Long?,
    val updatedAt: Long
)

/**
 * Key-value settings store.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long
)

/**
 * Per-project memory for the multi-AI system.
 */
@Entity(tableName = "project_memory")
data class ProjectMemoryEntity(
    @PrimaryKey val projectId: String,
    val memoryJson: String,
    val updatedAt: Long
)

// ════════════════════════════════════════════════════════════════════════════
//  DAOs
// ════════════════════════════════════════════════════════════════════════════

@androidx.room.Dao
interface ProviderConnectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProviderConnectionEntity)

    @Update
    suspend fun update(entity: ProviderConnectionEntity)

    @Query("SELECT * FROM provider_connections ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProviderConnectionEntity>>

    @Query("SELECT * FROM provider_connections ORDER BY createdAt DESC")
    suspend fun getAll(): List<ProviderConnectionEntity>

    @Query("SELECT * FROM provider_connections WHERE id = :id")
    suspend fun getById(id: String): ProviderConnectionEntity?

    @Query("SELECT * FROM provider_connections WHERE providerId = :providerId LIMIT 1")
    suspend fun getByProviderId(providerId: String): ProviderConnectionEntity?

    @Query("DELETE FROM provider_connections WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM provider_connections")
    suspend fun deleteAll()
}

@androidx.room.Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}

@androidx.room.Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getByConversation(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getRecent(conversationId: String, limit: Int): List<MessageEntity>

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun countByConversation(conversationId: String): Int
}

@androidx.room.Dao
interface RunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RunEntity)

    @Query("SELECT * FROM runs ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs ORDER BY startedAt DESC")
    suspend fun getAll(): List<RunEntity>

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getById(id: String): RunEntity?

    @Query("SELECT * FROM runs WHERE projectId = :projectId ORDER BY startedAt DESC")
    suspend fun getByProject(projectId: String): List<RunEntity>

    @Query("SELECT * FROM runs WHERE status = 'RUNNING' OR status = 'PENDING'")
    suspend fun getIncompleteRuns(): List<RunEntity>

    @Query("UPDATE runs SET status = :status, phase = :phase, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, phase: String, updatedAt: Long)

    @Query("UPDATE runs SET result = :result, status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun complete(id: String, result: String?, status: String, completedAt: Long, updatedAt: Long)

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM runs WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: String)
}

@androidx.room.Dao
interface SettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SettingsEntity)

    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun get(key: String): SettingsEntity?

    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingsEntity>

    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingsEntity>>

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}

@androidx.room.Dao
interface ProjectMemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectMemoryEntity)

    @Query("SELECT * FROM project_memory WHERE projectId = :projectId")
    suspend fun get(projectId: String): ProjectMemoryEntity?

    @Query("SELECT * FROM project_memory")
    suspend fun getAll(): List<ProjectMemoryEntity>

    @Query("DELETE FROM project_memory WHERE projectId = :projectId")
    suspend fun delete(projectId: String)
}

// ════════════════════════════════════════════════════════════════════════════
//  DATABASE
// ════════════════════════════════════════════════════════════════════════════

/**
 * Room database for the ALTREX CODE app.
 *
 * Holds all persistent data: provider connections, conversations, messages,
 * multi-AI runs, app settings, and project memory.
 *
 * Instantiate via Room.databaseBuilder() in the DI graph, supplying
 * [Converters] via .addTypeConverter().
 */
@Database(
    entities = [
        ProviderConnectionEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        RunEntity::class,
        SettingsEntity::class,
        ProjectMemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun providerConnectionDao(): ProviderConnectionDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun runDao(): RunDao
    abstract fun settingsDao(): SettingsDao
    abstract fun projectMemoryDao(): ProjectMemoryDao

    companion object {
        const val DATABASE_NAME: String = "altrex-code.db"
    }
}
