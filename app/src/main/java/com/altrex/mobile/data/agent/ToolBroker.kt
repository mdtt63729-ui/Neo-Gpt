package com.altrex.mobile.data.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Result of executing a tool.
 */
sealed class ToolResult {
    /** The tool executed successfully and returned [output]. */
    data class Success(val output: String) : ToolResult()

    /** The tool failed with an [error] message. */
    data class Error(val error: String, val code: String = "TOOL_ERROR") : ToolResult()

    /** The requested tool is unknown. */
    data class UnknownTool(val toolName: String) : ToolResult()
}

/**
 * Lightweight in-memory virtual file system.
 *
 * On Android, the agent cannot access arbitrary file paths. Instead, the
 * ToolBroker operates on this in-memory representation, which can be
 * populated from app-scoped storage or constructed during a session.
 *
 * File paths are normalised to use forward slashes and are stored relative
 * to a virtual root (e.g. "src/main.kt", "docs/README.md").
 */
class VirtualFileSystem {
    private val files: MutableMap<String, String> = mutableMapOf()

    /** Writes [content] to [path], creating parent directories implicitly. */
    fun writeFile(path: String, content: String) {
        files[normalisePath(path)] = content
    }

    /** Reads the content of [path], or null if it does not exist. */
    fun readFile(path: String): String? = files[normalisePath(path)]

    /** Returns true if [path] exists. */
    fun exists(path: String): Boolean = files.containsKey(normalisePath(path))

    /** Deletes [path]; returns true if a file was removed. */
    fun deleteFile(path: String): Boolean = files.remove(normalisePath(path)) != null

    /**
     * Lists all file paths that start with [dirPrefix].
     * If [dirPrefix] is empty or "/", returns all paths.
     */
    fun listFiles(dirPrefix: String = ""): List<String> {
        val normalised = normalisePath(dirPrefix)
        return files.keys
            .filter { normalised.isEmpty() || it.startsWith(normalised) }
            .sorted()
    }

    /**
     * Applies a text replacement to the file at [path].
     *
     * @return true if [oldText] was found and replaced, false otherwise.
     */
    fun editFile(path: String, oldText: String, newText: String): Boolean {
        val key = normalisePath(path)
        val content = files[key] ?: return false
        if (!content.contains(oldText)) return false
        files[key] = content.replace(oldText, newText)
        return true
    }

    /** Returns the total number of files in the virtual file system. */
    fun fileCount(): Int = files.size

    /** Returns a snapshot of all files (path -> content). */
    fun snapshot(): Map<String, String> = files.toMap()

    /** Clears all files. */
    fun clear() = files.clear()

    /** Imports a set of files from a map. */
    fun importFiles(fileMap: Map<String, String>>) {
        fileMap.forEach { (path, content) -> writeFile(path, content) }
    }

    private fun normalisePath(path: String): String {
        var normalised = path.replace("\\", "/")
        normalised = normalised.replace(Regex("/+"), "/")
        normalised = normalised.trimEnd('/')
        if (normalised.startsWith("/")) normalised = normalised.substring(1)
        return normalised
    }
}

/**
 * Project tool broker for the ALTREX CODE agent.
 *
 * Executes file-manipulation tools (edit_file, write_file, read_file,
 * list_files) against an in-memory [VirtualFileSystem]. This is the Android
 * adaptation of the desktop app's file-based tool broker — instead of touching
 * the real file system, all operations go through the VFS, which can later be
 * synced to app-scoped storage or surfaced to the user.
 *
 * Each tool is also responsible for building its own [ToolDefinition] for
 * inclusion in LLM tool-call requests.
 */
