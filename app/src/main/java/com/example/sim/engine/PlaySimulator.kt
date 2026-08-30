package com.example.sim.engine

import com.example.rules.data.EnforcementSpotRule
import com.example.rules.data.FoulType
import kotlin.random.Random

class PlaySimulator {

    /**
     * Headlessly simulates a single play from scrimmage.
     * Determines play call, yardage gained, and probabilistically injects true fouls.
     */
    fun generatePlay(state: GameState): PlayResult {
        // Determine play type logically based on down and distance
        val playType = if (state.distance > 7) {
            if (Random.nextBoolean()) PlayType.PASS_DEEP else PlayType.PASS_SHORT
        } else {
            if (Random.nextBoolean()) PlayType.RUN_INSIDE else PlayType.RUN_OUTSIDE
        }

        // Determine yards gained
        var yards = 0
        var isIncomplete = false
        when (playType) {
            PlayType.RUN_INSIDE -> yards = Random.nextInt(-2, 8)
            PlayType.RUN_OUTSIDE -> yards = Random.nextInt(-4, 15)
            PlayType.PASS_SHORT -> {
                if (Random.nextDouble() < 0.3) isIncomplete = true
                else yards = Random.nextInt(2, 12)
            }
            PlayType.PASS_DEEP -> {
                if (Random.nextDouble() < 0.55) isIncomplete = true
                else yards = Random.nextInt(15, 45)
            }
            else -> {}
        }

        // Calculate final yard line
        var finalYardLine = if (isIncomplete) state.yardLine else state.yardLine + yards
        var isTouchdown = false
        if (finalYardLine >= 100) {
            finalYardLine = 100
            isTouchdown = true
            yards = 100 - state.yardLine
        }

        // Probabilistically inject a true foul (15% chance)
        var foul: TrueFoulEvent? = null
        if (Random.nextDouble() < 0.15) {
            val possibleFouls = FoulType.values().filter { it != FoulType.NONE }
            val chosenFoul = possibleFouls.random()
            
            // Determine logical foul location based on foul type
            val foulSpot = when (chosenFoul.enforcementSpot) {
                EnforcementSpotRule.PREVIOUS_SPOT -> state.yardLine
                EnforcementSpotRule.SPOT_OF_FOUL -> state.yardLine + Random.nextInt(-5, 5)
                EnforcementSpotRule.DEAD_BALL_SPOT -> finalYardLine
                EnforcementSpotRule.SPOT_OF_FOUL_OR_PREVIOUS -> state.yardLine + Random.nextInt(-5, maxOf(1, yards))
            }.coerceIn(1, 99)
            
            val playerNum = if (chosenFoul.isOffensive) Random.nextInt(50, 79) else Random.nextInt(20, 59)
            foul = TrueFoulEvent(chosenFoul, playerNum, foulSpot)
        }

        return PlayResult(
            initialState = state,
            playType = playType,
            yardsGained = yards,
            isIncomplete = isIncomplete,
            finalDeadBallYardLine = finalYardLine,
            isTouchdown = isTouchdown,
            trueFoul = foul
        )
    }
}
