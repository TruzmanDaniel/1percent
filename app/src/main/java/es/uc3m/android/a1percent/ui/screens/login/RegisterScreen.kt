package es.uc3m.android.a1percent.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.navigation.AppScreens
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(navController: NavController){
    var username by remember { mutableStateOf("")}
    var email by remember { mutableStateOf("")}
    var password by remember { mutableStateOf("")}

    var isLoading by remember { mutableStateOf(false)}
    var errorMessage by remember { mutableStateOf<String?>(null)}
    var successMessage by remember { mutableStateOf<String?>(null)}

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create your account",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = null
                successMessage = null
            },
            label = { Text("Username")},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
                successMessage = null
            },
            label = { Text("Email")},
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
                successMessage = null
            },
            label = { Text("Password")},
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)

            )
        }

        if (successMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = successMessage!!,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)

            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (username.isBlank() || email.isBlank() || password.isBlank()) {
                    errorMessage = "Please fill in all fields"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    errorMessage = null
                    successMessage = null

                    val result = SessionRepository.registerWithApi(
                        email = email,
                        password = password,
                        username = username
                    )

                    result.onSuccess {
                        successMessage = "Account created successfully! Please log in."
                        navController.navigate(AppScreens.LoginScreen.route) {
                            popUpTo(AppScreens.RegisterScreen.route) { inclusive = true }
                        }
                    }.onFailure { error ->
                        errorMessage = error.message ?: "Unknown error"
                    }

                    isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.medium,
            enabled = !isLoading
        )   {
            Text(
                text = if (isLoading) "Registering..." else "Register",
                fontSize = 18.sp
            )
        }

        TextButton(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Already have an account? Login")
        }
    }
}