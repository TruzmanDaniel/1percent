package es.uc3m.android.a1percent.ui.components

import androidx.compose.ui.graphics.Color
import es.uc3m.android.a1percent.data.TaskDeadlineResolver
import es.uc3m.android.a1percent.data.model.TaskDeadline

object TaskDeadlineColors {
    val Overdue = Color(0xFFEF4444)
    val DueToday = Color(0xFFF59E0B)
    val Default = Color(0xFF6366F1)
}

fun taskDeadlineBorderColor(deadline: TaskDeadline?): Color {
    return when (TaskDeadlineResolver.deadlineStatus(deadline)) {
        TaskDeadlineResolver.DeadlineStatus.OVERDUE -> TaskDeadlineColors.Overdue
        TaskDeadlineResolver.DeadlineStatus.TODAY -> TaskDeadlineColors.DueToday
        TaskDeadlineResolver.DeadlineStatus.FUTURE -> TaskDeadlineColors.Default
        TaskDeadlineResolver.DeadlineStatus.NO_DEADLINE -> TaskDeadlineColors.Default
    }
}

val TaskDeadlineColors.Completed: Color get() = Color(0xFF22C55E)

fun taskDeadlineIndicatorColor(deadline: TaskDeadline?, isCompleted: Boolean): Color? {
    if (isCompleted) return Color(0xFF22C55E)
    return when (TaskDeadlineResolver.deadlineStatus(deadline)) {
        TaskDeadlineResolver.DeadlineStatus.OVERDUE -> TaskDeadlineColors.Overdue
        TaskDeadlineResolver.DeadlineStatus.TODAY -> TaskDeadlineColors.DueToday
        TaskDeadlineResolver.DeadlineStatus.FUTURE -> TaskDeadlineColors.Default
        TaskDeadlineResolver.DeadlineStatus.NO_DEADLINE -> null
    }
}

fun taskDeadlineLabel(deadline: TaskDeadline?): String? {
    return when (TaskDeadlineResolver.deadlineStatus(deadline)) {
        TaskDeadlineResolver.DeadlineStatus.OVERDUE -> "Overdue"
        TaskDeadlineResolver.DeadlineStatus.TODAY -> "Due Today"
        else -> null
    }
}
