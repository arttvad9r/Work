package com.worktime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.worktime.app.ui.WorkTimeApp
import com.worktime.app.ui.theme.WorkTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkTimeTheme {
                WorkTimeApp()
            }
        }
    }
}
