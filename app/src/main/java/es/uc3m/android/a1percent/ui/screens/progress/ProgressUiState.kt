package es.uc3m.android.a1percent.ui.screens.progress
/**
 * Placeholder screen state for Progress.
 * The current screen is still mock-driven, but this keeps the structure ready for real data later.
 */
data class ProgressUiState(
    val title: String = "Progress Dashboard",
    val intro: String = "Preview mode: these blocks are placeholders and will be replaced with real charts once data integration is ready.",
    val sectionTitles: List<String> = listOf(
        "1% Curve",
        "Completed Tasks (Total & This Month)",
        "Task Category Heatmap",
        "XP Gained Over the Month",
        "Average Difficulty of Completed Tasks",
        "Level Evolution and Progress to Next Level",
        "Cumulative XP Chart (Exponential Progress)"
    ),
    val footerNote: String = "Note: mock values for visual prototyping."
)
