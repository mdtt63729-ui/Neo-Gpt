package com.altrex.mobile.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.altrex.mobile.data.local.AppDatabase
import com.altrex.mobile.data.local.ProviderConnectionEntity
import com.altrex.mobile.data.model.ProviderConnectionInput
import com.altrex.mobile.data.provider.ProviderService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Domain representation of a stored provider connection.
 * Does NOT include the API key — that is kept in EncryptedSharedPreferences
 * and retrieved separately via [ProviderRepository.getApiKey].
 */
data class StoredProviderConnection(
    val id: String,
    val providerId: String,
    val displayName: String,
    val baseUrl: String?,
    val model: String?,
    val isConnected: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Repository for managing provider connections.
 *
 * Stores connection metadata in Room ([ProviderConnectionEntity]) and
 * encrypts API keys using [EncryptedSharedPreferences], which is the Android
 * equivalent of the desktop app's secure credential storage.
 *
 * Uses [ProviderService] to test and establish live connections.
 */
class ProviderRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val providerService: ProviderService
) {
    private val dao = database.providerConnectionDao()

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Connection CRUD ─────────────────────────────────────────────────────

    /**
     * Saves a provider connection. The API key is encrypted and stored
     * separately from the connection metadata.
     *
     * @return the ID assigned to the connection.
     */
    suspend fun saveConnection(input: ProviderConnectionInput): String {
        val connectionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Store metadata in Room
        dao.upsert(
            ProviderConnectionEntity(
                id = connectionId,
                providerId = input.providerId,
                displayName = input.displayName ?: input.providerId,
                baseUrl = input.baseUrl,
                model = input.model,
                isConnected = false,
                createdAt = now,
                updatedAt = now
            )
        )

        // Encrypt and store the API key
        encryptedPrefs.edit()
            .putString(apiKeyPrefKey(connectionId), input.apiKey)
            .apply()

        return connectionId
    }

    /**
     * Updates an existing connection's metadata and API key.
     */
    suspend fun updateConnection(
        connectionId: String,
        displayName: String? = null,
        baseUrl: String? = null,
        model: String? = null,
        apiKey: String? = null
    ) {
        val existing = dao.getById(connectionId) ?: return
        val now = System.currentTimeMillis()

        dao.upsert(
            existing.copy(
                displayName = displayName ?: existing.displayName,
                baseUrl = baseUrl ?: existing.baseUrl,
                model = model ?: existing.model,
                updatedAt = now
            )
        )

        if (apiKey != null) {
            encryptedPrefs.edit()
                .putString(apiKeyPrefKey(connectionId), apiKey)
                .apply()
        }
    }

    /**
     * Returns all stored connections (without API keys).
     */
    suspend fun getConnections(): List<StoredProviderConnection> {
        return dao.getAll().map { it.toDomain() }
    }

    /**
     * Observes all connections as a Flow.
     */
    fun observeConnections(): Flow<List<StoredProviderConnection>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Returns a single connection by ID.
     */
    suspend fun getConnection(connectionId: String): StoredProviderConnection? {
        return dao.getById(connectionId)?.toDomain()
    }

    /**
     * Returns the connection for a given provider ID, if one exists.
     */
    suspend fun getConnectionByProvider(providerId: String): StoredProviderConnection? {
        return dao.getByProviderId(providerId)?.toDomain()
    }

    /**
     * Deletes a connection and its encrypted API key.
     */
    suspend fun deleteConnection(connectionId: String) {
        dao.delete(connectionId)
        encryptedPrefs.edit()
            .remove(apiKeyPrefKey(connectionId))
            .apply()
    }

    // ── API key access ──────────────────────────────────────────────────────

    /**
     * Retrieves the decrypted API key for a connection.
     * Returns null if no key is stored.
     */
    fun getApiKey(connectionId: String): String? {
        return encryptedPrefs.getString(apiKeyPrefKey(connectionId), null)
    }

    /**
     * Returns the API key for a given provider ID, if a connection exists.
     */
    suspend fun getApiKeyForProvider(providerId: String): String? {
        val connection = dao.getByProviderId(providerId) ?: return null
        return getApiKey(connection.id)
    }

    // ── Connection state ────────────────────────────────────────────────────

    /**
     * Marks a connection as connected or disconnected.
     */
    suspend fun setConnected(connectionId: String, connected: Boolean) {
        val existing = dao.getById(connectionId) ?: return
        dao.upsert(
            existing.copy(
                isConnected = connected,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Tests a provider connection by attempting to connect via ProviderService.
     *
     * @return true if the connection succeeded, false otherwise.
     */
    suspend fun testConnection(input: ProviderConnectionInput): Boolean {
        return try {
            val result = providerService.connect(input)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Connects to a provider and marks the connection as active.
     *
     * @return the connection ID if successful, null otherwise.
     */
    suspend fun connect(connectionId: String): Boolean {
        val connection = dao.getById(connectionId) ?: return false
        val apiKey = getApiKey(connectionId) ?: return false

        val input = ProviderConnectionInput(
            providerId = connection.providerId,
            apiKey = apiKey,
            baseUrl = connection.baseUrl,
            model = connection.model,
            displayName = connection.displayName
        )

        val result = providerService.connect(input)
        if (result.isSuccess) {
            setConnected(connectionId, true)
            return true
        }
        return false
    }

    /**
     * Disconnects a provider.
     */
    suspend fun disconnect(connectionId: String) {
        setConnected(connectionId, false)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun apiKeyPrefKey(connectionId: String): String = "api_key_$connectionId"

    private fun ProviderConnectionEntity.toDomain(): StoredProviderConnection {
        return StoredProviderConnection(
            id = id,
            providerId = providerId,
            displayName = displayName,
            baseUrl = baseUrl,
            model = model,
            isConnected = isConnected,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        private const val PREFS_NAME = "altrex_provider_credentials"
    }
}
