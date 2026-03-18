package es.uc3m.android.a1percent.ui.screens.creategoal

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(navController: NavController) {
    var goalText by remember { mutableStateOf("") }
    var difficulty by remember { mutableFloatStateOf(3f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Define Objective") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "What is your main goal?",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                label = { Text("Your goal") },
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text(
                    text = "Difficulty: ${difficulty.toInt()}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val goal = Goal(
                        title = goalText,
                        category = Category.PERSONAL,
                        difficulty = difficulty.toInt(),
                        xp = difficulty.toInt() * 20
                    )
                    // Navigation logic: go back or home
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create missions")
            }
        }
    }
}
