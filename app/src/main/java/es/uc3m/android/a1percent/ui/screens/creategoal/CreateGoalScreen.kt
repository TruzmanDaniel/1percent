package es.uc3m.android.a1percent.ui.screens.creategoal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.Category

@Composable
fun CreateGoalScreen(navController: NavController) {

    var goalText by remember { mutableStateOf("") }

    var difficulty by remember { mutableFloatStateOf(3f) }

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
                    title = goalText,
                    category = Category.PERSONAL,
                    difficulty = difficulty.toInt(),
                    xp = difficulty.toInt() * 20
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
