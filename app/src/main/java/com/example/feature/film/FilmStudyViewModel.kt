package com.example.feature.film

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.local.CrewChiefDatabase
import com.example.core.data.local.entity.GameRecordEntity
import com.example.core.data.local.entity.SnapEvaluationEntity
import com.example.core.data.repository.CareerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilmStudyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CareerRepository

    init {
        val database = CrewChiefDatabase.getInstance(application)
        repository = CareerRepository(database.careerDao())
    }

    val recentGamesState: StateFlow<List<GameRecordEntity>> = repository.recentGameRecordsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedGameId = MutableStateFlow<Long?>(null)
    val selectedGameId = _selectedGameId.asStateFlow()

    val selectedGameEvaluations: StateFlow<List<SnapEvaluationEntity>> = _selectedGameId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getSnapEvaluations(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectGame(gameId: Long?) {
        _selectedGameId.value = gameId
    }
}
