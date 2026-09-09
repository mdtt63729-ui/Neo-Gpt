package com.altrex.mobile.data.multiai

import com.altrex.mobile.data.local.AppDatabase
import com.altrex.mobile.data.local.ProjectMemoryEntity
import com.altrex.mobile.data.local.RunEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * A single memory note attached to a project.
 */
@Serializable
data class MemoryNote(
    val timestamp: Long,
    val category: String,
    val content: String
)

/**
 * Per-project memory for the multi-AI director.
 *
 * Accumulates notes across runs — architectural decisions, known issues,
 * file inventories — so that future runs can build on prior context.
 */
@Serializable
data class ProjectMemory(
    val projectId: String,
    val notes: List<MemoryNote> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun addNote(category: String, content: String): ProjectMemory {
        val note = MemoryNote(
            timestamp = System.currentTimeMillis(),
            category = category,
            content = content
        )
        return copy(notes = notes + note, updatedAt = note.timestamp)
    }
}

/**
 * Serializable state of a single task within a run, persisted to Room.
 */
@Serializable
data class TaskState(
    val id: String,
    val name: String,
    val description: String,
    val specialistType: String,
    val dependencies: List<String> = emptyList(),
    val scope: List<String> = emptyList(),
    val model: String? = null,
    val status: String = "PENDING",
    val result: String? = null
)

/**
 * Snapshot of a run's persistent state, mapped from [RunEntity].
 */
data class RunState(
    val runId: String,
    val projectId: String,
    val status: String,
    val phase: String,
    val objective: String,
    val tasks: List<TaskState>,
    val result: String?,
    val startedAt: Long,
    val completedAt: Long?
)

/**
 * Persistent state store for the multi-AI director.
 *
 * Saves runs to Room, manages per-project memory, and supports crash
 * recovery by identifying incomplete runs.
 *
 * All operations are suspend functions backed by Room DAOs.
 */
