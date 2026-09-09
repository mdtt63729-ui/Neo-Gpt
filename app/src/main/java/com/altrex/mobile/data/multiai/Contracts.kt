package com.altrex.mobile.data.multiai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray

/**
 * Result of a validation check.
 */
sealed class ValidationResult {
    /** Validation passed. */
    object Valid : ValidationResult()

    /** Validation failed with one or more [errors]. */
    data class Invalid(val errors: List<String>) : ValidationResult() {
        constructor(error: String) : this(listOf(error))
        val isError: Boolean get() = true
    }

    fun isValid(): Boolean = this is Valid
    fun errors(): List<String> = if (this is Invalid) errors else emptyList()
}

/**
 * Path safety utilities for the multi-AI director.
 *
 * On Android, file paths must stay within app-scoped storage. This object
 * rejects path-traversal attempts (../), absolute paths outside the allowed
 * root, and other unsafe patterns.
 */
object PathSafety {

    /** Characters/patterns that are never allowed in a file path. */
    private val forbiddenPatterns = listOf(
        Regex("\\.\\."),
        Regex("^/"),
        Regex("~"),
        Regex("\u0000")
    )

    /**
     * Checks whether [path] is safe to use within the given [rootDir].
     *
     * A path is safe if:
     *   - It does not contain ".." (path traversal)
     *   - It is not an absolute path outside the root
     *   - It does not contain null bytes
     *   - The normalised path starts with the root
     */
    fun isSafe(path: String, rootDir: String): Boolean {
        if (path.isBlank()) return false

        // Reject forbidden patterns
        for (pattern in forbiddenPatterns) {
            if (pattern.containsMatchIn(path)) return false
        }

        // Normalise both paths and verify containment
        val normalisedRoot = normalise(rootDir)
        val normalisedPath = normalise("$rootDir/$path")

        return normalisedPath.startsWith(normalisedRoot)
    }

    /**
     * Validates a path and returns a [ValidationResult].
     */
    fun validate(path: String, rootDir: String): ValidationResult {
        if (path.isBlank()) {
            return ValidationResult.Invalid("Path is blank")
        }
        for (pattern in forbiddenPatterns) {
            if (pattern.containsMatchIn(path)) {
                return ValidationResult.Invalid(
                    "Path contains forbidden pattern: ${pattern.pattern} in '$path'"
                )
            }
        }
        if (!isSafe(path, rootDir)) {
            return ValidationResult.Invalid(
                "Path '$path' escapes the allowed root directory"
            )
        }
        return ValidationResult.Valid
    }

    /**
     * Filters a list of paths, keeping only the safe ones.
     */
    fun filterSafe(paths: List<String>, rootDir: String): List<String> =
        paths.filter { isSafe(it, rootDir) }

    private fun normalise(path: String): String {
        return path.replace("\\", "/")
            .replace(Regex("/+"), "/")
            .trimEnd('/')
    }
}

/**
 * Validates that specialist tasks stay within their declared scope.
 *
 * In the multi-AI system, each specialist task declares which files or
 * directories it is allowed to touch. This validator ensures no task
 * writes outside its declared scope.
 */
object ScopeValidator {

    /**
     * Checks whether [filePath] falls within any of the [allowedPaths].
     */
    fun isWithinScope(filePath: String, allowedPaths: List<String>): Boolean {
        if (allowedPaths.isEmpty()) return false
        val normalisedFile = PathSafety.run {
            filePath.replace("\\", "/").replace(Regex("/+"), "/").trimStart('/')
        }
        return allowedPaths.any { allowed ->
            val normalisedAllowed = allowed.replace("\\", "/")
                .replace(Regex("/+"), "/")
                .trimStart('/')
            normalisedFile.startsWith(normalisedAllowed)
        }
    }

