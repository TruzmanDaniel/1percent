package es.uc3m.android.a1percent.ui.screens.splash

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.uc3m.android.a1percent.R
import kotlinx.coroutines.delay

@SuppressLint("LocalContextResourcesRead")
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    splashDurationMillis: Long = 2200L
) {
    LaunchedEffect(Unit) {
        delay(splashDurationMillis)
        onSplashFinished()
    }

    val context = LocalContext.current
    val logoResId = context.resources.getIdentifier("splash_logo", "drawable", context.packageName)
    val finalLogoResId = if (logoResId != 0) logoResId else R.drawable.ic_launcher_foreground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = finalLogoResId),
            contentDescription = "App logo",
            modifier = Modifier.size(300.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Earn your 1% \nStay Consistent",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

