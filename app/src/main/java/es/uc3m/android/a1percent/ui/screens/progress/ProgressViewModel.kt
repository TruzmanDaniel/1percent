package es.uc3m.android.a1percent.ui.screens.progress
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
/**
 * Minimal ViewModel for Progress.
 * It mirrors the Home pattern and will later expose real progress data from a repository.
 */
class ProgressViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
}
