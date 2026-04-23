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
    MISSIONS("Missions"),
    SHARED("Shared")
}

data class TargetsUiState(
    val selectedTab: TargetsTab = TargetsTab.TASKS,
    val tasks: List<Task> = emptyList(),
    val goals: List<Goal> = emptyList(),

    // Empty means "show all tasks" by default.
    val selectedTaskFilters: Set<TaskQuickFilter> = emptySet(),

    val goalTitleById: Map<String, String> = emptyMap(),
    val categoryFilterLabel: String = "Category",
    val taskSortLabel: String = "Sort",
    val goalSortLabel: String = "Sort",
    
    // Task detail modal state
    val selectedTask: Task? = null
)
