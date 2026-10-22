package io.github.dailytrack.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.ProjectEntity
import io.github.dailytrack.data.db.entity.ProjectRiskEntity
import io.github.dailytrack.engine.Premortem
import io.github.dailytrack.engine.PremortemRisk
import io.github.dailytrack.engine.RiskReview
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.navigateToHome
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProjectsScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val projects by viewModel.projects.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    ScreenList(
        title = "Projects",
        subtitle = "Start with a pre-mortem, not an optimistic plan",
        onBack = { navController.navigateToHome() },
        compact = true
    ) {
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Imagine it failed in six months. Why?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Every named risk becomes a dated prevention task, and comes back at milestones for a still-on-track check.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryCta(
                    text = "New pre-mortem",
                    icon = Icons.Default.Add,
                    onClick = { showCreate = true }
                )
            }
        }

        if (projects.isEmpty()) {
            item {
                EmptyState(
                    title = "No projects yet",
                    message = "A pre-mortem makes the failure modes visible before they happen. Name the project and the most likely reason it would fail.",
                    icon = Icons.Default.ReportProblem
                )
            }
        }

        item {
            SectionTitle(
                title = "Active projects",
                subtitle = "${projects.size} project(s)"
            )
        }

        items(projects, key = { it.id }) { project ->
            ProjectCard(
                project = project,
                viewModel = viewModel,
                onArchive = { viewModel.archiveProject(project) }
            )
        }
    }

    if (showCreate) {
        CreateProjectDialog(
            onDismiss = { showCreate = false },
            onCreate = { title, description, failure, risks, milestoneDays ->
                viewModel.createProject(title, description, failure, risks, milestoneDays)
                showCreate = false
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    viewModel: MainViewModel,
    onArchive: () -> Unit
) {
    val risks by viewModel.getProjectRisks(project.id).collectAsState(initial = emptyList())
    var expanded by remember { mutableStateOf(false) }
    var newRisk by remember { mutableStateOf("") }
    var newPrevention by remember { mutableStateOf("") }

    val reviews = risks.map {
        RiskReview(it.id, it.text, it.dueAt, it.isResolved)
    }
    val milestone = Premortem.milestoneReview(reviews, System.currentTimeMillis())
    val milestoneDue = project.milestoneAt?.let { it <= System.currentTimeMillis() } == true
    val needsCheck = milestone.dueRisks.isNotEmpty() || (milestoneDue && milestone.unresolvedCount > 0)

    SurfaceCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (project.description.isNotBlank()) {
                    Text(
                        project.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "${milestone.resolvedCount}/${risks.size} risks addressed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Default.Archive, contentDescription = "Archive project", modifier = Modifier.size(18.dp))
            }
        }

        if (needsCheck) {
            Spacer(modifier = Modifier.height(8.dp))
            SurfaceCard(
                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                contentPadding = PaddingValues(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        milestone.headline + " - still on track?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide risks" else "Show ${risks.size} risk(s)")
        }

        if (expanded) {
            if (project.failureScenario.isNotBlank()) {
                Text(
                    "Failure picture: ${project.failureScenario}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            risks.forEach { risk ->
                RiskRow(
                    risk = risk,
                    onResolve = { resolved -> viewModel.setProjectRiskResolved(risk, resolved) },
                    onDelete = { viewModel.deleteRisk(risk) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newRisk,
                onValueChange = { newRisk = it },
                label = { Text("Another way it could fail") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = newPrevention,
                onValueChange = { newPrevention = it },
                label = { Text("Prevention task (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            TextButton(
                enabled = newRisk.isNotBlank(),
                onClick = {
                    viewModel.addProjectRisk(project.id, newRisk, newPrevention, 7)
                    newRisk = ""
                    newPrevention = ""
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add risk & prevention task")
            }
        }
    }
}

@Composable
private fun RiskRow(
    risk: ProjectRiskEntity,
    onResolve: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = risk.isResolved,
            onCheckedChange = { onResolve(it) }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                risk.text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (risk.isResolved) FontWeight.Normal else FontWeight.Medium
            )
            risk.dueAt?.let {
                Text(
                    "Check by " + Instant.ofEpochMilli(it)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d")),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (it <= System.currentTimeMillis() && !risk.isResolved) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (risk.preventionTodoId != null) {
                Text(
                    "Prevention task created",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete risk", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, List<PremortemRisk>, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf("") }
    var riskText by remember { mutableStateOf("") }
    var prevention by remember { mutableStateOf("") }
    var milestoneDays by remember { mutableIntStateOf(30) }
    val risks = remember { mutableStateOf<List<PremortemRisk>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pre-mortem a project") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What is it? (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                OutlinedTextField(
                    value = failure,
                    onValueChange = { failure = it },
                    label = { Text("Imagine it failed in six months. Why?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Risks become dated prevention tasks",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                risks.value.forEachIndexed { index, risk ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "• ${risk.text}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = {
                            risks.value = risks.value.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = riskText,
                    onValueChange = { riskText = it },
                    label = { Text("Risk") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = prevention,
                    onValueChange = { prevention = it },
                    label = { Text("Prevention (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TextButton(
                    enabled = riskText.isNotBlank(),
                    onClick = {
                        risks.value = risks.value + PremortemRisk(
                            text = riskText,
                            prevention = prevention,
                            dueInDays = 7
                        )
                        riskText = ""
                        prevention = ""
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add risk")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(14, 30, 60, 90).forEach { days ->
                        TextButton(onClick = { milestoneDays = days }) {
                            Text(
                                "$days d",
                                color = if (milestoneDays == days) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val finalRisks = risks.value.toMutableList()
                    if (riskText.isNotBlank()) {
                        finalRisks += PremortemRisk(riskText, prevention = prevention, dueInDays = 7)
                    }
                    if (finalRisks.isEmpty() && failure.isNotBlank()) {
                        finalRisks += PremortemRisk(failure, dueInDays = 7)
                    }
                    onCreate(title, description, failure, finalRisks, milestoneDays)
                }
            ) { Text("Create project") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
