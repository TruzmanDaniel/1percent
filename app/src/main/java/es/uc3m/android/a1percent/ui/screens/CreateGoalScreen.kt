package es.uc3m.android.a1percent.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.model.Goal
import java.util.UUID

@Composable
fun CreateGoalScreen(navController: NavController) {

    var goalText by remember { mutableStateOf("") }

    var difficulty by remember { mutableStateOf(3f) }

    Column(
        modifier = Modifier.padding(20.dp)
    ) {

        Text(
            text = "Define your objective",
            style = MaterialTheme.typography.headlineSmall
        )

        TextField(

            value = goalText,

            onValueChange = {
                goalText = it
            },

            label = {
                Text("Your goal")
            },

            modifier = Modifier.fillMaxWidth()
        )

        Text("Difficulty: ${difficulty.toInt()}")

        Slider(

            value = difficulty,

            onValueChange = {
                difficulty = it
            },

            valueRange = 1f..5f

        )

        Button(

            onClick = {

                val goal = Goal(

                    id = UUID.randomUUID().toString(),

                    title = goalText,

                    difficulty = difficulty.toInt()

                )

                println(goal)

                navController.navigate("home")

            },

            modifier = Modifier.fillMaxWidth()

        ) {

            Text("Create missions")

        }

    }

}