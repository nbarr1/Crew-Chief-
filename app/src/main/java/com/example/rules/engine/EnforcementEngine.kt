package com.example.rules.engine

import com.example.rules.data.EnforcementSpotRule
import com.example.rules.data.FoulPhase
import com.example.rules.data.FoulType
import com.example.sim.engine.GameState
import com.example.sim.engine.PlayResult

class EnforcementEngine {

    data class EnforcementResult(
        val accepted: Boolean,
        val newState: GameState,
        val explanation: String
    )

    /**
     * Applies penalty logic for a foul on a given play.
     * Calculates the new down, distance, and yard line based on basic spot principles
     * and half-the-distance-to-the-goal rules.
     */
    fun applyPenalty(play: PlayResult, foul: FoulType, accepted: Boolean): EnforcementResult {
        if (!accepted || foul == FoulType.NONE) {
            // Determine normal next play state (no penalty)
            val nextDown = if (play.yardsGained >= play.initialState.distance) 1 else play.initialState.down + 1
            val nextDistance = if (nextDown == 1) 10 else play.initialState.distance - play.yardsGained
            
            // Note: Simplistic assumption here - turnover on downs if nextDown > 4 not handled in this basic snippet.
            return EnforcementResult(
                accepted = false,
                newState = play.initialState.copy(
                    down = nextDown,
                    distance = nextDistance,
                    yardLine = play.finalDeadBallYardLine
                ),
                explanation = "Penalty declined or no foul. Result of the play stands."
            )
        }

        val basicSpot = play.initialState.yardLine
        val foulSpot = play.trueFoul?.foulLocationYardLine ?: basicSpot
        
        // Determine enforcement spot (All-but-one principle simplified)
        val enforceFrom = when (foul.enforcementSpot) {
            EnforcementSpotRule.PREVIOUS_SPOT -> basicSpot
            EnforcementSpotRule.SPOT_OF_FOUL -> foulSpot
            EnforcementSpotRule.DEAD_BALL_SPOT -> play.finalDeadBallYardLine
            EnforcementSpotRule.SPOT_OF_FOUL_OR_PREVIOUS -> {
                // If foul is behind basic spot, enforce from foul spot
                if (foulSpot < basicSpot) foulSpot else basicSpot
            }
        }

        // Apply yardage, respecting half-the-distance to the goal
        var penaltyYards = foul.yardage
        
        var newYardLine = enforceFrom
        var halfTheDistanceApplied = false

        if (foul.isOffensive) {
            val distanceToOwnGoal = enforceFrom // yards from offensive end zone
            if (penaltyYards >= distanceToOwnGoal / 2.0) {
                penaltyYards = distanceToOwnGoal / 2
                halfTheDistanceApplied = true
            }
            newYardLine -= penaltyYards
        } else {
            val distanceToGoal = 100 - enforceFrom
            if (penaltyYards >= distanceToGoal / 2.0) {
                penaltyYards = distanceToGoal / 2
                halfTheDistanceApplied = true
            }
            newYardLine += penaltyYards
        }

        // Determine next down and distance
        var nextDown = play.initialState.down
        var nextDistance = play.initialState.distance
        
        if (foul.isOffensive) {
            if (foul.isLossOfDown) {
                nextDown += 1
                nextDistance = play.initialState.distance + (play.initialState.yardLine - newYardLine)
            } else if (foul.phase == FoulPhase.PRE_SNAP) {
                // Replay down, distance increases
                nextDistance += penaltyYards
            } else {
                // Live ball offensive foul typically repeats the down, adds distance based on new yard line vs original
                nextDistance = play.initialState.distance + (play.initialState.yardLine - newYardLine)
            }
        } else {
            if (foul.isAutoFirstDown || ((newYardLine - play.initialState.yardLine) >= nextDistance)) {
                nextDown = 1
                nextDistance = 10
            } else if (foul.phase == FoulPhase.PRE_SNAP) {
                // Replay down, distance decreases
                nextDistance -= penaltyYards
            } else {
                // Live ball defensive foul (not auto first down), replay down with shorter distance
                nextDistance -= (newYardLine - play.initialState.yardLine)
            }
        }
        
        val htdString = if (halfTheDistanceApplied) " (Half the distance to the goal)" else ""
        val enforcementStr = if (enforceFrom == basicSpot) "the previous spot" else "the spot of the foul"
        val explanation = "${foul.foulName}. $penaltyYards yard penalty enforced from $enforcementStr$htdString."

        return EnforcementResult(
            accepted = true,
            newState = play.initialState.copy(
                down = nextDown,
                distance = nextDistance,
                yardLine = newYardLine
            ),
            explanation = explanation
        )
    }
}
