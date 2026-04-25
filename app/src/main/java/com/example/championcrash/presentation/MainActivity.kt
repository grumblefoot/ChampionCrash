package com.example.championcrash.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.championcrash.presentation.theme.ChampionCrashTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

enum class GameState {
    IDLE,
    CHARGING,
    FLYING,
    STOPPED
}

data class GameData(
    val x: Float = 0f,
    val y: Float = 0f,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val gameState: GameState = GameState.IDLE,
    val chargePower: Float = 0f,
    val chargeIncreasing: Boolean = true
)

@Composable
fun WearApp() {
    ChampionCrashTheme {
        AppScaffold {
            LaunchGameScreen()
        }
    }
}

@Composable
fun LaunchGameScreen() {
    var gameData by remember { mutableStateOf(GameData()) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        
        while (true) {
            val currentFrameTime = withFrameNanos { it }
            val deltaSeconds = (currentFrameTime - lastFrameTime) / 1_000_000_000f
            lastFrameTime = currentFrameTime

            gameData = when (gameData.gameState) {
                GameState.CHARGING -> {
                    // Oscillate chargePower rapidly between 0 and 100
                    val chargeSpeed = 200f * deltaSeconds // 200 units per second
                    var newCharge = gameData.chargePower + if (gameData.chargeIncreasing) chargeSpeed else -chargeSpeed
                    var newIncreasing = gameData.chargeIncreasing

                    if (newCharge >= 100f) {
                        newCharge = 100f
                        newIncreasing = false
                    } else if (newCharge <= 0f) {
                        newCharge = 0f
                        newIncreasing = true
                    }

                    gameData.copy(
                        chargePower = newCharge,
                        chargeIncreasing = newIncreasing
                    )
                }
                GameState.FLYING -> {
                    // Apply gravity
                    val gravity = -400f * deltaSeconds
                    var newVelocityY = gameData.velocityY + gravity
                    var newVelocityX = gameData.velocityX
                    
                    val newX = gameData.x + newVelocityX * deltaSeconds
                    var newY = gameData.y + newVelocityY * deltaSeconds
                    
                    var newState = GameState.FLYING
                    if (newY <= 0f) {
                        newY = 0f
                        if (abs(newVelocityY) > 30f || newVelocityX > 10f) {
                            // Bounce!
                            newVelocityY = -newVelocityY * 0.5f // Lose vertical speed
                            newVelocityX *= 0.9f // Air/Ground friction
                            
                            // If it's rolling very slowly, stop it
                            if (abs(newVelocityY) < 10f && newVelocityX < 10f) {
                                newState = GameState.STOPPED
                                newVelocityY = 0f
                                newVelocityX = 0f
                            }
                        } else {
                            newState = GameState.STOPPED
                            newVelocityY = 0f
                            newVelocityX = 0f
                        }
                    }

                    gameData.copy(
                        x = newX,
                        y = newY,
                        velocityX = newVelocityX,
                        velocityY = newVelocityY,
                        gameState = newState
                    )
                }
                else -> gameData
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    
                    if (gameData.gameState == GameState.IDLE || gameData.gameState == GameState.STOPPED) {
                        // Reset and start charging
                        gameData = GameData(gameState = GameState.CHARGING, chargePower = 0f)
                    }
                    
                    val up = waitForUpOrCancellation()
                    if (up != null && gameData.gameState == GameState.CHARGING) {
                        // Release and launch
                        val launchPower = gameData.chargePower
                        // Calculate velocity based on power (e.g., angle is fixed at 45 degrees)
                        val maxVelocity = 600f
                        val velocity = (launchPower / 100f) * maxVelocity
                        
                        // Roughly 45 degree launch
                        gameData = gameData.copy(
                            gameState = GameState.FLYING,
                            velocityX = velocity,
                            velocityY = velocity
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val groundY = canvasHeight * 0.8f // Ground is at 80% of screen height
            
            // Camera tracking
            val startOffsetX = 50f
            val cameraX = if (gameData.x > (canvasWidth / 2f - startOffsetX)) {
                gameData.x - (canvasWidth / 2f - startOffsetX)
            } else {
                0f
            }

            // Draw Ground
            drawLine(
                color = Color.Green,
                start = Offset(0f, groundY),
                end = Offset(canvasWidth, groundY),
                strokeWidth = 4.dp.toPx()
            )

            // Draw Ground Markers for sense of speed
            val markerSpacing = 100f
            val firstMarkerIndex = (cameraX / markerSpacing).toInt()
            for (i in 0..((canvasWidth / markerSpacing).toInt() + 2)) {
                val markerX = (firstMarkerIndex + i) * markerSpacing
                val screenMarkerX = markerX - cameraX + startOffsetX
                drawLine(
                    color = Color.LightGray,
                    start = Offset(screenMarkerX, groundY),
                    end = Offset(screenMarkerX, groundY + 10.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Draw Character (Circle)
            val circleRadius = 15.dp.toPx()
            val screenX = gameData.x - cameraX + startOffsetX
            val screenY = groundY - circleRadius - gameData.y // y goes up

            drawCircle(
                color = Color.Red,
                radius = circleRadius,
                center = Offset(screenX, screenY)
            )
            
            // Draw Charging Indicator Arrow
            if (gameData.gameState == GameState.CHARGING) {
                val arrowLength = 20.dp.toPx() + (gameData.chargePower / 100f) * 60.dp.toPx()
                val angle = PI / 4 // 45 degrees
                
                val arrowEndX = screenX + (arrowLength * cos(angle)).toFloat()
                val arrowEndY = screenY - (arrowLength * sin(angle)).toFloat()
                
                drawLine(
                    color = Color.Yellow,
                    start = Offset(screenX, screenY),
                    end = Offset(arrowEndX, arrowEndY),
                    strokeWidth = 4.dp.toPx()
                )
                
                // Draw arrow head
                val headLength = 10.dp.toPx()
                val angle1 = angle + PI / 6
                val angle2 = angle - PI / 6
                
                val head1X = arrowEndX - (headLength * cos(angle1)).toFloat()
                val head1Y = arrowEndY + (headLength * sin(angle1)).toFloat()
                
                val head2X = arrowEndX - (headLength * cos(angle2)).toFloat()
                val head2Y = arrowEndY + (headLength * sin(angle2)).toFloat()
                
                drawLine(
                    color = Color.Yellow,
                    start = Offset(arrowEndX, arrowEndY),
                    end = Offset(head1X, head1Y),
                    strokeWidth = 4.dp.toPx()
                )
                drawLine(
                    color = Color.Yellow,
                    start = Offset(arrowEndX, arrowEndY),
                    end = Offset(head2X, head2Y),
                    strokeWidth = 4.dp.toPx()
                )
            }

            // Draw Charging Bar
            if (gameData.gameState == GameState.CHARGING) {
                val barWidth = 100.dp.toPx()
                val barHeight = 20.dp.toPx()
                val barX = (canvasWidth - barWidth) / 2f
                val barY = canvasHeight * 0.2f

                // Background
                drawRoundRect(
                    color = Color.Gray,
                    topLeft = Offset(barX, barY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(10f)
                )

                // Foreground (Current Power)
                val currentPowerWidth = barWidth * (gameData.chargePower / 100f)
                drawRoundRect(
                    color = Color.Yellow,
                    topLeft = Offset(barX, barY),
                    size = Size(currentPowerWidth, barHeight),
                    cornerRadius = CornerRadius(10f)
                )
            }

            // Draw Distance Text
            if (gameData.gameState == GameState.FLYING || gameData.gameState == GameState.STOPPED) {
                val distanceText = "Dist: ${gameData.x.toInt()}m"
                val textLayoutResult = textMeasurer.measure(
                    text = distanceText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = (canvasWidth - textLayoutResult.size.width) / 2f,
                        y = canvasHeight * 0.1f
                    )
                )
            }
            
            // Draw Tap to start message
            if (gameData.gameState == GameState.IDLE) {
                val textLayoutResult = textMeasurer.measure(
                    text = "Tap & Hold to Charge",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = (canvasWidth - textLayoutResult.size.width) / 2f,
                        y = canvasHeight * 0.4f
                    )
                )
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun LaunchGameScreenPreview() {
    WearApp()
}
