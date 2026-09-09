package com.altrex.mobile.data.repository

import com.altrex.mobile.data.local.AppDatabase
import com.altrex.mobile.data.local.ConversationEntity
import com.altrex.mobile.data.local.MessageEntity
import com.altrex.mobile.data.model.ChatMessage
import com.altrex.mobile.data.model.ToolCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Domain representation of a conversation.
 */
data class Conversation(
    val id: String,
    val title: String,
    val providerId: String?,
    val model: String?,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0
)

/**
 * Repository for conversation and message persistence.
 *
 * Uses Room ([ConversationDao] and [MessageDao]) for storage, which serves
 * as the Android equivalent of the desktop app's localStorage-backed
 * conversation store.
 *
 * Converts between [ChatMessage] (domain model) and [MessageEntity] (Room
 * entity) at the repository boundary.
 */
class ConversationRepository(
    private val database: AppDatabase
) {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ── Conversation CRUD ───────────────────────────────────────────────────

    /**
     * Creates a new conversation.
     *
     * @return the created conversation.
     */
    suspend fun createConversation(
        title: String = "New Conversation",
        providerId: String? = null,
        model: String? = null,
        systemPrompt: String? = null
    ): Conversation {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        conversationDao.upsert(
            ConversationEntity(
                id = id,
                title = title,
                providerId = providerId,
                model = model,
                systemPrompt = systemPrompt,
                createdAt = now,
                updatedAt = now
            )
        )

        return Conversation(
            id = id,
            title = title,
            providerId = providerId,
            model = model,
            systemPrompt = systemPrompt,
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Observes all conversations as a Flow, sorted by last updated.
     */
    fun observeConversations(): Flow<List<Conversation>> {
        return conversationDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Returns all conversations.
     */
    suspend fun getConversations(): List<Conversation> {
        return conversationDao.getAll().map { it.toDomain() }
    }

    /**
     * Returns a single conversation by ID.
     */
    suspend fun getConversation(id: String): Conversation? {
        return conversationDao.getById(id)?.toDomain()
    }

    /**
     * Updates a conversation's title.
     */
    suspend fun updateTitle(id: String, title: String) {
        conversationDao.updateTitle(id, title, System.currentTimeMillis())
    }

    /**
     * Deletes a conversation and all its messages (cascade).
     */
    suspend fun deleteConversation(id: String) {
        conversationDao.delete(id)
    }

    /**
     * Deletes all conversations and messages.
     */
    suspend fun deleteAll() {
        conversationDao.deleteAll()
    }

    // ── Message CRUD ────────────────────────────────────────────────────────

    /**
     * Adds a message to a conversation.
     *
     * @param conversationId the conversation to add the message to
     * @param message the domain message to store
     * @return the ID assigned to the message
     */
    suspend fun addMessage(conversationId: String, message: ChatMessage): String {
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        messageDao.insert(
            message.toEntity(
                messageId = messageId,
                conversationId = conversationId,
                timestamp = now
            )
        )

        // Touch the conversation's updatedAt
        val conversation = conversationDao.getById(conversationId)
        if (conversation != null) {
            conversationDao.upsert(conversation.copy(updatedAt = now))
        }

        return messageId
    }

    /**
     * Adds multiple messages to a conversation in a single transaction.
     */
    suspend fun addMessages(conversationId: String, messages: List<ChatMessage>): List<String> {
        val now = System.currentTimeMillis()
        val ids = messages.map { UUID.randomUUID().toString() }
        val entities = messages.mapIndexed { index, msg ->
            msg.toEntity(
                messageId = ids[index],
                conversationId = conversationId,
                timestamp = now + index  // preserve order
            )
        }
        messageDao.insertAll(entities)

        // Touch the conversation's updatedAt
        val conversation = conversationDao.getById(conversationId)
        if (conversation != null) {
            conversationDao.upsert(conversation.copy(updatedAt = now))
        }

        return ids
    }

    /**
     * Observes messages in a conversation as a Flow, ordered by timestamp.
     */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.observeByConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Returns all messages in a conversation.
     */
    suspend fun getMessages(conversationId: String): List<ChatMessage> {
        return messageDao.getByConversation(conversationId).map { it.toDomain() }
    }

    /**
     * Returns the [limit] most recent messages in a conversation.
     */
    suspend fun getRecentMessages(conversationId: String, limit: Int = 50): List<ChatMessage> {
        return messageDao.getRecent(conversationId, limit).map { it.toDomain() }
    }

    /**
     * Returns the number of messages in a conversation.
     */
    suspend fun getMessageCount(conversationId: String): Int {
        return messageDao.countByConversation(conversationId)
    }

    /**
     * Deletes all messages in a conversation.
     */
    suspend fun clearMessages(conversationId: String) {
        messageDao.deleteByConversation(conversationId)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun ConversationEntity.toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        providerId = providerId,
        model = model,
        systemPrompt = systemPrompt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ChatMessage.toEntity(
        messageId: String,
        conversationId: String,
        timestamp: Long
    ): MessageEntity {
        return MessageEntity(
            id = messageId,
            conversationId = conversationId,
            role = role,
            content = content ?: "",
            images = images,
            toolCallsJson = serializeToolCalls(toolCalls),
            toolCallId = toolCallId,
            name = name,
            timestamp = timestamp
        )
    }

    private fun MessageEntity.toDomain(): ChatMessage {
        return ChatMessage(
            role = role,
            content = content,
            images = images,
            toolCalls = deserializeToolCalls(toolCallsJson),
            toolCallId = toolCallId,
            name = name
        )
    }

    /**
     * Serializes tool calls to JSON string.
     * Returns null if the list is null or empty.
     */
    private fun serializeToolCalls(toolCalls: List<ToolCall>?): String? {
        if (toolCalls.isNullOrEmpty()) return null
        return try {
            json.encodeToString(ListSerializer(ToolCall.serializer()), toolCalls)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deserializes tool calls from JSON string.
     * Returns null if the string is null or empty.
     */
    private fun deserializeToolCalls(toolCallsJson: String?): List<ToolCall>? {
        if (toolCallsJson.isNullOrEmpty()) return null
        return try {
            json.decodeFromString(ListSerializer(ToolCall.serializer()), toolCallsJson)
        } catch (e: Exception) {
            null
        }
    }
}
