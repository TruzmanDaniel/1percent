package es.uc3m.android.a1percent.ui.screens.home

data class HomeFilters(
    val missionsFilter: HomeMissionsFilter = HomeMissionsFilter.ALL,
    val sortBy: HomeSort = HomeSort.NONE,
    val statusFilter: HomeStatusFilter = HomeStatusFilter.PENDING
)

enum class HomeStatusFilter(val label: String) {
    ALL("Status: All"),
    PENDING("Status: Pending"),
    COMPLETED("Status: Completed")
}

enum class HomeFilterKey {
    STATUS,
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
            key = HomeFilterKey.STATUS,
            label = filters.statusFilter.label,
            isSelected = filters.statusFilter != HomeStatusFilter.PENDING,
            order = 0
        ),
        HomeFilterUiItem(
            key = HomeFilterKey.MISSIONS,
            label = filters.missionsFilter.label,
            isSelected = filters.missionsFilter != HomeMissionsFilter.ALL,
            order = 1
        ),
        HomeFilterUiItem(
            key = HomeFilterKey.SORT_BY_DATE,
            label = "Sort by Date",
            isSelected = filters.sortBy == HomeSort.DATE_ASC,
            order = 2
        )
    )
    return items.sortedWith(compareByDescending<HomeFilterUiItem> { it.isSelected }.thenBy { it.order })
}

enum class HomeSort {
    NONE,
    DATE_ASC
}

enum class HomeMissionsFilter(val label: String) {
    ALL("All"),
    MISSIONS("Missions"),
    TASKS("Tasks")
}

