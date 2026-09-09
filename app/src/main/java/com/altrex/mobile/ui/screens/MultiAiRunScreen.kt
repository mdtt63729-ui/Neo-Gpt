package com.altrex.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altrex.mobile.data.model.ProjectRun
import com.altrex.mobile.data.model.SpecialistTask
import com.altrex.mobile.ui.AltrexButton
import com.altrex.mobile.ui.AltrexStatus
import com.altrex.mobile.ui.Divider
import com.altrex.mobile.ui.SectionHeader
import com.altrex.mobile.ui.StatusBadge
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Local task status used by the multi-AI run visualization.
 */
enum class TaskStatus { PENDING, RUNNING, DONE, FAILED, SKIPPED }

/** Lightweight activity-log entry. */
data class ActivityEntry(val time: String, val message: String, val kind: ActivityKind)
enum class ActivityKind { INFO, SUCCESS, WARN, ERROR }

/**
 * Full-screen visualization of an in-progress or completed multi-AI run.
 *
 * Shows a run status header, an overall progress bar, the specialist task list
 * with per-task status badges, the master spec summary, a live activity log,
 * the final verification block, and a restart button.
 */
@Composable
fun MultiAiRunScreen(
    run: ProjectRun,
    tasks: List<SpecialistTask>,
    taskStatus: Map<String, TaskStatus>,
    progress: Float,
    masterSpecSummary: String,
    activityLog: List<ActivityEntry>,
    finalVerification: FinalVerification?,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = run.status
    val running = status.equals("running", ignoreCase = true)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AltrexColors.bgApp)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------- Run status header ----------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (running) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AltrexColors.accent
                        )
                    } else {
                        Icon(
                            if (status.equals("completed", ignoreCase = true)) Icons.Outlined.CheckCircle
                            else Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = if (status.equals("completed", ignoreCase = true)) AltrexColors.success else AltrexColors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            run.name.ifBlank { "Multi-AI Run" },
                            color = AltrexColors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            run.id,
                            color = AltrexColors.textMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                StatusBadge(
                    label = status.replaceFirstChar { it.uppercase() },
                    status = when {
                        status.equals("completed", ignoreCase = true) -> AltrexStatus.SUCCESS
                        status.equals("running", ignoreCase = true) -> AltrexStatus.ACTIVE
                        status.equals("failed", ignoreCase = true) -> AltrexStatus.DANGER
                        else -> AltrexStatus.NEUTRAL
                    }
                )
            }
        }

        // ---------- Progress bar ----------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Overall progress", color = AltrexColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(progress * 100).toInt()}%", color = AltrexColors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = AltrexColors.accent,
                    trackColor = AltrexColors.bgSurface
                )
            }
        }

        // ---------- Task list ----------
        item { SectionHeader(title = "Tasks") }
        items(tasks, key = { it.id }) { task ->
            TaskCard(task, taskStatus[task.id] ?: TaskStatus.PENDING)
        }

        // ---------- Master spec summary ----------
        item {
            SectionHeader(title = "Master spec")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Text(
                    masterSpecSummary.ifBlank { "No master spec captured for this run." },
                    color = AltrexColors.textPrimary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }

        // ---------- Activity log ----------
        item { SectionHeader(title = "Activity log") }
        items(activityLog.size) { index ->
            val entry = activityLog[index]
            ActivityRow(entry)
        }

        // ---------- Final verification ----------
        item {
            SectionHeader(title = "Final verification")
            FinalVerificationCard(finalVerification)
        }

        // ---------- Restart ----------
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AltrexButton(
                    text = "Restart run",
                    onClick = onRestart,
                    primary = true,
                    icon = Icons.Outlined.Refresh
                )
            }
        }
    }
}

@Composable
private fun TaskCard(task: SpecialistTask, status: TaskStatus) {
    val (badge, icon) = taskStatusVisual(status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
            .border(1.dp, AltrexColors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = badge.first, modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.width(0.dp).weight(1f)) {
                Text(
                    task.title.ifBlank { task.id },
                    color = AltrexColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                task.specialist?.let {
                    Text(it, color = AltrexColors.textMuted, fontSize = 11.sp)
                }
            }
        }
        StatusBadge(label = status.name.lowercase().replaceFirstChar { c -> c.uppercase() }, status = badge.second)
    }
}

@Composable
private fun ActivityRow(entry: ActivityEntry) {
    val (color, dot) = when (entry.kind) {
        ActivityKind.SUCCESS -> AltrexColors.success to "✓"
        ActivityKind.WARN -> AltrexColors.warning to "!"
        ActivityKind.ERROR -> AltrexColors.danger to "✕"
        ActivityKind.INFO -> AltrexColors.textMuted to "•"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(entry.time, color = AltrexColors.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(dot, color = color, fontSize = 11.sp)
        Text(entry.message, color = AltrexColors.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FinalVerificationCard(verification: FinalVerification?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AltrexColors.bgSurface, RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (verification?.passed == true) AltrexColors.success else AltrexColors.borderSubtle,
                RoundedCornerShape(8.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (verification == null) {
            Text("Verification has not run yet.", color = AltrexColors.textMuted, fontSize = 12.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (verification.passed) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = if (verification.passed) AltrexColors.success else AltrexColors.danger,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (verification.passed) "All checks passed" else "Verification found issues",
                    color = AltrexColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            verification.notes?.forEach { note ->
                Text("• $note", color = AltrexColors.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

data class FinalVerification(val passed: Boolean, val notes: List<String>? = null)

private fun taskStatusVisual(status: TaskStatus): Pair<Pair<androidx.compose.ui.graphics.Color, AltrexStatus>, ImageVector> {
    val (color, altrex) = when (status) {
        TaskStatus.DONE -> AltrexColors.success to AltrexStatus.SUCCESS
        TaskStatus.RUNNING -> AltrexColors.accent to AltrexStatus.ACTIVE
        TaskStatus.FAILED -> AltrexColors.danger to AltrexStatus.DANGER
        TaskStatus.SKIPPED -> AltrexColors.warning to AltrexStatus.WARNING
        TaskStatus.PENDING -> AltrexColors.textMuted to AltrexStatus.NEUTRAL
    }
    val icon = when (status) {
        TaskStatus.DONE -> Icons.Outlined.CheckCircle
        TaskStatus.RUNNING -> Icons.Outlined.PlayArrow
        TaskStatus.FAILED -> Icons.Outlined.ErrorOutline
        TaskStatus.SKIPPED -> Icons.Outlined.Schedule
        TaskStatus.PENDING -> Icons.Outlined.Schedule
    }
    return (color to altrex) to icon
}
