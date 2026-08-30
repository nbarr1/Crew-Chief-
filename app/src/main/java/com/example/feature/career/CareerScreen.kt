package com.example.feature.career

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.core.data.local.entity.CareerProfileEntity
import com.example.core.data.local.entity.GameRecordEntity
import com.example.core.model.OfficiatingTier
import com.example.core.model.OfficialPosition
import com.example.ui.theme.DownMarkerOrange
import com.example.ui.theme.FlagGold
import com.example.ui.theme.GradeCorrectGreen
import com.example.ui.theme.GradeIncorrectRed
import com.example.ui.theme.GradeWarningYellow
import com.example.ui.theme.ReviewBoothBlue
import com.example.ui.theme.StadiumBorder
import com.example.ui.theme.StadiumNightBg
import com.example.ui.theme.StadiumSurface
import com.example.ui.theme.StadiumSurfaceHighlight
import com.example.ui.theme.StadiumSurfaceVariant
import com.example.ui.theme.StripeGray
import com.example.ui.theme.StripeWhite
import com.example.ui.theme.TurfGreen
import com.example.ui.theme.WhistleChrome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerScreen(
    modifier: Modifier = Modifier,
    viewModel: CareerViewModel = viewModel(),
    onNavigateToGame: () -> Unit = {},
    onNavigateToFilmStudy: () -> Unit = {}
) {
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val gameRecords by viewModel.gameRecordsState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("career_screen"),
        containerColor = StadiumNightBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FlagGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sports,
                                contentDescription = "Officiating Whistle",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CREW CHIEF",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                ),
                                color = StripeWhite
                            )
                            Text(
                                text = "Career & Performance Ledger",
                                style = MaterialTheme.typography.labelSmall,
                                color = FlagGold
                            )
                        }
                    }
                },
                actions = {
                    if (currentUser == null) {
                        androidx.compose.material3.TextButton(onClick = { viewModel.signInWithGoogle() }) {
                            Text("SIGN IN", color = FlagGold, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Synced",
                            tint = ReviewBoothBlue,
                            modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StadiumSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = FlagGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile.careerPoints} PTS",
                                style = MaterialTheme.typography.labelMedium,
                                color = StripeWhite
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StadiumSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Career Rating Hero Card
            item {
                CareerHeroCard(
                    profile = profile,
                    onSimulateGame = onNavigateToGame,
                    onFilmStudy = onNavigateToFilmStudy
                )
            }

            // Position & Assignment Selector
            item {
                PositionSelectorSection(
                    currentPosition = profile.primaryPosition,
                    onPositionSelected = { viewModel.updatePosition(it) }
                )
            }

            // Tier Progress Tracker
            item {
                TierProgressSection(
                    currentTier = profile.currentTier,
                    onTierSelected = { viewModel.updateTier(it) }
                )
            }

            // Granular Performance Metrics Grid
            item {
                Text(
                    text = "OFFICIATING ACCURACY & STATS",
                    style = MaterialTheme.typography.labelLarge,
                    color = StripeGray,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                OfficiatingStatsGrid(profile = profile)
            }

            // Game History Ledger
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OFFICIATED GAMES LOG (${gameRecords.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = StripeGray
                    )
                    Text(
                        text = "ROOM PERSISTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = TurfGreen
                    )
                }
            }

            if (gameRecords.isEmpty()) {
                item {
                    EmptyGamesPlaceholder(onSimulate = { viewModel.simulateGameSession() })
                }
            } else {
                items(gameRecords, key = { it.id }) { game ->
                    GameRecordItemCard(game = game)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CareerHeroCard(
    profile: CareerProfileEntity,
    onSimulateGame: () -> Unit,
    onFilmStudy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("career_hero_card"),
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder)
    ) {
        Column {
            // Atmospheric Stadium Hero Banner with Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_stadium_1788041077376),
                    contentDescription = "Stadium Lights",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient Scrim Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    StadiumSurface.copy(alpha = 0.7f),
                                    StadiumSurface
                                )
                            )
                        )
                )
                // Badge overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FlagGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = FlagGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OFFICIAL PROFILE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StripeWhite
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = profile.officialName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = StripeWhite
                        )
                        Text(
                            text = "Assignment Tier: ${profile.currentTier.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FlagGold
                        )
                    }

                    // Performance Score Circle
                    Surface(
                        shape = CircleShape,
                        color = when {
                            profile.performanceRating >= 90.0 -> GradeCorrectGreen.copy(alpha = 0.15f)
                            profile.performanceRating >= 75.0 -> FlagGold.copy(alpha = 0.15f)
                            else -> GradeIncorrectRed.copy(alpha = 0.15f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            when {
                                profile.performanceRating >= 90.0 -> GradeCorrectGreen
                                profile.performanceRating >= 75.0 -> FlagGold
                                else -> GradeIncorrectRed
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", profile.performanceRating),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = when {
                                    profile.performanceRating >= 90.0 -> GradeCorrectGreen
                                    profile.performanceRating >= 75.0 -> FlagGold
                                    else -> GradeIncorrectRed
                                }
                            )
                            Text(
                                text = "RATING",
                                style = MaterialTheme.typography.labelSmall,
                                color = StripeGray
                            )
                        }
                    }
                }

            Spacer(modifier = Modifier.height(16.dp))

            // Rating Progress Bar to next tier
            val progress = (profile.performanceRating / 100.0).toFloat().coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Career Evaluation Level",
                    style = MaterialTheme.typography.labelSmall,
                    color = StripeGray
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = StripeWhite
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = FlagGold,
                trackColor = StadiumSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Simulate Game Button
                Button(
                    onClick = onSimulateGame,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_game_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlagGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NEXT ASSIGNMENT",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                // Film Study Button
                Button(
                    onClick = onFilmStudy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("film_study_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReviewBoothBlue,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow, // Replace with video later
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FILM STUDY",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
}

@Composable
private fun PositionSelectorSection(
    currentPosition: OfficialPosition,
    onPositionSelected: (OfficialPosition) -> Unit
) {
    Column {
        Text(
            text = "ASSIGNED POSITION",
            style = MaterialTheme.typography.labelLarge,
            color = StripeGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(OfficialPosition.values()) { position ->
                val isSelected = position == currentPosition
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) FlagGold else StadiumSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) FlagGold else StadiumBorder
                    ),
                    modifier = Modifier
                        .clickable { onPositionSelected(position) }
                        .testTag("position_chip_${position.name}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = position.abbrev,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = if (isSelected) Color.Black else StripeWhite
                        )
                        Text(
                            text = position.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.Black.copy(alpha = 0.8f) else StripeGray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierProgressSection(
    currentTier: OfficiatingTier,
    onTierSelected: (OfficiatingTier) -> Unit
) {
    Column {
        Text(
            text = "LEAGUE TIER PROGRESSION",
            style = MaterialTheme.typography.labelLarge,
            color = StripeGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(OfficiatingTier.values()) { tier ->
                        val isCurrent = tier == currentTier
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) TurfGreen else StadiumSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) GradeCorrectGreen else StadiumBorder
                            ),
                            modifier = Modifier.clickable { onTierSelected(tier) }
                        ) {
                            Text(
                                text = tier.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCurrent) StripeWhite else StripeGray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentTier.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StripeWhite.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Crew: ${currentTier.crewSize} Officials",
                        style = MaterialTheme.typography.labelSmall,
                        color = FlagGold
                    )
                    Text(
                        text = "Spotting Tolerance: ±${currentTier.spottingToleranceYards} yd",
                        style = MaterialTheme.typography.labelSmall,
                        color = ReviewBoothBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficiatingStatsGrid(profile: CareerProfileEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "GAMES WORKED",
                value = "${profile.gamesOfficiated}",
                subText = "${profile.totalSnapsJudged} Total Snaps",
                icon = Icons.Default.Gavel,
                iconColor = FlagGold,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "CALL ACCURACY",
                value = String.format(Locale.US, "%.1f%%", profile.accuracyPercentage),
                subText = "${profile.correctCallsCount + profile.correctNonCallsCount} Correct / ${profile.totalDecisionsEvaluated}",
                icon = Icons.Default.CheckCircle,
                iconColor = GradeCorrectGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "AVG SPOTTING ERROR",
                value = String.format(Locale.US, "%.2f yd", profile.avgSpottingErrorYards),
                subText = "Tolerance: ±${profile.currentTier.spottingToleranceYards} yd",
                icon = Icons.Default.Assessment,
                iconColor = ReviewBoothBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "BLOWN / MISSED",
                value = "${profile.incorrectCallsCount + profile.missedCallsCount}",
                subText = "${profile.missedCallsCount} Missed, ${profile.incorrectCallsCount} Ghosts",
                icon = Icons.Default.Warning,
                iconColor = if (profile.incorrectCallsCount + profile.missedCallsCount > 0) GradeIncorrectRed else GradeCorrectGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subText: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = StripeGray
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = StripeWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = StripeGray,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GameRecordItemCard(game: GameRecordEntity) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("game_record_${game.id}"),
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${game.awayTeamCity} ${game.awayTeamNickname} @ ${game.homeTeamCity} ${game.homeTeamNickname}",
                        style = MaterialTheme.typography.titleMedium,
                        color = StripeWhite
                    )
                    Text(
                        text = "Score: ${game.awayTeamNickname} ${game.finalAwayScore} - ${game.finalHomeScore} ${game.homeTeamNickname} • ${game.tier.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StripeGray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        game.gamePerformanceRating >= 85.0 -> GradeCorrectGreen.copy(alpha = 0.2f)
                        game.gamePerformanceRating >= 70.0 -> FlagGold.copy(alpha = 0.2f)
                        else -> GradeIncorrectRed.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", game.gamePerformanceRating),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            game.gamePerformanceRating >= 85.0 -> GradeCorrectGreen
                            game.gamePerformanceRating >= 70.0 -> FlagGold
                            else -> GradeIncorrectRed
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pos: ${game.assignedPosition.abbrev} (${game.assignedPosition.name})",
                    style = MaterialTheme.typography.labelSmall,
                    color = FlagGold
                )
                Text(
                    text = "Accuracy: ${String.format(Locale.US, "%.1f%%", game.accuracyPercentage)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StripeWhite
                )
                Text(
                    text = "+${game.pointsEarned} Pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurfGreen
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StadiumBorder)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SUPERVISOR REVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = FlagGold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = game.supervisorReviewSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StripeWhite.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Correct Calls: ${game.correctCalls}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GradeCorrectGreen
                        )
                        Text(
                            text = "Missed: ${game.missedCalls}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GradeIncorrectRed
                        )
                        Text(
                            text = "Avg Spot: ${String.format(Locale.US, "%.2f yd", game.avgSpottingErrorYards)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ReviewBoothBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGamesPlaceholder(onSimulate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Sports,
                contentDescription = null,
                tint = FlagGold,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No Games Officiated Yet",
                style = MaterialTheme.typography.titleMedium,
                color = StripeWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Simulate or officiate your first snap session to record grades and evaluate your performance rating in Room.",
                style = MaterialTheme.typography.bodyMedium,
                color = StripeGray,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
