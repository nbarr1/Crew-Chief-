package com.example.feature.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rules.data.FoulType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.core.model.CallGrade
import com.example.core.model.OfficialPosition
import com.example.core.ui.components.FieldCanvas
import com.example.core.util.SoundEffects
import com.example.ui.theme.DownMarkerOrange
import com.example.ui.theme.FlagGold
import com.example.ui.theme.GradeCorrectGreen
import com.example.ui.theme.GradeIncorrectRed
import com.example.ui.theme.ReviewBoothBlue
import com.example.ui.theme.StadiumBorder
import com.example.ui.theme.StadiumNightBg
import com.example.ui.theme.StadiumSurface
import com.example.ui.theme.StadiumSurfaceVariant
import com.example.ui.theme.StripeGray
import com.example.ui.theme.StripeWhite
import com.example.ui.theme.TurfGreen
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var showTunnelEntrance by remember { mutableStateOf(true) }
    var stadiumFlashAlpha by remember { mutableStateOf(0f) }
    var showPositionMenu by remember { mutableStateOf(false) }

    val animatedFlashAlpha by animateFloatAsState(
        targetValue = stadiumFlashAlpha,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "StadiumFlash"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StadiumNightBg,
        topBar = {
            GameTopBar(
                gameState = state.gameState,
                assignedPosition = state.assignedPosition,
                onNavigateBack = onNavigateBack,
                onPositionClick = { showPositionMenu = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            var lastSpot by remember { mutableStateOf(state.userSpottedYardLine) }

            // 3D Perspective Officiating Simulation Canvas
            FieldCanvas(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onPlayerTap = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.togglePlayerKey(it) 
                },
                onThrowFlag = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.throwFlag(it) 
                },
                onUpdateSpot = { newSpot -> 
                    if (lastSpot != newSpot) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastSpot = newSpot
                    }
                    viewModel.updateSpot(newSpot) 
                },
                onLookAround = { yawDelta, pitchDelta ->
                    viewModel.updateLookAngle(yawDelta, pitchDelta)
                }
            )

            // Dynamic Simulation HUD & Camera Angle Bar
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Simulation Bar with Camera Perspective Toggles
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    StatusBanner(phase = state.phase)
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    CameraPerspectiveSwitcher(
                        currentPerspective = state.cameraPerspective,
                        onPerspectiveSelected = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setCameraPerspective(it)
                        }
                    )
                }

                // Center Recenter Look Button if Gaze Panned
                if (Math.abs(state.headYawOffset) > 8f || Math.abs(state.headPitchOffset) > 4f) {
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.resetLookAngle()
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xCC000000),
                        border = BorderStroke(1.dp, FlagGold.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center Gaze", tint = FlagGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RE-CENTER GAZE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = StripeWhite)
                        }
                    }
                }

                // Bottom Action & Officiating Whistle / Flag Controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Replay Scrubber Bar (when frames are recorded)
                    if (state.phase == PlayPhase.DEAD_BALL_SPOTTING || state.phase == PlayPhase.REVIEW) {
                        ReplayControlBar(
                            progress = state.replayProgress,
                            onProgressChange = { viewModel.setReplayProgress(it) },
                            onStepBack = { viewModel.setReplayProgress(state.replayProgress - 0.05f) },
                            onStepForward = { viewModel.setReplayProgress(state.replayProgress + 0.05f) },
                            onReset = { viewModel.setReplayProgress(0f) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Precision Spotting Panel (during DEAD_BALL_SPOTTING)
                    if (state.phase == PlayPhase.DEAD_BALL_SPOTTING) {
                        PrecisionSpottingPanel(
                            spottedYard = state.userSpottedYardLine ?: state.gameState.yardLine.toFloat(),
                            onAdjustSpot = { delta ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.adjustSpot(delta)
                            },
                            onSnapToCarrier = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.snapSpotToBallCarrier()
                            },
                            onOpenPenaltyPicker = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.openPenaltyPicker()
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    BottomActionArea(
                        state = state,
                        onSnapBall = { viewModel.snapBall() },
                        onThrowFlagManual = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.throwFlag(state.gameState.yardLine.toFloat()) 
                        },
                        onConfirmSpot = { viewModel.confirmSpotAndEvaluate() },
                        onNextPlay = { viewModel.setupNextPlay() },
                        onBlowWhistle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundEffects.playWhistle()
                        },
                        onOpenFoulReport = {
                            viewModel.openPenaltyPicker()
                        }
                    )
                }
            }

            // Penalty / Foul Selection Dialog
            if (state.showPenaltyPicker) {
                PenaltyPickerDialog(
                    currentFoul = state.userSelectedFoul,
                    onDismiss = { viewModel.closePenaltyPicker() },
                    onSelectFoul = { foul, isOff, num ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.selectFoul(foul, isOff, num)
                    }
                )
            }

            // Post-Play Official Review Card Overlay
            AnimatedVisibility(
                visible = state.phase == PlayPhase.REVIEW,
                enter = scaleIn(initialScale = 0.85f, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                ReviewCard(state = state)
            }

            // Position Switching Dropdown Menu
            DropdownMenu(
                expanded = showPositionMenu,
                onDismissRequest = { showPositionMenu = false },
                modifier = Modifier.background(StadiumSurface)
            ) {
                OfficialPosition.values().filter { it != OfficialPosition.REPLAY_OFFICIAL }.forEach { pos ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(pos.fullName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = StripeWhite)
                                Text(pos.onFieldLocation, style = MaterialTheme.typography.labelSmall, color = FlagGold)
                            }
                        },
                        onClick = {
                            showPositionMenu = false
                            viewModel.changeAssignedPosition(pos)
                        }
                    )
                }
            }

            // Atmospheric Stadium Floodlight Flash Effect on entrance dismiss
            if (animatedFlashAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = animatedFlashAlpha))
                )
            }

            // Pre-Game "TAKING THE FIELD" Tunnel Walk Transition Overlay
            AnimatedVisibility(
                visible = showTunnelEntrance,
                enter = fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeOut(tween(400)),
                modifier = Modifier.fillMaxSize()
            ) {
                TunnelEntranceOverlay(
                    gameState = state.gameState,
                    position = state.assignedPosition,
                    onTakeTheField = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundEffects.playWhistle()
                        stadiumFlashAlpha = 0.8f
                        showTunnelEntrance = false
                    }
                )
            }
        }
    }

    LaunchedEffect(stadiumFlashAlpha) {
        if (stadiumFlashAlpha > 0f) {
            delay(100)
            stadiumFlashAlpha = 0f
        }
    }
}

