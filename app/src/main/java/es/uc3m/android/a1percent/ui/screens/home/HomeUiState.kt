package es.uc3m.android.a1percent.ui.screens.home

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.UserProfile

data class HomeUiState(
    val user: UserProfile? = null,
    val tasks: List<Task> = emptyList(),
    val visibleTasks: List<Task> = emptyList(),
    val filters: HomeFilters = HomeFilters(),
    val filterItems: List<HomeFilterUiItem> = buildHomeFilterUiItems(HomeFilters()),
    val goal: Goal? = null,
    val goals: List<Goal> = emptyList(),
    val navigateToRitual: String? = null,
    val isGeneratingWeek: Boolean = false,
    val selectedTask: Task? = null,
    val showDatePickerForTask: String? = null,
    val showShareSheet: Boolean = false,
    val shareTargetTask: Task? = null,
    val friends: List<UserProfile> = emptyList(),
    val snackbarMessage: String? = null,
    val sharedUserProfilesById: Map<String, UserProfile> = emptyMap(),
    val currentUserId: String = ""
)