class StateStore(
    private val database: AppDatabase
) {
    private val runDao = database.runDao()
    private val memoryDao = database.projectMemoryDao()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ── Run lifecycle ───────────────────────────────────────────────────────

    /**
     * Creates a new run record in the database.
     */
    suspend fun createRun(
        runId: String,
        projectId: String,
        objective: String,
        tasks: List<TaskState>
    ): RunState {
        val now = System.currentTimeMillis()
        val entity = RunEntity(
            id = runId,
            projectId = projectId,
            status = "PENDING",
            phase = DirectorPhase.PLANNING.value,
            planJson = objective,
            tasksJson = json.encodeToString(
                ListSerializer(TaskState.serializer()),
                tasks
            ),
            result = null,
            startedAt = now,
            completedAt = null,
            updatedAt = now
        )
        runDao.upsert(entity)
        return toRunState(entity, objective, tasks)
    }

    /**
     * Loads a run by ID.
     */
    suspend fun loadRun(runId: String): RunState? {
        val entity = runDao.getById(runId) ?: return null
        return entityToRunState(entity)
    }

    /**
     * Lists all runs.
     */
    suspend fun listRuns(): List<RunState> {
        return runDao.getAll().map { entityToRunState(it) }
    }

    /**
     * Observes all runs as a Flow.
     */
    fun observeRuns(): Flow<List<RunState>> {
        return runDao.observeAll().map { entities ->
            entities.map { entityToRunState(it) }
        }
    }

    /**
     * Returns all runs that were interrupted (status RUNNING or PENDING).
     */
    suspend fun getIncompleteRuns(): List<RunState> {
        return runDao.getIncompleteRuns().map { entityToRunState(it) }
    }

    /**
     * Updates the phase of a run.
     */
    suspend fun updatePhase(runId: String, phase: DirectorPhase) {
        val now = System.currentTimeMillis()
        val entity = runDao.getById(runId) ?: return
        runDao.upsert(
            entity.copy(
                phase = phase.value,
                status = if (phase == DirectorPhase.COMPLETED) "COMPLETED"
                         else if (phase == DirectorPhase.FAILED) "FAILED"
                         else "RUNNING",
                updatedAt = now
            )
        )
    }

    /**
     * Updates a single task's result and status within a run.
     */
    suspend fun updateTaskResult(
        runId: String,
        taskId: String,
        result: String,
        status: String
    ) {
        val now = System.currentTimeMillis()
        val entity = runDao.getById(runId) ?: return
        val tasks = parseTasks(entity.tasksJson).map { task ->
            if (task.id == taskId) task.copy(result = result, status = status)
            else task
        }
        runDao.upsert(
            entity.copy(
                tasksJson = json.encodeToString(
                    ListSerializer(TaskState.serializer()),
                    tasks
                ),
                updatedAt = now
            )
        )
    }

    /**
     * Marks a run as completed with the given [result].
     */
    suspend fun completeRun(runId: String, result: String) {
        val now = System.currentTimeMillis()
        runDao.complete(
            id = runId,
            result = result,
            status = "COMPLETED",
            completedAt = now,
            updatedAt = now
        )
    }

    /**
     * Marks a run as failed with the given [error] message.
     */
    suspend fun failRun(runId: String, error: String) {
        val now = System.currentTimeMillis()
        val entity = runDao.getById(runId) ?: return
        runDao.upsert(
            entity.copy(
                status = "FAILED",
                phase = DirectorPhase.FAILED.value,
                result = error,
                completedAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * Deletes a run.
     */
    suspend fun deleteRun(runId: String) {
        runDao.delete(runId)
    }

    // ── Project memory ──────────────────────────────────────────────────────

    /**
     * Saves or updates project memory.
     */
    suspend fun saveProjectMemory(projectId: String, memory: ProjectMemory) {
        memoryDao.upsert(
            ProjectMemoryEntity(
                projectId = projectId,
                memoryJson = json.encodeToString(ProjectMemory.serializer(), memory),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Loads project memory by project ID.
     */
    suspend fun loadProjectMemory(projectId: String): ProjectMemory? {
        val entity = memoryDao.get(projectId) ?: return null
        return try {
            json.decodeFromString(ProjectMemory.serializer(), entity.memoryJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Adds a note to the project memory, creating it if necessary.
     */
    suspend fun addMemoryNote(projectId: String, category: String, content: String) {
        val existing = loadProjectMemory(projectId) ?: ProjectMemory(projectId)
        val updated = existing.addNote(category, content)
        saveProjectMemory(projectId, updated)
    }

    // ── Crash recovery ──────────────────────────────────────────────────────

    /**
     * Recovers from a crash by marking all incomplete runs as FAILED.
     *
     * Returns the list of runs that were recovered, so the caller can
     * notify the user or attempt to restart them.
     */
    suspend fun recoverIncompleteRuns(): List<RunState> {
        val incomplete = getIncompleteRuns()
        for (run in incomplete) {
            failRun(run.runId, "Run was interrupted by app crash or closure")
        }
        return incomplete
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun parseTasks(tasksJson: String?): List<TaskState> {
        if (tasksJson.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(TaskState.serializer()), tasksJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun entityToRunState(entity: RunEntity): RunState {
        val tasks = parseTasks(entity.tasksJson)
        return RunState(
            runId = entity.id,
            projectId = entity.projectId,
            status = entity.status,
            phase = entity.phase,
            objective = entity.planJson ?: "",
            tasks = tasks,
            result = entity.result,
            startedAt = entity.startedAt,
            completedAt = entity.completedAt
        )
    }

    private fun toRunState(
        entity: RunEntity,
        objective: String,
        tasks: List<TaskState>
    ): RunState {
        return RunState(
            runId = entity.id,
            projectId = entity.projectId,
            status = entity.status,
            phase = entity.phase,
            objective = objective,
            tasks = tasks,
            result = entity.result,
            startedAt = entity.startedAt,
            completedAt = entity.completedAt
        )
    }
}
