package com.example.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.CallGrade
import com.example.core.model.OfficialPosition
import com.example.rules.data.FoulType
import com.example.sim.engine.GameState
import com.example.sim.engine.PlaySimulator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val simulator = PlaySimulator()
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        setupNextPlay()
    }

    fun setupNextPlay() {
        val currentGameState = _uiState.value.gameState
        val los = currentGameState.yardLine
        val position = _uiState.value.assignedPosition
        
        val range = when (position) {
            OfficialPosition.REFEREE -> (los - 15)..(los + 10)
            OfficialPosition.BACK_JUDGE -> (los - 5)..(los + 30)
            else -> (los - 10)..(los + 15)
        }

        val initialPlayers = generateInitialFormation(los)
        val initialBall = Ball3D(xYard = 26.65f, yYard = los.toFloat(), zHeight = 0.2f)

        _uiState.update { 
            it.copy(
                phase = PlayPhase.PRE_SNAP,
                currentPlayResult = null,
                flagYardLine = null,
                flag3D = null,
                userSpottedYardLine = null,
                refWalkingYardLine = los.toFloat(),
                grade = null,
                feedbackMessage = null,
                ruleCitation = null,
                spotAccuracyGrade = null,
                officialSignalGiven = null,
                headYawOffset = 0f,
                headPitchOffset = 0f,
                visibleYardRange = range,
                players = initialPlayers,
                initialPlayers = initialPlayers,
                recordedFrames = emptyList(),
                replayProgress = 1.0f,
                isReplaying = false,
                ball3D = initialBall,
                showPenaltyPicker = false,
                userSelectedFoul = null
            )
        }

        // Occasional pre-snap motion
        if (Math.random() < 0.45) {
            viewModelScope.launch {
                delay(700)
                if (_uiState.value.phase != PlayPhase.PRE_SNAP) return@launch
                
                val motionFrames = 30
                val delayPerFrame = 33L
                val movingPlayerIndex = initialPlayers.indexOfFirst { it.role == PlayerRole.WR }
                
                if (movingPlayerIndex >= 0) {
                    for (i in 1..motionFrames) {
                        delay(delayPerFrame)
                        if (_uiState.value.phase != PlayPhase.PRE_SNAP) break
                        
                        _uiState.update { state ->
                            val newPlayers = state.players.toMutableList()
                            val orig = newPlayers[movingPlayerIndex]
                            newPlayers[movingPlayerIndex] = orig.copy(
                                xYard = orig.xYard - 0.5f,
                                isMoving = true
                            )
                            state.copy(players = newPlayers)
                        }
                    }
                }
            }
        }
    }

    fun setCameraPerspective(perspective: CameraPerspective) {
        _uiState.update { it.copy(cameraPerspective = perspective) }
    }

    fun updateLookAngle(yawDelta: Float, pitchDelta: Float) {
        _uiState.update { state ->
            val newYaw = (state.headYawOffset + yawDelta).coerceIn(-65f, 65f)
            val newPitch = (state.headPitchOffset + pitchDelta).coerceIn(-25f, 25f)
            state.copy(headYawOffset = newYaw, headPitchOffset = newPitch)
        }
    }

    fun resetLookAngle() {
        _uiState.update { it.copy(headYawOffset = 0f, headPitchOffset = 0f) }
    }

    fun changeAssignedPosition(position: OfficialPosition) {
        _uiState.update { it.copy(assignedPosition = position) }
        setupNextPlay()
    }

    fun togglePlayerKey(playerId: Int) {
        if (_uiState.value.phase != PlayPhase.PRE_SNAP) return
        _uiState.update { state ->
            val updatedPlayers = state.players.map { player ->
                if (player.id == playerId) player.copy(isKey = !player.isKey) else player
            }
            state.copy(players = updatedPlayers)
        }
    }

    fun setCameraZoom(zoom: Float) {
        _uiState.update { it.copy(cameraZoom = zoom.coerceIn(0.7f, 2.5f)) }
    }

    fun snapBall() {
        if (_uiState.value.phase != PlayPhase.PRE_SNAP) return
        
        com.example.core.util.SoundEffects.playThud() // Cadence snap
        
        val playResult = simulator.generatePlay(_uiState.value.gameState)
        val initialPlayers = _uiState.value.players
        
        _uiState.update { 
            it.copy(
                phase = PlayPhase.LIVE_BALL,
                currentPlayResult = playResult,
                initialPlayers = initialPlayers,
                recordedFrames = emptyList()
            )
        }

        viewModelScope.launch {
            val frames = 45 // 1.5 seconds at 30fps
            val delayPerFrame = 33L
            val isPass = playResult.playType == com.example.sim.engine.PlayType.PASS_SHORT || playResult.playType == com.example.sim.engine.PlayType.PASS_DEEP
            
            val los = _uiState.value.gameState.yardLine.toFloat()
            val targetYard = playResult.finalDeadBallYardLine.toFloat()
            val passTargetX = 40.0f
            val recordedList = mutableListOf<PlayFrame>()
            
            for (i in 1..frames) {
                delay(delayPerFrame)
                if (_uiState.value.phase != PlayPhase.LIVE_BALL) break // Interrupted by flag
                
                if (i == 12) com.example.core.util.SoundEffects.playThud() // Lineman collision
                
                val progress = i.toFloat() / frames
                
                // Calculate 3D Ball Physics
                val ballX: Float
                val ballY: Float
                val ballZ: Float
                val isAirborne: Boolean
                
                if (isPass) {
                    if (progress < 0.35f) {
                        // In QB hands in pocket
                        val qbProg = progress / 0.35f
                        ballX = 26.65f
                        ballY = los - (3f * qbProg)
                        ballZ = 1.6f
                        isAirborne = false
                    } else {
                        // Pass flight in parabolic 3D arc
                        val flightProg = (progress - 0.35f) / 0.65f
                        val qbReleaseY = los - 3f
                        ballX = 26.65f + (passTargetX - 26.65f) * flightProg
                        ballY = qbReleaseY + (targetYard - qbReleaseY) * flightProg
                        // Parabolic height formula: peaks at ~4.2 yards height
                        ballZ = 1.8f + (4f * 3.5f * flightProg * (1f - flightProg))
                        isAirborne = true
                    }
                } else {
                    // Running play handoff to RB
                    val rbProg = progress
                    ballX = 26.65f + (2f * rbProg)
                    ballY = los - 3f + ((playResult.yardsGained + 3f) * rbProg)
                    ballZ = 1.1f
                    isAirborne = false
                }
                
                val newPlayers = initialPlayers.map { orig ->
                    var newY = orig.yYard
                    var newX = orig.xYard
                    
                    when (orig.role) {
                        PlayerRole.QB -> {
                            newY = if (isPass) orig.yYard - (3f * progress) else orig.yYard + (2f * progress)
                        }
                        PlayerRole.RB -> {
                            newY = if (!isPass) orig.yYard + (playResult.yardsGained * progress).coerceAtMost(14f) else orig.yYard + (4f * progress)
                            newX = if (!isPass) orig.xYard + (1.5f * progress) else orig.xYard
                        }
                        PlayerRole.OL -> {
                            newY = if (isPass) orig.yYard - (1.2f * progress) else orig.yYard + (1.8f * progress)
                        }
                        PlayerRole.TE -> {
                            newY = if (isPass) orig.yYard + (9f * progress) else orig.yYard + (2f * progress)
                            newX = orig.xYard + (1.2f * progress)
                        }
                        PlayerRole.DL -> {
                            newY = if (isPass) orig.yYard - (0.8f * progress) else orig.yYard - (0.5f * progress)
                        }
                        PlayerRole.LB -> {
                            newY = if (isPass) orig.yYard + (4f * progress) else orig.yYard - (2.5f * progress)
                            newX = orig.xYard + if (!isPass) (1.0f * progress) else 0f
                        }
                        PlayerRole.WR -> {
                            newY = orig.yYard + (16f * progress)
                        }
                        PlayerRole.DB -> {
                            newY = orig.yYard + (14f * progress)
                        }
                        PlayerRole.REF -> {
                            newY = orig.yYard + (2f * progress)
                        }
                    }
                    orig.copy(
                        xYard = newX,
                        yYard = newY,
                        isMoving = true
                    )
                }

                val currentBall = Ball3D(
                    xYard = ballX,
                    yYard = ballY,
                    zHeight = ballZ,
                    isAirborne = isAirborne,
                    spinAngle = (progress * 720f) % 360f
                )

                recordedList.add(PlayFrame(progress, currentBall, newPlayers))
                
                _uiState.update { state ->
                    state.copy(
                        players = newPlayers,
                        ball3D = currentBall,
                        recordedFrames = recordedList,
                        replayProgress = progress
                    )
                }
            }
            
            if (_uiState.value.phase == PlayPhase.LIVE_BALL) {
                com.example.core.util.SoundEffects.playWhistle() // Play dead
                _uiState.update { 
                    it.copy(
                        phase = PlayPhase.DEAD_BALL_SPOTTING,
                        refWalkingYardLine = targetYard,
                        userSpottedYardLine = targetYard,
                        recordedFrames = recordedList,
                        replayProgress = 1.0f
                    ) 
                }
            }
        }
    }

    fun setReplayProgress(progress: Float) {
        val frames = _uiState.value.recordedFrames
        if (frames.isEmpty()) return
        
        val clampedProg = progress.coerceIn(0f, 1f)
        val frameIndex = ((frames.size - 1) * clampedProg).toInt().coerceIn(0, frames.size - 1)
        val frame = frames[frameIndex]
        
        _uiState.update { 
            it.copy(
                replayProgress = clampedProg,
                players = frame.players,
                ball3D = frame.ball
            ) 
        }
    }

    fun throwFlag(yardLine: Float) {
        if (_uiState.value.phase != PlayPhase.LIVE_BALL) return
        
        com.example.core.util.SoundEffects.playFlagThrow()
        com.example.core.util.SoundEffects.playWhistle() // Play immediately dead
        
        _uiState.update { 
            it.copy(
                flagYardLine = yardLine,
                flag3D = Flag3D(xYard = 26.65f, yYard = yardLine, zHeight = 0f, flightProgress = 1f),
                phase = PlayPhase.DEAD_BALL_SPOTTING,
                userSpottedYardLine = yardLine,
                refWalkingYardLine = yardLine,
                showPenaltyPicker = true
            )
        }
    }

    fun openPenaltyPicker() {
        _uiState.update { it.copy(showPenaltyPicker = true) }
    }

    fun closePenaltyPicker() {
        _uiState.update { it.copy(showPenaltyPicker = false) }
    }

    fun selectFoul(foul: FoulType, isOffense: Boolean, playerNum: Int) {
        _uiState.update { 
            it.copy(
                userSelectedFoul = foul,
                userSelectedOffense = isOffense,
                userSelectedPlayerNum = playerNum,
                flagYardLine = if (foul != FoulType.NONE) (it.userSpottedYardLine ?: it.gameState.yardLine.toFloat()) else null,
                showPenaltyPicker = false
            )
        }
    }

    fun updateSpot(yardLine: Float) {
        if (_uiState.value.phase != PlayPhase.DEAD_BALL_SPOTTING) return
        val clamped = yardLine.coerceIn(1f, 99f)
        _uiState.update { it.copy(userSpottedYardLine = clamped, refWalkingYardLine = clamped) }
    }

    fun adjustSpot(delta: Float) {
        if (_uiState.value.phase != PlayPhase.DEAD_BALL_SPOTTING) return
        val current = _uiState.value.userSpottedYardLine ?: _uiState.value.gameState.yardLine.toFloat()
        val newSpot = (current + delta).coerceIn(1f, 99f)
        val rounded = Math.round(newSpot * 10f) / 10f
        updateSpot(rounded)
    }

    fun snapSpotToBallCarrier() {
        val playResult = _uiState.value.currentPlayResult ?: return
        val targetYard = playResult.finalDeadBallYardLine.toFloat()
        updateSpot(targetYard)
    }

    fun setOfficialSignal(signal: String) {
        _uiState.update { it.copy(officialSignalGiven = signal) }
    }

    fun confirmSpotAndEvaluate() {
        val state = _uiState.value
        if (state.phase != PlayPhase.DEAD_BALL_SPOTTING) return
        
        val result = state.currentPlayResult ?: return
        val trueFoul = result.trueFoul
        val selectedFoul = state.userSelectedFoul
        val userThrewFlag = (state.flagYardLine != null) || (selectedFoul != null && selectedFoul != FoulType.NONE)
        val userSpot = state.userSpottedYardLine ?: state.gameState.yardLine.toFloat()
        
        val grade: CallGrade
        val feedback: String
        val citation: String

        if (trueFoul != null) {
            if (selectedFoul == trueFoul.foulType) {
                grade = CallGrade.CORRECT_CALL
                feedback = "MASTERCLASS CALL! You accurately identified ${trueFoul.foulType.foulName} on #${trueFoul.committingPlayerNumber}."
                citation = trueFoul.foulType.plainLanguageRule
            } else if (userThrewFlag) {
                grade = CallGrade.MARGINAL_CALL
                feedback = "Flag thrown, but official foul was ${trueFoul.foulType.foulName} on #${trueFoul.committingPlayerNumber} (You selected ${selectedFoul?.foulName ?: "Unspecified"})."
                citation = trueFoul.foulType.plainLanguageRule
            } else {
                grade = CallGrade.MISSED_CALL
                feedback = "MISSED INFRACTION! A ${trueFoul.foulType.foulName} occurred on #${trueFoul.committingPlayerNumber} at the ${trueFoul.foulLocationYardLine} yd line."
                citation = trueFoul.foulType.plainLanguageRule
            }
        } else {
            if (userThrewFlag) {
                grade = CallGrade.INCORRECT_CALL
                feedback = "PHANTOM FLAG! Clean play by both squads; no foul occurred."
                citation = FoulType.NONE.plainLanguageRule
            } else {
                grade = CallGrade.CORRECT_NON_CALL
                feedback = "GREAT DISCIPLINE! Clean block and coverage; correct non-call."
                citation = FoulType.NONE.plainLanguageRule
            }
        }

        val trueSpot = result.finalDeadBallYardLine.toFloat()
        val spotDelta = Math.abs(userSpot - trueSpot)
        val spotAccuracy: String
        val spotFeedback = if (spotDelta <= 0.3f) {
            spotAccuracy = "EXACT SPOT (±0.2 YDS)"
            "Dead-ball spot: EXACT at the $trueSpot yard line (0 error)."
        } else if (spotDelta <= 1.0f) {
            spotAccuracy = "GOOD SPOT (±${String.format("%.1f", spotDelta)} YDS)"
            "Dead-ball spot: Close! Spotted at $userSpot (Actual: $trueSpot yd line)."
        } else {
            spotAccuracy = "OFF SPOT (±${String.format("%.1f", spotDelta)} YDS)"
            "Dead-ball spot: Off by ${String.format("%.1f", spotDelta)} yards (Actual: $trueSpot yd line)."
        }

        _uiState.update { 
            it.copy(
                phase = PlayPhase.REVIEW,
                grade = grade,
                spotAccuracyGrade = spotAccuracy,
                feedbackMessage = "$feedback\n\n$spotFeedback",
                ruleCitation = citation,
                gameState = state.gameState.copy(
                    down = if (result.yardsGained >= state.gameState.distance) 1 else (state.gameState.down % 4) + 1,
                    distance = if (result.yardsGained >= state.gameState.distance) 10 else (state.gameState.distance - result.yardsGained).coerceAtLeast(1),
                    yardLine = result.finalDeadBallYardLine
                )
            )
        }
    }

    private fun generateInitialFormation(los: Int): List<PlayerDot> {
        val players = mutableListOf<PlayerDot>()
        var idCounter = 1
        val userPos = _uiState.value.assignedPosition
        
        val formation = listOf("SHOTGUN_SPREAD", "PRO_SET", "TRIPS_RIGHT").random()
        
        // -------------------------------------------------------------
        // OFFENSE (Away White & Gold) - 11 Players
        // -------------------------------------------------------------
        // 5 Offensive Linemen (LT, LG, C, RG, RT)
        players.add(PlayerDot(idCounter++, 72, 21.0f, los.toFloat() - 0.2f, isOffense = true, role = PlayerRole.OL, facingYaw = 0f)) // LT
        players.add(PlayerDot(idCounter++, 65, 23.8f, los.toFloat() - 0.2f, isOffense = true, role = PlayerRole.OL, facingYaw = 0f)) // LG
        players.add(PlayerDot(idCounter++, 50, 26.65f, los.toFloat(), isOffense = true, role = PlayerRole.OL, facingYaw = 0f)) // Center
        players.add(PlayerDot(idCounter++, 66, 29.5f, los.toFloat() - 0.2f, isOffense = true, role = PlayerRole.OL, facingYaw = 0f)) // RG
        players.add(PlayerDot(idCounter++, 78, 32.3f, los.toFloat() - 0.2f, isOffense = true, role = PlayerRole.OL, facingYaw = 0f)) // RT

        // Tight End (TE)
        players.add(PlayerDot(idCounter++, 87, 35.0f, los.toFloat() - 0.3f, isOffense = true, role = PlayerRole.TE, facingYaw = 0f))

        when (formation) {
            "SHOTGUN_SPREAD" -> {
                players.add(PlayerDot(idCounter++, 12, 26.65f, los.toFloat() - 4.8f, isOffense = true, role = PlayerRole.QB, facingYaw = 0f)) // QB
                players.add(PlayerDot(idCounter++, 22, 23.5f, los.toFloat() - 4.8f, isOffense = true, role = PlayerRole.RB, facingYaw = 0f)) // RB
                players.add(PlayerDot(idCounter++, 88, 46.5f, los.toFloat() - 0.5f, isOffense = true, role = PlayerRole.WR, facingYaw = 0f)) // WR1 Wide Right
                players.add(PlayerDot(idCounter++, 81, 7.0f, los.toFloat() - 0.5f, isOffense = true, role = PlayerRole.WR, facingYaw = 0f))  // WR2 Wide Left
                players.add(PlayerDot(idCounter++, 17, 39.5f, los.toFloat() - 1.2f, isOffense = true, role = PlayerRole.WR, facingYaw = 0f)) // WR3 Slot
            }
            "PRO_SET" -> {
                players.add(PlayerDot(idCounter++, 12, 26.65f, los.toFloat() - 1.5f, isOffense = true, role = PlayerRole.QB, facingYaw = 0f)) // Under Center QB
                players.add(PlayerDot(idCounter++, 44, 26.65f, los.toFloat() - 4.2f, isOffense = true, role = PlayerRole.RB, facingYaw = 0f)) // Fullback
                players.add(PlayerDot(idCounter++, 22, 26.65f, los.toFloat() - 6.8f, isOffense = true, role = PlayerRole.RB, facingYaw = 0f)) // Tailback
                players.add(PlayerDot(idCounter++, 88, 47.0f, los.toFloat() - 0.5f, isOffense = true, role = PlayerRole.WR, facingYaw = 0f)) // WR1 Wide Right
                players.add(PlayerDot(idCounter++, 81, 6.5f, los.toFloat() - 0.5f, isOffense = true, role = PlayerRole.WR, facingYaw = 0f))  // WR2 Wide Left
            }
            else -> {
                players.add(PlayerDot(idCounter++, 12, 26.65f, los.toFloat() - 4.5f, isOffense = true, role = PlayerRole.QB, facingYaw = 0f))
                players.add(PlayerDot(idCounter++, 22, 24.0f, los.toFloat() - 4.5f, isOffense = true, role = PlayerRole.RB, facingYaw = 0f))
                players.add(PlayerDot(idCounter++, 88, 48.0f, los.toFloat() - 0.5f, isOffense = true, role = PlayerRole.WR, facingYaw = 0f))
                players.add(PlayerDot(idCounter++, 81, 41.5f, los.toFloat() - 1.0f, isOffense = true, role = PlayerRole.WR, facingYaw = 0f))
                players.add(PlayerDot(idCounter++, 17, 37.0f, los.toFloat() - 1.5f, isOffense = true, role = PlayerRole.WR, facingYaw = 0f))
            }
        }
        
        // -------------------------------------------------------------
        // DEFENSE (Home Navy & Cyan) - 11 Players
        // -------------------------------------------------------------
        // 4 Defensive Linemen (LDE, LDT, RDT, RDE)
        players.add(PlayerDot(idCounter++, 94, 20.8f, los.toFloat() + 1.2f, isOffense = false, role = PlayerRole.DL, facingYaw = 180f))
        players.add(PlayerDot(idCounter++, 99, 24.5f, los.toFloat() + 1.1f, isOffense = false, role = PlayerRole.DL, facingYaw = 180f))
        players.add(PlayerDot(idCounter++, 91, 28.8f, los.toFloat() + 1.1f, isOffense = false, role = PlayerRole.DL, facingYaw = 180f))
        players.add(PlayerDot(idCounter++, 97, 33.5f, los.toFloat() + 1.2f, isOffense = false, role = PlayerRole.DL, facingYaw = 180f))

        // 3 Linebackers (WLB, MLB, SLB)
        players.add(PlayerDot(idCounter++, 55, 21.5f, los.toFloat() + 4.5f, isOffense = false, role = PlayerRole.LB, facingYaw = 180f))
        players.add(PlayerDot(idCounter++, 54, 26.65f, los.toFloat() + 4.8f, isOffense = false, role = PlayerRole.LB, facingYaw = 180f))
        players.add(PlayerDot(idCounter++, 52, 33.0f, los.toFloat() + 4.5f, isOffense = false, role = PlayerRole.LB, facingYaw = 180f))

        // 4 Defensive Backs (LCB, RCB, FS, SS)
        players.add(PlayerDot(idCounter++, 21, 7.0f, los.toFloat() + 5.5f, isOffense = false, role = PlayerRole.DB, facingYaw = 180f))  // LCB
        players.add(PlayerDot(idCounter++, 24, 46.5f, los.toFloat() + 6.0f, isOffense = false, role = PlayerRole.DB, facingYaw = 180f)) // RCB
        players.add(PlayerDot(idCounter++, 32, 20.0f, los.toFloat() + 14.5f, isOffense = false, role = PlayerRole.DB, facingYaw = 180f)) // Free Safety
        players.add(PlayerDot(idCounter++, 33, 34.0f, los.toFloat() + 11.5f, isOffense = false, role = PlayerRole.DB, facingYaw = 180f)) // Strong Safety

        // -------------------------------------------------------------
        // ON-FIELD REFEREE CREWMATES (Zebra Officials)
        // -------------------------------------------------------------
        if (userPos != OfficialPosition.REFEREE) {
            players.add(PlayerDot(idCounter++, 1, 18.5f, los.toFloat() - 8.5f, isOffense = false, role = PlayerRole.REF, facingYaw = 20f))
        }
        if (userPos != OfficialPosition.UMPIRE) {
            players.add(PlayerDot(idCounter++, 2, 28.5f, los.toFloat() + 7.5f, isOffense = false, role = PlayerRole.REF, facingYaw = 180f))
        }
        if (userPos != OfficialPosition.BACK_JUDGE) {
            players.add(PlayerDot(idCounter++, 5, 26.65f, los.toFloat() + 22.0f, isOffense = false, role = PlayerRole.REF, facingYaw = 180f))
        }
        if (userPos != OfficialPosition.LINE_JUDGE) {
            players.add(PlayerDot(idCounter++, 4, 55.5f, los.toFloat(), isOffense = false, role = PlayerRole.REF, facingYaw = 270f))
        }
        if (userPos != OfficialPosition.DOWN_JUDGE) {
            players.add(PlayerDot(idCounter++, 3, -2.2f, los.toFloat(), isOffense = false, role = PlayerRole.REF, facingYaw = 90f))
        }

        return players
    }
}

