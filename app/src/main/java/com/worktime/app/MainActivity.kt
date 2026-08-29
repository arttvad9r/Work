package com.worktime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.worktime.app.ui.WorkTimeApp

class MainActivity : ComponentActivity() {
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
        val container = (application as WorkTimeApplication).container
        val openTodayOnStart = intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)
        setContent {
            WorkTimeApp(
                container = container,
                openTodayOnStart = openTodayOnStart,
            )
        }
    }

    companion object {
        const val EXTRA_OPEN_TODAY = "com.worktime.app.extra.OPEN_TODAY"
    }
}
