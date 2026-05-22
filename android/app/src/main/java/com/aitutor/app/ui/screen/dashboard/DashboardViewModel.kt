package com.aitutor.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.domain.model.*
import com.aitutor.app.domain.repository.AnalyticsRepository
import com.aitutor.app.domain.repository.GamificationRepository
import com.aitutor.app.domain.repository.WrongAnswerRepository
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
    val isEmpty: Boolean = false,
    // F46 游戏化数据
    val achievements: List<AchievementWithStatus> = emptyList(),
    val streak: StreakResult = StreakResult(),
    val rankings: List<RankEntry> = emptyList(),
    val userScore: UserScore? = null,
    // P1-5 错题本
    val wrongAnswerCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val gamificationRepository: GamificationRepository,
    private val wrongAnswerRepository: WrongAnswerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        observeGamificationData()
        observeWrongAnswerCount()
    }

    private fun observeGamificationData() {
        // 观察成就数据
        viewModelScope.launch {
            gamificationRepository.observeAchievements().collect { achievements ->
                _uiState.update { it.copy(achievements = achievements) }
            }
        }
        // 观察连胜数据
        viewModelScope.launch {
            gamificationRepository.observeStreak().collect { streak ->
                _uiState.update { it.copy(streak = streak) }
            }
        }
        // 观察排行榜数据
        viewModelScope.launch {
            gamificationRepository.observeLeaderboard().collect { rankings ->
                _uiState.update { it.copy(rankings = rankings) }
            }
        }
        // 观察用户积分数据
        viewModelScope.launch {
            gamificationRepository.observeUserScore().collect { userScore ->
                _uiState.update { it.copy(userScore = userScore) }
            }
        }
    }

    private fun observeWrongAnswerCount() {
        viewModelScope.launch {
            wrongAnswerRepository.getWrongAnswerCount().collect { count ->
                _uiState.update { it.copy(wrongAnswerCount = count) }
            }
        }
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

        viewModelScope.launch {
            gamificationRepository.initializeAchievements()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            analyticsRepository.refreshFromCloud()
            gamificationRepository.refreshLeaderboard()
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
