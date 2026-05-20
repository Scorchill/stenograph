package com.stenograph.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stenograph.app.data.PreferencesManager
import com.stenograph.app.ui.dictation.DictationScreen
import com.stenograph.app.ui.pairing.PairingScreen

@Composable
fun StenographNavGraph() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val isPaired by prefs.isPaired.collectAsState(initial = false)

    val navController = rememberNavController()
    val startDest = if (isPaired) "dictation" else "pairing"

    NavHost(navController = navController, startDestination = startDest) {
        composable("pairing") {
            PairingScreen(onPaired = {
                navController.navigate("dictation") {
                    popUpTo("pairing") { inclusive = true }
                }
            })
        }
        composable("dictation") {
            DictationScreen(
                onRePair = {
                    navController.navigate("pairing") {
                        popUpTo("dictation") { inclusive = true }
                    }
                }
            )
        }
    }
}
