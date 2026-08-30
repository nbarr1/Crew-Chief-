package com.example.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.OfficiatingTier
import com.example.core.model.OfficialPosition

@Entity(tableName = "career_profile")
data class CareerProfileEntity(
    @PrimaryKey
    val id: String = "primary_user_career",
    val officialName: String = "Stripes Veteran",
    val currentTier: OfficiatingTier = OfficiatingTier.YOUTH,
    val primaryPosition: OfficialPosition = OfficialPosition.DOWN_JUDGE,
    val performanceRating: Double = 75.0, // 0.0 to 100.0 scale
    val careerPoints: Int = 0,
    val gamesOfficiated: Int = 0,
    val totalSnapsJudged: Int = 0,
    val correctCallsCount: Int = 0,
    val correctNonCallsCount: Int = 0,
    val incorrectCallsCount: Int = 0,
    val missedCallsCount: Int = 0,
    val avgSpottingErrorYards: Double = 0.85, // in yards (lower is better)
    val mechanicsProficiencyPct: Double = 80.0, // 0 to 100%
    val appealsFiled: Int = 0,
    val appealsWon: Int = 0,
    val seasonsCompleted: Int = 0,
    val playoffAssignmentsCount: Int = 0,
    val championshipAssignmentsCount: Int = 0,
    val crewChiefAssignmentsCount: Int = 0,
    val currentWeekNumber: Int = 1,
    val reputationRepScore: Int = 100, // Crew trust rating
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    val totalDecisionsEvaluated: Int
        get() = correctCallsCount + correctNonCallsCount + incorrectCallsCount + missedCallsCount

    val accuracyPercentage: Double
        get() = if (totalDecisionsEvaluated > 0) {
            ((correctCallsCount + correctNonCallsCount).toDouble() / totalDecisionsEvaluated) * 100.0
        } else {
            100.0
        }
}
