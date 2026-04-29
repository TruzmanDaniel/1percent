package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import es.uc3m.android.a1percent.data.model.WeeklySummary
import es.uc3m.android.a1percent.data.remote.encodeToMap
import es.uc3m.android.a1percent.data.remote.toObjectsSerializable
import kotlinx.coroutines.tasks.await

object WeeklySummaryRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun summariesCollection(goalId: String) =
        db.collection("goals").document(goalId).collection("weeklySummaries")

    suspend fun saveSummary(goalId: String, summary: WeeklySummary): Result<Unit> {
        return try {
            summariesCollection(goalId)
                .document(summary.id)
                .set(summary.encodeToMap()!!)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestSummary(goalId: String): Result<WeeklySummary?> {
        return try {
            val snapshot = summariesCollection(goalId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val summaries = snapshot.toObjectsSerializable<WeeklySummary>()
            Result.success(summaries.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
