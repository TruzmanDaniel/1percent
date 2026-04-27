package es.uc3m.android.a1percent.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import es.uc3m.android.a1percent.data.TaskCategoryRepository
import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.data.model.enums.TaskType
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Holds the state and form logic for task creation.
 * The composable should only render and dispatch events.
 */
class CreateTaskViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        CreateTaskUiState(
            predefinedCategories = TaskCategoryRepository.predefinedCategories,
            customCategories = TaskCategoryRepository.customCategories.value
        )
    )
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    init {
        TaskCategoryRepository.customCategories
            .onEach { categories ->
                _uiState.update { it.copy(customCategories = categories) }
            }
            .launchIn(viewModelScope)
    }

    fun onTaskNameChange(newValue: String) {
        _uiState.update { it.copy(taskName = newValue) }
    }

    fun onTaskDescriptionChange(newValue: String) {
        _uiState.update { it.copy(taskDescription = newValue) }
    }

    fun onCategoryDropdownExpandedChange(expanded: Boolean) {  // 'expanded' is the new state of the dropdown (compose logic when calling 'onExpandedChange')
        _uiState.update { it.copy(isCategoryDropdownExpanded = expanded) }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update {
            it.copy(
                selectedCategory = category, // saves chosen category
                selectedCustomCategoryName = null, // cleans previous custom category
                isCategoryDropdownExpanded = false // closes dropdown after selecting
            )
        }
    }

    fun onCustomCategorySelected(customCategory: String) {
        _uiState.update {
            it.copy(
                selectedCategory = Category.AUTOMATIC,   // placeholder, useless (selectedCategory is from Category enum and not nullable)
                selectedCustomCategoryName = customCategory,
                isCategoryDropdownExpanded = false
            )
        }
    }

    fun onCreateCategoryClicked() {
        _uiState.update {
            it.copy(
                isCategoryDropdownExpanded = false,
                isCreateCategoryDialogVisible = true,
                newCategoryName = ""
            )
        }
    }

    fun onNewCategoryNameChange(newValue: String) {
        _uiState.update { it.copy(newCategoryName = newValue) }
    }

    fun onCreateCategoryConfirmed() {
        val rawName = _uiState.value.newCategoryName
        val savedName = TaskCategoryRepository.addCustomCategory(rawName)

        if (savedName != null) {
            _uiState.update {
                it.copy(
                    selectedCategory = Category.AUTOMATIC,
                    selectedCustomCategoryName = savedName,  // Select this new created category
                    isCreateCategoryDialogVisible = false,
                    newCategoryName = ""
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isCreateCategoryDialogVisible = false,
                    newCategoryName = ""
                )
            }
        }
    }

    fun onCreateCategoryDismissed() {
        _uiState.update {
            it.copy(
                isCreateCategoryDialogVisible = false,
                newCategoryName = ""
            )
        }
    }

    fun onDeadlineEnabledChange(enabled: Boolean) {
        _uiState.update {
            if (enabled) {
                it.copy(hasDeadline = true)
            } else {
                it.copy(
                    hasDeadline = false,
                    deadlineOption = DeadlineOption.THIS_WEEK,
                    selectedEpochDay = null,
                    isDatePickerVisible = false
                )
            }
        }
    }

    fun onDeadlineOptionSelected(option: DeadlineOption) {
        _uiState.update {
            it.copy(
                deadlineOption = option,
                isDatePickerVisible = option == DeadlineOption.EXACT_DATE
            )
        }
    }

    fun onDatePickerDismissed() {
        _uiState.update { it.copy(isDatePickerVisible = false) }
    }

    fun onDateSelected(epochDay: Long) {
        _uiState.update {
            it.copy(
                selectedEpochDay = epochDay,
                isDatePickerVisible = false
            )
        }
    }

    fun createTask(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: SessionRepository.currentUser.value?.id
        if (userId == null) {
            onError("No user logged in")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                val task = Task(
                    title = state.taskName,
                    description = state.taskDescription,
                    type = TaskType.ONE_TIME,
                    difficulty = 1,
                    xp = 10,
                    energyCost = null,
                    deadline = state.selectedDeadline,
                    status = TaskStatus.PENDING,
                    category = state.selectedCategory,
                    customCategoryName = state.selectedCustomCategoryName
                )

                val result = TaskRespository.saveTask(userId, task)

                result.onSuccess { onSuccess() }
                result.onFailure { onError(it.message ?: "Error creating task") }
            } catch (e: Exception) {
                onError(e.message ?: "Error creating task")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateTaskUiState(
            predefinedCategories = TaskCategoryRepository.predefinedCategories,
            customCategories = TaskCategoryRepository.customCategories.value
        )
    }

}


