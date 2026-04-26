package es.uc3m.android.a1percent.ui.screens.targets

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task

/**
 * Screen state for Targets.
 * This first version keeps browsing logic simple and mock-driven.
 */

// TOP-LEVEL TABS
enum class TargetsTab {
    TASKS,
    GOALS
}

data class TargetsUiState(
    val selectedTab: TargetsTab = TargetsTab.TASKS,
    val tasks: List<Task> = emptyList(),
    val goals: List<Goal> = emptyList(),

    // Filtering state for each tab
    val taskFilters: TaskFilters = TaskFilters(),
    val goalFilters: GoalFilters = GoalFilters(),
    val taskFilterItems: List<TaskFilterUiItem> = buildTaskFilterUiItems(TaskFilters()),
    val goalFilterItems: List<GoalFilterUiItem> = buildGoalFilterUiItems(GoalFilters()),

    val goalTitleById: Map<String, String> = emptyMap(),
    
    // Task detail modal state
    val selectedTask: Task? = null
)
