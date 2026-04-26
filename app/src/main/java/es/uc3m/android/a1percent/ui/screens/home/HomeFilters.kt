package es.uc3m.android.a1percent.ui.screens.home

/**
 * Filters and sorting options for Home tasks.
 * Keeping this separate makes adding future filters straightforward.
 */
data class HomeFilters(
    val showOnlyMissions: Boolean = false,
    val sortBy: HomeSort = HomeSort.NONE
)

enum class HomeFilterKey {
    MISSIONS,
    SORT_BY_DATE
}

data class HomeFilterUiItem(
    val key: HomeFilterKey,
    val label: String,
    val isSelected: Boolean,
    val order: Int
)

fun buildHomeFilterUiItems(filters: HomeFilters): List<HomeFilterUiItem> {
    val items = listOf(
        HomeFilterUiItem(
            key = HomeFilterKey.MISSIONS,
            label = "Missions",
            isSelected = filters.showOnlyMissions,
            order = 0
        ),
        HomeFilterUiItem(
            key = HomeFilterKey.SORT_BY_DATE,
            label = "Sort by Date",
            isSelected = filters.sortBy == HomeSort.DATE_ASC,
            order = 1
        )
    )
    // Sort by default order, then by selection
    return items.sortedWith(compareByDescending<HomeFilterUiItem> { it.isSelected }.thenBy { it.order })
}

enum class HomeSort {
    NONE,
    DATE_ASC
}

