package com.aitutor.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val trends: List<TrendPoint> = emptyList(),
    val knowledgeNodes: List<KnowledgeNode> = emptyList(),
    val trendDays: Int = 7,
    val selectedSubject: String = "all",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            analyticsRepository.getDashboardStats().collect { stats ->
                _uiState.update {
                    it.copy(
                        stats = stats,
                        isLoading = false,
                        isEmpty = stats.todaySolveCount == 0 && stats.todayDuration == 0 &&
                                it.trends.isEmpty()
                    )
                }
            }
        }
        
        viewModelScope.launch {
            analyticsRepository.refreshFromCloud()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            analyticsRepository.refreshFromCloud()
            loadDashboard()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setTrendDays(days: Int) {
        _uiState.update { it.copy(trendDays = days) }
        viewModelScope.launch {
            analyticsRepository.getTrend(days).collect { trends ->
                _uiState.update { it.copy(trends = trends) }
            }
        }
    }

    fun selectSubject(subject: String) {
        _uiState.update { it.copy(selectedSubject = subject) }
        viewModelScope.launch {
            analyticsRepository.getKnowledgeGraph(subject).collect { nodes ->
                _uiState.update { it.copy(knowledgeNodes = nodes) }
            }
        }
    }
}
