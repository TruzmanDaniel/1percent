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
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase connection
        FirebaseApp.initializeApp(this)

        // Connect emulators
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)

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