@Composable
private fun CameraPerspectiveSwitcher(
    currentPerspective: CameraPerspective,
    onPerspectiveSelected: (CameraPerspective) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xDD0B130E),
        border = BorderStroke(1.dp, StadiumBorder)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CameraPerspective.values().forEach { perspective ->
                val isSelected = currentPerspective == perspective
                val bgColor = if (isSelected) TurfGreen else Color.Transparent
                val textColor = if (isSelected) Color.White else StripeGray

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .clickable { onPerspectiveSelected(perspective) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = perspective.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TunnelEntranceOverlay(
    gameState: com.example.sim.engine.GameState,
    position: OfficialPosition,
    onTakeTheField: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TunnelPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    var countdownProgress by remember { mutableStateOf(1f) }
    LaunchedEffect(Unit) {
        val totalMs = 3500L
        val interval = 50L
        var elapsed = 0L
        while (elapsed < totalMs) {
            delay(interval)
            elapsed += interval
            countdownProgress = (1f - (elapsed.toFloat() / totalMs)).coerceIn(0f, 1f)
        }
        onTakeTheField()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumNightBg)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_stadium_1788041077376),
            contentDescription = "Stadium Tunnel",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StadiumNightBg.copy(alpha = 0.85f),
                            StadiumNightBg.copy(alpha = 0.6f),
                            StadiumNightBg.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xDD000000),
                border = BorderStroke(1.5.dp, FlagGold)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sports,
                        contentDescription = null,
                        tint = FlagGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CREW OFFICIATING SIMULATION",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = FlagGold
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(glowScale),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, FlagGold.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = TurfGreen.copy(alpha = 0.2f),
                        border = BorderStroke(2.dp, TurfGreen),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = TurfGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = position.fullName.uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = StripeWhite,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "ON-FIELD SIMULATION • ${position.onFieldLocation.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = FlagGold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = StadiumNightBg,
                            border = BorderStroke(1.dp, StadiumBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SITUATION", style = MaterialTheme.typography.labelSmall, color = StripeGray)
                                Text(gameState.formattedDownAndDistance, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = DownMarkerOrange)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = StadiumNightBg,
                            border = BorderStroke(1.dp, StadiumBorder)
                        ) {
                            val side = if (gameState.yardLine < 50) "OWN" else if (gameState.yardLine > 50) "OPP" else "MID"
                            val yl = if (gameState.yardLine > 50) 100 - gameState.yardLine else gameState.yardLine
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("BALL SPOT", style = MaterialTheme.typography.labelSmall, color = StripeGray)
                                Text("$side $yl YD", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = StripeWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StadiumSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "SIMULATION MECHANICS:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = FlagGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Drag on turf to turn your head gaze in 1st/3rd person\n• Watch lineman keys for holding / neutral zone infractions\n• Quick-flick up or tap flag to penalize infractions in 3D\n• Drag forward progress laser line to spot dead ball",
                                style = MaterialTheme.typography.bodySmall,
                                color = StripeWhite
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onTakeTheField,
                    colors = ButtonDefaults.buttonColors(containerColor = FlagGold),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TAKE THE FIELD",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { countdownProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = FlagGold.copy(alpha = 0.6f),
                    trackColor = StadiumBorder
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AUTO-ENTER IN ${(countdownProgress * 3.5).toInt() + 1}S",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = StripeGray
                )
            }
        }
    }
}

