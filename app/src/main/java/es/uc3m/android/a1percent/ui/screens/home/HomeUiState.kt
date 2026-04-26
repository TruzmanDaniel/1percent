package es.uc3m.android.a1percent.ui.screens.home

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.UserProfile

/**
 * Data class to represent the screen state (DATA to be displayed by the UI).
 *  We use it to define an immutable class so UI always receives coherent data, and we know what data the UI needs
 *  Being separate from the ViewModel allows reusability and testing (creating an object of this class directly in the screen)
 */

data class HomeUiState(
    val user: UserProfile? = null,
    val tasks: List<Task> = emptyList(),
    val visibleTasks: List<Task> = emptyList(),
    val filters: HomeFilters = HomeFilters(),
    val filterItems: List<HomeFilterUiItem> = buildHomeFilterUiItems(HomeFilters()),
    val goal: Goal? = null
)
