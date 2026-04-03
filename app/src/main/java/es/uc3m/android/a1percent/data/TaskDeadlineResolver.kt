package es.uc3m.android.a1percent.data

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

