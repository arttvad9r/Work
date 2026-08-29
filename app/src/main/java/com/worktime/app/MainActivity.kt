package com.worktime.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.worktime.app.ui.WorkTimeApp

class MainActivity : ComponentActivity() {
    private var openTodayRequest by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(160L)
                .withEndAction { provider.remove() }
                .start()
        }

        enableEdgeToEdge()
        consumeLaunchIntent(intent)
        val container = (application as WorkTimeApplication).container
        setContent {
            WorkTimeApp(
                container = container,
                openTodayRequest = openTodayRequest,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchIntent(intent)
    }

    private fun consumeLaunchIntent(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
            openTodayRequest++
        }
    }

    companion object {
        const val EXTRA_OPEN_TODAY = "com.worktime.app.extra.OPEN_TODAY"
    }
}
