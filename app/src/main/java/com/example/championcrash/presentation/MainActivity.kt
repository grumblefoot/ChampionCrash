package com.example.championcrash.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.material3.AppScaffold
import com.example.championcrash.domain.models.DevOptions
import com.example.championcrash.presentation.screens.DevToolsScreen
import com.example.championcrash.presentation.screens.GameScreen
import com.example.championcrash.presentation.screens.MenuScreen
import com.example.championcrash.presentation.theme.ChampionCrashTheme

enum class AppScreen {
    MENU,
    GAME,
    DEV_TOOLS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChampionCrashApp()
        }
    }
}

@Composable
fun ChampionCrashApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.MENU) }
    var devOptions by remember { mutableStateOf(DevOptions()) }

    ChampionCrashTheme {
        AppScaffold {
            when (currentScreen) {
                AppScreen.MENU -> {
                    MenuScreen(
                        onStartGame = { currentScreen = AppScreen.GAME },
                        onDevTools = { currentScreen = AppScreen.DEV_TOOLS }
                    )
                }
                AppScreen.DEV_TOOLS -> {
                    DevToolsScreen(
                        options = devOptions,
                        onOptionsChanged = { devOptions = it },
                        onBack = { currentScreen = AppScreen.MENU }
                    )
                }
                AppScreen.GAME -> {
                    GameScreen(
                        devOptions = devOptions,
                        onBackToMenu = { currentScreen = AppScreen.MENU }
                    )
                }
            }
        }
    }
}
