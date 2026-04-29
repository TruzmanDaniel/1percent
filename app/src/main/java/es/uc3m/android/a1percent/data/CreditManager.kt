package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.remote.toObjectSerializable
import es.uc3m.android.a1percent.data.remote.encodeToMap
import kotlinx.coroutines.tasks.await

object CreditManager {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val usersCollection = db.collection("users")

    private const val WEEKLY_CREDITS = 5
    private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000

    suspend fun deductCredit(userId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val docRef = usersCollection.document(userId)
                val snapshot = transaction.get(docRef)
                val profile = snapshot.toObjectSerializable<UserProfile>()
                    ?: throw IllegalStateException("User profile not found")

                val now = System.currentTimeMillis()
                val needsReset = profile.creditsResetDate == null || profile.creditsResetDate < now

                val currentCredits = if (needsReset) WEEKLY_CREDITS else profile.availableCredits

                if (currentCredits <= 0) {
                    throw IllegalStateException("No credits available")
                }

                val updatedProfile = profile.copy(
                    availableCredits = currentCredits - 1,
                    creditsResetDate = if (needsReset) now + WEEK_MILLIS else profile.creditsResetDate
                )
                transaction.set(docRef, updatedProfile.encodeToMap()!!)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refundCredit(userId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val docRef = usersCollection.document(userId)
                val snapshot = transaction.get(docRef)
                val profile = snapshot.toObjectSerializable<UserProfile>()
                    ?: throw IllegalStateException("User profile not found")

                val updatedProfile = profile.copy(
                    availableCredits = minOf(profile.availableCredits + 1, WEEKLY_CREDITS)
                )
                transaction.set(docRef, updatedProfile.encodeToMap()!!)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetCreditsIfNeeded(userId: String): Result<Int> {
        return try {
            val result = db.runTransaction { transaction ->
                val docRef = usersCollection.document(userId)
                val snapshot = transaction.get(docRef)
                val profile = snapshot.toObjectSerializable<UserProfile>()
                    ?: throw IllegalStateException("User profile not found")

                val now = System.currentTimeMillis()
                if (profile.creditsResetDate == null || profile.creditsResetDate < now) {
                    val updatedProfile = profile.copy(
                        availableCredits = WEEKLY_CREDITS,
                        creditsResetDate = now + WEEK_MILLIS
                    )
                    transaction.set(docRef, updatedProfile.encodeToMap()!!)
                    WEEKLY_CREDITS
                } else {
                    profile.availableCredits
                }
            }.await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
