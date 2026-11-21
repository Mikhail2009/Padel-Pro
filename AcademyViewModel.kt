package com.padelpro.app.feature.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.core.common.LevelUtils
import com.padelpro.app.domain.model.AcademyCourse
import com.padelpro.app.domain.model.CourseCategory
import com.padelpro.app.domain.model.RulebookCategory
import com.padelpro.app.domain.model.RulebookItem
import com.padelpro.app.domain.model.UserProgress
import com.padelpro.app.domain.repository.AcademyRepository
import com.padelpro.app.domain.repository.PlayerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AcademyUiState(
    val level: Int = 1,
    val levelTitle: String = "ROOKIE",
    val totalXp: Int = 0,
    val levelProgress: Float = 0f,
    val streakDays: Int = 0,
    val completedLessons: Int = 0,
    val totalLessons: Int = 32,
    val selectedCategory: CourseCategory = CourseCategory.ALL,
    val courses: List<AcademyCourse> = emptyList(),
    val highlightedCourseId: String? = null,
    val rulebookSearch: String = "",
    val rulebookCategory: RulebookCategory = RulebookCategory.ALL,
    val rulebookItems: List<RulebookItem> = emptyList()
) {
    val academyPercent: Int
        get() = if (totalLessons == 0) 0 else ((completedLessons.toFloat() / totalLessons.toFloat()) * 100f).toInt()

    val filteredCourses: List<AcademyCourse>
        get() = if (selectedCategory == CourseCategory.ALL) {
            courses
        } else {
            courses.filter { it.category == selectedCategory }
        }
}

class AcademyViewModel(
    playerRepository: PlayerRepository,
    private val academyRepository: AcademyRepository
) : ViewModel() {

    private val selectedCategory = MutableStateFlow(CourseCategory.ALL)
    private val rulebookSearch = MutableStateFlow("")
    private val rulebookCategory = MutableStateFlow(RulebookCategory.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredRulebookFlow = combine(
        rulebookSearch,
        rulebookCategory
    ) { search, category -> search to category }
        .flatMapLatest { (search, category) ->
            academyRepository.observeRulebook(searchQuery = search, category = category)
        }

    val uiState: StateFlow<AcademyUiState> = combine(
        playerRepository.observeUserProgress(),
        academyRepository.observeCourses(),
        academyRepository.observeCompletedLessonsCount(),
        selectedCategory,
        rulebookSearch,
        rulebookCategory,
        filteredRulebookFlow
    ) { values ->
        val progress = values[0] as? UserProgress
        @Suppress("UNCHECKED_CAST")
        val courses = values[1] as List<AcademyCourse>
        val completed = values[2] as Int
        val cat = values[3] as CourseCategory
        val search = values[4] as String
        val ruleCat = values[5] as RulebookCategory
        @Suppress("UNCHECKED_CAST")
        val rules = values[6] as List<RulebookItem>

        val xp = progress?.totalXp ?: 0
        val level = LevelUtils.calculateLevel(xp)
        val highlightedCourse = courses.firstOrNull { it.isUnlocked && !it.isCompleted }?.id

        AcademyUiState(
            level = level,
            levelTitle = LevelUtils.levelTitle(level),
            totalXp = xp,
            levelProgress = LevelUtils.levelProgressFraction(xp),
            streakDays = progress?.streakCount ?: 0,
            completedLessons = completed,
            totalLessons = academyRepository.observeTotalLessonsCount(),
            selectedCategory = cat,
            courses = courses,
            highlightedCourseId = highlightedCourse,
            rulebookSearch = search,
            rulebookCategory = ruleCat,
            rulebookItems = rules
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AcademyUiState()
    )

    fun selectCategory(category: CourseCategory) {
        selectedCategory.value = category
    }

    fun updateRulebookSearch(query: String) {
        rulebookSearch.value = query
    }

    fun selectRulebookCategory(category: RulebookCategory) {
        rulebookCategory.value = category
    }

    fun toggleRuleFavorite(ruleId: String) {
        viewModelScope.launch {
            academyRepository.toggleRuleFavorite(ruleId)
        }
    }
}
