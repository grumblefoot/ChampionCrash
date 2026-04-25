package com.example.championcrash.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text

@Composable
fun MenuScreen(
    onStartGame: () -> Unit,
    onDevTools: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onStartGame) {
            Text("Start Game")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDevTools) {
            Text("Dev Tools")
        }
    }
}
