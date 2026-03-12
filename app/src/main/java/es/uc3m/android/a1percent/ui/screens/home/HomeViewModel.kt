package es.uc3m.android.a1percent.ui.screens.home

import androidx.lifecycle.ViewModel
import es.uc3m.android.a1percent.data.model.MockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class to manage logic and screen state. It lets the interface observe changes and update
 */


class HomeViewModel : ViewModel() {

    // Modified by the ViewModel when something changes:
    private val _uiState = MutableStateFlow(
        HomeUiState(
            user = MockData.mockUser,
            tasks = MockData.mockTasks,
            goal = MockData.mockGoal
        ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
        // .asState Flow() makes it Immutable. 'uiState' is observed by the UI

    // Function that returns a parameter to be passed (it is an example)
    fun onProfileClicked(): String {
        return "Parametro Hardcodeado en HomeViewModel"
    }
}
