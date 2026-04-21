package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import kotlinx.coroutines.tasks.await

object GoalRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun userGoalsCollection(userId: String) =
        db.collection("users").document(userId).collection("goals")

    suspend fun saveGoal(userId: String, goal: Goal): Result<Unit> {
        return try {
            val data = mapOf(
                "id" to goal.id,
                "title" to goal.title,
                "description" to goal.description,
                "category" to goal.category.name,
                "difficulty" to goal.difficulty,
                "xp" to goal.xp,
                "status" to goal.status.name,
                "progress" to goal.progress,
                "createdAt" to goal.createdAt
            )
            userGoalsCollection(userId).document(goal.id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoals(userId: String): Result<List<Goal>> {
        return try {
            val snapshot = userGoalsCollection(userId).get().await()
            val goals = snapshot.documents.mapNotNull { doc ->
                try {
                    Goal(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = Category.valueOf(doc.getString("category") ?: "AUTOMATIC"),
                        difficulty = doc.getLong("difficulty")?.toInt() ?: 1,
                        xp = doc.getLong("xp")?.toInt() ?: 0,
                        status = GoalStatus.valueOf(doc.getString("status") ?: "ACTIVE"),
                        progress = doc.getLong("progress")?.toInt() ?: 0,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } catch (e: Exception) { null }
            }
            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}