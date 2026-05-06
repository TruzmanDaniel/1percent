package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.GoalType
import java.util.Locale

val Goal.isFinite: Boolean get() = goalType == GoalType.FINITE
val Goal.isInfinite: Boolean get() = goalType == GoalType.INFINITE

fun Goal.weekLabel(currentWeek: Int): String {
    return if (isFinite) {
        val totalWeeks = totalWeeks()
        "Semana $currentWeek de $totalWeeks"
    } else {
        "Semana $currentWeek"
    }
}

fun Goal.progressDisplay(): Int? = if (isFinite) progress else null

fun Goal.intensityDisplay(): String? {
    if (isFinite) return null
    return if (currentIntensity == currentIntensity.toLong().toFloat()) {
        currentIntensity.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", currentIntensity)
    }
}

fun Goal.streakDisplay(): String? =
    if (isInfinite && weeklyStreak > 0) "$weeklyStreak sem" else if (isInfinite) "0 sem" else null

fun Goal.weeksRemaining(): Int? {
    if (isInfinite || deadline == null) return null
    val now = System.currentTimeMillis()
    val remainingMs = (deadline - now).coerceAtLeast(0)
    return (remainingMs / (7L * 24 * 3600 * 1000)).toInt()
}

fun Goal.nextMilestone(): Int? {
    if (isFinite) return null
    val milestones = listOf(4, 12, 26, 52)
    for (m in milestones) {
        if (weeklyStreak < m) return m
    }
    val posInCycle = weeklyStreak % 52
    for (m in milestones) {
        if (posInCycle < m) return weeklyStreak - posInCycle + m
    }
    return weeklyStreak - posInCycle + 52 + 4
}

fun Goal.justReachedMilestone(): Int? {
    if (isFinite || weeklyStreak == 0) return null
    val milestones = listOf(52, 26, 12, 4)
    return milestones.firstOrNull { weeklyStreak % it == 0 }
}

private fun Goal.totalWeeks(): Int {
    if (deadline == null) return 0
    val weekMs = 7L * 24 * 3600 * 1000
    return ((deadline - createdAt + weekMs - 1) / weekMs).toInt().coerceAtLeast(1)
}
