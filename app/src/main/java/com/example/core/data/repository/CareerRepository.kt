package com.example.core.data.repository

import com.example.core.data.local.dao.CareerDao
import com.example.core.data.local.entity.CareerProfileEntity
import com.example.core.data.local.entity.GameRecordEntity
import com.example.core.data.local.entity.SnapEvaluationEntity
import com.example.core.model.OfficiatingTier
import com.example.core.model.OfficialPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CareerRepository(private val careerDao: CareerDao) {

    val careerProfileFlow: Flow<CareerProfileEntity> = careerDao.getProfileFlow()
        .map { it ?: CareerProfileEntity() }

    val allGameRecordsFlow: Flow<List<GameRecordEntity>> = careerDao.getAllGameRecords()

    val recentGameRecordsFlow: Flow<List<GameRecordEntity>> = careerDao.getRecentGames(5)

    suspend fun initializeIfEmpty(defaultOfficialName: String = "Official #42") = withContext(Dispatchers.IO) {
        val existing = careerDao.getProfileSync()
        if (existing == null) {
            val initial = CareerProfileEntity(
                officialName = defaultOfficialName,
                currentTier = OfficiatingTier.YOUTH,
                primaryPosition = OfficialPosition.DOWN_JUDGE,
                performanceRating = 75.0,
                careerPoints = 100,
                gamesOfficiated = 0,
                totalSnapsJudged = 0,
                correctCallsCount = 0,
                correctNonCallsCount = 0,
                incorrectCallsCount = 0,
                missedCallsCount = 0,
                avgSpottingErrorYards = 0.9,
                mechanicsProficiencyPct = 80.0
            )
            careerDao.upsertProfile(initial)
        }
    }

    suspend fun getProfileSync(): CareerProfileEntity = withContext(Dispatchers.IO) {
        careerDao.getProfileSync() ?: CareerProfileEntity()
    }

    suspend fun saveProfile(profile: CareerProfileEntity) = withContext(Dispatchers.IO) {
        careerDao.upsertProfile(profile)
    }

    suspend fun changePosition(position: OfficialPosition) = withContext(Dispatchers.IO) {
        careerDao.updatePrimaryPosition(newPositionName = position.name)
    }

    suspend fun changeTier(tier: OfficiatingTier) = withContext(Dispatchers.IO) {
        careerDao.updateTier(newTierName = tier.name)
    }

    suspend fun recordGameResult(
        gameRecord: GameRecordEntity,
        snapEvaluations: List<SnapEvaluationEntity>,
        newPerformanceRating: Double,
        pointsEarned: Int
    ): Long = withContext(Dispatchers.IO) {
        careerDao.recordCompletedGameWithEvaluations(
            game = gameRecord,
            evaluations = snapEvaluations,
            updatedRating = newPerformanceRating,
            pointsAwarded = pointsEarned
        )
    }

    fun getSnapEvaluations(gameId: Long): Flow<List<SnapEvaluationEntity>> {
        return careerDao.getSnapEvaluationsForGame(gameId)
    }
}
