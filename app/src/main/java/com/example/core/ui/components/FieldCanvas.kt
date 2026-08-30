package com.example.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.OfficialPosition
import com.example.feature.game.CameraPerspective
import com.example.feature.game.GameUiState
import com.example.feature.game.PlayPhase
import com.example.feature.game.PlayerDot
import com.example.feature.game.PlayerRole
import com.example.ui.theme.DownMarkerOrange
import com.example.ui.theme.FlagGold
import com.example.ui.theme.GradeIncorrectRed
import com.example.ui.theme.ReviewBoothBlue
import com.example.ui.theme.StripeGray
import com.example.ui.theme.StripeWhite
import com.example.ui.theme.TurfGreen
import com.example.ui.theme.TurfGreenDark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ProjectedPoint(
    val x: Float,
    val y: Float,
    val depth: Float,
    val scale: Float,
    val isVisible: Boolean
)

@Composable
fun FieldCanvas(
    state: GameUiState,
    modifier: Modifier = Modifier,
    onPlayerTap: (Int) -> Unit,
    onThrowFlag: (Float) -> Unit,
    onUpdateSpot: (Float) -> Unit,
    onLookAround: (Float, Float) -> Unit = { _, _ -> }
) {
    val textMeasurer = rememberTextMeasurer()

    // Key assignment pulsing glow
    val infiniteTransition = rememberInfiniteTransition(label = "Field3DAnimations")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "3DPulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070E09))
            .pointerInput(state.phase, state.cameraPerspective) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        when (state.phase) {
                            PlayPhase.PRE_SNAP -> {
                                if (state.cameraPerspective != CameraPerspective.TACTICAL_2D) {
                                    // First or Third Person: Dragging pans the official's head gaze
                                    val yawDelta = dragAmount.x * 0.15f
                                    val pitchDelta = -dragAmount.y * 0.12f
                                    onLookAround(yawDelta, pitchDelta)
                                } else {
                                    // 2D tap/drag
                                    val yardSpan = (state.visibleYardRange.last - state.visibleYardRange.first).coerceAtLeast(1)
                                    val tappedYardY = state.visibleYardRange.first + (1f - (change.position.y / size.height)) * yardSpan
                                    val tappedLateralX = (change.position.x / size.width) * 53.3f
                                    state.players.forEach { p ->
                                        val distSq = Math.pow((p.xYard - tappedLateralX).toDouble(), 2.0) + Math.pow((p.yYard - tappedYardY).toDouble(), 2.0)
                                        if (distSq < 10.0) onPlayerTap(p.id)
                                    }
                                }
                            }
                            PlayPhase.LIVE_BALL -> {
                                if (Math.abs(dragAmount.y) > 20f && dragAmount.y < 0) {
                                    // Quick flick up throws penalty flag
                                    onThrowFlag(state.gameState.yardLine.toFloat())
                                } else if (state.cameraPerspective != CameraPerspective.TACTICAL_2D) {
                                    // Track ball with head gaze
                                    val yawDelta = dragAmount.x * 0.18f
                                    val pitchDelta = -dragAmount.y * 0.12f
                                    onLookAround(yawDelta, pitchDelta)
                                }
                            }
                            PlayPhase.DEAD_BALL_SPOTTING -> {
                                // Drag along the yardlines to adjust ball spot
                                val dragDeltaY = -dragAmount.y * 0.05f
                                val currentSpot = state.userSpottedYardLine ?: state.gameState.yardLine.toFloat()
                                val newSpot = (currentSpot + dragDeltaY).coerceIn(1f, 99f)
                                val snappedSpot = Math.round(newSpot * 2) / 2.0f
                                onUpdateSpot(snappedSpot)
                            }
                            else -> {}
                        }
                    }
                )
            }
            .pointerInput(state.phase) {
                detectTapGestures { offset ->
                    if (state.phase == PlayPhase.PRE_SNAP) {
                        // Check if tapped near any player in screen space
                        // We will allow player keying on tap
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val los = state.gameState.yardLine.toFloat()
        val ltg = (state.gameState.yardLine + state.gameState.distance).toFloat()

        if (state.cameraPerspective == CameraPerspective.TACTICAL_2D) {
            draw2DOverheadField(state, textMeasurer, pulseAnim)
            return@Canvas
        }

        // ==========================================
        // 3D PERSPECTIVE SIMULATION CAMERA SETUP
        // ==========================================
        val isFirstPerson = state.cameraPerspective == CameraPerspective.FIRST_PERSON
        val isThirdPerson = state.cameraPerspective == CameraPerspective.THIRD_PERSON

        // Official Position vantage points
        val refStationX: Float
        val refStationY: Float
        val baseYaw: Float

        when (state.assignedPosition) {
            OfficialPosition.DOWN_JUDGE -> {
                refStationX = -2.5f
                refStationY = los
                baseYaw = 90f // Looking across field (+X)
            }
            OfficialPosition.LINE_JUDGE -> {
                refStationX = 55.8f
                refStationY = los
                baseYaw = 270f // Looking across field (-X)
            }
            OfficialPosition.REFEREE -> {
                refStationX = 26.65f
                refStationY = (los - 8.5f).coerceAtLeast(1f)
                baseYaw = 0f // Looking forward (+Y)
            }
            OfficialPosition.UMPIRE -> {
                refStationX = 26.65f
                refStationY = (los + 7.5f).coerceAtMost(99f)
                baseYaw = 180f // Looking backward (-Y)
            }
            OfficialPosition.BACK_JUDGE -> {
                refStationX = 26.65f
                refStationY = (los + 20.0f).coerceAtMost(99f)
                baseYaw = 180f // Looking backward (-Y)
            }
            OfficialPosition.FIELD_JUDGE -> {
                refStationX = -2.5f
                refStationY = (los + 16.0f).coerceAtMost(99f)
                baseYaw = 75f
            }
            OfficialPosition.SIDE_JUDGE -> {
                refStationX = 55.8f
                refStationY = (los + 16.0f).coerceAtMost(99f)
                baseYaw = 285f
            }
            else -> {
                refStationX = -2.5f
                refStationY = los
                baseYaw = 90f
            }
        }

        // Camera World Coordinates
        val camX: Float
        val camY: Float
        val camZ: Float
        val camYaw: Float
        val camPitch: Float

        when (state.cameraPerspective) {
            CameraPerspective.FIRST_PERSON -> {
                camX = refStationX
                camY = refStationY
                camZ = 1.85f // Official eye level
                camYaw = baseYaw + state.headYawOffset
                camPitch = 8f + state.headPitchOffset
            }
            CameraPerspective.THIRD_PERSON -> {
                val yawRad = (baseYaw * PI / 180.0).toFloat()
                camX = refStationX - 3.8f * sin(yawRad)
                camY = refStationY - 3.8f * cos(yawRad)
                camZ = 2.9f
                camYaw = baseYaw + state.headYawOffset
                camPitch = 12f + state.headPitchOffset
            }
            CameraPerspective.SIDELINE_CAM -> {
                camX = -12.0f
                camY = los
                camZ = 8.5f
                camYaw = 90f + state.headYawOffset * 0.7f
                camPitch = 22f + state.headPitchOffset * 0.7f
            }
            CameraPerspective.ENDZONE_CAM -> {
                camX = 26.65f
                camY = (los + 28f).coerceAtMost(108f)
                camZ = 5.2f
                camYaw = 180f + state.headYawOffset * 0.7f
                camPitch = 14f + state.headPitchOffset * 0.7f
            }
            CameraPerspective.TACTICAL_2D -> {
                camX = 26.65f
                camY = los
                camZ = 24.0f
                camYaw = 0f + state.headYawOffset * 0.5f
                camPitch = 70f + state.headPitchOffset * 0.5f
            }
        }

        val zoomFactor = state.cameraZoom.coerceIn(0.7f, 2.5f)
        val focalLength = w * 0.95f * zoomFactor

        val yawRad = (camYaw * PI / 180.0).toFloat()
        val pitchRad = (camPitch * PI / 180.0).toFloat()

        val sinYaw = sin(yawRad)
        val cosYaw = cos(yawRad)
        val sinPitch = sin(pitchRad)
        val cosPitch = cos(pitchRad)

        // 3D Perspective Projection Function
        fun project3D(xYard: Float, yYard: Float, zHeight: Float): ProjectedPoint {
            val dx = xYard - camX
            val dy = yYard - camY
            val dz = zHeight - camZ

            // Yaw rotation: 0 deg = looking +Y, 90 deg = looking +X, 180 deg = looking -Y, 270 deg = looking -X
            val xCam = dx * cosYaw - dy * sinYaw
            val yCam = dx * sinYaw + dy * cosYaw

            // Pitch rotation: looking down tilts ground up on screen
            val yFinal = yCam * cosPitch - dz * sinPitch
            val zFinal = dz * cosPitch + yCam * sinPitch

            if (yFinal <= 0.25f) {
                return ProjectedPoint(0f, 0f, yFinal, 0f, false)
            }

            val scale = focalLength / yFinal
            val screenX = (w / 2f) + (xCam * scale)
            val screenY = (h / 2f) - (zFinal * scale)

            return ProjectedPoint(screenX, screenY, yFinal, scale, true)
        }

        // ==========================================
        // 1. DRAW NIGHT STADIUM SKY, LIGHTS & BOWL
        // ==========================================
        val horizonY = ((h / 2f) - (sinPitch / cosPitch.coerceAtLeast(0.01f) * focalLength)).coerceIn(h * 0.08f, h * 0.65f)

        // Twilight Stadium Sky Dome with Atmospheric Lighting
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF020406),
                    Color(0xFF09141F),
                    Color(0xFF0F222D),
                    Color(0xFF163228),
                    Color(0xFF1B402B)
                ),
                startY = 0f,
                endY = horizonY
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, horizonY + 2.dp.toPx())
        )

        // Stadium Cantilever Roof Trusses & Upper Deck
        val roofTrussPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.28f, horizonY * 0.35f)
            lineTo(w * 0.72f, horizonY * 0.35f)
            lineTo(w, 0f)
            close()
        }
        drawPath(roofTrussPath, Color(0x330F172A))

        // Multi-tier Stadium Bowl Silhouette with Luxury Suites & Cheering Crowd
        val standsPath = Path().apply {
            moveTo(0f, horizonY)
            lineTo(0f, horizonY - 45.dp.toPx())
            lineTo(w * 0.25f, horizonY - 60.dp.toPx())
            lineTo(w * 0.5f, horizonY - 68.dp.toPx())
            lineTo(w * 0.75f, horizonY - 60.dp.toPx())
            lineTo(w, horizonY - 45.dp.toPx())
            lineTo(w, horizonY)
            close()
        }
        drawPath(
            path = standsPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF090D14)),
                startY = horizonY - 70.dp.toPx(),
                endY = horizonY
            )
        )

        // Luxury Suites Glass Ribbon Glow
        val suiteY = horizonY - 32.dp.toPx()
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0x44FFE082), Color(0x99FFE082), Color(0x44FFE082)),
                startX = w * 0.1f,
                endX = w * 0.9f
            ),
            start = Offset(w * 0.1f, suiteY),
            end = Offset(w * 0.9f, suiteY),
            strokeWidth = 3.dp.toPx()
        )

        // 4 Stadium Floodlight Towers with Volumetric Beams
        drawStadiumLightTower(Offset(w * 0.12f, horizonY * 0.4f), w, h)
        drawStadiumLightTower(Offset(w * 0.38f, horizonY * 0.32f), w, h)
        drawStadiumLightTower(Offset(w * 0.62f, horizonY * 0.32f), w, h)
        drawStadiumLightTower(Offset(w * 0.88f, horizonY * 0.4f), w, h)

        // Giant Endzone Stadium Jumbotron Videoboard
        val jumbotronPt = project3D(26.65f, 114f, 14f)
        if (jumbotronPt.isVisible && jumbotronPt.scale > 1.5f) {
            val jbW = (75.dp.toPx() * jumbotronPt.scale * 0.05f).coerceIn(50f, 220f)
            val jbH = (42.dp.toPx() * jumbotronPt.scale * 0.05f).coerceIn(28f, 120f)
            // Jumbotron Screen Chassis
            drawRoundRect(
                color = Color(0xFF0A0F1D),
                topLeft = Offset(jumbotronPt.x - jbW / 2f, jumbotronPt.y - jbH / 2f),
                size = Size(jbW, jbH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            // Jumbotron Screen Surface (Glow & Live Game Display)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF0284C7), Color(0xFF0F172A)),
                    startY = jumbotronPt.y - jbH * 0.45f,
                    endY = jumbotronPt.y + jbH * 0.45f
                ),
                topLeft = Offset(jumbotronPt.x - jbW * 0.46f, jumbotronPt.y - jbH * 0.42f),
                size = Size(jbW * 0.92f, jbH * 0.84f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            // Screen Glow Frame
            drawRoundRect(
                color = FlagGold.copy(alpha = 0.8f),
                topLeft = Offset(jumbotronPt.x - jbW / 2f, jumbotronPt.y - jbH / 2f),
                size = Size(jbW, jbH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // ==========================================
        // 2. DRAW BASE VIBRANT TURF GROUND PLANE
        // ==========================================
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TurfGreenDark,
                    TurfGreen,
                    Color(0xFF1A532C),
                    Color(0xFF164826)
                ),
                startY = horizonY,
                endY = h
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY)
        )

        // ==========================================
        // 3. DRAW 3D ENDZONES WITH TEAM LETTERING
        // ==========================================
        // Far Endzone (Yard 100 to 110) - Home Midnight Navy
        val ezFarTL = project3D(0f, 110f, 0f)
        val ezFarTR = project3D(53.3f, 110f, 0f)
        val ezFarBR = project3D(53.3f, 100f, 0f)
        val ezFarBL = project3D(0f, 100f, 0f)

        if (ezFarBL.isVisible || ezFarBR.isVisible || ezFarTL.isVisible) {
            val ezPath = Path().apply {
                val pt1 = if (ezFarTL.isVisible) Offset(ezFarTL.x, ezFarTL.y) else Offset(0f, horizonY)
                val pt2 = if (ezFarTR.isVisible) Offset(ezFarTR.x, ezFarTR.y) else Offset(w, horizonY)
                val pt3 = if (ezFarBR.isVisible) Offset(ezFarBR.x, ezFarBR.y) else Offset(w, horizonY)
                val pt4 = if (ezFarBL.isVisible) Offset(ezFarBL.x, ezFarBL.y) else Offset(0f, horizonY)
                moveTo(pt1.x, pt1.y)
                lineTo(pt2.x, pt2.y)
                lineTo(pt3.x, pt3.y)
                lineTo(pt4.x, pt4.y)
                close()
            }
            drawPath(ezPath, Color(0xFF172554))
            
            // Painted Endzone Wordmark "WILDCATS"
            val ezTextPt = project3D(26.65f, 105f, 0f)
            if (ezTextPt.isVisible && ezTextPt.scale > 3f) {
                val ezFontSize = (ezTextPt.scale * 1.6f).coerceIn(10f, 32f)
                val ezLayout = textMeasurer.measure(
                    "WILDCATS",
                    TextStyle(color = StripeWhite.copy(alpha = 0.85f), fontSize = ezFontSize.sp, fontWeight = FontWeight.Black)
                )
                drawText(ezLayout, topLeft = Offset(ezTextPt.x - ezLayout.size.width / 2f, ezTextPt.y - ezLayout.size.height / 2f))
            }
        }

        // Near Endzone (Yard 0 to -10) - Crimson / Gold
        val ezNearTL = project3D(0f, 0f, 0f)
        val ezNearTR = project3D(53.3f, 0f, 0f)
        val ezNearBR = project3D(53.3f, -10f, 0f)
        val ezNearBL = project3D(0f, -10f, 0f)

        if (ezNearTL.isVisible || ezNearTR.isVisible) {
            val ezPath = Path().apply {
                val pt1 = if (ezNearTL.isVisible) Offset(ezNearTL.x, ezNearTL.y) else Offset(0f, h)
                val pt2 = if (ezNearTR.isVisible) Offset(ezNearTR.x, ezNearTR.y) else Offset(w, h)
                val pt3 = if (ezNearBR.isVisible) Offset(ezNearBR.x, ezNearBR.y) else Offset(w, h)
                val pt4 = if (ezNearBL.isVisible) Offset(ezNearBL.x, ezNearBL.y) else Offset(0f, h)
                moveTo(pt1.x, pt1.y)
                lineTo(pt2.x, pt2.y)
                lineTo(pt3.x, pt3.y)
                lineTo(pt4.x, pt4.y)
                close()
            }
            drawPath(ezPath, Color(0xFF881337))
            
            // Painted Endzone Wordmark "TITANS"
            val ezTextPt = project3D(26.65f, -5f, 0f)
            if (ezTextPt.isVisible && ezTextPt.scale > 3f) {
                val ezFontSize = (ezTextPt.scale * 1.6f).coerceIn(10f, 32f)
                val ezLayout = textMeasurer.measure(
                    "TITANS",
                    TextStyle(color = FlagGold.copy(alpha = 0.85f), fontSize = ezFontSize.sp, fontWeight = FontWeight.Black)
                )
                drawText(ezLayout, topLeft = Offset(ezTextPt.x - ezLayout.size.width / 2f, ezTextPt.y - ezLayout.size.height / 2f))
            }
        }

        // ==========================================
        // 3. DRAW 3D 5-YARD ALTERNATING STRIPS & YARDLINES
        // ==========================================
        val minYard = (los - 35).toInt().coerceAtLeast(0)
        val maxYard = (los + 45).toInt().coerceAtMost(100)

        for (yard in (minYard / 5 * 5)..(maxYard / 5 * 5) step 5) {
            val y1 = yard.toFloat()
            val y2 = (yard + 5).toFloat()

            val pTL = project3D(0f, y2, 0f)
            val pTR = project3D(53.3f, y2, 0f)
            val pBR = project3D(53.3f, y1, 0f)
            val pBL = project3D(0f, y1, 0f)

            val isDarkStrip = ((yard / 5) % 2 == 0)
            if (isDarkStrip && (pTL.isVisible || pBL.isVisible)) {
                // If any strip vertices are visible, render alternating subtle strip overlay
                val stripColor = TurfGreenDark.copy(alpha = 0.55f)
                val ptTopL = if (pTL.isVisible) Offset(pTL.x, pTL.y) else Offset(0f, horizonY)
                val ptTopR = if (pTR.isVisible) Offset(pTR.x, pTR.y) else Offset(w, horizonY)
                val ptBotR = if (pBR.isVisible) Offset(pBR.x, pBR.y) else Offset(w, h)
                val ptBotL = if (pBL.isVisible) Offset(pBL.x, pBL.y) else Offset(0f, h)

                val quadPath = Path().apply {
                    moveTo(ptTopL.x, ptTopL.y)
                    lineTo(ptTopR.x, ptTopR.y)
                    lineTo(ptBotR.x, ptBotR.y)
                    lineTo(ptBotL.x, ptBotL.y)
                    close()
                }
                drawPath(quadPath, stripColor)
            }

            // 5-yard and 10-yard major chalk lines
            if (pBL.isVisible && pBR.isVisible) {
                val is10Yd = (yard % 10 == 0)
                drawLine(
                    color = StripeWhite.copy(alpha = if (is10Yd) 0.95f else 0.7f),
                    start = Offset(pBL.x, pBL.y),
                    end = Offset(pBR.x, pBR.y),
                    strokeWidth = if (is10Yd) (2.5f * pBL.scale * 0.05f).coerceIn(1.5f, 5f) else (1.5f * pBL.scale * 0.05f).coerceIn(1f, 3f)
                )

                // 10-Yard Painted Numerals & Directional Chevrons in 3D Perspective
                if (is10Yd && yard > 0 && yard < 100) {
                    val displayYard = if (yard > 50) 100 - yard else yard
                    val chevronText = when {
                        yard < 50 -> "◄ $displayYard"
                        yard > 50 -> "$displayYard ►"
                        else -> "$displayYard"
                    }
                    val numPtLeft = project3D(8.5f, y1 + 1.2f, 0f)
                    val numPtRight = project3D(44.8f, y1 + 1.2f, 0f)

                    if (numPtLeft.isVisible && numPtLeft.scale > 5f) {
                        val fontSize = (numPtLeft.scale * 0.78f).coerceIn(9f, 22f)
                        val numLayout = textMeasurer.measure(
                            chevronText,
                            TextStyle(color = StripeWhite.copy(alpha = 0.9f), fontSize = fontSize.sp, fontWeight = FontWeight.Black)
                        )
                        drawText(numLayout, topLeft = Offset(numPtLeft.x - numLayout.size.width / 2f, numPtLeft.y - numLayout.size.height / 2f))
                    }
                    if (numPtRight.isVisible && numPtRight.scale > 5f) {
                        val fontSize = (numPtRight.scale * 0.78f).coerceIn(9f, 22f)
                        val numLayout = textMeasurer.measure(
                            chevronText,
                            TextStyle(color = StripeWhite.copy(alpha = 0.9f), fontSize = fontSize.sp, fontWeight = FontWeight.Black)
                        )
                        drawText(numLayout, topLeft = Offset(numPtRight.x - numLayout.size.width / 2f, numPtRight.y - numLayout.size.height / 2f))
                    }
                }
            }
        }

        // ==========================================
        // 4. DRAW 3D HASH MARKS & 1-YARD SIDELINE TICKS
        // ==========================================
        for (yard in minYard until maxYard step 5) {
            val y1 = yard.toFloat()
            val y2 = (yard + 5).toFloat()
            val pL1 = project3D(0f, y1, 0f)
            val pL2 = project3D(0f, y2, 0f)
            val pR1 = project3D(53.3f, y1, 0f)
            val pR2 = project3D(53.3f, y2, 0f)

            if (pL1.isVisible && pL2.isVisible) {
                drawLine(StripeWhite, Offset(pL1.x, pL1.y), Offset(pL2.x, pL2.y), strokeWidth = 3.5.dp.toPx())
            }
            if (pR1.isVisible && pR2.isVisible) {
                drawLine(StripeWhite, Offset(pR1.x, pR1.y), Offset(pR2.x, pR2.y), strokeWidth = 3.5.dp.toPx())
            }
        }

        // 1-Yard Sideline and College Hash Ticks
        for (y in minYard..maxYard) {
            if (y % 5 != 0) {
                val yf = y.toFloat()
                // Left & Right Sideline Inbound Ticks
                val sL1 = project3D(0f, yf, 0f)
                val sL2 = project3D(0.7f, yf, 0f)
                val sR1 = project3D(52.6f, yf, 0f)
                val sR2 = project3D(53.3f, yf, 0f)

                if (sL1.isVisible && sL2.isVisible) {
                    drawLine(StripeWhite.copy(alpha = 0.75f), Offset(sL1.x, sL1.y), Offset(sL2.x, sL2.y), strokeWidth = 1.8.dp.toPx())
                }
                if (sR1.isVisible && sR2.isVisible) {
                    drawLine(StripeWhite.copy(alpha = 0.75f), Offset(sR1.x, sR1.y), Offset(sR2.x, sR2.y), strokeWidth = 1.8.dp.toPx())
                }

                // NCAA College Hash Marks
                val h1L = project3D(20f, yf, 0f)
                val h1R = project3D(20.8f, yf, 0f)
                val h2L = project3D(32.5f, yf, 0f)
                val h2R = project3D(33.3f, yf, 0f)

                if (h1L.isVisible && h1R.isVisible) {
                    drawLine(StripeWhite.copy(alpha = 0.75f), Offset(h1L.x, h1L.y), Offset(h1R.x, h1R.y), strokeWidth = 1.8.dp.toPx())
                }
                if (h2L.isVisible && h2R.isVisible) {
                    drawLine(StripeWhite.copy(alpha = 0.75f), Offset(h2L.x, h2L.y), Offset(h2R.x, h2R.y), strokeWidth = 1.8.dp.toPx())
                }
            }
        }

        // Sideline Coaching Box restricted zone (from 25 to 75 yard line)
        val coachBoxY1 = 25f
        val coachBoxY2 = 75f
        val cbL1 = project3D(-2.0f, coachBoxY1, 0f)
        val cbL2 = project3D(-2.0f, coachBoxY2, 0f)
        if (cbL1.isVisible && cbL2.isVisible) {
            drawLine(
                color = StripeWhite.copy(alpha = 0.5f),
                start = Offset(cbL1.x, cbL1.y),
                end = Offset(cbL2.x, cbL2.y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
            )
        }

        // ==========================================
        // 5. DRAW 3D GOAL LINE PYLONS
        // ==========================================
        val pylonLocations = listOf(
            Pair(0f, 0f), Pair(53.3f, 0f),
            Pair(0f, 100f), Pair(53.3f, 100f)
        )
        pylonLocations.forEach { (px, py) ->
            val pBase = project3D(px, py, 0f)
            val pTop = project3D(px, py, 0.45f)
            if (pBase.isVisible && pTop.isVisible) {
                val pScale = pBase.scale * 0.05f
                val pWidth = (6.dp.toPx() * pScale).coerceIn(4f, 14f)
                // Black base anchor
                drawCircle(Color(0xFF1E293B), radius = pWidth * 0.7f, center = Offset(pBase.x, pBase.y))
                // Fluorescent Orange 3D Pylon Body
                drawLine(
                    color = DownMarkerOrange,
                    start = Offset(pBase.x, pBase.y),
                    end = Offset(pTop.x, pTop.y),
                    strokeWidth = pWidth,
                    cap = StrokeCap.Square
                )
            }
        }

        // ==========================================
        // 6. DRAW 3D LINE OF SCRIMMAGE (BLUE LASER)
        // ==========================================
        val losL = project3D(-1.5f, los, 0f)
        val losR = project3D(54.8f, los, 0f)
        if (losL.isVisible && losR.isVisible) {
            // Laser Glow
            drawLine(ReviewBoothBlue.copy(alpha = 0.35f), Offset(losL.x, losL.y), Offset(losR.x, losR.y), strokeWidth = 8.dp.toPx())
            // Core Beam
            drawLine(ReviewBoothBlue, Offset(losL.x, losL.y), Offset(losR.x, losR.y), strokeWidth = 3.dp.toPx())
        }

        // ==========================================
        // 7. DRAW 3D LINE TO GAIN & CHAIN GANG DOWN BOX
        // ==========================================
        val ltgL = project3D(-1.5f, ltg, 0f)
        val ltgR = project3D(54.8f, ltg, 0f)
        if (ltgL.isVisible && ltgR.isVisible) {
            // Laser Glow
            drawLine(FlagGold.copy(alpha = 0.4f), Offset(ltgL.x, ltgL.y), Offset(ltgR.x, ltgR.y), strokeWidth = 8.dp.toPx())
            // Core Beam
            drawLine(FlagGold, Offset(ltgL.x, ltgL.y), Offset(ltgR.x, ltgR.y), strokeWidth = 3.dp.toPx())

            // 3D Chain Gang: Down Box at LOS
            val dbBase = project3D(-1.5f, los, 0f)
            val dbTop = project3D(-1.5f, los, 2.6f)
            if (dbBase.isVisible && dbTop.isVisible) {
                // Down Box Pole
                drawLine(Color(0xFF1E293B), Offset(dbBase.x, dbBase.y), Offset(dbTop.x, dbTop.y), strokeWidth = 3.dp.toPx())
                // Down Box Flip Numeral Head
                val boxRadius = (9.dp.toPx() * dbTop.scale * 0.05f).coerceIn(7f, 18f)
                drawCircle(DownMarkerOrange, radius = boxRadius, center = Offset(dbTop.x, dbTop.y))
                drawCircle(Color(0xFF111827), radius = boxRadius, center = Offset(dbTop.x, dbTop.y), style = Stroke(width = 2.dp.toPx()))
                val downTextLayout = textMeasurer.measure(
                    "${state.gameState.down}",
                    TextStyle(color = Color(0xFF111827), fontSize = (boxRadius * 1.3f).sp, fontWeight = FontWeight.Black)
                )
                drawText(downTextLayout, topLeft = Offset(dbTop.x - downTextLayout.size.width / 2f, dbTop.y - downTextLayout.size.height / 2f))
            }

            // 3D Chain Gang: LTG Bullseye Stake
            val stakeBase = project3D(-1.5f, ltg, 0f)
            val stakeTop = project3D(-1.5f, ltg, 2.6f)
            if (stakeBase.isVisible && stakeTop.isVisible) {
                drawLine(Color(0xFF1E293B), Offset(stakeBase.x, stakeBase.y), Offset(stakeTop.x, stakeTop.y), strokeWidth = 3.dp.toPx())
                val bullseyeRadius = (9.dp.toPx() * stakeTop.scale * 0.05f).coerceIn(7f, 18f)
                drawCircle(FlagGold, radius = bullseyeRadius, center = Offset(stakeTop.x, stakeTop.y))
                drawCircle(Color(0xFF111827), radius = bullseyeRadius * 0.5f, center = Offset(stakeTop.x, stakeTop.y))
            }

            // 10-Yard Chain between Down Box & LTG Stake
            if (dbBase.isVisible && stakeBase.isVisible) {
                drawLine(
                    color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
                    start = Offset(dbBase.x, dbBase.y),
                    end = Offset(stakeBase.x, stakeBase.y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }
        }

        // ==========================================
        // 6. DRAW 3D FOOTBALL GOALPOST ON HORIZON
        // ==========================================
        val gpBase = project3D(26.65f, 100f, 0f)
        val gpCrossbar = project3D(26.65f, 100f, 3.3f)
        val gpUprightL = project3D(23.5f, 100f, 10f)
        val gpUprightR = project3D(29.8f, 100f, 10f)

        if (gpBase.isVisible && gpCrossbar.isVisible) {
            val gpColor = Color(0xFFFFD600)
            drawLine(gpColor, Offset(gpBase.x, gpBase.y), Offset(gpCrossbar.x, gpCrossbar.y), strokeWidth = 3.dp.toPx())
            val crossL = project3D(23.5f, 100f, 3.3f)
            val crossR = project3D(29.8f, 100f, 3.3f)
            if (crossL.isVisible && crossR.isVisible) {
                drawLine(gpColor, Offset(crossL.x, crossL.y), Offset(crossR.x, crossR.y), strokeWidth = 2.5.dp.toPx())
                if (gpUprightL.isVisible) drawLine(gpColor, Offset(crossL.x, crossL.y), Offset(gpUprightL.x, gpUprightL.y), strokeWidth = 2.dp.toPx())
                if (gpUprightR.isVisible) drawLine(gpColor, Offset(crossR.x, crossR.y), Offset(gpUprightR.x, gpUprightR.y), strokeWidth = 2.dp.toPx())
            }
        }

        // ==========================================
        // 7. DRAW DEPTH-SORTED 3D PLAYERS & FOOTBALL
        // ==========================================
        data class RenderableEntity(
            val depth: Float,
            val drawAction: () -> Unit
        )

        val renderQueue = mutableListOf<RenderableEntity>()

        // 3D Players
        state.players.forEach { player ->
            val feetPt = project3D(player.xYard, player.yYard, 0f)
            val headPt = project3D(player.xYard, player.yYard, 1.85f)

            if (feetPt.isVisible && headPt.isVisible) {
                renderQueue.add(
                    RenderableEntity(feetPt.depth) {
                        draw3DPlayer(
                            player = player,
                            feetPt = feetPt,
                            headPt = headPt,
                            textMeasurer = textMeasurer,
                            pulseScale = if (player.isKey) pulseAnim else 1f
                        )
                    }
                )
            }
        }

        // 3D Football
        val ball = state.ball3D
        val ballShadowPt = project3D(ball.xYard, ball.yYard, 0f)
        val ballPt = project3D(ball.xYard, ball.yYard, ball.zHeight)

        if (ballShadowPt.isVisible && ballPt.isVisible) {
            renderQueue.add(
                RenderableEntity(ballShadowPt.depth) {
                    draw3DFootball(ballShadowPt, ballPt, ball.spinAngle)
                }
            )
        }

        // 3D Penalty Flag
        state.flag3D?.let { flag ->
            val flagShadowPt = project3D(flag.xYard, flag.yYard, 0f)
            val flagPt = project3D(flag.xYard, flag.yYard, flag.zHeight)
            if (flagShadowPt.isVisible && flagPt.isVisible) {
                renderQueue.add(
                    RenderableEntity(flagShadowPt.depth) {
                        draw3DPenaltyFlag(flagShadowPt, flagPt)
                    }
                )
            }
        }

        // 3D Ball Spotting Target & Laser Line (DEAD_BALL_SPOTTING)
        state.userSpottedYardLine?.let { spotYard ->
            if (state.phase == PlayPhase.DEAD_BALL_SPOTTING) {
                val spotPtL = project3D(0f, spotYard, 0f)
                val spotPtR = project3D(53.3f, spotYard, 0f)
                val spotCenter = project3D(26.65f, spotYard, 0f)

                if (spotPtL.isVisible && spotPtR.isVisible && spotCenter.isVisible) {
                    renderQueue.add(
                        RenderableEntity(spotCenter.depth - 0.1f) {
                            // Spotting laser across field
                            drawLine(
                                color = DownMarkerOrange,
                                start = Offset(spotPtL.x, spotPtL.y),
                                end = Offset(spotPtR.x, spotPtR.y),
                                strokeWidth = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                            )
                            // 3D Pulsing Target Reticle on spot
                            drawCircle(
                                color = DownMarkerOrange.copy(alpha = 0.25f),
                                radius = (18.dp.toPx() * spotCenter.scale * 0.05f * pulseAnim).coerceIn(12f, 40f),
                                center = Offset(spotCenter.x, spotCenter.y)
                            )
                            drawCircle(
                                color = DownMarkerOrange,
                                radius = (14.dp.toPx() * spotCenter.scale * 0.05f).coerceIn(8f, 28f),
                                center = Offset(spotCenter.x, spotCenter.y),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    )
                }
            }
        }

        // Render back-to-front (furthest depth first)
        renderQueue.sortByDescending { it.depth }
        renderQueue.forEach { it.drawAction() }

        // ==========================================
        // 8. THIRD PERSON REFEREE FOREGROUND MODEL
        // ==========================================
        if (isThirdPerson) {
            drawForegroundOfficialZebra(w, h)
        }

        // ==========================================
        // 9. FIRST PERSON REF HUD & COMPASS ORIENTATION
        // ==========================================
        if (isFirstPerson) {
            drawFirstPersonRefOverlay(w, h, state.headYawOffset, state.assignedPosition)
        }
    }
}

private fun DrawScope.drawStadiumLightTower(center: Offset, w: Float, h: Float) {
    // 1. Volumetric Downward Stadium Light Cone / Beam
    val beamPath = Path().apply {
        moveTo(center.x - 12.dp.toPx(), center.y)
        lineTo(center.x + 12.dp.toPx(), center.y)
        lineTo(center.x + 85.dp.toPx(), h)
        lineTo(center.x - 85.dp.toPx(), h)
        close()
    }
    drawPath(
        path = beamPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x22FFE082), Color(0x0CFFE082), Color(0x00000000)),
            startY = center.y,
            endY = h
        )
    )

    // 2. Light Tower Glare Bloom & Lens Flare
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xE6FFFFFF), Color(0x88FFE082), Color(0x22FFE082), Color(0x00FFE082)),
            center = center,
            radius = 50.dp.toPx()
        ),
        radius = 50.dp.toPx(),
        center = center
    )
    // 3. Steel Lattice Truss Bank
    drawRect(
        color = Color(0xFFE2E8F0),
        topLeft = Offset(center.x - 16.dp.toPx(), center.y - 8.dp.toPx()),
        size = Size(32.dp.toPx(), 16.dp.toPx())
    )
    // 4. Individual High-Output LED Bulbs
    for (row in 0..2) {
        for (col in 0..5) {
            val bx = center.x - 13.dp.toPx() + (col * 5.dp.toPx())
            val by = center.y - 6.dp.toPx() + (row * 5.dp.toPx())
            drawCircle(Color.White, radius = 1.8.dp.toPx(), center = Offset(bx, by))
        }
    }
}

