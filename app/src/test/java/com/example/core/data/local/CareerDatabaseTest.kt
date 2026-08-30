package com.example.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.local.dao.CareerDao
import com.example.core.data.local.entity.CareerProfileEntity
import com.example.core.data.local.entity.GameRecordEntity
import com.example.core.data.local.entity.SnapEvaluationEntity
import com.example.core.model.CallGrade
import com.example.core.model.OfficiatingTier
import com.example.core.model.OfficialPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CareerDatabaseTest {

    private lateinit var database: CrewChiefDatabase
    private lateinit var careerDao: CareerDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CrewChiefDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        careerDao = database.careerDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveCareerProfile() = runTest {
        val initialProfile = CareerProfileEntity(
            officialName = "Down Judge Miller",
            currentTier = OfficiatingTier.PREP,
            primaryPosition = OfficialPosition.DOWN_JUDGE,
            performanceRating = 82.5,
            gamesOfficiated = 12,
            totalSnapsJudged = 94,
            correctCallsCount = 18,
            correctNonCallsCount = 72,
            incorrectCallsCount = 2,
            missedCallsCount = 2
        )

        careerDao.upsertProfile(initialProfile)

        val retrieved = careerDao.getProfileFlow().first()
        assertNotNull(retrieved)
        assertEquals("Down Judge Miller", retrieved?.officialName)
        assertEquals(OfficiatingTier.PREP, retrieved?.currentTier)
        assertEquals(82.5, retrieved?.performanceRating ?: 0.0, 0.01)
        assertEquals(12, retrieved?.gamesOfficiated)
        assertEquals(95.74, retrieved?.accuracyPercentage ?: 0.0, 0.1)
    }

    @Test
    fun recordGameWithEvaluationsTransaction() = runTest {
        val initialProfile = CareerProfileEntity(
            performanceRating = 75.0,
            gamesOfficiated = 0,
            careerPoints = 0
        )
        careerDao.upsertProfile(initialProfile)

        val game = GameRecordEntity(
            homeTeamCity = "Ironwood",
            homeTeamNickname = "Forge",
            awayTeamCity = "Cascades",
            awayTeamNickname = "Osprey",
            tier = OfficiatingTier.PREP,
            assignedPosition = OfficialPosition.DOWN_JUDGE,
            finalHomeScore = 24,
            finalAwayScore = 17,
            totalPlaysOfficiated = 8,
            gamePerformanceRating = 91.0,
            accuracyPercentage = 100.0,
            correctCalls = 2,
            missedCalls = 0,
            incorrectCalls = 0,
            correctNonCalls = 6,
            avgSpottingErrorYards = 0.45,
            supervisorReviewSummary = "Exemplary line of scrimmage control.",
            pointsEarned = 250
        )

        val snap = SnapEvaluationEntity(
            gameId = 0,
            playIndexInGame = 1,
            quarter = 1,
            gameClock = "12:44",
            downAndDistance = "1st & 10",
            ballPositionYardLine = 25,
            assignedPosition = OfficialPosition.DOWN_JUDGE,
            preSnapKeyPlayerNumber = 72,
            preSnapKeySuccess = true,
            trueFoulOccurred = "OFFENSIVE_HOLDING",
            userActionTaken = "FLAG_THROWN",
            callGrade = CallGrade.CORRECT_CALL,
            spottingOffsetDeltaYards = 0.3,
            spottingScore = 95.0,
            mechanicsNotes = "Good depth",
            supervisorRulingFeedback = "Confirmed holding",
            ruleCitationPlainLanguage = "Jersey tug outside the framework."
        )

        val gameId = careerDao.recordCompletedGameWithEvaluations(
            game = game,
            evaluations = listOf(snap),
            updatedRating = 78.5,
            pointsAwarded = 250
        )

        assertTrue(gameId > 0)

        val updatedProfile = careerDao.getProfileSync()
        assertNotNull(updatedProfile)
        assertEquals(1, updatedProfile?.gamesOfficiated)
        assertEquals(78.5, updatedProfile?.performanceRating ?: 0.0, 0.01)
        assertEquals(250, updatedProfile?.careerPoints)
        assertEquals(8, updatedProfile?.totalSnapsJudged)

        val games = careerDao.getAllGameRecords().first()
        assertEquals(1, games.size)
        assertEquals("Forge", games.first().homeTeamNickname)

        val snapEvals = careerDao.getSnapEvaluationsForGame(gameId).first()
        assertEquals(1, snapEvals.size)
        assertEquals(CallGrade.CORRECT_CALL, snapEvals.first().callGrade)
    }
}
