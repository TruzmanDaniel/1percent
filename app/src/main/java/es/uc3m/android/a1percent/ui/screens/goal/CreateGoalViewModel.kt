package es.uc3m.android.a1percent.ui.screens.goal

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the state and form logic for goal creation.
 * Minimal implementation for now, ready for future expansion.
 */
class CreateGoalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGoalUiState())
    val uiState: StateFlow<CreateGoalUiState> = _uiState.asStateFlow()

    fun onGoalNameChange(newValue: String) {
        _uiState.update { it.copy(goalName = newValue) }
    }

    fun onDifficultyChange(newValue: Float) {
        _uiState.update { it.copy(difficulty = newValue) }
    }

    // TODO: Add more event handlers when goal definition expands
    // - onCategoryChange
    // - onDeadlineChange
    // - onDescriptionChange
    // - etc.
}
