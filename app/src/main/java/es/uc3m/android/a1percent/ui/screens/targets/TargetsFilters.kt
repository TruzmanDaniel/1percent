package es.uc3m.android.a1percent.ui.screens.targets

import androidx.annotation.StringRes
import es.uc3m.android.a1percent.R
import es.uc3m.android.a1percent.data.model.enums.TaskStatus

/**
 * Filter and sorting definitions for Targets.
 * UI consumes prebuilt filter items from UiState; ViewModel owns behavior.
 */

// TASKS
enum class TaskQuickFilter(@StringRes val labelRes: Int) {
    MISSIONS(R.string.targets_filter_missions_label),
    TASKS(R.string.targets_filter_tasks_label),
    SHARED(R.string.targets_filter_shared_label)
}

enum class TaskFilterKey {
    MISSIONS,
    SHARED,
    CATEGORY,
    SORT
}

enum class TaskSort(@StringRes val labelRes: Int) {
    NONE(R.string.targets_sort_none),
    DEADLINE_ASC(R.string.targets_sort_date),
    XP_DESC(R.string.targets_sort_xp)
}

data class TaskFilters(
    val quickFilters: Set<TaskQuickFilter> = emptySet(),
    val categoryLabel: String = "Category",
    val selectedStatus: TaskStatus? = TaskStatus.PENDING,
    val sort: TaskSort = TaskSort.NONE
)

// TASK STATUS FILTER Rotation
@StringRes
fun statusFilterLabel(selectedStatus: TaskStatus?): Int {
    return when (selectedStatus) {
        TaskStatus.PENDING -> R.string.targets_filter_status_pending
        TaskStatus.COMPLETED -> R.string.targets_filter_status_completed
        else -> R.string.targets_filter_status_all
    }
}

data class TaskFilterUiItem(
    val key: TaskFilterKey,
    @StringRes val labelRes: Int,
    val isSelected: Boolean,
    val order: Int
)

fun buildTaskFilterUiItems(filters: TaskFilters): List<TaskFilterUiItem> {
    val items = listOf(
        TaskFilterUiItem(
            key = TaskFilterKey.MISSIONS,
            labelRes = taskMissionsFilterLabelRes(filters.quickFilters),
            isSelected = filters.quickFilters.contains(TaskQuickFilter.MISSIONS) || filters.quickFilters.contains(TaskQuickFilter.TASKS),
            order = 0
        ),
        TaskFilterUiItem(
            key = TaskFilterKey.SHARED,
            labelRes = TaskQuickFilter.SHARED.labelRes,
            isSelected = filters.quickFilters.contains(TaskQuickFilter.SHARED),
            order = 1
        )
    )

    return items.sortedWith(compareByDescending<TaskFilterUiItem> { it.isSelected }.thenBy { it.order })
}

// Returns a string resource ID reflecting the current missions/tasks quick filter state.
@StringRes
fun taskMissionsFilterLabelRes(quickFilters: Set<TaskQuickFilter>): Int {
    return when {
        quickFilters.contains(TaskQuickFilter.MISSIONS) -> TaskQuickFilter.MISSIONS.labelRes
        quickFilters.contains(TaskQuickFilter.TASKS) -> TaskQuickFilter.TASKS.labelRes
        else -> R.string.targets_filter_all_label
    }
}


// GOALS
enum class GoalFilterKey {
    SORT
}

enum class GoalSort(@StringRes val labelRes: Int) {
    NONE(R.string.targets_goal_sort_none),
    PROGRESS_DESC(R.string.targets_goal_sort_progress)
}

data class GoalFilters(
    val sort: GoalSort = GoalSort.NONE
)

data class GoalFilterUiItem(
    val key: GoalFilterKey,
    @StringRes val labelRes: Int,
    val isSelected: Boolean
)

fun buildGoalFilterUiItems(filters: GoalFilters): List<GoalFilterUiItem> {
    return listOf(
        GoalFilterUiItem(
            key = GoalFilterKey.SORT,
            labelRes = filters.sort.labelRes,
            isSelected = filters.sort != GoalSort.NONE
        )
    )
}
