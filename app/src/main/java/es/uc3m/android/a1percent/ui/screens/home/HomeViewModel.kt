package es.uc3m.android.a1percent.ui.screens.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class to manage logic and screen state. It lets the interface observe changes and update
 */


class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Function that returns a parameter to be passed (it is an example)
    fun onProfileClicked(): String {
        return "Parametro Hardcodeado en HomeViewModel"
    }
}
