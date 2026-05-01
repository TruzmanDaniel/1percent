package es.uc3m.android.a1percent.data

import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.TaskDeadline
import java.util.Calendar

/**
 * Centraliza la lógica temporal de deadlines para tasks
 *
 * Regla de negocio reutilizable
 * (sorting, reminders, overdue, etc.) y evitar duplicar cálculos en la UI.
 */
object TaskDeadlineResolver {
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val END_OF_DAY_OFFSET_MILLIS = 86_399_999L

    /**
     * Convierte un [TaskDeadline] a una clave comparable en milisegundos.
     * - null -> al final del ordenado
     * - ThisWeek -> domingo actual a las 23:59:59.999
     * - OnDate -> fin del día seleccionado (23:59:59.999)
     */
    fun toSortKey(deadline: TaskDeadline?): Long {
        return when (deadline) {
            null -> Long.MAX_VALUE
            TaskDeadline.ThisWeek -> sundayEndOfCurrentWeekMillis()
            is TaskDeadline.OnDate -> deadline.epochDay * MILLIS_PER_DAY + END_OF_DAY_OFFSET_MILLIS
        }
    }

    enum class DeadlineStatus { OVERDUE, TODAY, FUTURE, NO_DEADLINE }

    fun deadlineStatus(deadline: TaskDeadline?): DeadlineStatus {
        if (deadline == null) return DeadlineStatus.NO_DEADLINE
        val deadlineMillis = toSortKey(deadline)
        val now = System.currentTimeMillis()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrowStart = todayStart + MILLIS_PER_DAY

        return when {
            deadlineMillis < todayStart -> DeadlineStatus.OVERDUE
            deadlineMillis < tomorrowStart -> DeadlineStatus.TODAY
            else -> DeadlineStatus.FUTURE
        }
    }

    private fun deadlineStatusOrder(status: DeadlineStatus): Int = when (status) {
        DeadlineStatus.OVERDUE -> 0
        DeadlineStatus.TODAY -> 1
        DeadlineStatus.FUTURE -> 2
        DeadlineStatus.NO_DEADLINE -> 3
    }

    fun taskDeadlineComparator(): Comparator<Task> {
        return compareBy<Task> { deadlineStatusOrder(deadlineStatus(it.deadline)) }
            .thenBy { toSortKey(it.deadline) }
    }

    private fun sundayEndOfCurrentWeekMillis(): Long {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysUntilSunday = (Calendar.SUNDAY - dayOfWeek + 7) % 7

        calendar.add(Calendar.DAY_OF_YEAR, daysUntilSunday)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        return calendar.timeInMillis
    }
}

