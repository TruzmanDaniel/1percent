package es.uc3m.android.a1percent.ui.screens.targets

import es.uc3m.android.a1percent.data.model.enums.TaskStatus

/**
 * Filter and sorting definitions for Targets.
 * UI consumes prebuilt filter items from UiState; ViewModel owns behavior.
 */

// TASKS
enum class TaskQuickFilter(val label: String) {
    MISSIONS("Missions"),
    TASKS("Tasks"),
    SHARED("Shared with me")
}

enum class TaskFilterKey {
    MISSIONS,
    SHARED,
    CATEGORY,
    SORT
}

enum class TaskSort(val label: String) {
    NONE("Sort"),
    DEADLINE_ASC("Sort: Date"),
    XP_DESC("Sort: XP")
}

data class TaskFilters(
    val quickFilters: Set<TaskQuickFilter> = emptySet(),
    val categoryLabel: String = "Category",
    val selectedStatus: TaskStatus? = TaskStatus.PENDING,
    val sort: TaskSort = TaskSort.NONE
)

// TASK STATUS FILTER Rotation
fun statusFilterLabel(selectedStatus: TaskStatus?): String {
    return when (selectedStatus) {
        TaskStatus.PENDING -> "Status: Pending"
        TaskStatus.COMPLETED -> "Status: Completed"
        else -> "Status: All"
    }
}

data class TaskFilterUiItem(
    val key: TaskFilterKey,
    val label: String,
    val isSelected: Boolean,
    val order: Int
)

fun buildTaskFilterUiItems(filters: TaskFilters): List<TaskFilterUiItem> {
    val items = listOf(
        TaskFilterUiItem(
            key = TaskFilterKey.MISSIONS,
            label = taskMissionsFilterLabel(filters.quickFilters),
            isSelected = filters.quickFilters.contains(TaskQuickFilter.MISSIONS) || filters.quickFilters.contains(TaskQuickFilter.TASKS),
            order = 0
        ),
        TaskFilterUiItem(
            key = TaskFilterKey.SHARED,
            label = TaskQuickFilter.SHARED.label,
            isSelected = filters.quickFilters.contains(TaskQuickFilter.SHARED),
            order = 1
        )
    )

    return items.sortedWith(compareByDescending<TaskFilterUiItem> { it.isSelected }.thenBy { it.order })
}

// Returns a label reflecting the current missions/tasks quick filter state.
fun taskMissionsFilterLabel(quickFilters: Set<TaskQuickFilter>): String {
    return when {
        quickFilters.contains(TaskQuickFilter.MISSIONS) -> TaskQuickFilter.MISSIONS.label
        quickFilters.contains(TaskQuickFilter.TASKS) -> TaskQuickFilter.TASKS.label
        else -> "All"
    }
}



// GOALS
enum class GoalFilterKey {
    SORT
}

enum class GoalSort(val label: String) {
    NONE("Sort"),
    PROGRESS_DESC("Sort: Progress")
}

data class GoalFilters(
    val sort: GoalSort = GoalSort.NONE
)

data class GoalFilterUiItem(
    val key: GoalFilterKey,
    val label: String,
    val isSelected: Boolean
)

fun buildGoalFilterUiItems(filters: GoalFilters): List<GoalFilterUiItem> {
    return listOf(
        GoalFilterUiItem(
            key = GoalFilterKey.SORT,
            label = filters.sort.label,
            isSelected = filters.sort != GoalSort.NONE
        )
    )
}


