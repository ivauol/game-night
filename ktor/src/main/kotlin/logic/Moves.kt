package com.example

import kotlinx.serialization.Serializable

@Serializable
data class MoveRequest(
    val gameId: Int,
    val playerId: Int,
    val move: List<List<Int>>,
)

@Serializable
data class MoveResponse(
    val success: Boolean,
    val message: String,
    val board: String? = null,
    val winner: String? = null,
)
