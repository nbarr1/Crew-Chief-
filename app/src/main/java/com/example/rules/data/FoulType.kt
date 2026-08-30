package com.example.rules.data

enum class FoulPhase {
    PRE_SNAP,
    SIMULTANEOUS_WITH_SNAP,
    LIVE_BALL,
    DEAD_BALL
}

enum class EnforcementSpotRule {
    PREVIOUS_SPOT,
    SPOT_OF_FOUL,
    DEAD_BALL_SPOT,
    SPOT_OF_FOUL_OR_PREVIOUS // Basic spot principles (All-but-one principle)
}

enum class FoulType(
    val foulName: String,
    val plainLanguageRule: String,
    val phase: FoulPhase,
    val yardage: Int,
    val isAutoFirstDown: Boolean = false,
    val isLossOfDown: Boolean = false,
    val enforcementSpot: EnforcementSpotRule = EnforcementSpotRule.PREVIOUS_SPOT,
    val isOffensive: Boolean = false
) {
    NONE(
        "No Foul", 
        "Clean play.", 
        FoulPhase.LIVE_BALL, 
        0
    ),
    FALSE_START(
        "False Start", 
        "Interior lineman flinched or moved forward prior to the snap.", 
        FoulPhase.PRE_SNAP, 
        5, 
        enforcementSpot = EnforcementSpotRule.DEAD_BALL_SPOT,
        isOffensive = true
    ),
    OFFSIDE(
        "Offside / Encroachment", 
        "Defender in the neutral zone at the snap.", 
        FoulPhase.SIMULTANEOUS_WITH_SNAP, 
        5,
        isOffensive = false
    ),
    OFFENSIVE_HOLDING(
        "Offensive Holding", 
        "Grasping an opponent outside the frame of the jersey and restricting movement.", 
        FoulPhase.LIVE_BALL, 
        10, 
        enforcementSpot = EnforcementSpotRule.SPOT_OF_FOUL_OR_PREVIOUS,
        isOffensive = true
    ),
    DEFENSIVE_HOLDING(
        "Defensive Holding",
        "Grasping an eligible receiver beyond 5 yards restricting their route.",
        FoulPhase.LIVE_BALL,
        5,
        isAutoFirstDown = true,
        isOffensive = false
    ),
    DEFENSIVE_PASS_INTERFERENCE(
        "Defensive Pass Interference", 
        "Early contact significantly hindering a receiver's attempt to catch a forward pass.", 
        FoulPhase.LIVE_BALL, 
        15, 
        isAutoFirstDown = true,
        isOffensive = false
    ),
    OFFENSIVE_PASS_INTERFERENCE(
        "Offensive Pass Interference",
        "Creating separation via push-off beyond 1 yard while pass is in flight.",
        FoulPhase.LIVE_BALL,
        10,
        isOffensive = true
    ),
    FACE_MASK(
        "Face Mask", 
        "Grasping and twisting the face mask or helmet opening of an opponent.", 
        FoulPhase.LIVE_BALL, 
        15, 
        isAutoFirstDown = true
    ),
    HORSE_COLLAR_TACKLE(
        "Horse Collar Tackle",
        "Grabbing inside collar or jersey opening and pulling runner down immediately.",
        FoulPhase.LIVE_BALL,
        15,
        isAutoFirstDown = true
    ),
    UNNECESSARY_ROUGHNESS(
        "Unnecessary Roughness",
        "Blow to the head/neck area or contact out of bounds.",
        FoulPhase.LIVE_BALL,
        15,
        isAutoFirstDown = true
    ),
    ROUGHING_THE_PASSER(
        "Roughing the Passer",
        "Illegal forcible hit to quarterback after release of the football.",
        FoulPhase.LIVE_BALL,
        15,
        isAutoFirstDown = true,
        isOffensive = false
    ),
    ILLEGAL_BLOCK_IN_THE_BACK(
        "Illegal Block in the Back",
        "Block directed above the waist into the back of an opponent.",
        FoulPhase.LIVE_BALL,
        10,
        isOffensive = true
    ),
    INTENTIONAL_GROUNDING(
        "Intentional Grounding", 
        "Pass thrown without realistic chance of completion by a passer facing imminent loss of yardage.", 
        FoulPhase.LIVE_BALL, 
        5, 
        isLossOfDown = true, 
        enforcementSpot = EnforcementSpotRule.SPOT_OF_FOUL,
        isOffensive = true
    )
}
