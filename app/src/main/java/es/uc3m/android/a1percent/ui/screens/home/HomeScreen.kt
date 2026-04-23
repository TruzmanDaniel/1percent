package es.uc3m.android.a1percent.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.TaskDeadlineResolver
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    // Observing the screen state (DATA) to update the UI when it changes
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeBodyContent(uiState = uiState)
}

@Composable
fun HomeBodyContent(
    uiState: HomeUiState
) {
    var isFocusMode by rememberSaveable { mutableStateOf(false) }

    // FILTERS & SORTING: BASIC IMPLEMENTATION
        // TODO: extract functionality (we will create more filters) into a modular approach.
        //  home/HomeFilters.kt with functionalities, save HomeFilterState in HomeViewModel,
        //     create reusable components (FilterBar, SortMenu...)...

    var showOnlyMissions by rememberSaveable { mutableStateOf(false) }
    var sortByDate by rememberSaveable { mutableStateOf(false) }
        // Control which tasks are shown (filtered)
    val visibleTasks = uiState.tasks
        .let { tasks -> if (showOnlyMissions) tasks.filter { task -> task.goalId != null } else tasks }
        .let { tasks -> if (sortByDate) tasks.sortedBy { task -> TaskDeadlineResolver.toSortKey(task.deadline) } else tasks }

    // Scrollable Column containing the elements
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {

    // 1. HEADER SECTION

            // FocusMode hides Header Section
        if (!isFocusMode) {
            item {
                HeaderSection(uiState = uiState)
            }
        }
        else {
            item {
                Text(
                    text = "Focus mode enabled",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

    // 2. TASKS SECTION

        // TODO: Add task filters and special quick-action buttons


        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today's missions",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { isFocusMode = !isFocusMode }) {
                    Icon(
                        imageVector = if (isFocusMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isFocusMode) "Disable focus mode" else "Enable focus mode"
                    )
                }
            }
        }

        // FILTERS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { showOnlyMissions = !showOnlyMissions }) {
                    Text("Missions")
                }
                Button(onClick = { sortByDate = !sortByDate }) {
                    Text("Sort by Date")
                }
            }
        }

        if (visibleTasks.isEmpty()) {
            item {
                Text(
                    text = "No tasks for today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // TODO: CAMBIAR uiState, ahora solo maneja un GOAL! + logica de extraer goal title del task.goalId
            items(visibleTasks) { task ->
                val goalTitle = if (task.goalId != null) uiState.goal?.title else null
                TaskItem(task = task, goalTitle = goalTitle)
            }
        }

    // 3. PROGRESS SECTION
            // FocusMode hides this section
        if (!isFocusMode) {
            item {
                ProgressSection()
            }
        }
    }
}

private fun TaskDeadline.toUiLabel(): String {
    return when (this) {
        TaskDeadline.ThisWeek -> "This Week"
        // Format Date when it is an specific date
        is TaskDeadline.OnDate -> {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)
            val millisAtUtcMidnight = epochDay * 24L * 60L * 60L * 1000L
            formatter.format(Date(millisAtUtcMidnight))
        }
    }
}


// Private composables (SECTIONS, ITEMS)
@Composable
private fun HeaderSection(uiState: HomeUiState) {
    val user = uiState.user ?: return
    val xpProgress = user.currentXp / user.xpToNextLevel.toFloat()

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Hey ${user.name}!",
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Level ${user.level}  ·  ${user.currentXp} / ${user.xpToNextLevel} XP",
                style = MaterialTheme.typography.bodyMedium 
            )
        }

        LinearProgressIndicator(
            progress = { xpProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )

        Text(
            text = "🔥 ${user.streakDays} day streak",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun TaskItem(task: Task, goalTitle: String?) {
    val isCompleted = task.status == TaskStatus.COMPLETED

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isCompleted,
            onCheckedChange = null   // non-interactive for now TODO: check complete tasks
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = task.title,
                style = if (isCompleted)
                    MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
                else
                    MaterialTheme.typography.bodyLarge
            )

            task.deadline?.let { deadline ->
                Text(
                    text = "Due: ${deadline.toUiLabel()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (goalTitle != null) {
                Text(
                    text = "⚡ Mission · $goalTitle",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = "+${task.xp} XP",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ProgressSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Your 1% progress",
            style = MaterialTheme.typography.titleMedium
        )

        Box(
            modifier = Modifier.fillMaxWidth().height(150.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("📈 Progress chart coming soon")
        }

        Text(
            text = "1% better every day = 37x better in a year",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
