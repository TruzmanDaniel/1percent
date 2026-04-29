package es.uc3m.android.a1percent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import es.uc3m.android.a1percent.navigation.NavGraph
import es.uc3m.android.a1percent.notifications.AlarmScheduler
import es.uc3m.android.a1percent.notifications.NotificationHelper
import es.uc3m.android.a1percent.ui.theme._1percentTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    companion object {
        // Holds the navigation destination requested by a notification tap.
        // NavGraph observes this and navigates when the user is authenticated.
        val pendingNavigation = MutableStateFlow<String?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        NotificationHelper.createChannels(this)
        AlarmScheduler.scheduleDailyReminder(this)

        // App was launched (or restarted) by tapping a notification
        handleNavigationIntent(intent)

        setContent {
            _1percentTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NavGraph()
                }
            }
        }
    }

    // Called when the app is already running and a notification is tapped (FLAG_SINGLE_TOP)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent) {
        intent.getStringExtra(NotificationHelper.EXTRA_NAVIGATE_TO)?.let { destination ->
            pendingNavigation.value = destination
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
