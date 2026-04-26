package es.uc3m.android.a1percent.ui.screens.targets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import androidx.compose.ui.unit.dp

@Composable
@Suppress("UNUSED_PARAMETER")
fun TargetsScreen(
    navController: NavController,
    viewModel: TargetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TargetsBodyContent(
        uiState = uiState,
        navController = navController,
        onTabSelected = viewModel::onTabSelected,
        onTaskFilterClicked = viewModel::onTaskFilterClicked,
        onGoalFilterClicked = viewModel::onGoalFilterClicked,
        onGoalClicked = { goalId ->
            navController.navigate("targets/goal/$goalId")
        },
        onTaskClicked = viewModel::onTaskClicked,
        onCloseTaskDetail = viewModel::onCloseTaskDetail,
        onTaskComplete = viewModel::onTaskComplete,
        onTaskPostpone = viewModel::onTaskPostpone,
        onTaskDelete = viewModel::onTaskDelete
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun TargetsBodyContent(
    uiState: TargetsUiState,
    navController: NavController,
    onTabSelected: (TargetsTab) -> Unit,
    onTaskFilterClicked: (TaskFilterKey) -> Unit,
    onGoalFilterClicked: (GoalFilterKey) -> Unit,
    onGoalClicked: (String) -> Unit,
    onTaskClicked: (Task) -> Unit,
    onCloseTaskDetail: () -> Unit,
    onTaskComplete: (String) -> Unit,
    onTaskPostpone: (String) -> Unit,
    onTaskDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        TabRow(selectedTabIndex = if (uiState.selectedTab == TargetsTab.TASKS) 0 else 1) {
            Tab(
                selected = uiState.selectedTab == TargetsTab.TASKS,
                onClick = { onTabSelected(TargetsTab.TASKS) },
                text = { Text("Tasks") }
            )
            Tab(
                selected = uiState.selectedTab == TargetsTab.GOALS,
                onClick = { onTabSelected(TargetsTab.GOALS) },
                text = { Text("Goals") }
            )
        }

        when (uiState.selectedTab) {
            TargetsTab.TASKS -> TasksTabContent(
                uiState = uiState,
                onTaskFilterClicked = onTaskFilterClicked,
                onTaskClicked = onTaskClicked,
                onTaskComplete = onTaskComplete,
                onTaskPostpone = onTaskPostpone,
                onTaskDelete = onTaskDelete
            )

            TargetsTab.GOALS -> GoalsTabContent(
                uiState = uiState,
                onGoalFilterClicked = onGoalFilterClicked,
                onGoalClicked = onGoalClicked
            )
        }

        // Task Detail Modal
        if (uiState.selectedTask != null) {
            TaskDetailModal(
                task = uiState.selectedTask,
                parentGoalTitle = uiState.selectedTask.goalId?.let { uiState.goalTitleById[it] },
                onClose = onCloseTaskDetail
            )
        }
    }
}

@Composable
private fun TasksTabContent(
    uiState: TargetsUiState,
    onTaskFilterClicked: (TaskFilterKey) -> Unit,
    onTaskClicked: (Task) -> Unit,
    onTaskComplete: (String) -> Unit,
    onTaskPostpone: (String) -> Unit,
    onTaskDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Browse all your tasks and missions",
                style = MaterialTheme.typography.titleSmall
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // TODO: Placeholder filter controls. Replace with advanced filter sheet/dropdowns.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.taskFilterItems.forEach { filter ->
                        FilterChip(
                            onClick = { onTaskFilterClicked(filter.key) },
                            label = { Text(filter.label) },
                            selected = filter.isSelected
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        onClick = { onTaskFilterClicked(TaskFilterKey.CATEGORY) },
                        label = { Text(uiState.taskFilters.categoryLabel) },
                        selected = false
                    )
                    FilterChip(
                        onClick = { onTaskFilterClicked(TaskFilterKey.SORT) },
                        label = { Text(uiState.taskFilters.sort.label) },
                        selected = uiState.taskFilters.sort != TaskSort.NONE
                    )
                }
            }
        }

        items(items = uiState.tasks, key = { it.id }) { task ->
            TaskRowWithActions(
                task = task,
                parentGoalTitle = task.goalId?.let { uiState.goalTitleById[it] },
                onTaskDetail = { onTaskClicked(task) },
                onTaskComplete = { onTaskComplete(task.id) },
                onTaskPostpone = { onTaskPostpone(task.id) },
                onTaskDelete = { onTaskDelete(task.id) }
            )
        }
    }
}

