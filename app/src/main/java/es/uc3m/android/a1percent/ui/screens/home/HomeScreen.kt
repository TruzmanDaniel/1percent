package es.uc3m.android.a1percent.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.ui.components.ShareBottomSheet
import es.uc3m.android.a1percent.navigation.AppScreens
import es.uc3m.android.a1percent.ui.components.taskDeadlineBorderColor
import es.uc3m.android.a1percent.ui.components.taskDeadlineIndicatorColor
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import es.uc3m.android.a1percent.data.TaskDeadlineResolver
import es.uc3m.android.a1percent.ui.components.TaskDeadlineColors
import es.uc3m.android.a1percent.ui.screens.targets.TaskDetailModal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val ritualGoalId = uiState.navigateToRitual
    LaunchedEffect(ritualGoalId) {
        if (ritualGoalId != null) {
            viewModel.onRitualNavigated()
            navController.navigate(AppScreens.RitualScreen.route + "/$ritualGoalId")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    HomeBodyContent(
        uiState = uiState,
        onFilterClick = viewModel::onFilterClicked,
        onTaskChecked = viewModel::onTaskChecked,
        onTaskClicked = viewModel::onTaskClicked
    )

    if (uiState.selectedTask != null) {
        TaskDetailModal(
            task = uiState.selectedTask!!,
            parentGoalTitle = uiState.selectedTask!!.goalId?.let { goalId ->
                uiState.goals.find { it.id == goalId }?.title
            },
            sharedProfiles = uiState.sharedUserProfilesById,
            currentUserId = uiState.currentUserId,
            onClose = { viewModel.dismissTaskDetail() },
            onComplete = { viewModel.onTaskChecked(uiState.selectedTask!!.id) },
            onProfileClicked = { userId ->
                navController.navigate(AppScreens.ProfileScreen.route + "/$userId")
            }
        )
    }

    if (uiState.showShareSheet && uiState.shareTargetTask != null) {
        ShareBottomSheet(
            itemName = uiState.shareTargetTask!!.title,
            friends = uiState.friends,
            onShareWith = viewModel::onShareWithFriend,
            onDismiss = viewModel::onShareDismissed
        )
    }
}

@Composable
fun HomeBodyContent(
    uiState: HomeUiState,
    onFilterClick: (HomeFilterKey) -> Unit,
    onTaskChecked: (String) -> Unit = {},
    onTaskClicked: (Task) -> Unit = {}
) {
    var isFocusMode by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (!isFocusMode) {
            item { HeaderSection(uiState = uiState) }
        } else {
            item {
                Text(
                    text = "Focus mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

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
                        contentDescription = if (isFocusMode) "Disable focus mode" else "Enable focus mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filterItems, key = { it.key }) { filter ->
                    FilterChip(
                        selected = filter.isSelected,
                        onClick = { onFilterClick(filter.key) },
                        label = {
                            Text(
                                text = filter.label,
                                fontWeight = if (filter.isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        if (uiState.visibleTasks.isEmpty()) {
            item {
                Text(
                    text = "No tasks for today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.visibleTasks) { task ->
                val goalTitle = if (task.goalId != null) {
                    uiState.goals.find { it.id == task.goalId }?.title
                } else null
                TaskItem(
                    task = task,
                    goalTitle = goalTitle,
                    currentUserId = SessionRepository.currentUser.value?.id ?: "",
                    onCheckedChange = onTaskChecked,
                    onClick = { onTaskClicked(task) }
                )
            }
        }

        if (!isFocusMode) {
            item { ProgressSection() }
        }
    }
}

private fun TaskDeadline.toUiLabel(): String {
    return when (this) {
        TaskDeadline.ThisWeek -> "This Week"
        is TaskDeadline.OnDate -> {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)
            val millisAtUtcMidnight = epochDay * 24L * 60L * 60L * 1000L
            formatter.format(Date(millisAtUtcMidnight))
        }
    }
}

@Composable
private fun HeaderSection(uiState: HomeUiState) {
    val user = uiState.user ?: return
    val xpProgress = user.currentXp / user.xpToNextLevel.toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hey ${user.name}!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Text(
                        text = "🔥 ${user.streakDays} days",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Level ${user.level}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "·",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                )
                Text(
                    text = "${user.currentXp} / ${user.xpToNextLevel} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }

            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    goalTitle: String?,
    currentUserId: String = "",
    onCheckedChange: (String) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val isCompleted = currentUserId in task.completedBy
    // val borderColor = taskDeadlineBorderColor(task.deadline)
    val indicatorColor = taskDeadlineIndicatorColor(task.deadline, isCompleted)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // border = BorderStroke(2.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onCheckedChange(task.id) }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = task.title,
                        style = if (isCompleted)
                            MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
                        else
                            MaterialTheme.typography.bodyLarge
                    )

                    task.deadline?.let { deadline ->
                        val deadlineStatus = TaskDeadlineResolver.deadlineStatus(deadline)
                        val deadlinePrefix = if (deadlineStatus == TaskDeadlineResolver.DeadlineStatus.OVERDUE) "Overdue" else "Due"
                        val deadlineColor = if (deadlineStatus == TaskDeadlineResolver.DeadlineStatus.OVERDUE) {
                            TaskDeadlineColors.Overdue
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = "$deadlinePrefix: ${deadline.toUiLabel()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = deadlineColor
                        )
                    }

                    if (goalTitle != null) {
                        Text(
                            text = "⚡ Mission · $goalTitle",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Text(
                    text = "+${task.xp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            // Indicador vertical — overlay interno, borde derecho
            if (indicatorColor != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()        // toma el tamaño del Box padre
                        .wrapContentWidth(Alignment.End)  // empuja al borde derecho
                        .width(4.dp)
                        .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                        .background(indicatorColor)
                )
            }
        }
    }
}

@Composable
private fun ProgressSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Your 1% progress",
                style = MaterialTheme.typography.titleMedium
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📈 Progress chart coming soon",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "1% better every day = 37x better in a year",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
