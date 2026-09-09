package com.altrex.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MasterSpec(
    val project: String,
    val goal: String,
    val stack: List<String> = emptyList(),
    val architecture: List<String> = emptyList(),
    val designRules: List<String> = emptyList(),
    val apiContracts: List<String> = emptyList(),
    val dataModels: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val decisions: List<String> = emptyList()
)

@Serializable
data class TaskContract(
    val id: String,
    val title: String,
    val description: String,
    val role: String,
    val priority: Int,
    val dependencies: List<String> = emptyList(),
    val allowedFiles: List<String> = emptyList(),
    val restrictedFiles: List<String> = emptyList(),
    val inputs: List<String> = emptyList(),
    val outputs: List<String> = emptyList(),
    val acceptance: List<String> = emptyList()
)

@Serializable
data class Verification(
    val passed: Boolean,
    val summary: String,
    val commands: List<VerificationCommand> = emptyList(),
    val reviewer: String,
    val checkedAt: String
)

@Serializable
data class VerificationCommand(
    val command: String,
    val exitCode: Int? = null,
    val output: String
)

@Serializable
data class SpecialistTask(
    val id: String,
    val title: String,
    val description: String,
    val role: String,
    val priority: Int,
    val dependencies: List<String> = emptyList(),
    val allowedFiles: List<String> = emptyList(),
    val restrictedFiles: List<String> = emptyList(),
    val inputs: List<String> = emptyList(),
    val outputs: List<String> = emptyList(),
    val acceptance: List<String> = emptyList(),
    val status: String,
    val model: String? = null,
    val provider: String? = null,
    val attempt: Int = 0,
    val filesChanged: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val result: String = "",
    val verification: Verification? = null,
    val error: String? = null
)

@Serializable
data class ProjectRun(
    val version: Int = 1,
    val id: String,
    val projectPath: String,
    val request: String,
    val createdAt: String,
    val updatedAt: String,
    val status: String,
    val spec: MasterSpec? = null,
    val tasks: List<SpecialistTask> = emptyList(),
    val activity: List<String> = emptyList(),
    val finalVerification: Verification? = null,
    val revisions: List<String> = emptyList(),
    val filesChanged: List<String> = emptyList(),
    val error: String? = null
)

@Serializable
data class DirectorPlan(
    val spec: MasterSpec,
    val tasks: List<TaskContract>
)
