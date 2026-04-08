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

// TASK FITLERS
enum class TaskQuickFilter(val label: String) {
    TASKS("Tasks"), // TODO creo que sobra porque como tal las missions si que son tasks, y estamos ya en el tab de "tasks"
    MISSIONS("Missions"),
    SHARED("Shared")
}

data class TargetsUiState(
    val selectedTab: TargetsTab = TargetsTab.TASKS,
    val tasks: List<Task> = emptyList(),
    val goals: List<Goal> = emptyList(),

    val selectedTaskFilters: Set<TaskQuickFilter> = setOf(TaskQuickFilter.TASKS),
    val goalTitleById: Map<String, String> = emptyMap(),
    val categoryFilterLabel: String = "Category",
    val taskSortLabel: String = "Sort",
    val goalSortLabel: String = "Sort",
    
    // Task detail modal state
    val selectedTask: Task? = null
)
