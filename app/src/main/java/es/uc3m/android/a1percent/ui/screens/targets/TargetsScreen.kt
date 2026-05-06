package es.uc3m.android.a1percent.ui.screens.targets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.DropdownMenuItem
import es.uc3m.android.a1percent.data.TaskCategoryRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.R
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import es.uc3m.android.a1percent.data.model.weeksRemaining
import androidx.compose.material3.LinearProgressIndicator
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.navigation.AppScreens
import es.uc3m.android.a1percent.ui.components.CollaboratorAvatars
import es.uc3m.android.a1percent.ui.components.EditTaskCard
import es.uc3m.android.a1percent.ui.components.ShareBottomSheet
import es.uc3m.android.a1percent.ui.components.SharedWithDropdown
import es.uc3m.android.a1percent.ui.components.taskDeadlineBorderColor
import es.uc3m.android.a1percent.ui.components.taskDeadlineIndicatorColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun TargetsScreen(
    navController: NavController,
    viewModel: TargetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearErrorMessage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TargetsBodyContent(
            uiState = uiState,
            navController = navController,
            onTabSelected = viewModel::onTabSelected,
            onTaskStatusFilterClicked = viewModel::onTaskStatusFilterClicked,
            onTaskFilterClicked = viewModel::onTaskFilterClicked,
            onTaskCategoryClick = viewModel::onTaskCategoryClick,
            onGoalFilterClicked = viewModel::onGoalFilterClicked,
            onGoalClicked = { goalId ->
                navController.navigate("targets/goal/$goalId")
            },
            onTaskClicked = viewModel::onTaskClicked,
            onCloseTaskDetail = viewModel::onCloseTaskDetail,
            onTaskComplete = viewModel::onTaskComplete,
            onTaskDelete = viewModel::onTaskDelete,
            onTaskEdit = viewModel::onTaskEdit,
            onTaskShare = viewModel::onShareTaskRequested,
            onGoalDelete = viewModel::onGoalDeleteRequested
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // Snackbar for share success
        LaunchedEffect(uiState.snackbarMessage) {
            val message = uiState.snackbarMessage ?: return@LaunchedEffect
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbarMessage()
        }

        // Edit task dialog
        val editingTask = uiState.editingTask
        if (editingTask != null) {
            EditTaskCard(
                task = editingTask,
                onSave = { viewModel.onTaskUpdate(it) },
                onDismiss = { viewModel.onEditDismissed() }
            )
        }

        // Share bottom sheet
        if (uiState.showShareSheet) {
            val itemName = uiState.shareTargetTask?.title ?: uiState.shareTargetGoal?.title ?: ""
            ShareBottomSheet(
                itemName = itemName,
                friends = uiState.friends,
                onShareWith = { userId, name -> viewModel.onShareWithFriend(userId, name) },
                onDismiss = { viewModel.onShareDismissed() }
            )
        }

        // Goal delete confirmation
        val deleteGoalId = uiState.showDeleteGoalConfirm
        if (deleteGoalId != null) {
            AlertDialog(
                onDismissRequest = { viewModel.onGoalDeleteDismissed() },
                title = { Text(stringResource(R.string.targets_delete_goal_title)) },
                text = { Text(stringResource(R.string.targets_delete_goal_message)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onGoalDeleteConfirmed() }) { Text(stringResource(R.string.targets_delete_goal_confirm), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onGoalDeleteDismissed() }) { Text(stringResource(R.string.targets_cancel)) }
                }
            )
        }
        // Category selector dialog
        if (uiState.showCategorySelector) {
            CategorySelectorDialog(
                uiState = uiState,
                onCategorySelected = { label -> viewModel.onCategorySelected(label) },
                onDismiss = { viewModel.onCategorySelectorDismissed() }
            )
        }
    }
}

