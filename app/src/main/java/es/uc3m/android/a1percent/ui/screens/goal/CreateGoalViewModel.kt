package es.uc3m.android.a1percent.ui.screens.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateGoalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGoalUiState())
    val uiState: StateFlow<CreateGoalUiState> = _uiState.asStateFlow()

    fun onGoalNameChange(newValue: String) {
        _uiState.update { it.copy(goalName = newValue) }
    }

    fun onDifficultyChange(newValue: Float) {
        _uiState.update { it.copy(difficulty = newValue) }
    }

    fun createGoal(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = SessionRepository.currentUser.value?.id
        println("DEBUG userId: $userId")
        if (userId == null) {
            onError("No user logged in")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val goal = Goal(
                    title = _uiState.value.goalName,
                    category = Category.AUTOMATIC,
                    difficulty = _uiState.value.difficulty.toInt(),
                    xp = _uiState.value.difficulty.toInt() * 50
                )

                val result = GoalRepository.saveGoal(userId, goal)

                result.onSuccess { onSuccess() }
                result.onFailure { onError(it.message ?: "Error creating goal") }
            } catch (e: Exception) {
                onError(e.message ?: "Error creating goal")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateGoalUiState()
    }
}