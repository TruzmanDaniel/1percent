package es.uc3m.android.a1percent.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import es.uc3m.android.a1percent.navigation.AppScreens

@Composable
fun ProfileScreen(navController: NavController, text: String?) {
    ProfileBodyContent(navController, text)
}

@Composable
fun ProfileBodyContent(navController: NavController, text: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("This is your Profile")

        text?.let { // if text exists (it is optional parameter)
            Text(it)
        }

        Button(onClick = { navController.navigate(AppScreens.HomeScreen.route) }) {
            Text("Home")
        }
    }
}