// Category selector dialog (glass style) shown when TargetsUiState.showCategorySelector == true
@Composable
private fun CategorySelectorDialog(
    uiState: TargetsUiState,
    onCategorySelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    // Collect categories
    val customCategories by TaskCategoryRepository.customCategories.collectAsStateWithLifecycle()
    val predefined = TaskCategoryRepository.predefinedCategories

    Dialog(onDismissRequest = onDismiss) {
        val glassShape = RoundedCornerShape(20.dp)
        val glassGradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = glassShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier
                .background(brush = glassGradient)
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.targets_category_selector_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))

                // All option
                DropdownMenuItem(text = { Text(stringResource(R.string.targets_category_all)) }, onClick = { onCategorySelected(null) })

                // Predefined
                predefined.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat.displayName) }, onClick = { onCategorySelected(cat.displayName) })
                }

                if (customCategories.isNotEmpty()) {
                    HorizontalDivider()
                    customCategories.forEach { custom ->
                        DropdownMenuItem(text = { Text(custom) }, onClick = { onCategorySelected(custom) })
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun TargetsBodyContent(
    uiState: TargetsUiState,
    navController: NavController,
    onTabSelected: (TargetsTab) -> Unit,
    onTaskStatusFilterClicked: () -> Unit,
    onTaskFilterClicked: (TaskFilterKey) -> Unit,
    onTaskCategoryClick: () -> Unit,
    onGoalFilterClicked: (GoalFilterKey) -> Unit,
    onGoalClicked: (String) -> Unit,
    onTaskClicked: (Task) -> Unit,
    onCloseTaskDetail: () -> Unit,
    onTaskComplete: (String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskEdit: (Task) -> Unit,
    onTaskShare: (Task) -> Unit,
    onGoalDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        TabRow(selectedTabIndex = if (uiState.selectedTab == TargetsTab.TASKS) 0 else 1) {
            Tab(
                selected = uiState.selectedTab == TargetsTab.TASKS,
                onClick = { onTabSelected(TargetsTab.TASKS) },
                text = { Text(stringResource(R.string.targets_tab_tasks)) }
            )
            Tab(
                selected = uiState.selectedTab == TargetsTab.GOALS,
                onClick = { onTabSelected(TargetsTab.GOALS) },
                text = { Text(stringResource(R.string.targets_tab_goals)) }
            )
        }

        when (uiState.selectedTab) {
            TargetsTab.TASKS -> TasksTabContent(
                uiState = uiState,
                onTaskStatusFilterClicked = onTaskStatusFilterClicked,
                onTaskFilterClicked = onTaskFilterClicked,
                onTaskCategoryClick = onTaskCategoryClick,
                onTaskClicked = onTaskClicked,
                onTaskComplete = onTaskComplete,
                onTaskDelete = onTaskDelete,
                onTaskEdit = onTaskEdit,
                onTaskShare = onTaskShare
            )

            TargetsTab.GOALS -> GoalsTabContent(
                uiState = uiState,
                onGoalFilterClicked = onGoalFilterClicked,
                onGoalClicked = onGoalClicked,
                onGoalDelete = onGoalDelete
            )
        }

        // Task Detail Modal
        val selectedTask = uiState.selectedTask
        if (selectedTask != null) {
            TaskDetailModal(
                task = selectedTask,
                parentGoalTitle = selectedTask.goalId?.let { uiState.goalTitleById[it] },
                sharedProfiles = uiState.sharedUserProfilesById,
                currentUserId = uiState.currentUserId,
                onClose = onCloseTaskDetail,
                onComplete = { onTaskComplete(selectedTask.id) },
                onDelete = { onTaskDelete(selectedTask.id) },
                onProfileClicked = { userId ->
                    navController.navigate(AppScreens.ProfileScreen.route + "/$userId")
                }
            )
        }
    }
}

@Composable
private fun TasksTabContent(
    uiState: TargetsUiState,
    onTaskStatusFilterClicked: () -> Unit,
    onTaskFilterClicked: (TaskFilterKey) -> Unit,
    onTaskCategoryClick: () -> Unit,
    onTaskClicked: (Task) -> Unit,
    onTaskComplete: (String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskEdit: (Task) -> Unit,
    onTaskShare: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.targets_tasks_browse_subtitle),
                style = MaterialTheme.typography.titleSmall
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    onClick = onTaskStatusFilterClicked,
                    label = { Text(stringResource(statusFilterLabel(uiState.taskFilters.selectedStatus))) },
                    selected = uiState.taskFilters.selectedStatus != null
                )

                // TODO: Placeholder filter controls. Replace with advanced filter sheet/dropdowns.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.taskFilterItems.forEach { filter ->
                        FilterChip(
                            onClick = { onTaskFilterClicked(filter.key) },
                            label = { Text(stringResource(filter.labelRes)) },
                            selected = filter.isSelected
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        onClick = { onTaskCategoryClick() },
                        label = { Text(uiState.taskFilters.categoryLabel) },
                        selected = uiState.taskFilters.categoryLabel != "Category"
                    )
                    FilterChip(
                        onClick = { onTaskFilterClicked(TaskFilterKey.SORT) },
                        label = { Text(stringResource(uiState.taskFilters.sort.labelRes)) },
                        selected = uiState.taskFilters.sort != TaskSort.NONE
                    )
                }
            }
        }

        items(items = uiState.tasks, key = { it.id }) { task ->
            TaskRowWithActions(
                task = task,
                parentGoalTitle = task.goalId?.let { uiState.goalTitleById[it] },
                friends = uiState.friends,
                currentUserId = SessionRepository.currentUser.value?.id ?: "",
                currentUserProfile = uiState.currentUserProfile,
                onTaskDetail = { onTaskClicked(task) },
                onTaskComplete = { onTaskComplete(task.id) },
                onTaskDelete = { onTaskDelete(task.id) },
                onTaskEdit = { onTaskEdit(task) },
                onTaskShare = { onTaskShare(task) }
            )
        }
    }
}

