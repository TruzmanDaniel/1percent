package es.uc3m.android.a1percent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import es.uc3m.android.a1percent.navigation.NavGraph
import es.uc3m.android.a1percent.ui.theme._1percentTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            _1percentTheme {

                NavGraph()

            }

        }
    }
}