package es.uc3m.android.a1percent.ui.screens.tasks

import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.data.model.enums.Category

/**
 * UI state for the task creation flow.
 * Keeps form data and lightweight presentation state out of the composable.
 */
enum class DeadlineOption {
    THIS_WEEK,
    EXACT_DATE
}

data class CreateTaskUiState(
    val taskName: String = "",
    val taskDescription: String = "",

    val selectedCategory: Category = Category.AUTOMATIC, // Default is AUTOMATIC
    val selectedCustomCategoryName: String? = null,
    val predefinedCategories: List<Category> = emptyList(),
    val customCategories: List<String> = emptyList(),
    val isCategoryDropdownExpanded: Boolean = false,
    val isCreateCategoryDialogVisible: Boolean = false,
    val newCategoryName: String = "",

    val selectedEpochDay: Long? = null, // Default is no deadline
    val hasDeadline: Boolean = false, // Default is no deadline
    val deadlineOption: DeadlineOption = DeadlineOption.THIS_WEEK,
    val isDatePickerVisible: Boolean = false
) {
    val selectedCategoryLabel: String
        get() = selectedCustomCategoryName ?: selectedCategory.displayName

    val selectedDeadline: TaskDeadline?
        get() = when {
            !hasDeadline -> null
            deadlineOption == DeadlineOption.THIS_WEEK -> TaskDeadline.ThisWeek
            else -> selectedEpochDay?.let(TaskDeadline::OnDate)
        }

    // avoid creating tasks with unselected but intended deadline
    val canCreateTask: Boolean
        get() = !hasDeadline || selectedDeadline != null
}

