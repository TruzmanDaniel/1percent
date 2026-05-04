package es.uc3m.android.a1percent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ShareRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val toUserId: String = "",
    val itemId: String = "",
    val itemType: String = "",  // "TASK" or "GOAL"
    val itemTitle: String = "",
    val createdAt: Long = 0L
)
