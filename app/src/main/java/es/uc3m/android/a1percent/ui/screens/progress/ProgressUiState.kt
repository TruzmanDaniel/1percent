package es.uc3m.android.a1percent.ui.screens.progress

import es.uc3m.android.a1percent.data.model.enums.Category

data class ProgressUiState(
    val isLoading: Boolean = true,

    // Chart 5 — Level / XP / Streak stat card
    val userLevel: Int = 1,
    val currentXp: Int = 0,
    val xpToNextLevel: Int = 100,
    val streakDays: Int = 0,
    val totalTasksCompleted: Int = 0,

    // Chart 2 — Active goals progress bars
    val activeGoals: List<GoalProgressItem> = emptyList(),

    // Chart 1 — Cumulative XP curve, last 30 days (one Float per day)
    val xpCurvePoints: List<Float> = emptyList(),

    // Chart 3 — Task completion count per day of week (index 0 = Mon, 6 = Sun)
    val thisWeekByDay: List<Int> = emptyList(),
    val lastWeekByDay: List<Int> = emptyList(),

    // Chart 4 — Completed tasks grouped by category
    val categoryBreakdown: List<CategorySlice> = emptyList(),

    // Chart 6 — Per-goal weekly completion rate sparklines
    val goalSparklines: List<GoalSparkline> = emptyList(),

    // Chart 7 — Milestones achieved per goal
    val milestones: List<GoalMilestoneItem> = emptyList()
)

data class GoalProgressItem(
    val title: String,
    val progress: Float,   // 0f..1f
    val category: Category
)

data class CategorySlice(
    val category: Category,
    val count: Int,
    val fraction: Float    // 0f..1f, pre-computed share of total
)

data class GoalSparkline(
    val goalTitle: String,
    val weeklyCompletionRates: List<Float>  // 0f..1f per week, oldest first
)

data class GoalMilestoneItem(
    val goalTitle: String,
    val weekThreshold: Int,
    val milestoneName: String,
    val unlockedAt: Long
)
