package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.enums.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object TaskCategoryRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    val predefinedCategories: List<Category> = Category.entries.toList()

    val inferenceCandidates: List<Category> = predefinedCategories

    fun observeCustomCategories(userId: String) {
        db.collection("users").document(userId).collection("categories")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val categories = snapshot.documents.mapNotNull { it.getString("name") }
                    _customCategories.value = categories
                }
            }
    }

    suspend fun addCustomCategory(userId: String, rawName: String): String? {
        val normalized = rawName.trim()
        if (normalized.isEmpty()) return null

        val existing = _customCategories.value.firstOrNull { it.equals(normalized, ignoreCase = true) }
        if (existing != null) return existing

        return try {
            db.collection("users").document(userId).collection("categories")
                .add(mapOf("name" to normalized))
                .await()
            normalized
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteCustomCategory(userId: String, name: String): Result<Unit> {
        return try {
            val snapshot = db.collection("users").document(userId).collection("categories")
                .whereEqualTo("name", name)
                .get()
                .await()
            snapshot.documents.forEach { it.reference.delete().await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
