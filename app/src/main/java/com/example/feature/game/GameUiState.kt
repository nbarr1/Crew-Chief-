package com.example.feature.game

import com.example.core.model.CallGrade
import com.example.core.model.OfficialPosition
import com.example.sim.engine.GameState
import com.example.sim.engine.PlayResult

enum class PlayPhase {
    PRE_SNAP,
    LIVE_BALL,
    DEAD_BALL_SPOTTING,
    PENALTY_REPORT,
    REVIEW
}

enum class PlayerRole {
    QB, RB, OL, WR, TE, DL, LB, DB, REF
}

enum class CameraPerspective(val displayName: String, val shortLabel: String) {
    FIRST_PERSON("1st Person POV (Ref Eyes)", "1ST PERSON"),
    THIRD_PERSON("3rd Person (Follow Ref)", "3RD PERSON"),
    SIDELINE_CAM("Sideline Broadcast Cam", "SIDELINE"),
    ENDZONE_CAM("Endzone Replay Cam", "END ZONE"),
    TACTICAL_2D("All-22 Overhead Skycam", "ALL-22 SKY")
}

data class Ball3D(
    val xYard: Float,
    val yYard: Float,
    val zHeight: Float = 0f,
    val isAirborne: Boolean = false,
    val spinAngle: Float = 0f
)

data class Flag3D(
    val xYard: Float,
    val yYard: Float,
    val zHeight: Float = 0f,
    val flightProgress: Float = 1f
)

data class PlayerDot(
    val id: Int,
    val number: Int,
    val xYard: Float,
    val yYard: Float,
    val zHeight: Float = 0f,
    val isOffense: Boolean,
    val isKey: Boolean = false,
    val role: PlayerRole = PlayerRole.OL,
    val facingYaw: Float = 0f, // 0 = facing downfield (+Y), 180 = facing back (-Y)
    val isMoving: Boolean = false
)

data class PlayFrame(
    val progress: Float,
    val ball: Ball3D,
    val players: List<PlayerDot>
)

data class GameUiState(
    val phase: PlayPhase = PlayPhase.PRE_SNAP,
    val assignedPosition: OfficialPosition = OfficialPosition.DOWN_JUDGE,
    val cameraPerspective: CameraPerspective = CameraPerspective.FIRST_PERSON,
    val headYawOffset: Float = 0f,
    val headPitchOffset: Float = 0f,
    val cameraZoom: Float = 1.0f,
    val gameState: GameState = GameState(down = 1, distance = 10, yardLine = 25),
    val currentPlayResult: PlayResult? = null,
    
    val visibleYardRange: IntRange = 15..35,
    val players: List<PlayerDot> = emptyList(),
    val initialPlayers: List<PlayerDot> = emptyList(),
    val recordedFrames: List<PlayFrame> = emptyList(),
    val replayProgress: Float = 1.0f,
    val isReplaying: Boolean = false,
    
    val ball3D: Ball3D = Ball3D(xYard = 26.65f, yYard = 25f, zHeight = 0.2f),
    val flag3D: Flag3D? = null,
    val flagYardLine: Float? = null,
    
    val userSpottedYardLine: Float? = null,
    val refWalkingYardLine: Float = 25f,
    
    // Penalty Assessment
    val showPenaltyPicker: Boolean = false,
    val userSelectedFoul: com.example.rules.data.FoulType? = null,
    val userSelectedOffense: Boolean = true,
    val userSelectedPlayerNum: Int = 50,
    
    val grade: CallGrade? = null,
    val feedbackMessage: String? = null,
    val ruleCitation: String? = null,
    val spotAccuracyGrade: String? = null,
    val officialSignalGiven: String? = null
)

