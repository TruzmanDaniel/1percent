package es.uc3m.android.a1percent.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.SessionRepository
import kotlinx.coroutines.launch

/**
 * Root ViewModel for the app.
 * Keeps startup/session restoration out of the repository singleton.
 */
class AppViewModel : ViewModel() {

	init {
		viewModelScope.launch {
			SessionRepository.restoreCurrentUserIfAvailable()
		}
	}
}

