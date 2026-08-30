package com.example.feature.film

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.data.local.entity.GameRecordEntity
import com.example.core.data.local.entity.SnapEvaluationEntity
import com.example.core.model.CallGrade
import com.example.ui.theme.DownMarkerOrange
import com.example.ui.theme.GradeIncorrectRed
import com.example.ui.theme.StadiumBorder
import com.example.ui.theme.StadiumSurface
import com.example.ui.theme.StadiumSurfaceVariant
import com.example.ui.theme.StripeGray
import com.example.ui.theme.StripeWhite
import com.example.ui.theme.TurfGreenLight

@Composable
fun FilmStudyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilmStudyViewModel = viewModel()
) {
    val recentGames by viewModel.recentGamesState.collectAsStateWithLifecycle()
    val selectedGameId by viewModel.selectedGameId.collectAsStateWithLifecycle()
    val snapEvaluations by viewModel.selectedGameEvaluations.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(color = StadiumSurface, border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedGameId != null) {
                            viewModel.selectGame(null)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StripeWhite)
                    }
                    Text(
                        text = if (selectedGameId != null) "Game Tape Review" else "Film Study",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = StripeWhite,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (selectedGameId == null) {
            GameSelectionList(
                games = recentGames,
                onGameSelected = { viewModel.selectGame(it.id) },
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
        } else {
            SnapEvaluationList(
                evaluations = snapEvaluations,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
        }
    }
}

@Composable
private fun GameSelectionList(
    games: List<GameRecordEntity>,
    onGameSelected: (GameRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (games.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No games available for film study.", color = StripeGray)
        }
        return
    }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(games) { game ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onGameSelected(game) },
                colors = CardDefaults.cardColors(containerColor = StadiumSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${game.awayTeamNickname} @ ${game.homeTeamNickname}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StripeWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Score: ${game.finalAwayScore} - ${game.finalHomeScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StripeGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Rating: ${String.format("%.1f", game.gamePerformanceRating)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (game.gamePerformanceRating >= 85.0) TurfGreenLight else if (game.gamePerformanceRating >= 70.0) DownMarkerOrange else GradeIncorrectRed
                        )
                        Text(
                            text = "Accuracy: ${String.format("%.1f", game.accuracyPercentage)}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = StripeWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapEvaluationList(
    evaluations: List<SnapEvaluationEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(evaluations) { eval ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Q${eval.quarter} | ${eval.gameClock} | ${eval.downAndDistance}",
                            style = MaterialTheme.typography.labelMedium,
                            color = StripeGray
                        )
                        Text(
                            text = eval.callGrade.name.replace("_", " "),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when (eval.callGrade) {
                                CallGrade.CORRECT_CALL, CallGrade.CORRECT_NON_CALL -> TurfGreenLight
                                CallGrade.INCORRECT_CALL, CallGrade.MISSED_CALL, CallGrade.UNNECESSARY_CALL -> GradeIncorrectRed
                                CallGrade.MARGINAL_CALL -> DownMarkerOrange
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Action: ${eval.userActionTaken.replace("_", " ")}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = StripeWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rule: ${eval.ruleCitationPlainLanguage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StripeGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Feedback: ${eval.supervisorRulingFeedback}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DownMarkerOrange
                    )
                }
            }
        }
    }
}
