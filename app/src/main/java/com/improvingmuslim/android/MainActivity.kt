package com.improvingmuslim.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.improvingmuslim.android.ui.RootScreen
import com.improvingmuslim.android.ui.theme.ImprovingMuslimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImprovingMuslimTheme {
                RootScreen()
            }
        }
    }
}
