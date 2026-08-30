package com.example.feature.career

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.local.CrewChiefDatabase
import com.example.core.data.local.entity.CareerProfileEntity
import com.example.core.data.local.entity.GameRecordEntity
import com.example.core.data.local.entity.SnapEvaluationEntity
import com.example.core.data.remote.AuthManager
import com.example.core.data.remote.CloudSyncManager
import com.example.core.data.repository.CareerRepository
import com.example.core.model.CallGrade
import com.example.core.model.OfficiatingTier
import com.example.core.model.OfficialPosition
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CareerUiState(
    val profile: CareerProfileEntity = CareerProfileEntity(),
    val gameRecords: List<GameRecordEntity> = emptyList(),
    val isLoading: Boolean = false
)

class CareerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CareerRepository
    private val authManager = AuthManager(application)
    private val syncManager = CloudSyncManager(application)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authManager.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        val database = CrewChiefDatabase.getInstance(application)
        repository = CareerRepository(database.careerDao())
        viewModelScope.launch {
            repository.initializeIfEmpty("Ref #77 (You)")
        }
    }

    val profileState: StateFlow<CareerProfileEntity> = repository.careerProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CareerProfileEntity()
        )

    val gameRecordsState: StateFlow<List<GameRecordEntity>> = repository.allGameRecordsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updatePosition(position: OfficialPosition) {
        viewModelScope.launch {
            repository.changePosition(position)
        }
    }

    fun updateTier(tier: OfficiatingTier) {
        viewModelScope.launch {
            repository.changeTier(tier)
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle()
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                syncToCloud()
            }
        }
    }

    private fun syncToCloud() {
        viewModelScope.launch {
            val profile = repository.getProfileSync()
            syncManager.syncProfileToCloud(profile)
            val records = repository.allGameRecordsFlow.first()
            records.forEach { record ->
                syncManager.syncGameRecordToCloud(record)
            }
        }
    }

    /**
     * Simulates officiating a quick game to showcase Room database persistence,
     * performance rating calculation, accuracy tracking, and snap evaluation logging.
     */
    fun simulateGameSession() {
        viewModelScope.launch {
            val current = repository.getProfileSync()
            val tier = current.currentTier
            val position = current.primaryPosition

            val teams = listOf(
                "Ironwood Forge" to "Cascades Osprey",
                "Metro Rail" to "Coastal Gators",
                "Summit Raptors" to "Highland Stags",
                "Pinecrest Badgers" to "Valley Vipers"
            ).random()

            val homeScore = Random.nextInt(10, 38)
            val awayScore = Random.nextInt(10, 38)
            val snapsCount = Random.nextInt(6, 12)

            var correctCalls = 0
            var correctNonCalls = 0
            var incorrectCalls = 0
            var missedCalls = 0
            var totalSpotError = 0.0

            val snapEvals = mutableListOf<SnapEvaluationEntity>()

            val foulTypes = listOf(
                "OFFENSIVE_HOLDING" to "Offensive Holding: Grasping outside the frame of the jersey.",
                "DEFENSIVE_PASS_INTERFERENCE" to "Pass Interference: Early contact significantly hindering receiver's attempt to catch.",
                "FALSE_START" to "False Start: Interior lineman flinches prior to snap.",
                "OFFSIDE" to "Defensive Offside: Defender in neutral zone at the snap.",
                "NONE" to "Clean legal block and catch along the boundary."
            )

            for (i in 1..snapsCount) {
                val sampleFoul = foulTypes.random()
                val isFoul = sampleFoul.first != "NONE"
                val spotOffset = Random.nextDouble(0.0, 1.2)
                totalSpotError += spotOffset

                // 85% competent officiating probability
                val madeGoodCall = Random.nextDouble() < 0.85
                val grade: CallGrade
                val userAction: String

                if (isFoul) {
                    if (madeGoodCall) {
                        grade = CallGrade.CORRECT_CALL
                        userAction = "FLAG_THROWN"
                        correctCalls++
                    } else {
                        grade = CallGrade.MISSED_CALL
                        userAction = "NO_CALL"
                        missedCalls++
                    }
                } else {
                    if (madeGoodCall) {
                        grade = CallGrade.CORRECT_NON_CALL
                        userAction = "NO_CALL"
                        correctNonCalls++
                    } else {
                        grade = CallGrade.INCORRECT_CALL
                        userAction = "FLAG_THROWN"
                        incorrectCalls++
                    }
                }

                snapEvals.add(
                    SnapEvaluationEntity(
                        gameId = 0, // Will be set by transaction
                        playIndexInGame = i,
                        quarter = (i % 4) + 1,
                        gameClock = "${Random.nextInt(1, 14)}:${Random.nextInt(10, 59)}",
                        downAndDistance = "${listOf("1st", "2nd", "3rd", "4th").random()} & ${Random.nextInt(1, 10)}",
                        ballPositionYardLine = Random.nextInt(10, 90),
                        assignedPosition = position,
                        preSnapKeyPlayerNumber = Random.nextInt(50, 99),
                        preSnapKeySuccess = true,
                        trueFoulOccurred = sampleFoul.first,
                        userActionTaken = userAction,
                        callGrade = grade,
                        spottingOffsetDeltaYards = spotOffset,
                        spottingScore = (100.0 - (spotOffset * 25)).coerceIn(0.0, 100.0),
                        mechanicsNotes = "Good initial stance and sideline depth.",
                        supervisorRulingFeedback = if (grade == CallGrade.CORRECT_CALL || grade == CallGrade.CORRECT_NON_CALL) {
                            "Grade: Acceptable execution on ${sampleFoul.first}."
                        } else {
                            "Supervisor Flag: Critical review on down $i."
                        },
                        ruleCitationPlainLanguage = sampleFoul.second
                    )
                )
            }

            val totalDecisions = correctCalls + correctNonCalls + incorrectCalls + missedCalls
            val accuracy = (correctCalls + correctNonCalls).toDouble() / totalDecisions * 100.0
            val avgSpot = totalSpotError / snapsCount

            // Calculate game rating
            val baseGameScore = 80.0 + (if (accuracy > 90) 10.0 else -5.0) - (avgSpot * 8.0)
            val gameRating = baseGameScore.coerceIn(50.0, 99.0)

            val ratingDelta = (gameRating - current.performanceRating) * 0.15
            val updatedCareerRating = (current.performanceRating + ratingDelta).coerceIn(0.0, 100.0)
            val pointsEarned = (gameRating * 2.5).toInt()

            val gameRecord = GameRecordEntity(
                homeTeamCity = teams.first.split(" ").first(),
                homeTeamNickname = teams.first.split(" ").last(),
                awayTeamCity = teams.second.split(" ").first(),
                awayTeamNickname = teams.second.split(" ").last(),
                tier = tier,
                assignedPosition = position,
                isCrewChief = position == OfficialPosition.REFEREE,
                isPlayoff = current.gamesOfficiated > 0 && current.gamesOfficiated % 6 == 0,
                finalHomeScore = homeScore,
                finalAwayScore = awayScore,
                totalPlaysOfficiated = snapsCount,
                gamePerformanceRating = gameRating,
                accuracyPercentage = accuracy,
                correctCalls = correctCalls,
                missedCalls = missedCalls,
                incorrectCalls = incorrectCalls,
                correctNonCalls = correctNonCalls,
                avgSpottingErrorYards = avgSpot,
                supervisorReviewSummary = "Officiated as ${position.abbrev} in ${tier.displayName}. Accuracy: ${String.format("%.1f", accuracy)}%",
                ratingDelta = ratingDelta,
                pointsEarned = pointsEarned
            )

            repository.recordGameResult(
                gameRecord = gameRecord,
                snapEvaluations = snapEvals,
                newPerformanceRating = updatedCareerRating,
                pointsEarned = pointsEarned
            )
            
            // Sync new records to cloud if authenticated
            if (currentUser.value != null) {
                syncToCloud()
            }
        }
    }
}
