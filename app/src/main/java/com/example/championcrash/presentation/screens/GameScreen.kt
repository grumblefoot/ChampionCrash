package com.example.championcrash.presentation.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.example.championcrash.R
import com.example.championcrash.domain.models.BoostItem
import com.example.championcrash.domain.models.BoostType
import com.example.championcrash.domain.models.DevOptions
import com.example.championcrash.domain.models.GameData
import com.example.championcrash.domain.models.GameState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameScreen(
    devOptions: DevOptions,
    onBackToMenu: () -> Unit
) {
    var gameData by remember { mutableStateOf(GameData()) }
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE) }
    var maxDistance by remember { mutableIntStateOf(prefs.getInt("max_distance", 0)) }

    val androidVector = ImageVector.vectorResource(id = R.drawable.ic_launcher_foreground)
    val androidPainter = rememberVectorPainter(image = androidVector)
    val rocketPainter = rememberVectorPainter(image = Icons.Filled.Send)
    val barrelPainter = rememberVectorPainter(image = Icons.Filled.Warning)
    val stopPainter = rememberVectorPainter(image = Icons.Filled.Block)

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var currentTiltAngle by remember { mutableFloatStateOf((PI / 4).toFloat()) }
    var isPaused by remember { mutableStateOf(false) }

    DisposableEffect(devOptions) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val sensorVal = if (devOptions.useYAxis) it.values[1] else it.values[0]
                        val rawAngleDegrees = devOptions.baseAngleDegrees + (sensorVal * devOptions.sensitivity)
                        val rawAngleRadians = (rawAngleDegrees * (PI / 180f)).toFloat()
                        currentTiltAngle = rawAngleRadians.coerceIn(0f, PI.toFloat())
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        
        while (true) {
            val currentFrameTime = withFrameNanos { it }
            val deltaSeconds = (currentFrameTime - lastFrameTime) / 1_000_000_000f
            lastFrameTime = currentFrameTime

            if (!isPaused) {
                gameData = when (gameData.gameState) {
                GameState.CHARGING -> {
                    val chargeSpeed = 200f * deltaSeconds
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
                    val gravity = -400f * deltaSeconds
                    var newVelocityY = gameData.velocityY + gravity
                    var newVelocityX = gameData.velocityX
                    
                    val newX = gameData.x + newVelocityX * deltaSeconds
                    var newY = gameData.y + newVelocityY * deltaSeconds
                    
                    var newState = GameState.FLYING
                    if (newY <= 0f) {
                        newY = 0f
                        if (abs(newVelocityY) > 30f || newVelocityX > 10f) {
                            newVelocityY = -newVelocityY * 0.5f
                            newVelocityX *= 0.9f
                            
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

                    // Boost Generation
                    var currentBoosts = gameData.boosts.toMutableList()
                    var lastGenX = gameData.lastGeneratedBoostX

                    while (lastGenX < newX + 1500f) {
                        val rand = Random.nextFloat()
                        val type = when {
                            rand < 0.15f -> BoostType.STOP_SIGN
                            rand < 0.55f -> BoostType.ROCKET
                            else -> BoostType.BARREL
                        }
                        val bx = lastGenX + 200f + Random.nextFloat() * 400f
                        val by = if (type == BoostType.ROCKET) 100f + Random.nextFloat() * 300f else 0f
                        currentBoosts.add(BoostItem(bx, by, type))
                        lastGenX = bx
                    }

                    // Collision Detection
                    currentBoosts = currentBoosts.map { boost ->
                        if (boost.active) {
                            // Virtual centers (character icon is ~40dp, boost icon is ~30dp)
                            val charCenterX = newX + 20f
                            val charCenterY = newY + 20f
                            val boostCenterX = boost.x + 15f
                            val boostCenterY = boost.y + 15f
                            val dist = hypot(charCenterX - boostCenterX, charCenterY - boostCenterY)
                            
                            if (dist < 40f) { // Collision radius
                                if (boost.type == BoostType.STOP_SIGN) {
                                    newVelocityX = 0f
                                    newVelocityY = 0f
                                    newState = GameState.STOPPED
                                } else if (boost.type == BoostType.ROCKET) {
                                    newVelocityX += 400f // Big speed boost
                                    newVelocityY = maxOf(newVelocityY + 300f, 400f) // Shoot upwards
                                    newState = GameState.FLYING // Re-launch if was rolling/stopped
                                } else {
                                    newVelocityX += 200f // Small speed boost
                                    newVelocityY = maxOf(newVelocityY + 150f, 250f) // Small bounce
                                    newState = GameState.FLYING
                                }
                                boost.copy(active = false)
                            } else {
                                boost
                            }
                        } else {
                            boost
                        }
                    }.toMutableList()

                    // Cleanup passed boosts
                    currentBoosts.removeAll { it.x < newX - 1000f }
                    
                    if (newState == GameState.STOPPED) {
                        val finalDistance = newX.toInt()
                        if (finalDistance > maxDistance) {
                            maxDistance = finalDistance
                            with(prefs.edit()) {
                                putInt("max_distance", maxDistance)
                                apply()
                            }
                        }
                    }

                    gameData.copy(
                        x = newX,
                        y = newY,
                        velocityX = newVelocityX,
                        velocityY = newVelocityY,
                        gameState = newState,
                        boosts = currentBoosts,
                        lastGeneratedBoostX = lastGenX
                    )
                }
                else -> gameData
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    
                    if (gameData.gameState == GameState.IDLE && !isPaused) {
                        gameData = gameData.copy(gameState = GameState.CHARGING, chargePower = 0f)
                    }
                    
                    val up = waitForUpOrCancellation()
                    if (up != null && gameData.gameState == GameState.CHARGING && !isPaused) {
                        val launchPower = gameData.chargePower
                        val maxVelocity = 600f
                        val velocity = (launchPower / 100f) * maxVelocity
                        
                        gameData = gameData.copy(
                            gameState = GameState.FLYING,
                            velocityX = velocity * cos(currentTiltAngle),
                            velocityY = velocity * sin(currentTiltAngle)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val groundY = canvasHeight * 0.8f
            
            val startOffsetX = 50f
            val cameraX = if (gameData.x > (canvasWidth / 2f - startOffsetX)) {
                gameData.x - (canvasWidth / 2f - startOffsetX)
            } else {
                0f
            }

            drawLine(
                color = Color.Green,
                start = Offset(0f, groundY),
                end = Offset(canvasWidth, groundY),
                strokeWidth = 4.dp.toPx()
            )

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

            // Draw Boosts
            gameData.boosts.forEach { boost ->
                if (boost.active) {
                    val boostSize = 30.dp.toPx()
                    val screenBX = boost.x - cameraX + startOffsetX
                    val screenBY = groundY - boostSize - boost.y
                    
                    translate(left = screenBX, top = screenBY) {
                        if (boost.type == BoostType.ROCKET) {
                            with(rocketPainter) { draw(Size(boostSize, boostSize), colorFilter = ColorFilter.tint(Color.Cyan)) }
                        } else if (boost.type == BoostType.STOP_SIGN) {
                            with(stopPainter) { draw(Size(boostSize, boostSize), colorFilter = ColorFilter.tint(Color.Red)) }
                        } else {
                            with(barrelPainter) { draw(Size(boostSize, boostSize), colorFilter = ColorFilter.tint(Color(0xFFFFA500))) } // Orange
                        }
                    }
                }
            }

            val iconSize = 40.dp.toPx()
            val screenX = gameData.x - cameraX + startOffsetX
            val screenY = groundY - iconSize - gameData.y

            val centerX = screenX + iconSize / 2f
            val centerY = screenY + iconSize / 2f

            translate(left = screenX, top = screenY) {
                with(androidPainter) {
                    draw(Size(iconSize, iconSize))
                }
            }
            
            if (gameData.gameState == GameState.IDLE || gameData.gameState == GameState.CHARGING) {
                val baseArrowLength = 20.dp.toPx()
                val chargeExtension = if (gameData.gameState == GameState.CHARGING) (gameData.chargePower / 100f) * 60.dp.toPx() else 0f
                val arrowLength = baseArrowLength + chargeExtension
                val angle = currentTiltAngle.toDouble()
                
                val arrowEndX = centerX + (arrowLength * cos(angle)).toFloat()
                val arrowEndY = centerY - (arrowLength * sin(angle)).toFloat()
                
                drawLine(
                    color = Color.Yellow,
                    start = Offset(centerX, centerY),
                    end = Offset(arrowEndX, arrowEndY),
                    strokeWidth = 4.dp.toPx()
                )
                
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

            if (gameData.gameState == GameState.CHARGING) {
                val barWidth = 100.dp.toPx()
                val barHeight = 20.dp.toPx()
                val barX = (canvasWidth - barWidth) / 2f
                val barY = canvasHeight * 0.2f

                drawRoundRect(
                    color = Color.Gray,
                    topLeft = Offset(barX, barY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(10f)
                )

                val currentPowerWidth = barWidth * (gameData.chargePower / 100f)
                drawRoundRect(
                    color = Color.Yellow,
                    topLeft = Offset(barX, barY),
                    size = Size(currentPowerWidth, barHeight),
                    cornerRadius = CornerRadius(10f)
                )
            }

            if (gameData.gameState == GameState.FLYING || gameData.gameState == GameState.STOPPED) {
                val distanceText = "Dist: ${gameData.x.toInt()}m\nMax: ${maxDistance}m"
                val textLayoutResult = textMeasurer.measure(
                    text = distanceText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
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
            
            if (gameData.gameState == GameState.IDLE) {
                val textLayoutResult = textMeasurer.measure(
                    text = "Tilt Wrist to Aim\nTap & Hold to Charge",
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
        
        if (!isPaused && gameData.gameState != GameState.STOPPED) {
            Button(
                onClick = { isPaused = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Text("Pause", fontSize = 10.sp)
            }
        }
        
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { isPaused = false }) { Text("Resume", fontSize = 12.sp) }
                    Button(onClick = { 
                        isPaused = false
                        gameData = gameData.copy(gameState = GameState.STOPPED, velocityX = 0f, velocityY = 0f)
                    }) { Text("End Game", fontSize = 12.sp) }
                    Button(onClick = onBackToMenu) { Text("Menu", fontSize = 12.sp) }
                }
            }
        }

        if (gameData.gameState == GameState.STOPPED && !isPaused) {
            Button(
                onClick = {
                    gameData = gameData.copy(
                        x = 0f,
                        y = 0f,
                        velocityX = 0f,
                        velocityY = 0f,
                        gameState = GameState.IDLE,
                        chargePower = 0f,
                        boosts = emptyList(),
                        lastGeneratedBoostX = 500f
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Text("Play Again", fontSize = 12.sp)
            }
            
            Button(
                onClick = onBackToMenu,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text("Menu", fontSize = 10.sp)
            }
        }
    }
}
