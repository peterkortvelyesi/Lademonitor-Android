package com.dominiqueherbrigpersonalteam.lademonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dominiqueherbrigpersonalteam.lademonitor.ui.LademonitorRoot
import com.dominiqueherbrigpersonalteam.lademonitor.ui.theme.LademonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LademonitorTheme {
                LademonitorRoot()
            }
        }
    }
}
