package es.uc3m.android.a1percent.data.model

import java.util.Calendar

fun Goal.weeksRemaining(): Int {
    val now = System.currentTimeMillis()
    val remainingMs = (deadline - now).coerceAtLeast(0)
    return (remainingMs / (7L * 24 * 3600 * 1000)).toInt()
}

fun Goal.totalWeeks(): Int {
    val weekMs = 7L * 24 * 3600 * 1000
    return ((deadline - createdAt + weekMs - 1) / weekMs).toInt().coerceAtLeast(1)
}

fun Goal.weekLabel(currentWeek: Int): String {
    return "Semana $currentWeek de ${totalWeeks()}"
}

fun Goal.isDeadlineWeek(): Boolean {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val weekStart = cal.timeInMillis
    val weekEnd = weekStart + 7L * 24 * 3600 * 1000
    return deadline in weekStart until weekEnd
}
