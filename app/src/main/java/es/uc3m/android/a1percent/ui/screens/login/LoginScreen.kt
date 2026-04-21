package es.uc3m.android.a1percent.ui.screens.login

import android.se.omapi.Session
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.navigation.AppScreens
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to 1%",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                showError = false 
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = showError
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                showError = false
            },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = showError,
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                }
            }
        )

        if (showError) {
            Text(
                text = errorMessage ?: "Invalid email or password",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                ///**
                if (email.isBlank() || password.isBlank()) {
                    showError = true
                    errorMessage = "Please fill in all fields"
                    return@Button
                }
                isLoading = true
                showError = false
                errorMessage = null

                SessionRepository.loginWithFirebase(
                    email = email.trim(),
                    password = password
                ) { success, error ->
                    isLoading = false
                    if (success) {
                        navController.navigate(AppScreens.HomeScreen.route) {
                            popUpTo(AppScreens.LoginScreen.route){ inclusive = true}
                        }
                    } else {
                        showError = true
                        errorMessage = error ?: "Invalid email or password"
                    }
                }
                //*/

                /**
                if (email.isBlank() || password.isBlank()) {
                    showError = true
                    errorMessage = "Please fill in all fields"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    showError = false
                    errorMessage = null

                    val result = SessionRepository.loginWithApi(
                        email = email.trim(),
                        password = password
                    )

                    result.onSuccess {
                        navController.navigate(AppScreens.HomeScreen.route) {
                            popUpTo(AppScreens.LoginScreen.route){ inclusive = true}
                        }
                    }.onFailure { error ->
                        showError = true
                        errorMessage = error.message ?: "Invalid email or password"
                        println("LOGIN ERROR: ${error.message}")
                    }

                    isLoading = false
                }
                */
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(if (isLoading) "Logging in..." else "Login", fontSize = 18.sp)
        }

        TextButton(
            onClick = {
                navController.navigate(AppScreens.RegisterScreen.route)
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Don't have an account? Sign Up")
        }
    }
}