private fun DrawScope.draw3DPlayer(
    player: PlayerDot,
    feetPt: ProjectedPoint,
    headPt: ProjectedPoint,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    pulseScale: Float
) {
    val playerHeight = (feetPt.y - headPt.y).coerceAtLeast(10f)
    val isLineman = player.role == PlayerRole.OL || player.role == PlayerRole.DL
    val isRef = player.role == PlayerRole.REF
    val playerWidth = if (isLineman) playerHeight * 0.52f else playerHeight * 0.42f
    val centerX = feetPt.x
    val isOff = player.isOffense

    // 1. 3D Turf Ground Shadow
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(centerX - playerWidth * 0.65f, feetPt.y - playerHeight * 0.08f),
        size = Size(playerWidth * 1.3f, playerHeight * 0.16f)
    )

    // 2. Pre-Snap Key Targeting Halo & Tactical Reticle
    if (player.isKey) {
        // Pulsing ground ring
        drawOval(
            color = DownMarkerOrange.copy(alpha = 0.3f),
            topLeft = Offset(centerX - playerWidth * 0.95f * pulseScale, feetPt.y - playerHeight * 0.16f * pulseScale),
            size = Size(playerWidth * 1.9f * pulseScale, playerHeight * 0.32f * pulseScale)
        )
        drawOval(
            color = DownMarkerOrange,
            topLeft = Offset(centerX - playerWidth * 0.85f, feetPt.y - playerHeight * 0.12f),
            size = Size(playerWidth * 1.7f, playerHeight * 0.24f),
            style = Stroke(width = 2.dp.toPx())
        )
        // Tactical Target Corner Brackets around player
        val bracketSize = (playerHeight * 0.15f).coerceIn(4f, 16f)
        val bTop = headPt.y - playerHeight * 0.1f
        val bBottom = feetPt.y + playerHeight * 0.05f
        val bLeft = centerX - playerWidth * 0.75f
        val bRight = centerX + playerWidth * 0.75f
        val strokeW = 1.5.dp.toPx()
        
        // Top-Left bracket
        drawLine(DownMarkerOrange, Offset(bLeft, bTop), Offset(bLeft + bracketSize, bTop), strokeW)
        drawLine(DownMarkerOrange, Offset(bLeft, bTop), Offset(bLeft, bTop + bracketSize), strokeW)
        // Top-Right bracket
        drawLine(DownMarkerOrange, Offset(bRight, bTop), Offset(bRight - bracketSize, bTop), strokeW)
        drawLine(DownMarkerOrange, Offset(bRight, bTop), Offset(bRight, bTop + bracketSize), strokeW)
        // Bottom-Left bracket
        drawLine(DownMarkerOrange, Offset(bLeft, bBottom), Offset(bLeft + bracketSize, bBottom), strokeW)
        drawLine(DownMarkerOrange, Offset(bLeft, bBottom), Offset(bLeft, bBottom - bracketSize), strokeW)
        // Bottom-Right bracket
        drawLine(DownMarkerOrange, Offset(bRight, bBottom), Offset(bRight - bracketSize, bBottom), strokeW)
        drawLine(DownMarkerOrange, Offset(bRight, bBottom), Offset(bRight, bBottom - bracketSize), strokeW)
    }

    // -------------------------------------------------------------
    // ON-FIELD ZEBRA REFEREE CREWMATE RENDERING
    // -------------------------------------------------------------
    if (isRef) {
        // Legs / Black Trousers with White Stripe
        drawRoundRect(
            color = Color(0xFF111827),
            topLeft = Offset(centerX - playerWidth * 0.3f, feetPt.y - playerHeight * 0.52f),
            size = Size(playerWidth * 0.6f, playerHeight * 0.48f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
        // White Side Stripe on Trousers
        drawLine(
            color = Color.White,
            start = Offset(centerX, feetPt.y - playerHeight * 0.52f),
            end = Offset(centerX, feetPt.y - playerHeight * 0.08f),
            strokeWidth = 1.2.dp.toPx()
        )
        // Cleats
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(centerX - playerWidth * 0.35f, feetPt.y - playerHeight * 0.08f),
            size = Size(playerWidth * 0.7f, playerHeight * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
        // Striped Zebra Jersey Body
        val refTorsoTop = headPt.y + playerHeight * 0.22f
        val refTorsoHeight = playerHeight * 0.38f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(centerX - playerWidth * 0.42f, refTorsoTop),
            size = Size(playerWidth * 0.84f, refTorsoHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(playerWidth * 0.15f)
        )
        // Vertical Stripes
        val stripeCount = 4
        val stripeStep = (playerWidth * 0.84f) / (stripeCount * 2)
        for (s in 0 until stripeCount) {
            val sx = (centerX - playerWidth * 0.42f) + (s * 2 * stripeStep) + (stripeStep * 0.5f)
            drawRect(
                color = Color.Black,
                topLeft = Offset(sx, refTorsoTop),
                size = Size(stripeStep, refTorsoHeight)
            )
        }
        // Lanyard
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(centerX, refTorsoTop),
            end = Offset(centerX, refTorsoTop + refTorsoHeight * 0.5f),
            strokeWidth = 1.5.dp.toPx()
        )
        // Yellow Penalty Flag tucked in waistband
        drawCircle(
            color = FlagGold,
            radius = (playerHeight * 0.05f).coerceIn(2f, 6f),
            center = Offset(centerX + playerWidth * 0.38f, refTorsoTop + refTorsoHeight * 0.85f)
        )
        // Official Black Cap with White Piping
        val capRadius = playerHeight * 0.13f
        val capCenter = Offset(centerX, headPt.y + capRadius)
        drawCircle(
            color = Color(0xFF111827),
            radius = capRadius,
            center = capCenter
        )
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(capCenter.x - capRadius, capCenter.y - capRadius),
            size = Size(capRadius * 2, capRadius * 2),
            style = Stroke(width = 1.2.dp.toPx())
        )
        return
    }

    // -------------------------------------------------------------
    // FOOTBALL PLAYER (OFFENSE / DEFENSE) RENDERING
    // -------------------------------------------------------------
    // 3. Cleats & White Turf Sock Tape
    val cleatColor = if (isOff) Color(0xFF111827) else Color(0xFF1E293B)
    drawRoundRect(
        color = cleatColor,
        topLeft = Offset(centerX - playerWidth * 0.36f, feetPt.y - playerHeight * 0.07f),
        size = Size(playerWidth * 0.72f, playerHeight * 0.07f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFFF1F5F9), // White ankle tape
        topLeft = Offset(centerX - playerWidth * 0.32f, feetPt.y - playerHeight * 0.16f),
        size = Size(playerWidth * 0.64f, playerHeight * 0.09f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
    )

    // 4. Football Pants (Thigh pads & side stripe)
    val pantsColor = if (isOff) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val pantsTop = if (isLineman) headPt.y + playerHeight * 0.55f else headPt.y + playerHeight * 0.48f
    val pantsHeight = feetPt.y - pantsTop - (playerHeight * 0.15f)
    drawRoundRect(
        color = pantsColor,
        topLeft = Offset(centerX - playerWidth * 0.34f, pantsTop),
        size = Size(playerWidth * 0.68f, pantsHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
    )
    // Pants side stripe
    val stripeColor = if (isOff) Color(0xFFFFB300) else Color(0xFF38BDF8)
    drawLine(
        color = stripeColor,
        start = Offset(centerX, pantsTop),
        end = Offset(centerX, pantsTop + pantsHeight),
        strokeWidth = 1.5.dp.toPx()
    )

    // 5. Athletic Jersey & Shoulder Pads (3D Bulk & Contours)
    val jerseyTop = headPt.y + playerHeight * 0.18f
    val jerseyHeight = playerHeight * 0.38f
    val jerseyBrush = if (isOff) {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFFCBD5E1)),
            start = Offset(centerX - playerWidth * 0.5f, jerseyTop),
            end = Offset(centerX + playerWidth * 0.5f, jerseyTop + jerseyHeight)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF1E3A8A), Color(0xFF172554), Color(0xFF0F172A)),
            start = Offset(centerX - playerWidth * 0.5f, jerseyTop),
            end = Offset(centerX + playerWidth * 0.5f, jerseyTop + jerseyHeight)
        )
    }

    drawRoundRect(
        brush = jerseyBrush,
        topLeft = Offset(centerX - playerWidth * 0.5f, jerseyTop),
        size = Size(playerWidth, jerseyHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(playerWidth * 0.22f)
    )

    // Shoulder Pad Cap Accents & Sleeve Cuffs
    val trimColor = if (isOff) Color(0xFFFFB300) else Color(0xFF38BDF8)
    drawRoundRect(
        color = trimColor,
        topLeft = Offset(centerX - playerWidth * 0.52f, jerseyTop + 2.dp.toPx()),
        size = Size(playerWidth * 0.18f, playerHeight * 0.12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = trimColor,
        topLeft = Offset(centerX + playerWidth * 0.34f, jerseyTop + 2.dp.toPx()),
        size = Size(playerWidth * 0.18f, playerHeight * 0.12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
    )

    // 6. Lineman 3-Point Stance Hand on Ground
    if (isLineman) {
        drawCircle(
            color = Color(0xFFF1F5F9), // White taped lineman glove on turf
            radius = (playerHeight * 0.05f).coerceIn(2f, 7f),
            center = Offset(centerX + playerWidth * 0.3f, feetPt.y - playerHeight * 0.04f)
        )
    }

    // 7. 3D Molded Helmet with Facemask & Gloss Shine
    val helmetRadius = playerHeight * 0.14f
    val helmetCenter = Offset(centerX, headPt.y + helmetRadius)
    val helmetColor = if (isOff) Color(0xFFFFC107) else Color(0xFF0284C7)
    
    // Helmet Shell
    drawCircle(
        color = helmetColor,
        radius = helmetRadius,
        center = helmetCenter
    )
    // Helmet Center Stripe
    drawRect(
        color = if (isOff) Color(0xFF1E3A8A) else Color.White,
        topLeft = Offset(helmetCenter.x - helmetRadius * 0.16f, helmetCenter.y - helmetRadius),
        size = Size(helmetRadius * 0.32f, helmetRadius * 1.8f)
    )
    // Helmet Gloss Specular Curve
    drawArc(
        color = Color(0x77FFFFFF),
        startAngle = 200f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(helmetCenter.x - helmetRadius * 0.85f, helmetCenter.y - helmetRadius * 0.85f),
        size = Size(helmetRadius * 1.7f, helmetRadius * 1.7f),
        style = Stroke(width = 1.5.dp.toPx())
    )
    // Steel Facemask Bars
    val maskColor = Color(0xFF475569)
    val maskY1 = helmetCenter.y + helmetRadius * 0.15f
    val maskY2 = helmetCenter.y + helmetRadius * 0.45f
    drawLine(
        color = maskColor,
        start = Offset(helmetCenter.x - helmetRadius * 0.6f, maskY1),
        end = Offset(helmetCenter.x + helmetRadius * 0.6f, maskY1),
        strokeWidth = (playerHeight * 0.03f).coerceIn(1.2f, 3.5f)
    )
    drawLine(
        color = maskColor,
        start = Offset(helmetCenter.x - helmetRadius * 0.5f, maskY2),
        end = Offset(helmetCenter.x + helmetRadius * 0.5f, maskY2),
        strokeWidth = (playerHeight * 0.03f).coerceIn(1.2f, 3.5f)
    )

    // 8. Jersey Varsity Number
    if (playerHeight > 22.dp.toPx()) {
        val numFontSize = (playerHeight * 0.22f).coerceIn(8f, 15f)
        val numColor = if (isOff) Color(0xFF1E3A8A) else Color(0xFF38BDF8)
        val numLayout = textMeasurer.measure(
            "${player.number}",
            TextStyle(color = numColor, fontSize = numFontSize.sp, fontWeight = FontWeight.Black)
        )
        drawText(
            numLayout,
            topLeft = Offset(centerX - numLayout.size.width / 2f, jerseyTop + jerseyHeight * 0.16f)
        )
    }
}

private fun DrawScope.draw3DFootball(shadowPt: ProjectedPoint, ballPt: ProjectedPoint, spinAngle: Float) {
    val scale = ballPt.scale * 0.05f
    val w = (10.dp.toPx() * scale).coerceIn(6f, 24f)
    val h = (16.dp.toPx() * scale).coerceIn(10f, 36f)

    // 1. Ground Shadow (shrinks when high in air)
    val shadowW = w * 0.9f
    val shadowH = h * 0.45f
    drawOval(
        color = Color(0x55000000),
        topLeft = Offset(shadowPt.x - shadowW / 2f, shadowPt.y - shadowH / 2f),
        size = Size(shadowW, shadowH)
    )

    // 2. Leather Pigskin Body in 3D Air
    val pigskinBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF8D5B4C), Color(0xFF5D3222), Color(0xFF3E1F14)),
        center = Offset(ballPt.x - w * 0.2f, ballPt.y - h * 0.2f),
        radius = h * 0.6f
    )
    drawOval(
        brush = pigskinBrush,
        topLeft = Offset(ballPt.x - w / 2f, ballPt.y - h / 2f),
        size = Size(w, h)
    )

    // Dual White Stripe Rings
    drawLine(
        color = StripeWhite.copy(alpha = 0.9f),
        start = Offset(ballPt.x - w * 0.4f, ballPt.y - h * 0.25f),
        end = Offset(ballPt.x + w * 0.4f, ballPt.y - h * 0.25f),
        strokeWidth = 1.5.dp.toPx()
    )
    drawLine(
        color = StripeWhite.copy(alpha = 0.9f),
        start = Offset(ballPt.x - w * 0.4f, ballPt.y + h * 0.25f),
        end = Offset(ballPt.x + w * 0.4f, ballPt.y + h * 0.25f),
        strokeWidth = 1.5.dp.toPx()
    )

    // Laces
    drawLine(
        color = StripeWhite,
        start = Offset(ballPt.x, ballPt.y - h * 0.18f),
        end = Offset(ballPt.x, ballPt.y + h * 0.18f),
        strokeWidth = 1.5.dp.toPx()
    )
}

