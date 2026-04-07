package es.uc3m.android.a1percent.data

import es.uc3m.android.a1percent.data.model.enums.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Memory source for user-created (CUSTOM) categories.
 * For the moment data is not persistent, it is reset when the app is restarted
 */
object TaskCategoryRepository {

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    val predefinedCategories: List<Category> =
        Category.entries.filter { it != Category.AUTOMATIC }

    // Automatic categorization must only evaluate predefined categories (except 'AUTOMATIC')
    val inferenceCandidates: List<Category> = predefinedCategories

    fun addCustomCategory(rawName: String): String? {
        val normalized = rawName.trim()
        if (normalized.isEmpty())
            return null

        // Check already existing
        val existing = _customCategories.value.firstOrNull { it.equals(normalized, ignoreCase = true) }
        if (existing != null)
            return existing

        _customCategories.update { it + normalized }
        return normalized
    }
}

// TODO: persist with DataStore/Room when persistence layer is introduced.
// Significa que hay que cambiar la forma en la que recordamos la categoria con estados a tenerlo simplemente en la database?
// SÍ: ahora se guardan con StateFlow: si se cierra la app, se pierden