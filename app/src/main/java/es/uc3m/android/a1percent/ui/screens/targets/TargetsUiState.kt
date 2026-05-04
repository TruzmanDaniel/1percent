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

    val taskFilters: TaskFilters = TaskFilters(),
    val goalFilters: GoalFilters = GoalFilters(),
    val taskFilterItems: List<TaskFilterUiItem> = buildTaskFilterUiItems(TaskFilters()),
    val goalFilterItems: List<GoalFilterUiItem> = buildGoalFilterUiItems(GoalFilters()),

    val goalTitleById: Map<String, String> = emptyMap(),

    val selectedTask: Task? = null,
    val editingTask: Task? = null,
    val showDatePickerForTask: String? = null,
    val showShareSheet: Boolean = false,
    val shareTargetTask: Task? = null,
    val shareTargetGoal: Goal? = null,
    val friends: List<es.uc3m.android.a1percent.data.model.UserProfile> = emptyList(),
    val showDeleteGoalConfirm: String? = null,
    val sharedUserProfilesById: Map<String, es.uc3m.android.a1percent.data.model.UserProfile> = emptyMap(),
    val currentUserId: String = "",

    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)
