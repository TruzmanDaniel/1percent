package es.uc3m.android.a1percent.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class TaskType(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    ONE_TIME("One Time")
}
