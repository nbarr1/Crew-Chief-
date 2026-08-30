package com.example.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.OfficiatingTier
import com.example.core.model.OfficialPosition

@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameDateTimestamp: Long = System.currentTimeMillis(),
    val homeTeamCity: String,
    val homeTeamNickname: String,
    val awayTeamCity: String,
    val awayTeamNickname: String,
    val tier: OfficiatingTier,
    val assignedPosition: OfficialPosition,
    val isCrewChief: Boolean = false,
    val isPlayoff: Boolean = false,
    val finalHomeScore: Int,
    val finalAwayScore: Int,
    val totalPlaysOfficiated: Int,
    val gamePerformanceRating: Double, // e.g., 88.5
    val accuracyPercentage: Double,    // e.g., 94.2%
    val correctCalls: Int,
    val missedCalls: Int,
    val incorrectCalls: Int,
    val correctNonCalls: Int,
    val avgSpottingErrorYards: Double,
    val supervisorReviewSummary: String,
    val ratingDelta: Double = 0.0,
    val pointsEarned: Int = 0
)
