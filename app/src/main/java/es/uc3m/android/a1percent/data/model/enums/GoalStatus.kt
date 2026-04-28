package es.uc3m.android.a1percent.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class GoalStatus(val displayName: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    ARCHIVED("Archived"),
    UPCOMING("Upcoming")
}
