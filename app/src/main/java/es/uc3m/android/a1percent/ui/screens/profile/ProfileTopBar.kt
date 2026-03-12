package es.uc3m.android.a1percent.ui.screens.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

// PROFILE TOP BAR: custom top bar for the Profile screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(
    username: String = "username_placeholder", // Will be replaced with real user data
    onBack: () -> Unit // Function that returns to previous screen (popBackStack)
) {
    TopAppBar(
        title = { Text(username) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = "Back")
            }
        }
    )
}

