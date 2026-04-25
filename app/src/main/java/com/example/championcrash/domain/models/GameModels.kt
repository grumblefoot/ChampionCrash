package com.example.championcrash.domain.models

enum class GameState {
    IDLE,       // Waiting for player to press and hold (aiming arrow is visible)
    CHARGING,   // Player is holding the screen, power oscillates
    FLYING,     // The character is in the air
    STOPPED     // The character has come to a halt
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
