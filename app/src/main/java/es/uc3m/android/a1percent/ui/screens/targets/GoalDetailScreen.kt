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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.intensityDisplay
import es.uc3m.android.a1percent.data.model.isFinite
import es.uc3m.android.a1percent.data.model.streakDisplay
import es.uc3m.android.a1percent.data.model.weeksRemaining
import es.uc3m.android.a1percent.navigation.AppScreens
import es.uc3m.android.a1percent.ui.components.EditTaskCard
import es.uc3m.android.a1percent.ui.components.ShareBottomSheet
import es.uc3m.android.a1percent.ui.components.SharedWithDropdown

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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbarMessage()
    }

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
                        IconButton(onClick = { viewModel.onTogglePause() }) {
                            Icon(
                                if (goal.aiRoadmapStatus == AiRoadmapStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (goal.aiRoadmapStatus == AiRoadmapStatus.PAUSED) "Reanudar" else "Pausar"
                            )
                        }
                        IconButton(onClick = { viewModel.onShareGoalRequested() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share goal")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                        friends = uiState.friends,
                        currentUserId = SessionRepository.currentUser.value?.id ?: "",
                        onProfileClicked = { userId ->
                            navController.navigate(AppScreens.ProfileScreen.route + "/$userId")
                        }
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
                            friends = uiState.friends,
                            currentUserId = SessionRepository.currentUser.value?.id ?: "",
                            currentUserProfile = SessionRepository.currentUser.value,
                            onTaskDetail = { viewModel.onMissionClicked(mission) },
                            onTaskComplete = { viewModel.onTaskComplete(mission.id) },
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
        if (selectedMission != null && goal != null) {
            TaskDetailModal(
                task = selectedMission,
                parentGoalTitle = goal.title,
                sharedProfiles = goal.sharedWith.mapNotNull { userId ->
                    uiState.friends.find { it.id == userId }
                }.associateBy { it.id },
                currentUserId = SessionRepository.currentUser.value?.id ?: "",
                onClose = viewModel::onCloseMissionDetail,
                onComplete = { viewModel.onTaskComplete(selectedMission.id) },
                onDelete = { viewModel.onTaskDelete(selectedMission.id) },
                onProfileClicked = { userId ->
                    navController.navigate(AppScreens.ProfileScreen.route + "/$userId")
                }
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

        if (uiState.showShareSheet && goal != null) {
            ShareBottomSheet(
                itemName = goal.title,
                friends = uiState.friends,
                onShareWith = { userId, name -> viewModel.onShareWithFriend(userId, name) },
                onDismiss = { viewModel.onShareDismissed() }
            )
        }
    }
}

@Composable
private fun GoalHeaderCard(
    goal: Goal,
    friends: List<UserProfile>,
    currentUserId: String,
    onProfileClicked: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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

            if (goal.isFinite) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Progreso", style = MaterialTheme.typography.labelSmall)
                        Text(text = "${goal.progress}%", style = MaterialTheme.typography.labelSmall)
                    }
                    LinearProgressIndicator(
                        progress = { goal.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatColumn("Progreso", "${goal.progress}%")
                    StatColumn("Semanas", "${goal.weeksRemaining() ?: 0}")
                    StatColumn("Intensidad", "%.1f".format(goal.currentIntensity))
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatColumn("Nivel", goal.intensityDisplay() ?: "1")
                    StatColumn("Racha", goal.streakDisplay() ?: "0 sem")
                    StatColumn("Misiones", "—")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Category", style = MaterialTheme.typography.labelSmall)
                    Text(text = goal.category.displayName, style = MaterialTheme.typography.bodyMedium)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Status", style = MaterialTheme.typography.labelSmall)
                    Text(text = goal.status.displayName, style = MaterialTheme.typography.bodyMedium)
                }
            }

            SharedWithDropdown(
                sharedProfiles = goal.sharedWith.mapNotNull { userId -> friends.find { it.id == userId } },
                currentUserId = currentUserId,
                onProfileClicked = onProfileClicked
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
