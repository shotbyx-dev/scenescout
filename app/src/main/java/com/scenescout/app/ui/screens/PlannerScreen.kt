package com.scenescout.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scenescout.app.data.Spot
import com.scenescout.app.data.planner.GearItem
import com.scenescout.app.data.planner.PlannerLogic
import com.scenescout.app.data.planner.PlannerStore
import com.scenescout.app.data.planner.ScheduleBlock
import com.scenescout.app.data.planner.ShootProject
import com.scenescout.app.data.planner.ShotItem
import com.scenescout.app.ui.brand.BrandWatermark
import java.time.LocalDate
import java.time.ZoneId

/**
 * Shoot-day planner: projects with treatment notes, shot list, an
 * auto-built golden-hour day schedule, and a gear packing checklist.
 * Everything persists locally via [PlannerStore].
 */
@Composable
fun PlannerScreen(spots: List<Spot>, store: PlannerStore) {
    var projects by remember { mutableStateOf<List<ShootProject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        projects = store.load()
        selectedId = projects.firstOrNull()?.id
        loaded = true
    }
    LaunchedEffect(projects, loaded) {
        if (loaded) store.save(projects)
    }

    val updateSelected: ((ShootProject) -> ShootProject) -> Unit = { transform ->
        selectedId?.let { id ->
            projects = projects.map { if (it.id == id) transform(it) else it }
        }
    }

    val project = projects.firstOrNull { it.id == selectedId }

    Box(Modifier.fillMaxSize()) {
        BrandWatermark()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        // Project picker
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Shoot planner", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { showNewDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New project")
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(projects, key = { it.id }) { p ->
                FilterChip(
                    selected = p.id == selectedId,
                    onClick = { selectedId = p.id },
                    label = { Text(p.title.ifBlank { "Untitled" }) },
                )
            }
        }

        if (project == null) {
            Text(
                "No projects yet — tap + to plan your first shoot day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TreatmentSection(project,
                onChange = { text -> updateSelected { it.copy(treatment = text) } })
            ShotListSection(project, spots, updateSelected)
            ScheduleSection(project, spots, updateSelected)
            GearSection(project, updateSelected)
        }
    }

    if (showNewDialog) {
        NewProjectDialog(
            onDismiss = { showNewDialog = false },
            onCreate = { title, client, date ->
                val p = PlannerLogic.newProject(title, client, date)
                projects = projects + p
                selectedId = p.id
                showNewDialog = false
            },
        )
    }
    }
}

@Composable
private fun TreatmentSection(
    project: ShootProject,
    onChange: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Treatment", style = MaterialTheme.typography.titleMedium)
            if (project.client.isNotBlank() || project.shootDate.isNotBlank()) {
                Text(
                    listOf(project.client, project.shootDate)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = project.treatment,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text("Concept, story, mood, palette, references…") },
                label = { Text("Treatment notes") },
            )
        }
    }
}

@Composable
private fun ShotListSection(
    project: ShootProject,
    spots: List<Spot>,
    updateSelected: ((ShootProject) -> ShootProject) -> Unit,
) {
    var adding by remember(project.id) { mutableStateOf(false) }
    var desc by remember(project.id) { mutableStateOf("") }
    var location by remember(project.id) { mutableStateOf("") }
    var lens by remember(project.id) { mutableStateOf("") }
    var locMenu by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shot list", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { adding = !adding }) {
                    Text(if (adding) "Cancel" else "+ Add shot")
                }
            }
            if (project.shots.isEmpty() && !adding) {
                Text("No shots yet.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            project.shots.forEach { shot ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = shot.done,
                        onCheckedChange = { checked ->
                            updateSelected { p ->
                                p.copy(shots = p.shots.map {
                                    if (it.id == shot.id) it.copy(done = checked) else it
                                })
                            }
                        },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(shot.description, style = MaterialTheme.typography.bodyMedium)
                        val meta = listOf(shot.locationName, shot.lens)
                            .filter { it.isNotBlank() }.joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(meta, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = {
                        updateSelected { p -> p.copy(shots = p.shots - shot) }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove shot")
                    }
                }
            }
            if (adding) {
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("Shot description") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { locMenu = true }, modifier = Modifier.weight(1f)) {
                        Text(if (location.isBlank()) "Pick location" else location,
                            maxLines = 1)
                    }
                    DropdownMenu(expanded = locMenu, onDismissRequest = { locMenu = false }) {
                        spots.forEach { spot ->
                            DropdownMenuItem(
                                text = { Text(spot.name) },
                                onClick = { location = spot.name; locMenu = false },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Custom…") },
                            onClick = { locMenu = false },
                        )
                    }
                }
                OutlinedTextField(value = lens, onValueChange = { lens = it },
                    label = { Text("Lens / notes (optional)") },
                    modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (desc.isNotBlank()) {
                            updateSelected { p ->
                                p.copy(shots = p.shots + ShotItem(
                                    description = desc.trim(),
                                    locationName = location.trim(),
                                    lens = lens.trim(),
                                ))
                            }
                            desc = ""; location = ""; lens = ""; adding = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add to shot list") }
            }
        }
    }
}

