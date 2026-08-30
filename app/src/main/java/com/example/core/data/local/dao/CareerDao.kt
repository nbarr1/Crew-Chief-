package com.example.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.core.data.local.entity.CareerProfileEntity
import com.example.core.data.local.entity.GameRecordEntity
import com.example.core.data.local.entity.SnapEvaluationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CareerDao {

    // --- Profile Operations ---

    @Query("SELECT * FROM career_profile WHERE id = :profileId LIMIT 1")
    fun getProfileFlow(profileId: String = "primary_user_career"): Flow<CareerProfileEntity?>

    @Query("SELECT * FROM career_profile WHERE id = :profileId LIMIT 1")
    suspend fun getProfileSync(profileId: String = "primary_user_career"): CareerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: CareerProfileEntity)

    @Query("""
        UPDATE career_profile 
        SET performanceRating = :newRating, 
            gamesOfficiated = gamesOfficiated + 1,
            totalSnapsJudged = totalSnapsJudged + :snapsAdded,
            correctCallsCount = correctCallsCount + :correctCallsAdded,
            correctNonCallsCount = correctNonCallsCount + :correctNonCallsAdded,
            incorrectCallsCount = incorrectCallsCount + :incorrectCallsAdded,
            missedCallsCount = missedCallsCount + :missedCallsAdded,
            careerPoints = careerPoints + :pointsEarned,
            lastUpdatedTimestamp = :timestamp
        WHERE id = :profileId
    """)
    suspend fun updateStatsAfterGame(
        profileId: String = "primary_user_career",
        newRating: Double,
        snapsAdded: Int,
        correctCallsAdded: Int,
        correctNonCallsAdded: Int,
        incorrectCallsAdded: Int,
        missedCallsAdded: Int,
        pointsEarned: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE career_profile SET currentTier = :newTierName WHERE id = :profileId")
    suspend fun updateTier(profileId: String = "primary_user_career", newTierName: String)

    @Query("UPDATE career_profile SET primaryPosition = :newPositionName WHERE id = :profileId")
    suspend fun updatePrimaryPosition(profileId: String = "primary_user_career", newPositionName: String)

    // --- Game Record Operations ---

    @Query("SELECT * FROM game_records ORDER BY gameDateTimestamp DESC")
    fun getAllGameRecords(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records ORDER BY gameDateTimestamp DESC LIMIT :limit")
    fun getRecentGames(limit: Int = 10): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records WHERE id = :gameId LIMIT 1")
    fun getGameRecordById(gameId: Long): Flow<GameRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameRecord(game: GameRecordEntity): Long

    // --- Snap Evaluation Operations ---

    @Query("SELECT * FROM snap_evaluations WHERE gameId = :gameId ORDER BY playIndexInGame ASC")
    fun getSnapEvaluationsForGame(gameId: Long): Flow<List<SnapEvaluationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapEvaluation(snap: SnapEvaluationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapEvaluations(snaps: List<SnapEvaluationEntity>)

    // --- Compound Transactions ---

    @Transaction
    suspend fun recordCompletedGameWithEvaluations(
        game: GameRecordEntity,
        evaluations: List<SnapEvaluationEntity>,
        updatedRating: Double,
        pointsAwarded: Int
    ): Long {
        val gameId = insertGameRecord(game)
        val linkedEvals = evaluations.map { it.copy(gameId = gameId) }
        insertSnapEvaluations(linkedEvals)

        val profile = getProfileSync() ?: CareerProfileEntity()
        val newTotalSnaps = profile.totalSnapsJudged + game.totalPlaysOfficiated
        val newCorrectCalls = profile.correctCallsCount + game.correctCalls
        val newCorrectNonCalls = profile.correctNonCallsCount + game.correctNonCalls
        val newIncorrect = profile.incorrectCallsCount + game.incorrectCalls
        val newMissed = profile.missedCallsCount + game.missedCalls
        val newPoints = profile.careerPoints + pointsAwarded

        // Running average for spotting error
        val currentAvgSpot = profile.avgSpottingErrorYards
        val newAvgSpot = if (profile.gamesOfficiated == 0) {
            game.avgSpottingErrorYards
        } else {
            (currentAvgSpot * profile.gamesOfficiated + game.avgSpottingErrorYards) / (profile.gamesOfficiated + 1)
        }

        upsertProfile(
            profile.copy(
                performanceRating = updatedRating.coerceIn(0.0, 100.0),
                gamesOfficiated = profile.gamesOfficiated + 1,
                totalSnapsJudged = newTotalSnaps,
                correctCallsCount = newCorrectCalls,
                correctNonCallsCount = newCorrectNonCalls,
                incorrectCallsCount = newIncorrect,
                missedCallsCount = newMissed,
                careerPoints = newPoints,
                avgSpottingErrorYards = newAvgSpot,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        )

        return gameId
    }
}