class ToolBroker(
    private val vfs: VirtualFileSystem = VirtualFileSystem()
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** The virtual file system this broker operates on. */
    fun fileSystem(): VirtualFileSystem = vfs

    /**
     * Executes a tool by name with the given JSON [params].
     *
     * Supported tools:
     * - [write_file] — params: {path: String, content: String}
     * - [read_file] — params: {path: String}
     * - [edit_file] — params: {path: String, old_text: String, new_text: String}
     * - [list_files] — params: {dir: String?}
     */
    suspend fun executeTool(toolName: String, params: JsonObject): ToolResult {
        return when (toolName) {
            "write_file" -> executeWriteFile(params)
            "read_file" -> executeReadFile(params)
            "edit_file" -> executeEditFile(params)
            "list_files" -> executeListFiles(params)
            else -> ToolResult.UnknownTool(toolName)
        }
    }

    /**
     * Executes multiple tool calls in sequence.
     * Returns a list of results in the same order as the inputs.
     */
    suspend fun executeTools(toolCalls: List<Pair<String, JsonObject>>): List<ToolResult> {
        return toolCalls.map { (name, params) -> executeTool(name, params) }
    }

    // ── Tool implementations ────────────────────────────────────────────────

    private fun executeWriteFile(params: JsonObject): ToolResult {
        val path = params["path"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("Missing 'path' parameter", "MISSING_PARAM")
        val content = params["content"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("Missing 'content' parameter", "MISSING_PARAM")

        val existed = vfs.exists(path)
        vfs.writeFile(path, content)
        return ToolResult.Success(
            if (existed) "File '$path' updated successfully (${content.length} bytes)"
            else "File '$path' created successfully (${content.length} bytes)"
        )
    }

    private fun executeReadFile(params: JsonObject): ToolResult {
        val path = params["path"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("Missing 'path' parameter", "MISSING_PARAM")

        val content = vfs.readFile(path)
            ?: return ToolResult.Error("File '$path' does not exist", "FILE_NOT_FOUND")

        return ToolResult.Success(content)
    }

    private fun executeEditFile(params: JsonObject): ToolResult {
        val path = params["path"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("Missing 'path' parameter", "MISSING_PARAM")
        val oldText = params["old_text"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("Missing 'old_text' parameter", "MISSING_PARAM")
        val newText = params["new_text"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.Error("Missing 'new_text' parameter", "MISSING_PARAM")

        if (!vfs.exists(path)) {
            return ToolResult.Error("File '$path' does not exist", "FILE_NOT_FOUND")
        }

        val success = vfs.editFile(path, oldText, newText)
        return if (success) {
            ToolResult.Success("File '$path' edited successfully")
        } else {
            ToolResult.Error(
                "old_text not found in '$path'. The file may have changed since last read.",
                "TEXT_NOT_FOUND"
            )
        }
    }

    private fun executeListFiles(params: JsonObject): ToolResult {
        val dir = params["dir"]?.jsonPrimitive?.contentOrNull ?: ""
        val files = vfs.listFiles(dir)
        return if (files.isEmpty()) {
            ToolResult.Success("(no files found)")
        } else {
            ToolResult.Success(files.joinToString("\n"))
        }
    }

    // ── Tool definitions for LLM ────────────────────────────────────────────

    /**
     * Returns the list of tool definitions that can be sent to the LLM
     * via [ProviderService.completeWithTools].
     */
    fun toolDefinitions(): List<ToolDefinition> = listOf(
        writeFileSync(),
        readFileSync(),
        editFileSync(),
        listFilesSync()
    )

    private fun writeFileSync(): ToolDefinition = ToolDefinition(
        type = "function",
        function = ToolFunctionSchema(
            name = "write_file",
            description = "Write content to a file. Creates the file if it does not exist, " +
                "or overwrites it if it does.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("path") {
                        put("type", "string")
                        put("description", "The file path relative to the project root")
                    }
                    putJsonObject("content") {
                        put("type", "string")
                        put("description", "The full content to write to the file")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("path"))
                    add(JsonPrimitive("content"))
                }
            }
        )
    )

    private fun readFileSync(): ToolDefinition = ToolDefinition(
        type = "function",
        function = ToolFunctionSchema(
            name = "read_file",
            description = "Read the full content of a file.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("path") {
                        put("type", "string")
                        put("description", "The file path to read")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("path"))
                }
            }
        )
    )

    private fun editFileSync(): ToolDefinition = ToolDefinition(
        type = "function",
        function = ToolFunctionSchema(
            name = "edit_file",
            description = "Replace a specific text occurrence in a file. The old_text must " +
                "match exactly (including whitespace). If old_text appears multiple times, " +
                "all occurrences are replaced.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("path") {
                        put("type", "string")
                        put("description", "The file path to edit")
                    }
                    putJsonObject("old_text") {
                        put("type", "string")
                        put("description", "The exact text to find in the file")
                    }
                    putJsonObject("new_text") {
                        put("type", "string")
                        put("description", "The replacement text")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("path"))
                    add(JsonPrimitive("old_text"))
                    add(JsonPrimitive("new_text"))
                }
            }
        )
    )

    private fun listFilesSync(): ToolDefinition = ToolDefinition(
        type = "function",
        function = ToolFunctionSchema(
            name = "list_files",
            description = "List all files in the project, optionally filtered by directory prefix.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("dir") {
                        put("type", "string")
                        put("description", "Optional directory prefix to filter files. " +
                            "If omitted, lists all files.")
                    }
                }
            }
        )
    )

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Converts a [ToolResult] to a JSON object suitable for inclusion as a
     * tool-result message in the conversation.
     */
    fun resultToJson(result: ToolResult): String = when (result) {
        is ToolResult.Success -> result.output
        is ToolResult.Error -> "ERROR [${result.code}]: ${result.error}"
        is ToolResult.UnknownTool -> "ERROR: Unknown tool '${result.toolName}'"
    }
}

// ── Tool definition types (OpenAI function-calling format) ──────────────────

/**
 * Definition of a tool that can be called by the LLM.
 * Follows the OpenAI function-calling JSON schema format.
 */
@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunctionSchema
)

@Serializable
data class ToolFunctionSchema(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

/**
 * Builder DSL for constructing JSON objects, used internally by [ToolBroker]
 * for tool parameter schemas.
 */
private class JsonBuilder {
    val content = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()

    fun put(key: String, value: String) {
        content[key] = JsonPrimitive(value)
    }

    fun put(key: String, value: kotlinx.serialization.json.JsonElement) {
        content[key] = value
    }

    fun putJsonObject(key: String, block: JsonBuilder.() -> Unit) {
        val builder = JsonBuilder()
        builder.block()
        content[key] = JsonObject(builder.content.toMap())
    }

    fun putJsonArray(key: String, block: JsonArrayBuilder.() -> Unit) {
        val builder = JsonArrayBuilder()
        builder.block()
        content[key] = kotlinx.serialization.json.JsonArray(builder.elements)
    }

    fun build(): JsonObject = JsonObject(content.toMap())
}

private class JsonArrayBuilder {
    val elements = mutableListOf<kotlinx.serialization.json.JsonElement>()

    fun add(element: kotlinx.serialization.json.JsonElement) {
        elements.add(element)
    }
}

/**
 * Convenience function to build a JsonObject using the DSL.
 */
private fun buildJsonObject(block: JsonBuilder.() -> Unit): JsonObject {
    val builder = JsonBuilder()
    builder.block()
    return builder.build()
}
