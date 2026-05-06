package es.uc3m.android.a1percent.data

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.UserProfile
import java.util.Calendar

object XpManager {

    suspend fun awardTaskXp(userId: String, task: Task): Result<Unit> {
        val profile = SessionRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("No logged-in user"))
        if (profile.id != userId) return Result.failure(IllegalStateException("User mismatch"))

        val base = task.difficulty * 10
        val bonus = if (task.deadline != null && task.completedAt != null) {
            val deadlineMillis = TaskDeadlineResolver.toSortKey(task.deadline)
            if (task.completedAt < deadlineMillis) (base * 0.2).toInt() else 0
        } else 0
        val total = base + bonus

        TaskRespository.updateXpAwarded(task.id, total)

        val today = startOfDay()
        val yesterday = today - 86_400_000L
        val newStreak = when (profile.lastActivityDate) {
            today     -> profile.streakDays
            yesterday -> profile.streakDays + 1
            else      -> 1
        }

        val updated = applyXpGain(profile, total).copy(
            totalTasksCompleted = profile.totalTasksCompleted + 1,
            streakDays = newStreak,
            lastActivityDate = today
        )
        return SessionRepository.updateUserProfile(updated)
    }

    suspend fun revokeTaskXp(userId: String, task: Task): Result<Unit> {
        val profile = SessionRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("No logged-in user"))
        if (profile.id != userId) return Result.failure(IllegalStateException("User mismatch"))

        val freshTask = TaskRespository.getTask(task.id).getOrNull() ?: task
        val xpToRevoke = freshTask.xpAwarded ?: return Result.success(Unit)

        TaskRespository.updateXpAwarded(task.id, null)

        val updated = profile.copy(
            currentXp = (profile.currentXp - xpToRevoke).coerceAtLeast(0),
            totalTasksCompleted = (profile.totalTasksCompleted - 1).coerceAtLeast(0)
        )
        return SessionRepository.updateUserProfile(updated)
    }

    suspend fun awardEpicWeeklyBonus(userId: String, goal: Goal): Result<Unit> {
        val profile = SessionRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("No logged-in user"))
        if (profile.id != userId) return Result.failure(IllegalStateException("User mismatch"))

        val bonus = goal.difficulty * 30
        val updated = applyXpGain(profile, bonus)
        return SessionRepository.updateUserProfile(updated)
    }

    suspend fun awardGoalCompletionBonus(userId: String, goal: Goal): Result<Unit> {
        val profile = SessionRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("No logged-in user"))
        if (profile.id != userId) return Result.failure(IllegalStateException("User mismatch"))

        val bonus = goal.difficulty * 50
        val updated = applyXpGain(profile, bonus)
        return SessionRepository.updateUserProfile(updated)
    }

    // Level n requires n×100 XP to advance. Carry over excess XP across multiple level-ups.
    private fun applyXpGain(profile: UserProfile, xpGain: Int): UserProfile {
        var xp = profile.currentXp + xpGain
        var level = profile.level
        var xpToNext = profile.xpToNextLevel.coerceAtLeast(1)

        while (xp >= xpToNext) {
            xp -= xpToNext
            level++
            xpToNext = level * 100
        }

        return profile.copy(currentXp = xp, level = level, xpToNextLevel = xpToNext)
    }

    private fun startOfDay(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
