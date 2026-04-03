package es.uc3m.android.a1percent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import es.uc3m.android.a1percent.navigation.NavGraph
import es.uc3m.android.a1percent.ui.theme._1percentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { // Compose Entry Point
            _1percentTheme { // Apply our custom theme defined in ui/theme/theme.kt

                // A Surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    // HomeScreen1()
                    // We call the component that handles navigation, it already knows the starting Screen
                    NavGraph()
                }
            }
        }
    }
}


@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    _1percentTheme {
        NavGraph()
    }
}