package es.uc3m.android.a1percent.ui.screens.targets

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.ui.components.EditTaskCard
import es.uc3m.android.a1percent.ui.components.ShareBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun GoalDetailScreen(
    navController: NavController,
    goalId: String,
    viewModel: GoalDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbarMessage()
    }

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
                },
                actions = {
                    if (goal != null) {
                        IconButton(onClick = { viewModel.onShareGoalRequested() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share goal")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    GoalHeaderCard(
                        goal = goal,
                        sharedWithProfiles = uiState.sharedWithProfiles
                    )
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

        if (uiState.showShareSheet) {
            ShareBottomSheet(
                itemName = goal?.title ?: "",
                friends = uiState.friends,
                onShareWith = viewModel::onShareWithFriend,
                onDismiss = viewModel::onShareDismissed
            )
        }
    }
}

@Composable
private fun GoalHeaderCard(goal: Goal, sharedWithProfiles: List<UserProfile> = emptyList()) {
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

            if (sharedWithProfiles.isNotEmpty()) {
                HorizontalDivider()
                GoalSharedWithSection(profiles = sharedWithProfiles)
            }
        }
    }
}

@Composable
private fun GoalSharedWithSection(profiles: List<UserProfile>) {
    var expanded by remember { mutableStateOf(false) }
    val maxVisible = 3
    val visibleProfiles = if (expanded) profiles else profiles.take(maxVisible)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Shared with ${profiles.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (profiles.size > maxVisible) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = if (expanded) "Show less" else "Show all",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        visibleProfiles.forEach { profile ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = profile.name,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = profile.name.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!expanded && profiles.size > maxVisible) {
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}