private fun DrawScope.draw3DPenaltyFlag(shadowPt: ProjectedPoint, flagPt: ProjectedPoint) {
    val scale = flagPt.scale * 0.05f
    val r = (7.dp.toPx() * scale).coerceIn(4f, 14f)

    // Trailing golden ribbon cloth
    val flagPath = Path().apply {
        moveTo(flagPt.x, flagPt.y)
        quadraticTo(flagPt.x + r * 2f, flagPt.y - r * 2.5f, flagPt.x + r * 3f, flagPt.y - r)
        quadraticTo(flagPt.x + r * 1.5f, flagPt.y - r * 0.3f, flagPt.x, flagPt.y)
        close()
    }
    drawPath(flagPath, FlagGold)
    drawPath(flagPath, Color(0xFFFFA000), style = Stroke(width = 1.dp.toPx()))

    // Weighted Black Ball Head
    drawCircle(Color(0xFF1E293B), radius = r, center = Offset(flagPt.x, flagPt.y))
    drawCircle(FlagGold, radius = r, center = Offset(flagPt.x, flagPt.y), style = Stroke(width = 1.5.dp.toPx()))
}

private fun DrawScope.drawForegroundOfficialZebra(w: Float, h: Float) {
    // 3rd Person Follow-Cam: Referee in zebra striped uniform rendered in lower foreground
    val refCenterX = w * 0.28f
    val refBaseY = h
    val bodyWidth = 130.dp.toPx()
    val bodyHeight = 170.dp.toPx()

    // 1. Zebra Striped Back & Torso
    val zebraPath = Path().apply {
        moveTo(refCenterX - bodyWidth * 0.45f, refBaseY)
        lineTo(refCenterX - bodyWidth * 0.35f, refBaseY - bodyHeight * 0.7f)
        lineTo(refCenterX - bodyWidth * 0.2f, refBaseY - bodyHeight * 0.85f)
        lineTo(refCenterX + bodyWidth * 0.2f, refBaseY - bodyHeight * 0.85f)
        lineTo(refCenterX + bodyWidth * 0.35f, refBaseY - bodyHeight * 0.7f)
        lineTo(refCenterX + bodyWidth * 0.45f, refBaseY)
        close()
    }

    drawPath(zebraPath, Color.White)

    // Vertical Zebra Stripes
    for (i in -4..4) {
        val sx = refCenterX + (i * 12.dp.toPx())
        drawLine(
            color = Color.Black,
            start = Offset(sx, refBaseY - bodyHeight * 0.85f),
            end = Offset(sx, refBaseY),
            strokeWidth = 6.dp.toPx()
        )
    }

    // Official Lanyard & Whistle
    val lanyardPath = Path().apply {
        moveTo(refCenterX - 18.dp.toPx(), refBaseY - bodyHeight * 0.8f)
        quadraticTo(refCenterX, refBaseY - bodyHeight * 0.45f, refCenterX + 18.dp.toPx(), refBaseY - bodyHeight * 0.8f)
    }
    drawPath(lanyardPath, Color(0xFF1E293B), style = Stroke(width = 3.dp.toPx()))

    // Official Black Cap with White Piping
    drawCircle(
        color = Color(0xFF111827),
        radius = 28.dp.toPx(),
        center = Offset(refCenterX, refBaseY - bodyHeight * 0.88f)
    )
    drawArc(
        color = Color.White,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(refCenterX - 28.dp.toPx(), refBaseY - bodyHeight * 0.88f - 28.dp.toPx()),
        size = Size(56.dp.toPx(), 56.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawFirstPersonRefOverlay(
    w: Float,
    h: Float,
    headYaw: Float,
    position: OfficialPosition
) {
    // Top Compass / Gaze Heading Reticle
    val compassCenter = Offset(w / 2f, 40.dp.toPx())
    val compassWidth = 140.dp.toPx()

    drawRoundRect(
        color = Color(0xAA000000),
        topLeft = Offset(compassCenter.x - compassWidth / 2f, compassCenter.y - 12.dp.toPx()),
        size = Size(compassWidth, 24.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
    )

    // Center Crosshair Tick
    drawLine(
        color = FlagGold,
        start = Offset(compassCenter.x, compassCenter.y - 8.dp.toPx()),
        end = Offset(compassCenter.x, compassCenter.y + 8.dp.toPx()),
        strokeWidth = 2.dp.toPx()
    )

    // Dynamic Horizon Indicator Tick
    val tickX = compassCenter.x + (headYaw * 0.8f)
    drawCircle(
        color = Color.White,
        radius = 3.dp.toPx(),
        center = Offset(tickX.coerceIn(compassCenter.x - compassWidth * 0.4f, compassCenter.x + compassWidth * 0.4f), compassCenter.y)
    )
}

private fun DrawScope.draw2DOverheadField(
    state: GameUiState,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    pulseAnim: Float
) {
    val visibleRange = state.visibleYardRange
    val yardSpan = (visibleRange.last - visibleRange.first).coerceAtLeast(1)
    val pixelsPerYardY = size.height / yardSpan
    val pixelsPerYardX = size.width / 53.3f

    // 5-Yard Strips
    for (yard in visibleRange.first..visibleRange.last) {
        val yPos = size.height - ((yard - visibleRange.first) * pixelsPerYardY)
        if (yard % 5 == 0) {
            val isDarkStrip = ((yard / 5) % 2 == 0)
            drawRect(
                color = if (isDarkStrip) TurfGreenDark else TurfGreen,
                topLeft = Offset(0f, yPos - (5 * pixelsPerYardY)),
                size = Size(size.width, 5 * pixelsPerYardY)
            )
            drawLine(StripeWhite.copy(alpha = 0.8f), Offset(0f, yPos), Offset(size.width, yPos), strokeWidth = 2.dp.toPx())
        }
    }

    // Players
    state.players.forEach { p ->
        val px = p.xYard * pixelsPerYardX
        val py = size.height - ((p.yYard - visibleRange.first) * pixelsPerYardY)
        drawCircle(if (p.isOffense) Color.White else Color(0xFF1E88E5), radius = 12.dp.toPx(), center = Offset(px, py))
    }
}
