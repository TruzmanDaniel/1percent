package es.uc3m.android.a1percent.data.model

/**
 * Deadline options for a task:
 * null: no deadline
 * ThisWeek: due before end of current week (shown as "This Week", technically Sunday 23.59)
 * OnDate: due on a specific calendar day
 *
 * TODO: create a TaskDeadlineResolver that converts TaskDeadline to an absolute
 *      to support SORTING, REMINDERS, notifications and overdue checks.
 *            (when resolving ThisWeek, map it explicitly to Sunday 23:59)
 * */

// Interface with 2 possible values

sealed interface TaskDeadline {
    object ThisWeek : TaskDeadline

    // TODO: add optional time-of-day when the user needs hour precision.
    data class OnDate(val epochDay: Long) : TaskDeadline
}


