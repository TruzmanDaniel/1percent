package es.uc3m.android.a1percent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import es.uc3m.android.a1percent.navigation.AppScreens

/** Creado por César. Otro formato de hacer las screens, más limpio. **/



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, text: String?) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("PROFILE") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.Filled.ArrowBackIosNew, contentDescription = "Arrow Back")
                }}
        )
    }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            ProfileBodyContent(navController, text)
        }
    }
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

        Button(onClick = {navController.navigate(AppScreens.HomeScreen.route) }) {
            Text("Home")
        }
    }
}
