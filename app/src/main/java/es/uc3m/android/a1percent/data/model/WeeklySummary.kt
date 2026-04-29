package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.EnergyFeedback
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WeeklySummary(
    val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val userId: String,
    val weekNumber: Int,
    val tasksCompleted: Int,
    val totalTasks: Int,
    val epicMissionPassed: Boolean,
    val userFeedback: EnergyFeedback? = null,
    val intensityUsed: Float,
    val createdAt: Long = System.currentTimeMillis()
)
