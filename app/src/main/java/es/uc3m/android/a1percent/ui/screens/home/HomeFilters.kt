package es.uc3m.android.a1percent.ui.screens.home

import androidx.annotation.StringRes
import es.uc3m.android.a1percent.R

data class HomeFilters(
    val missionsFilter: HomeMissionsFilter = HomeMissionsFilter.ALL,
    val sortBy: HomeSort = HomeSort.NONE,
    val statusFilter: HomeStatusFilter = HomeStatusFilter.PENDING
)

enum class HomeStatusFilter(@StringRes val labelRes: Int) {
    ALL(R.string.home_filter_status_all),
    PENDING(R.string.home_filter_status_pending),
    COMPLETED(R.string.home_filter_status_completed)
}

enum class HomeFilterKey {
    STATUS,
    MISSIONS,
    SORT_BY_DATE
}

data class HomeFilterUiItem(
    val key: HomeFilterKey,
    @StringRes val labelRes: Int,
    val isSelected: Boolean,
    val order: Int
)

fun buildHomeFilterUiItems(filters: HomeFilters): List<HomeFilterUiItem> {
    val items = listOf(
        HomeFilterUiItem(
            key = HomeFilterKey.STATUS,
            labelRes = filters.statusFilter.labelRes,
            isSelected = filters.statusFilter != HomeStatusFilter.PENDING,
            order = 0
        ),
        HomeFilterUiItem(
            key = HomeFilterKey.MISSIONS,
            labelRes = filters.missionsFilter.labelRes,
            isSelected = filters.missionsFilter != HomeMissionsFilter.ALL,
            order = 1
        ),
        HomeFilterUiItem(
            key = HomeFilterKey.SORT_BY_DATE,
            labelRes = R.string.home_filter_sort_by_date,
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

enum class HomeMissionsFilter(@StringRes val labelRes: Int) {
    ALL(R.string.home_filter_all),
    MISSIONS(R.string.home_filter_missions),
    TASKS(R.string.home_filter_tasks)
}
