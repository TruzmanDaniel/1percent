package es.uc3m.android.a1percent.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MilestoneRecord(
    val id: String = UUID.randomUUID().toString(),
    val milestone: Int,
    val weeklyStreak: Int,
    val xpAwarded: Int,
    val unlockedAt: Long = System.currentTimeMillis()
)
