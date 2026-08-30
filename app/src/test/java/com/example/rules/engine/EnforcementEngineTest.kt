package com.example.rules.engine

import com.example.rules.data.FoulType
import com.example.sim.engine.GameState
import com.example.sim.engine.PlayResult
import com.example.sim.engine.PlayType
import com.example.sim.engine.TrueFoulEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EnforcementEngineTest {

    private lateinit var engine: EnforcementEngine

    @Before
    fun setup() {
        engine = EnforcementEngine()
    }

    @Test
    fun testHalfTheDistance_Offense() {
        // Offense is on their own 6 yard line. Holding is a 10 yard penalty.
        // It should be half the distance, taking them back 3 yards to the 3 yard line.
        val startState = GameState(down = 1, distance = 10, yardLine = 6)
        val play = PlayResult(
            initialState = startState,
            playType = PlayType.RUN_INSIDE,
            yardsGained = 2,
            finalDeadBallYardLine = 8,
            trueFoul = TrueFoulEvent(FoulType.OFFENSIVE_HOLDING, 72, 6)
        )

        val result = engine.applyPenalty(play, FoulType.OFFENSIVE_HOLDING, accepted = true)

        assertEquals(3, result.newState.yardLine) // Half of 6 is 3. 6 - 3 = 3.
        assertEquals(1, result.newState.down)
        assertEquals(13, result.newState.distance) // Original 10 + 3 yards penalized = 13
        assertTrue(result.explanation.contains("Half the distance"))
    }

    @Test
    fun testHalfTheDistance_Defense() {
        // Offense is on defense's 8 yard line. Defensive Offside is 5 yards.
        // It should be half the distance, advancing 4 yards to the 4 yard line.
        val startState = GameState(down = 2, distance = 8, yardLine = 92) // 100 - 8 = 92
        val play = PlayResult(
            initialState = startState,
            playType = PlayType.PASS_SHORT,
            yardsGained = 0,
            isIncomplete = true,
            finalDeadBallYardLine = 92,
            trueFoul = TrueFoulEvent(FoulType.OFFSIDE, 90, 92)
        )

        val result = engine.applyPenalty(play, FoulType.OFFSIDE, accepted = true)

        assertEquals(96, result.newState.yardLine) // 92 + 4 = 96 (which is the 4 yard line)
        assertEquals(2, result.newState.down)
        assertEquals(4, result.newState.distance) // 8 - 4 = 4
        assertTrue(result.explanation.contains("Half the distance"))
    }

    @Test
    fun testAutoFirstDown_DefensivePassInterference() {
        // 3rd & 20. DPI is a 15 yard penalty AND an automatic first down.
        val startState = GameState(down = 3, distance = 20, yardLine = 30)
        val play = PlayResult(
            initialState = startState,
            playType = PlayType.PASS_DEEP,
            yardsGained = 0,
            isIncomplete = true,
            finalDeadBallYardLine = 30,
            trueFoul = TrueFoulEvent(FoulType.DEFENSIVE_PASS_INTERFERENCE, 24, 45)
        )

        val result = engine.applyPenalty(play, FoulType.DEFENSIVE_PASS_INTERFERENCE, accepted = true)

        assertEquals(45, result.newState.yardLine) // Enforced 15 yds from previous spot
        assertEquals(1, result.newState.down) // Auto first down
        assertEquals(10, result.newState.distance) 
    }

    @Test
    fun testLossOfDown_IntentionalGrounding() {
        // 2nd & 10. Intentional grounding is 5 yards from the spot and loss of down.
        val startState = GameState(down = 2, distance = 10, yardLine = 40)
        val play = PlayResult(
            initialState = startState,
            playType = PlayType.PASS_SHORT,
            yardsGained = -8,
            isIncomplete = true,
            finalDeadBallYardLine = 32,
            trueFoul = TrueFoulEvent(FoulType.INTENTIONAL_GROUNDING, 12, 32) // Spot of foul is where thrown
        )

        val result = engine.applyPenalty(play, FoulType.INTENTIONAL_GROUNDING, accepted = true)

        assertEquals(27, result.newState.yardLine) // 32 - 5 = 27
        assertEquals(3, result.newState.down) // Loss of down, so 3rd down
        assertEquals(23, result.newState.distance) // 10 original + (40 start - 27 end) = 23 yards to go
    }
}
