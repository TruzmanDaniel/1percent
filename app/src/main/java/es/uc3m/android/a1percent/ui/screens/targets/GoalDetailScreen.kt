package es.uc3m.android.a1percent.ui.screens.targets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.model.Task

/**
 * Goal Detail Screen: Shows goal info and its related missions (Tasks with goalId).
 * Missions are displayed as task rows with action buttons (Complete, Postpone, Delete).
 * TODO: Replace visible action buttons with swipe-reveal interaction in future iteration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun GoalDetailScreen(
    navController: NavController,
    goalId: String,
    viewModel: GoalDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Find the goal in the list (mock data lookup)
    val goal = uiState.goal
    val missions = uiState.missions

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(goal?.title ?: "Goal Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (goal != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Goal Header
                item {
                    GoalHeaderCard(goal = goal)
                }

                // Missions Section
                if (missions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Missions (${missions.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    items(items = missions, key = { it.id }) { mission ->
                        TaskRowWithActions(
                            task = mission,
                            onComplete = { viewModel.onTaskComplete(mission.id) },
                            onPostpone = { viewModel.onTaskPostpone(mission.id) },
                            onDelete = { viewModel.onTaskDelete(mission.id) }
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "No missions yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Goal not found
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Goal not found", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun GoalHeaderCard(goal: es.uc3m.android.a1percent.data.model.Goal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(text = goal.title, style = MaterialTheme.typography.headlineSmall)

            // Description
            if (goal.description.isNotEmpty()) {
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Category and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Category", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = goal.category.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Status", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = goal.status.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Progress
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Progress", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${goal.progress}%", style = MaterialTheme.typography.labelSmall)
                }
                LinearProgressIndicator(
                    progress = { goal.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }

            // XP and Difficulty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "XP Reward", style = MaterialTheme.typography.labelSmall)
                    Text(text = "+${goal.xp} XP", style = MaterialTheme.typography.bodyMedium)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Difficulty", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${goal.difficulty}/5", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * TaskRowWithActions: Reusable task row with visible action buttons.
 * Shows task info in a card with Complete, Postpone, and Delete buttons below.
 * TODO: Replace this with swipe-reveal interaction once gesture handling is optimized.
 */
@Composable
private fun TaskRowWithActions(
    task: Task,
    onComplete: () -> Unit,
    onPostpone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Task Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Status: ${task.status.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(text = "+${task.xp} XP", style = MaterialTheme.typography.labelMedium)
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = onComplete,
                    label = { Text("Complete") },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onPostpone,
                    label = { Text("Postpone") },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onDelete,
                    label = { Text("Delete") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
