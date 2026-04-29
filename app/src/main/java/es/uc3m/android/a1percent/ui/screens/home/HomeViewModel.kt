package es.uc3m.android.a1percent.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.CreditManager
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskDeadlineResolver
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.WeeklySummaryRepository
import es.uc3m.android.a1percent.data.ai.AICoachService
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.WeeklySummary
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.EnergyFeedback
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null
    private var goalsJob: Job? = null

    companion object {
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    init {
        SessionRepository.currentUser
            .onEach { user ->
                if (user != null) {
                    _uiState.update { it.copy(user = user) }
                    startObservingData(user.id)
                } else {
                    stopObservingData()
                    _uiState.value = HomeUiState()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startObservingData(userId: String) {
        tasksJob?.cancel()
        goalsJob?.cancel()

        tasksJob = TaskRespository.observeTasks(userId)
            .onEach { tasks ->
                _uiState.update { current ->
                    reduceHomeState(current.copy(tasks = tasks))
                }
            }
            .launchIn(viewModelScope)

        goalsJob = GoalRepository.observeGoals(userId)
            .onEach { goals ->
                _uiState.update { it.copy(
                    goals = goals,
                    goal = goals.firstOrNull()
                ) }
                checkWeeklyRituals(goals)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            CreditManager.resetCreditsIfNeeded(userId)
        }
    }

    private fun stopObservingData() {
        tasksJob?.cancel()
        goalsJob?.cancel()
    }

    private fun checkWeeklyRituals(goals: List<Goal>) {
        val now = System.currentTimeMillis()

        val pendingGoal = goals.firstOrNull { goal ->
            goal.aiRoadmapStatus == AiRoadmapStatus.READY
                && goal.nextGenerationDate != null
                && now >= goal.nextGenerationDate
        } ?: return

        val isCatchUp = (now - (pendingGoal.nextGenerationDate ?: 0)) > SEVEN_DAYS_MILLIS * 2

        if (isCatchUp) {
            _uiState.update { it.copy(
                showCatchUp = true,
                ritualGoal = pendingGoal
            ) }
        } else {
            val goalTasks = _uiState.value.tasks.filter {
                it.goalId == pendingGoal.id && it.isAiGenerated
            }
            val completed = goalTasks.count { it.status == TaskStatus.COMPLETED }
            val epicTask = goalTasks.find { it.dayIndex == 7 }
            val epicPassed = epicTask?.status == TaskStatus.COMPLETED
            val xpEarned = goalTasks.filter { it.status == TaskStatus.COMPLETED }.sumOf { it.xp }

            _uiState.update { it.copy(
                showWeeklyRitual = true,
                ritualGoal = pendingGoal,
                ritualTasksCompleted = completed,
                ritualTotalTasks = goalTasks.size,
                ritualEpicPassed = epicPassed,
                ritualXpEarned = xpEarned
            ) }
        }
    }

    fun onWeeklyFeedback(feedback: String) {
        val goal = _uiState.value.ritualGoal ?: return
        val userId = SessionRepository.currentUser.value?.id ?: return
        val isCatchUp = _uiState.value.showCatchUp

        viewModelScope.launch {
            _uiState.update { it.copy(
                showWeeklyRitual = false,
                showCatchUp = false,
                isGeneratingWeek = true
            ) }

            val latestSummary = WeeklySummaryRepository.getLatestSummary(goal.id).getOrNull()
            val weekNumber = (latestSummary?.weekNumber ?: 0) + 1

            if (!isCatchUp) {
                val summary = WeeklySummary(
                    goalId = goal.id,
                    userId = userId,
                    weekNumber = weekNumber - 1,
                    tasksCompleted = _uiState.value.ritualTasksCompleted,
                    totalTasks = _uiState.value.ritualTotalTasks,
                    epicMissionPassed = _uiState.value.ritualEpicPassed,
                    userFeedback = try { EnergyFeedback.valueOf(feedback) } catch (_: Exception) { null },
                    intensityUsed = goal.currentIntensity
                )
                WeeklySummaryRepository.saveSummary(goal.id, summary)
            }

            val maxIntensity = goal.difficulty * 2.0f
            val newIntensity = if (isCatchUp) {
                AICoachService.calculateCatchUpIntensity(goal.currentIntensity, feedback, maxIntensity)
            } else {
                AICoachService.calculateNewIntensity(
                    goal.currentIntensity, _uiState.value.ritualEpicPassed, feedback, maxIntensity
                )
            }

            val updatedGoal = goal.copy(currentIntensity = newIntensity)

            val isWeekend = Calendar.getInstance().let {
                it.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            }

            val result = AICoachService.generateWeeklyTasks(
                goal = updatedGoal,
                weeklySummary = if (isCatchUp) null else latestSummary,
                isWeekend = isWeekend,
                userFeedback = feedback,
                userId = userId,
                weekNumber = weekNumber
            )

            result.onSuccess { tasks ->
                TaskRespository.saveTaskBatch(userId, tasks.map { it.copy(goalId = goal.id) })

                val finalGoal = updatedGoal.copy(
                    nextGenerationDate = System.currentTimeMillis() + SEVEN_DAYS_MILLIS
                )
                GoalRepository.updateGoal(finalGoal)
            }

            _uiState.update { it.copy(
                isGeneratingWeek = false,
                ritualGoal = null
            ) }
        }
    }

    fun dismissRitual() {
        _uiState.update { it.copy(
            showWeeklyRitual = false,
            showCatchUp = false,
            ritualGoal = null
        ) }
    }

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

    private fun applyFiltersAndSort(tasks: List<Task>, filters: HomeFilters): List<Task> {
        val pendingTasks = tasks.filter { task ->
            task.status == TaskStatus.PENDING
        }

        val filtered = if (filters.showOnlyMissions) {
            pendingTasks.filter { it.goalId != null }
        } else {
            pendingTasks
        }

        return when (filters.sortBy) {
            HomeSort.NONE -> filtered
            HomeSort.DATE_ASC -> filtered.sortedBy { task ->
                TaskDeadlineResolver.toSortKey(task.deadline)
            }
        }
    }

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