    /**
     * Validates that a task's file targets are within its allowed scope.
     *
     * @param targetFiles files the task wants to modify
     * @param allowedScope paths the task is permitted to touch
     */
    fun validateScope(
        targetFiles: List<String>,
        allowedScope: List<String>
    ): ValidationResult {
        val violations = targetFiles.filter { !isWithinScope(it, allowedScope) }
        return if (violations.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                violations.map { "File '$it' is outside the allowed scope" }
            )
        }
    }
}

/**
 * Validates a [DirectorPlan] before execution.
 *
 * Checks:
 *   - The plan has at least one task
 *   - All task IDs are unique
 *   - All dependency references point to existing tasks
 *   - There are no circular dependencies
 */
object PlanValidator {

    /**
     * Validates a director plan represented as a JSON object.
     *
     * Expected structure:
     * ```
     * {
     *   "tasks": [
     *     {
     *       "id": "task-1",
     *       "name": "...",
     *       "dependencies": ["task-0"],
     *       "scope": ["src/"],
     *       ...
     *     }
     *   ]
     * }
     * ```
     */
    fun validatePlan(planJson: JsonObject): ValidationResult {
        val errors = mutableListOf<String>()

        // Check tasks array exists
        val tasksElement = planJson["tasks"]
            ?: return ValidationResult.Invalid("Plan is missing 'tasks' array")

        val tasksArray = tasksElement as? JsonArray
            ?: return ValidationResult.Invalid("'tasks' must be an array")

        if (tasksArray.isEmpty()) {
            return ValidationResult.Invalid("Plan has no tasks")
        }

        // Extract task IDs and check for uniqueness
        val taskIds = mutableListOf<String>()
        val taskDependencies = mutableMapOf<String, List<String>>()

        for ((index, taskElement) in tasksArray.withIndex()) {
            val taskObj = taskElement as? JsonObject
            if (taskObj == null) {
                errors.add("Task at index $index is not an object")
                continue
            }

            val id = taskObj["id"]?.let { (it as? JsonPrimitive)?.contentOrNull }
            if (id == null) {
                errors.add("Task at index $index is missing 'id'")
                continue
            }

            if (taskIds.contains(id)) {
                errors.add("Duplicate task id: '$id'")
            }
            taskIds.add(id)

            val depsElement = taskObj["dependencies"] as? JsonArray
            val deps = depsElement?.mapNotNull {
                (it as? JsonPrimitive)?.contentOrNull
            } ?: emptyList()
            taskDependencies[id] = deps

            // Validate task name exists
            val name = taskObj["name"]?.let { (it as? JsonPrimitive)?.contentOrNull }
            if (name.isNullOrBlank()) {
                errors.add("Task '$id' is missing 'name'")
            }
        }

        // Check that all dependency references exist
        for ((taskId, deps) in taskDependencies) {
            for (dep in deps) {
                if (dep !in taskIds) {
                    errors.add("Task '$taskId' depends on unknown task '$dep'")
                }
            }
        }

        // Check for circular dependencies
        val cycleError = detectCycle(taskIds, taskDependencies)
        if (cycleError != null) {
            errors.add(cycleError)
        }

        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    /**
     * Detects circular dependencies using DFS.
     * Returns an error message if a cycle is found, null otherwise.
     */
    private fun detectCycle(
        taskIds: List<String>,
        dependencies: Map<String, List<String>>
    ): String? {
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        fun dfs(taskId: String): String? {
            visited.add(taskId)
            recursionStack.add(taskId)

            val deps = dependencies[taskId] ?: emptyList()
            for (dep in deps) {
                if (dep !in visited) {
                    dfs(dep)?.let { return it }
                } else if (dep in recursionStack) {
                    return "Circular dependency detected: $taskId -> $dep"
                }
            }

            recursionStack.remove(taskId)
            return null
        }

        for (taskId in taskIds) {
            if (taskId !in visited) {
                dfs(taskId)?.let { return it }
            }
        }
        return null
    }
}