@Composable
private fun ScheduleSection(
    project: ShootProject,
    spots: List<Spot>,
    updateSelected: ((ShootProject) -> ShootProject) -> Unit,
) {
    var adding by remember(project.id) { mutableStateOf(false) }
    var time by remember(project.id) { mutableStateOf("") }
    var title by remember(project.id) { mutableStateOf("") }
    val zone = ZoneId.of("America/New_York")

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Day schedule", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { adding = !adding }) {
                    Text(if (adding) "Cancel" else "+ Block")
                }
            }
            OutlinedButton(
                onClick = {
                    val date = project.shootDate.toLocalDateOrNull() ?: LocalDate.now()
                    val spotsByName = spots.associateBy { it.name }
                    val built = PlannerLogic.buildDaySchedule(project, spotsByName, date, zone)
                    updateSelected { p -> p.copy(schedule = built) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Auto-build from golden hours")
            }
            if (project.schedule.isEmpty() && !adding) {
                Text("No blocks yet — auto-build uses your shot locations' golden hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            project.schedule.sortedBy { it.time }.forEach { block ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(block.time, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.width(56.dp),
                        color = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(block.title, style = MaterialTheme.typography.bodyMedium)
                        if (block.note.isNotBlank()) {
                            Text(block.note, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = {
                        updateSelected { p -> p.copy(schedule = p.schedule - block) }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove block")
                    }
                }
            }
            if (adding) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = time, onValueChange = { time = it },
                        label = { Text("Time (HH:mm)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = title, onValueChange = { title = it },
                        label = { Text("Block") }, modifier = Modifier.weight(2f))
                }
                Button(
                    onClick = {
                        if (time.matches(Regex("\\d{1,2}:\\d{2}")) && title.isNotBlank()) {
                            val normalized = time.padStart(5, '0')
                            updateSelected { p ->
                                p.copy(schedule = (p.schedule +
                                    ScheduleBlock(time = normalized,
                                        title = title.trim()))
                                    .sortedBy { it.time })
                            }
                            time = ""; title = ""; adding = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add block") }
            }
        }
    }
}

@Composable
private fun GearSection(
    project: ShootProject,
    updateSelected: ((ShootProject) -> ShootProject) -> Unit,
) {
    var adding by remember(project.id) { mutableStateOf(false) }
    var name by remember(project.id) { mutableStateOf("") }
    val packed = project.gear.count { it.packed }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gear checklist", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                Text("$packed/${project.gear.size} packed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { adding = !adding }) {
                    Text(if (adding) "Cancel" else "+ Add")
                }
            }
            project.gear.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        updateSelected { p ->
                            p.copy(gear = p.gear.map {
                                if (it.id == item.id) it.copy(packed = !it.packed) else it
                            })
                        }
                    }) {
                    Checkbox(checked = item.packed, onCheckedChange = null)
                    Text(item.name, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        updateSelected { p ->
                            p.copy(gear = p.gear.filter { it.id != item.id })
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove gear")
                    }
                }
            }
            if (adding) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        label = { Text("Gear item") }, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            updateSelected { p ->
                                p.copy(gear = p.gear + GearItem(name = name.trim()))
                            }
                            name = ""; adding = false
                        }
                    }) { Text("Add") }
                }
            }
        }
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, client: String, date: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New shoot project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Project title") })
                OutlinedTextField(value = client, onValueChange = { client = it },
                    label = { Text("Client (optional)") })
                OutlinedTextField(value = date, onValueChange = { date = it },
                    label = { Text("Shoot date (yyyy-MM-dd)") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onCreate(title, client, date) },
                enabled = title.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun String.toLocalDateOrNull(): LocalDate? =
    try { LocalDate.parse(this) } catch (e: Exception) { null }
