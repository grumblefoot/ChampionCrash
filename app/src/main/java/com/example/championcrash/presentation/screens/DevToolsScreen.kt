package com.example.championcrash.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.example.championcrash.domain.models.DevOptions

@Composable
fun DevToolsScreen(
    options: DevOptions,
    onOptionsChanged: (DevOptions) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dev Tools", fontSize = 18.sp)

        // Axis Toggle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Axis: ${if (options.useYAxis) "Y (Pitch)" else "X (Roll)"}", fontSize = 12.sp)
            Button(onClick = { onOptionsChanged(options.copy(useYAxis = !options.useYAxis)) }) {
                Text("Toggle Axis")
            }
        }

        // Sensitivity
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sensitivity: ${options.sensitivity}", fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOptionsChanged(options.copy(sensitivity = options.sensitivity - 1f)) }) {
                    Text("-")
                }
                Button(onClick = { onOptionsChanged(options.copy(sensitivity = options.sensitivity + 1f)) }) {
                    Text("+")
                }
            }
        }

        // Base Angle Offset
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Base Angle: ${options.baseAngleDegrees}°", fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOptionsChanged(options.copy(baseAngleDegrees = options.baseAngleDegrees - 5f)) }) {
                    Text("-")
                }
                Button(onClick = { onOptionsChanged(options.copy(baseAngleDegrees = options.baseAngleDegrees + 5f)) }) {
                    Text("+")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