@Composable
private fun GameTopBar(
    gameState: com.example.sim.engine.GameState,
    assignedPosition: OfficialPosition,
    onNavigateBack: () -> Unit,
    onPositionClick: () -> Unit
) {
    Surface(
        color = StadiumSurface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, StadiumBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StripeWhite)
            }
            
            // Official Role Selector Button
            Surface(
                onClick = onPositionClick,
                color = Color(0xDD000000),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FlagGold.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = assignedPosition.abbrev,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = FlagGold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.SwitchCamera, contentDescription = "Switch Position", tint = FlagGold, modifier = Modifier.size(14.dp))
                }
            }

            Surface(
                color = DownMarkerOrange,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = gameState.formattedDownAndDistance,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            Surface(
                color = StadiumNightBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StadiumBorder)
            ) {
                val side = if (gameState.yardLine < 50) "OWN" else if (gameState.yardLine > 50) "OPP" else "MID"
                val yl = if (gameState.yardLine > 50) 100 - gameState.yardLine else gameState.yardLine
                Text(
                    text = "$side $yl",
                    style = MaterialTheme.typography.labelLarge,
                    color = StripeWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(phase: PlayPhase) {
    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            (slideInVertically { -it / 2 } + fadeIn(tween(250)))
                .togetherWith(slideOutVertically { it / 2 } + fadeOut(tween(200)))
        },
        label = "StatusBannerTransition"
    ) { currentPhase ->
        val (text, color) = when (currentPhase) {
            PlayPhase.PRE_SNAP -> "SCAN PRE-SNAP FORMATION • DRAG TO LOOK" to ReviewBoothBlue
            PlayPhase.LIVE_BALL -> "LIVE ACTION • FLICK OR TAP FLAG FOR FOUL" to GradeIncorrectRed
            PlayPhase.DEAD_BALL_SPOTTING -> "DRAG TO ALIGN 3D FORWARD PROGRESS SPOT" to FlagGold
            PlayPhase.PENALTY_REPORT -> "CONFERRING WITH CREW CHIEF" to StripeWhite
            PlayPhase.REVIEW -> "OFFICIAL CALL EVALUATION" to StripeWhite
        }

        Surface(
            color = StadiumSurface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = color,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ReplayControlBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xEE0B130E),
        border = BorderStroke(1.dp, ReviewBoothBlue.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SlowMotionVideo,
                        contentDescription = "Replay",
                        tint = ReviewBoothBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INSTANT REPLAY / VAR SCRUBBER",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                        color = ReviewBoothBlue
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}% PLAY TIME",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = StripeWhite
                )
            }

            Slider(
                value = progress,
                onValueChange = onProgressChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = FlagGold,
                    activeTrackColor = ReviewBoothBlue,
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Replay, contentDescription = "Snap Start", tint = StripeWhite, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onStepBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Step Back", tint = StripeWhite, modifier = Modifier.size(18.dp))
                }
                Surface(
                    onClick = { onProgressChange(1f) },
                    shape = RoundedCornerShape(8.dp),
                    color = TurfGreen.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, TurfGreen)
                ) {
                    Text(
                        text = "LIVE END",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                        color = TurfGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onStepForward, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FastForward, contentDescription = "Step Forward", tint = StripeWhite, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PrecisionSpottingPanel(
    spottedYard: Float,
    onAdjustSpot: (Float) -> Unit,
    onSnapToCarrier: () -> Unit,
    onOpenPenaltyPicker: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xEE0B130E),
        border = BorderStroke(1.5.dp, DownMarkerOrange),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PinDrop, contentDescription = null, tint = DownMarkerOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BALL SPOTTER",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = DownMarkerOrange
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DownMarkerOrange.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, DownMarkerOrange)
                ) {
                    val side = if (spottedYard < 50f) "OWN" else if (spottedYard > 50f) "OPP" else "MID"
                    val yardVal = if (spottedYard > 50f) 100f - spottedYard else spottedYard
                    Text(
                        text = "$side ${String.format("%.1f", yardVal)} YD",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = DownMarkerOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Step Nudge Buttons for pinpoint spot placement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onAdjustSpot(-1.0f) },
                    shape = RoundedCornerShape(8.dp),
                    color = StadiumSurface,
                    border = BorderStroke(1.dp, StadiumBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "-1.0",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StripeWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Surface(
                    onClick = { onAdjustSpot(-0.5f) },
                    shape = RoundedCornerShape(8.dp),
                    color = StadiumSurface,
                    border = BorderStroke(1.dp, StadiumBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "-0.5",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StripeWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Surface(
                    onClick = onSnapToCarrier,
                    shape = RoundedCornerShape(8.dp),
                    color = TurfGreen.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, TurfGreen),
                    modifier = Modifier.weight(1.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = TurfGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SNAP TO RUNNER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                            color = TurfGreen
                        )
                    }
                }
                Surface(
                    onClick = { onAdjustSpot(0.5f) },
                    shape = RoundedCornerShape(8.dp),
                    color = StadiumSurface,
                    border = BorderStroke(1.dp, StadiumBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+0.5",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StripeWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Surface(
                    onClick = { onAdjustSpot(1.0f) },
                    shape = RoundedCornerShape(8.dp),
                    color = StadiumSurface,
                    border = BorderStroke(1.dp, StadiumBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+1.0",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StripeWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PenaltyPickerDialog(
    currentFoul: FoulType?,
    onDismiss: () -> Unit,
    onSelectFoul: (FoulType, isOffense: Boolean, playerNum: Int) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(0) }
    var isOffense by remember { mutableStateOf(true) }
    var selectedPlayerNum by remember { mutableStateOf(72) }

    val offenseFouls = listOf(
        FoulType.OFFENSIVE_HOLDING,
        FoulType.FALSE_START,
        FoulType.OFFENSIVE_PASS_INTERFERENCE,
        FoulType.ILLEGAL_BLOCK_IN_THE_BACK,
        FoulType.FACE_MASK,
        FoulType.INTENTIONAL_GROUNDING
    )

    val defenseFouls = listOf(
        FoulType.DEFENSIVE_PASS_INTERFERENCE,
        FoulType.DEFENSIVE_HOLDING,
        FoulType.OFFSIDE,
        FoulType.FACE_MASK,
        FoulType.UNNECESSARY_ROUGHNESS,
        FoulType.HORSE_COLLAR_TACKLE,
        FoulType.ROUGHING_THE_PASSER
    )

    val quickJerseyNumbers = if (isOffense) listOf(72, 75, 54, 88, 12, 28) else listOf(99, 90, 52, 21, 24, 33)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StadiumNightBg),
            border = BorderStroke(2.dp, FlagGold)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = FlagGold, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OFFICIAL FOUL REPORT",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = FlagGold
                        )
                    }

                    Surface(
                        onClick = { onSelectFoul(FoulType.NONE, isOffense, 0) },
                        shape = RoundedCornerShape(12.dp),
                        color = TurfGreen.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, TurfGreen)
                    ) {
                        Text(
                            text = "CLEAN PLAY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = TurfGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Offense vs Defense Squad Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { isOffense = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isOffense) FlagGold else StadiumSurface,
                        border = BorderStroke(1.dp, if (isOffense) FlagGold else StadiumBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "OFFENSE (WHITE)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = if (isOffense) Color.Black else StripeGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    Surface(
                        onClick = { isOffense = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isOffense) ReviewBoothBlue else StadiumSurface,
                        border = BorderStroke(1.dp, if (!isOffense) ReviewBoothBlue else StadiumBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "DEFENSE (NAVY)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = if (!isOffense) Color.Black else StripeGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Offending Player Number Selector
                Text(
                    text = "OFFENDING PLAYER NUMBER:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = StripeGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickJerseyNumbers.forEach { num ->
                        val isNumSelected = selectedPlayerNum == num
                        Surface(
                            onClick = { selectedPlayerNum = num },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isNumSelected) TurfGreen else StadiumSurface,
                            border = BorderStroke(1.dp, if (isNumSelected) TurfGreen else StadiumBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "#$num",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = if (isNumSelected) Color.Black else StripeWhite,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SELECT INFRACTION:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = StripeGray
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Foul Types List
                val foulList = if (isOffense) offenseFouls else defenseFouls
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(foulList) { foul ->
                        Surface(
                            onClick = { onSelectFoul(foul, isOffense, selectedPlayerNum) },
                            shape = RoundedCornerShape(10.dp),
                            color = StadiumSurface,
                            border = BorderStroke(1.dp, StadiumBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = foul.foulName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = StripeWhite
                                    )
                                    Text(
                                        text = foul.plainLanguageRule,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StripeGray,
                                        maxLines = 1
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DownMarkerOrange.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${foul.yardage} YDS",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = DownMarkerOrange,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomActionArea(
    state: GameUiState,
    onSnapBall: () -> Unit,
    onThrowFlagManual: () -> Unit,
    onConfirmSpot: () -> Unit,
    onNextPlay: () -> Unit,
    onBlowWhistle: () -> Unit,
    onOpenFoulReport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Physical Ref Whistle Button
        Surface(
            onClick = onBlowWhistle,
            shape = CircleShape,
            color = Color(0xDD1E293B),
            border = BorderStroke(2.dp, StripeWhite),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Blow Whistle",
                    tint = StripeWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Secondary Report Foul Button during dead-ball spotting
        if (state.phase == PlayPhase.DEAD_BALL_SPOTTING) {
            Surface(
                onClick = onOpenFoulReport,
                shape = RoundedCornerShape(14.dp),
                color = FlagGold.copy(alpha = 0.2f),
                border = BorderStroke(1.5.dp, FlagGold),
                modifier = Modifier.height(52.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = FlagGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.userSelectedFoul != null) state.userSelectedFoul.foulName.take(10) + ".." else "CALL FOUL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = FlagGold
                    )
                }
            }
        }

        // Primary Phase Action Button
        Box(modifier = Modifier.weight(1f)) {
            when (state.phase) {
                PlayPhase.PRE_SNAP -> {
                    Button(
                        onClick = onSnapBall,
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SNAP BALL",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.Black
                        )
                    }
                }
                PlayPhase.LIVE_BALL -> {
                    Button(
                        onClick = onThrowFlagManual,
                        colors = ButtonDefaults.buttonColors(containerColor = FlagGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = "Throw Flag", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "THROW 3D FLAG",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.Black
                        )
                    }
                }
                PlayPhase.DEAD_BALL_SPOTTING -> {
                    Button(
                        onClick = onConfirmSpot,
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LOCK IN SPOT",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.Black
                        )
                    }
                }
                PlayPhase.REVIEW -> {
                    Button(
                        onClick = onNextPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = ReviewBoothBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "NEXT SNAP",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.Black
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ReviewCard(state: GameUiState) {
    val gradeColor = when (state.grade) {
        CallGrade.CORRECT_CALL, CallGrade.CORRECT_NON_CALL -> GradeCorrectGreen
        CallGrade.MISSED_CALL, CallGrade.INCORRECT_CALL -> GradeIncorrectRed
        CallGrade.MARGINAL_CALL -> FlagGold
        CallGrade.UNNECESSARY_CALL -> GradeIncorrectRed
        else -> StripeWhite
    }
    
    val gradeIcon = when (state.grade) {
        CallGrade.CORRECT_CALL, CallGrade.CORRECT_NON_CALL -> Icons.Default.CheckCircle
        else -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, gradeColor)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = gradeIcon,
                contentDescription = null,
                tint = gradeColor,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = state.grade?.label?.uppercase() ?: "EVALUATION",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = gradeColor
            )

            state.spotAccuracyGrade?.let { spotGrade ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DownMarkerOrange.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, DownMarkerOrange)
                ) {
                    Text(
                        text = spotGrade,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = DownMarkerOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = state.feedbackMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = StripeWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Surface(
                color = StadiumNightBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StadiumBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "RULE CITATION & MECHANICS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = FlagGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.ruleCitation ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = StripeGray
                    )
                }
            }
        }
    }
}
