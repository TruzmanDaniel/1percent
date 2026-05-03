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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.ui.components.EditTaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun GoalDetailScreen(
    navController: NavController,
    goalId: String,
    viewModel: GoalDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                item {
                    GoalHeaderCard(goal = goal)
                }

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
                            parentGoalTitle = goal.title,
                            onTaskDetail = { viewModel.onMissionClicked(mission) },
                            onTaskComplete = { viewModel.onTaskComplete(mission.id) },
                            onTaskPostpone = { viewModel.onTaskPostpone(mission.id) },
                            onTaskEdit = { viewModel.onTaskEdit(mission) },
                            onTaskDelete = { viewModel.onTaskDelete(mission.id) }
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

        val selectedMission = uiState.selectedMission
        if (selectedMission != null) {
            TaskDetailModal(
                task = selectedMission,
                parentGoalTitle = goal?.title,
                onClose = viewModel::onCloseMissionDetail,
                onComplete = { viewModel.onTaskComplete(selectedMission.id) },
                onPostpone = { viewModel.onTaskPostpone(selectedMission.id) },
                onDelete = { viewModel.onTaskDelete(selectedMission.id) }
            )
        }

        val editingTask = uiState.editingTask
        if (editingTask != null) {
            EditTaskCard(
                task = editingTask,
                onSave = { viewModel.onTaskUpdate(it) },
                onDismiss = { viewModel.onEditDismissed() }
            )
        }

        if (uiState.showDatePickerForTask != null) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { viewModel.onDatePickerDismissed() },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDatePickerResult(
                                uiState.showDatePickerForTask!!,
                                millis / 86_400_000L
                            )
                        }
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onDatePickerDismissed() }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
private fun GoalHeaderCard(goal: Goal) {
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
            Text(text = goal.title, style = MaterialTheme.typography.headlineSmall)

            if (goal.description.isNotEmpty()) {
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

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
