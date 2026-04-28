package es.uc3m.android.a1percent.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus(val displayName: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    SKIPPED("Skipped"),
    POSTPONED("Postponed")
}
