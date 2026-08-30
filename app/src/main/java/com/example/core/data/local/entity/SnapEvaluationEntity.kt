package com.example.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.CallGrade
import com.example.core.model.OfficialPosition

@Entity(
    tableName = "snap_evaluations",
    foreignKeys = [
        ForeignKey(
            entity = GameRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId")]
)
data class SnapEvaluationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val playIndexInGame: Int,
    val quarter: Int,
    val gameClock: String,
    val downAndDistance: String,
    val ballPositionYardLine: Int,
    val assignedPosition: OfficialPosition,
    val preSnapKeyPlayerNumber: Int?,
    val preSnapKeySuccess: Boolean,
    val trueFoulOccurred: String, // e.g. "OFFENSIVE_HOLDING" or "NONE"
    val userActionTaken: String,  // e.g. "FLAG_THROWN", "NO_CALL"
    val callGrade: CallGrade,
    val spottingOffsetDeltaYards: Double, // 0.0 = perfect spot
    val spottingScore: Double,           // 0.0 to 100.0
    val mechanicsNotes: String,
    val supervisorRulingFeedback: String,
    val ruleCitationPlainLanguage: String,
    val timestamp: Long = System.currentTimeMillis()
)
