package es.uc3m.android.a1percent.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.navigation.AppScreens

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    // Observing the screen state (DATA) to update the UI when it changes
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeBodyContent(uiState = uiState,
        onProfileClick = {val param = viewModel.onProfileClicked() // parameter returned by ViewModel
            navController.navigate(AppScreens.ProfileScreen.route + "/$param")}
        )
}

@Composable
fun HomeBodyContent(uiState: HomeUiState,
                    onProfileClick: () -> Unit  // Callback function
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Welcome, ${uiState.user.name}")
        Text("Goal: ${uiState.goal.title}")
        Text("Tasks: ${uiState.tasks.size}")

        Button(onClick = onProfileClick) {
            Text("Profile")
        }
    }
}
