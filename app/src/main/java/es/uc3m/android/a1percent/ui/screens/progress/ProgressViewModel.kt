package es.uc3m.android.a1percent.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.WeeklySummaryRepository
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class ProgressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null
    private var goalsJob: Job? = null
    private var observedUserId: String? = null

    init {
        SessionRepository.currentUser
            .onEach { user ->
                if (user != null) {
                    applyUserProfile(user)
                    // Only restart Firestore observations when the logged-in user changes,
                    // not on every profile update (e.g. each XP award).
                    if (user.id != observedUserId) {
                        observedUserId = user.id
                        startObservingData(user.id)
                    }
                } else {
                    observedUserId = null
                    stopObservingData()
                    _uiState.value = ProgressUiState()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun applyUserProfile(user: UserProfile) {
        _uiState.update {
            it.copy(
                userLevel = user.level,
                currentXp = user.currentXp,
                xpToNextLevel = user.xpToNextLevel,
                streakDays = user.streakDays,
                totalTasksCompleted = user.totalTasksCompleted
            )
        }
    }

    private fun startObservingData(userId: String) {
        tasksJob?.cancel()
        goalsJob?.cancel()

        tasksJob = TaskRespository.observeTasks(userId)
            .onEach { tasks -> recomputeTaskStats(tasks, userId) }
            .launchIn(viewModelScope)

        goalsJob = GoalRepository.observeGoals(userId)
            .onEach { goals ->
                recomputeGoalStats(goals)
                loadWeeklySummaries(goals)
            }
            .launchIn(viewModelScope)
    }

    private fun stopObservingData() {
        tasksJob?.cancel()
        goalsJob?.cancel()
    }

    private fun recomputeTaskStats(tasks: List<Task>, userId: String) {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - 30L * 24 * 3600 * 1000

        // Chart 1: daily XP buckets → cumulative sum over 30 days
        val dailyXp = FloatArray(30)
        tasks.forEach { task ->
            val ca = task.completedAt
            if (ca != null && ca >= thirtyDaysAgo && userId in task.completedBy) {
                val day = ((ca - thirtyDaysAgo) / (24 * 3600 * 1000)).toInt().coerceIn(0, 29)
                dailyXp[day] += (task.xpAwarded ?: task.xp)
            }
        }
        var running = 0f
        val xpCurve = dailyXp.map { xp -> running += xp; running }

        // Chart 3: completions per day-of-week for this week and last week
        val weekStart = mondayOfCurrentWeek()
        val lastWeekStart = weekStart - 7L * 24 * 3600 * 1000
        val thisWeek = IntArray(7)
        val lastWeek = IntArray(7)
        tasks
            .filter { userId in it.completedBy && it.completedAt != null }
            .forEach { task ->
                val ca = task.completedAt!!
                // Calendar.DAY_OF_WEEK: Sun=1..Sat=7 → Mon=0..Sun=6
                val dayIndex = (Calendar.getInstance().apply { timeInMillis = ca }
                    .get(Calendar.DAY_OF_WEEK) + 5) % 7
                when {
                    ca >= weekStart -> thisWeek[dayIndex]++
                    ca >= lastWeekStart -> lastWeek[dayIndex]++
                }
            }

        // Chart 4: completed tasks grouped by category
        val counts = tasks
            .filter { userId in it.completedBy }
            .groupBy { it.category }
            .mapValues { it.value.size }
        val total = counts.values.sum().toFloat().coerceAtLeast(1f)
        val breakdown = counts.entries
            .sortedByDescending { it.value }
            .map { (cat, count) -> CategorySlice(cat, count, count / total) }

        _uiState.update {
            it.copy(
                xpCurvePoints = xpCurve,
                thisWeekByDay = thisWeek.toList(),
                lastWeekByDay = lastWeek.toList(),
                categoryBreakdown = breakdown,
                isLoading = false
            )
        }
    }

    private fun recomputeGoalStats(goals: List<Goal>) {
        val items = goals
            .filter { it.status == GoalStatus.ACTIVE }
            .map { goal ->
                GoalProgressItem(
                    title = goal.title,
                    progress = goal.progress / 100f,
                    category = goal.category
                )
            }
        _uiState.update { it.copy(activeGoals = items) }
    }

    private fun loadWeeklySummaries(goals: List<Goal>) {
        viewModelScope.launch {
            val sparklines = goals
                .filter { it.status == GoalStatus.ACTIVE }
                .mapNotNull { goal ->
                    val summaries = WeeklySummaryRepository.getSummaries(goal.id, limit = 6)
                        .getOrNull() ?: return@mapNotNull null
                    if (summaries.isEmpty()) return@mapNotNull null
                    val rates = summaries
                        .sortedBy { it.weekNumber }
                        .map { s -> if (s.totalTasks > 0) s.tasksCompleted.toFloat() / s.totalTasks else 0f }
                    GoalSparkline(goalTitle = goal.title, weeklyCompletionRates = rates)
                }
            _uiState.update { it.copy(goalSparklines = sparklines) }
        }
    }

    private fun mondayOfCurrentWeek(): Long =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