@Composable
private fun GoalsTabContent(
    uiState: TargetsUiState,
    onGoalFilterClicked: (GoalFilterKey) -> Unit,
    onGoalClicked: (String) -> Unit,
    onGoalDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.targets_goals_browse_subtitle),
                style = MaterialTheme.typography.titleSmall
            )
        }

        item {
            // TODO: Placeholder for future goal sorting/filtering controls.
            uiState.goalFilterItems.forEach { filter ->
                FilterChip(
                    onClick = { onGoalFilterClicked(filter.key) },
                    label = { Text(stringResource(filter.labelRes)) },
                    selected = filter.isSelected
                )
            }
        }

        items(items = uiState.goals, key = { it.id }) { goal ->
            GoalCompactItem(
                goal = goal,
                friends = uiState.friends,
                currentUserId = uiState.currentUserId,
                onClick = { onGoalClicked(goal.id) },
                onDelete = { onGoalDelete(goal.id) }
            )
        }
    }
}

/**
 * Reused in both Tasks tab and GoalDetailScreen for consistency.
 * TODO: Replace visible action buttons with swipe-reveal interaction once gesture handling is implemented.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun TaskRowWithActions(
    task: Task,
    parentGoalTitle: String? = null,
    friends: List<UserProfile> = emptyList(),
    currentUserId: String = "",
    currentUserProfile: UserProfile? = null,
    onTaskDetail: () -> Unit = {},
    onTaskComplete: () -> Unit,
    onTaskDelete: () -> Unit,
    onTaskEdit: () -> Unit = {},
    onTaskShare: () -> Unit = {}
) {
    // val borderColor = taskDeadlineBorderColor(task.deadline)
    val isCompletedByMe = currentUserId in task.completedBy
    val indicatorColor = taskDeadlineIndicatorColor(task.deadline, isCompletedByMe)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTaskDetail),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        // border = BorderStroke(2.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isCompletedByMe = currentUserId in task.completedBy

                // Title + XP pill badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "+${task.xp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Type badge + goal name (missions) + status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DESPUÉS
                    if (parentGoalTitle != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.targets_task_badge_mission),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.targets_task_badge_task),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (parentGoalTitle != null) {
                        Text(
                            text = "· $parentGoalTitle",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    val statusColor = if (isCompletedByMe) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isCompletedByMe) stringResource(R.string.targets_task_status_completed) else stringResource(R.string.targets_task_status_pending),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                CollaboratorAvatars(
                    sharedWith = task.sharedWith,
                    currentUserId = currentUserId,
                    friends = friends
                )

                if (task.completedBy.isNotEmpty() && task.sharedWith.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.targets_task_completed_by),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CollaboratorAvatars(
                            sharedWith = task.completedBy,
                            currentUserId = "",
                            friends = friends + listOfNotNull(currentUserProfile)
                        )
                    }
                }

                HorizontalDivider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = onTaskComplete,
                        label = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = if (isCompletedByMe) stringResource(R.string.targets_task_mark_pending_content_description) else stringResource(R.string.targets_task_complete_content_description),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isCompletedByMe) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            labelColor = if (isCompletedByMe) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = onTaskEdit,
                        label = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.targets_task_edit_content_description),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (task.goalId == null) {
                        AssistChip(
                            onClick = onTaskShare,
                            label = {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(R.string.targets_task_share_content_description),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AssistChip(
                        onClick = onTaskDelete,
                        label = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.targets_task_delete_content_description),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
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
private fun GoalCompactItem(
    goal: Goal,
    friends: List<UserProfile>,
    currentUserId: String,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val isPaused = goal.aiRoadmapStatus == AiRoadmapStatus.PAUSED
    val alpha = if (isPaused) 0.5f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = alpha)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(goal.category.displayName, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
                val statusColor = when (goal.status) {
                    GoalStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    GoalStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
                    GoalStatus.ARCHIVED -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.tertiary
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = goal.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.targets_goal_delete_content_description), modifier = Modifier.size(16.dp))
                }
            }

            Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.targets_goal_progress_label), style = MaterialTheme.typography.labelSmall)
                    Text("${goal.progress}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                LinearProgressIndicator(
                    progress = { goal.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            val weeksLeft = goal.weeksRemaining()
            Text(
                stringResource(R.string.targets_goal_weeks_remaining, weeksLeft),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Task Detail Modal — redesigned
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDetailModal(
    task: Task,
    parentGoalTitle: String?,
    sharedProfiles: Map<String, UserProfile> = emptyMap(),
    currentUserId: String = "",
    onClose: () -> Unit,
    onComplete: () -> Unit = {},
    onDelete: () -> Unit = {},
    onProfileClicked: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {

            // ── HEADER CARD ──────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Task/Mission badge + Status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (parentGoalTitle != null) stringResource(R.string.targets_task_badge_mission) else stringResource(R.string.targets_task_badge_task),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        val isCompletedByMe = currentUserId in task.completedBy
                        val statusColor = if (isCompletedByMe) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = if (isCompletedByMe) stringResource(R.string.targets_task_status_completed) else stringResource(R.string.targets_task_status_pending),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Title
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Parent goal row (missions only)
                    if (parentGoalTitle != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = parentGoalTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── STATS ROW ────────────────────────────────────────────
            // XP | Difficulty (dots) | Energy  OR  Type (if no energy)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.EmojiEvents,
                    label = stringResource(R.string.targets_task_detail_xp_reward),
                    value = "+${task.xp}",
                    valueColor = MaterialTheme.colorScheme.primary
                )

                // Difficulty with dot indicators
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.targets_task_detail_difficulty),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index < task.difficulty)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                        )
                                )
                            }
                        }
                    }
                }

                if (task.energyCost != null) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bolt,
                        label = stringResource(R.string.targets_task_detail_energy),
                        value = "${task.energyCost}",
                        valueColor = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Loop,
                        label = stringResource(R.string.targets_task_detail_type),
                        value = task.type.displayName,
                        valueColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── DESCRIPTION ──────────────────────────────────────────
            if (task.description.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.targets_task_detail_description),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── DETAILS ──────────────────────────────────────────────
            // Type (only if energy was in the stats row), Deadline, Category
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (task.energyCost != null) {
                    DetailRow(
                        icon = Icons.Default.Loop,
                        label = stringResource(R.string.targets_task_detail_type),
                        value = task.type.displayName
                    )
                }
                if (task.deadline != null) {
                    DetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = stringResource(R.string.targets_task_detail_deadline),
                        value = formatDeadline(task.deadline)
                    )
                }
                DetailRow(
                    icon = Icons.AutoMirrored.Default.Label,
                    label = stringResource(R.string.targets_task_detail_category),
                    value = task.customCategoryName ?: task.category.displayName
                )

                // Shared with section (only if task is shared)
                val otherSharedUsers = task.sharedWith.filter { it != currentUserId }
                if (otherSharedUsers.isNotEmpty()) {
                    val sharedUsersList = otherSharedUsers.mapNotNull { userId ->
                        sharedProfiles[userId]
                    }
                    if (sharedUsersList.isNotEmpty()) {
                        SharedWithDropdown(
                            sharedProfiles = sharedUsersList,
                            currentUserId = currentUserId,
                            onProfileClicked = { userId ->
                                onClose()
                                onProfileClicked(userId)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // ── ACTION BUTTONS ───────────────────────────────────────
            val isCompletedByMe = currentUserId in task.completedBy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isCompletedByMe) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (isCompletedByMe) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isCompletedByMe) stringResource(R.string.targets_task_status_completed) else stringResource(R.string.targets_task_detail_button_complete))
                }
                OutlinedButton(
                    onClick = { onDelete(); onClose() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.targets_task_detail_button_delete))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDeadline(deadline: TaskDeadline): String {
    return when (deadline) {
        TaskDeadline.ThisWeek -> "This Week"
        is TaskDeadline.OnDate -> {
            val cal = java.util.Calendar.getInstance().also {
                it.timeInMillis = deadline.epochDay * 86_400_000L
            }
            val months = arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val month = months[cal.get(java.util.Calendar.MONTH)]
            val year = cal.get(java.util.Calendar.YEAR)
            "$day $month $year"
        }
    }
}
