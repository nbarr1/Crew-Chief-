package com.example.sim.engine

import com.example.rules.data.FoulType

enum class PlayType {
    RUN_INSIDE, 
    RUN_OUTSIDE, 
    PASS_SHORT, 
    PASS_DEEP, 
    PUNT, 
    FIELD_GOAL,
    KICKOFF
}

data class GameState(
    val down: Int,
    val distance: Int,
    val yardLine: Int, // 1 to 99, where 1 is offense's 1 yard line, 99 is defense's 1 yard line.
    val clockSeconds: Int = 900 // 15 minutes
) {
    val formattedDownAndDistance: String
        get() = when (down) {
            1 -> "1st & $distance"
            2 -> "2nd & $distance"
            3 -> "3rd & $distance"
            4 -> "4th & $distance"
            else -> "Unknown Down"
        }
}

data class TrueFoulEvent(
    val foulType: FoulType,
    val committingPlayerNumber: Int,
    val foulLocationYardLine: Int
)

data class PlayResult(
    val initialState: GameState,
    val playType: PlayType,
    val yardsGained: Int,
    val isIncomplete: Boolean = false,
    val isTurnover: Boolean = false,
    val isTouchdown: Boolean = false,
    val finalDeadBallYardLine: Int,
    val trueFoul: TrueFoulEvent?
)