@Composable
private fun GoalsTabContent(
    uiState: TargetsUiState,
    onGoalFilterClicked: (GoalFilterKey) -> Unit,
    onGoalClicked: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Browse and manage all goals",
                style = MaterialTheme.typography.titleSmall
            )
        }

        item {
            // TODO: Placeholder for future goal sorting/filtering controls.
            uiState.goalFilterItems.forEach { filter ->
                FilterChip(
                    onClick = { onGoalFilterClicked(filter.key) },
                    label = { Text(filter.label) },
                    selected = filter.isSelected
                )
            }
        }

        items(items = uiState.goals, key = { it.id }) { goal ->
            GoalCompactItem(
                goal = goal,
                onClick = { onGoalClicked(goal.id) }
            )
        }
    }
}

/**
 * TaskRowWithActions: Task row with visible action buttons for Complete, Postpone, Delete.
 * Reused in both Tasks tab and GoalDetailScreen for consistency.
 * TODO: Replace visible action buttons with swipe-reveal interaction once gesture handling is implemented.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
private fun TaskRowWithActions(
    task: Task,
    parentGoalTitle: String? = null,
    onTaskDetail: () -> Unit = {},
    onTaskComplete: () -> Unit,
    onTaskPostpone: () -> Unit,
    onTaskDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTaskDetail),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Task Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                    val badge = if (parentGoalTitle == null) "Task" else "Mission · $parentGoalTitle"
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(text = "+${task.xp} XP", style = MaterialTheme.typography.labelMedium)
            }

            HorizontalDivider()

            Text(
                text = "Status: ${task.status.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = if (task.status == TaskStatus.COMPLETED) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = onTaskComplete,
                    label = { Text("Complete") },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onTaskPostpone,
                    label = { Text("Postpone") },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onTaskDelete,
                    label = { Text("Delete") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GoalCompactItem(goal: Goal, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = goal.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${goal.progress}% progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Status: ${goal.status.name}",
                style = MaterialTheme.typography.labelSmall,
                color = if (goal.status == GoalStatus.ACTIVE) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailModal(
    task: Task,
    parentGoalTitle: String?,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title + Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                val badge = if (parentGoalTitle == null) "Task" else "Mission"
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(6.dp, 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            HorizontalDivider()

            // Description
            if (task.description.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Parent Goal (if mission)
            if (parentGoalTitle != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Parent Goal",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = parentGoalTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Task Type & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = task.type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = task.status.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (task.status == TaskStatus.COMPLETED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Difficulty & XP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Difficulty",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${task.difficulty}/5",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "XP Reward",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "+${task.xp} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Energy Cost
            if (task.energyCost != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Energy Cost",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${task.energyCost} energy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Deadline
            if (task.deadline != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Deadline",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = task.deadline.toString(), // TODO: format deadline nicely (e.g., "This Week", "Jan 15, 2026")
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge
                )
                val categoryName = if (task.customCategoryName != null) {
                    task.customCategoryName
                } else {
                    task.category.displayName
                }
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Action Buttons (TODO: implement in future steps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { /* TODO: implement complete/mark as done action */ },
                    label = { Text("Mark Complete") }
                )
                AssistChip(
                    onClick = { /* TODO: implement edit action */ },
                    label = { Text("Edit") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { /* TODO: implement postpone action */ },
                    label = { Text("Postpone") }
                )
                AssistChip(
                    onClick = { /* TODO: implement delete action */ },
                    label = { Text("Delete") }
                )
            }

            // Bottom padding for scrollability
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(12.dp))
        }
    }
}
