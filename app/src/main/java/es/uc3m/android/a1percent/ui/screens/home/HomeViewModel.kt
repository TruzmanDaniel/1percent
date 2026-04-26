package es.uc3m.android.a1percent.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskDeadlineResolver
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class to manage logic and screen state. It lets the interface observe changes and update
 */
class HomeViewModel : ViewModel() {

    // The UI state is remote-backed: session user + Firestore tasks/goals.
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe changes in the current user from the SessionRepository
        SessionRepository.currentUser
            .onEach { user ->
                if (user != null) {
                    _uiState.update { it.copy(user = user) }
                    loadUserData(user.id)
                } else {
                    _uiState.value = HomeUiState()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            // Load tasks
            TaskRespository.getTasks(userId).onSuccess { tasks ->
                _uiState.update { current ->
                    reduceHomeState(current.copy(tasks = tasks))
                }
            }
            // Load first goal
            GoalRepository.getGoals(userId).onSuccess { goals ->
                if (goals.isNotEmpty()) {
                    _uiState.update { it.copy(goal = goals.first()) }
                }
            }
        }
    }


    // Toggle missions filters
    fun onMissionsFilterToggled() {
        _uiState.update { current ->
            val updatedFilters = current.filters.copy(showOnlyMissions = !current.filters.showOnlyMissions)
            reduceHomeState(current.copy(filters = updatedFilters))
        }
    }

    fun onSortByDateToggled() {
        _uiState.update { current ->
            val nextSort = if (current.filters.sortBy == HomeSort.DATE_ASC) {
                HomeSort.NONE
            } else {
                HomeSort.DATE_ASC
            }
            val updatedFilters = current.filters.copy(sortBy = nextSort)
            reduceHomeState(current.copy(filters = updatedFilters))
        }
    }

    fun onFilterClicked(filterKey: HomeFilterKey) {
        when (filterKey) {
            HomeFilterKey.MISSIONS -> onMissionsFilterToggled()
            HomeFilterKey.SORT_BY_DATE -> onSortByDateToggled()
        }
    }

    // Define logic for filtering and sorting
    private fun applyFiltersAndSort(tasks: List<Task>, filters: HomeFilters): List<Task> {
        val filtered = if (filters.showOnlyMissions) {
            tasks.filter { it.goalId != null }
        } else {
            tasks
        }

        return when (filters.sortBy) {
            HomeSort.NONE -> filtered
            HomeSort.DATE_ASC -> filtered.sortedBy { task ->
                TaskDeadlineResolver.toSortKey(task.deadline)
            }
        }
    }

    // State what to show in the UI depending on current filters
    private fun reduceHomeState(base: HomeUiState): HomeUiState {
        val visibleTasks = applyFiltersAndSort(base.tasks, base.filters)
        val filterItems = buildHomeFilterUiItems(base.filters)
        return base.copy(
            visibleTasks = visibleTasks,
            filterItems = filterItems
        )
    }

    fun onProfileClicked(): String {
        return SessionRepository.currentUser.value?.name ?: "Unknown User"
    }
}
