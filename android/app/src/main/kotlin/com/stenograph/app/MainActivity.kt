package com.stenograph.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stenograph.app.ui.navigation.StenographNavGraph
import com.stenograph.app.ui.theme.StenographTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StenographTheme {
                StenographNavGraph()
            }
        }
    }
}
