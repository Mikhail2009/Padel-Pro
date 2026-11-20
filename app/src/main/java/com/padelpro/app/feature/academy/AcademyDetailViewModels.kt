package com.padelpro.app.feature.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelpro.app.domain.academy.QuizValidator
import com.padelpro.app.domain.model.CourseDetailData
import com.padelpro.app.domain.model.LessonCompletionOutcome
import com.padelpro.app.domain.model.LessonDetailData
import com.padelpro.app.domain.repository.AcademyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseDetailUiState(
    val isLoading: Boolean = true,
    val detail: CourseDetailData? = null
)

class CourseDetailViewModel(
    private val academyRepository: AcademyRepository
) : ViewModel() {

    private val courseIdFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CourseDetailUiState> = courseIdFlow
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flowOf(CourseDetailUiState(isLoading = false, detail = null))
            } else {
                academyRepository.observeCourseDetail(id).map { detail ->
                    CourseDetailUiState(isLoading = false, detail = detail)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CourseDetailUiState()
        )

    fun loadCourse(courseId: String) {
        if (courseIdFlow.value != courseId) {
            courseIdFlow.value = courseId
        }
    }
}

data class LessonUiState(
    val isLoading: Boolean = true,
    val lesson: LessonDetailData? = null,
    val currentPageIndex: Int = 0
) {
    val totalPages: Int
        get() = lesson?.blocks?.size ?: 0

    val isFirstPage: Boolean
        get() = currentPageIndex <= 0

    val isLastPage: Boolean
        get() = totalPages > 0 && currentPageIndex >= totalPages - 1
}

class LessonViewModel(
    private val academyRepository: AcademyRepository
) : ViewModel() {

    private val lessonIdFlow = MutableStateFlow<String?>(null)
    private val pageIndexFlow = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lessonFlow = lessonIdFlow.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(null) else academyRepository.observeLesson(id)
    }

    val uiState: StateFlow<LessonUiState> = combine(
        lessonIdFlow,
        lessonFlow,
        pageIndexFlow
    ) { requestedId, lesson, pageIdx ->
        if (requestedId.isNullOrBlank()) {
            LessonUiState(isLoading = false, lesson = null, currentPageIndex = 0)
        } else if (lesson == null) {
            LessonUiState(isLoading = false, lesson = null, currentPageIndex = 0)
        } else {
            val clampedIdx = pageIdx.coerceIn(0, (lesson.blocks.size - 1).coerceAtLeast(0))
            LessonUiState(
                isLoading = false,
                lesson = lesson,
                currentPageIndex = clampedIdx
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LessonUiState()
    )

    fun loadLesson(lessonId: String) {
        if (lessonIdFlow.value != lessonId) {
            pageIndexFlow.value = 0
            lessonIdFlow.value = lessonId
        }
    }

    fun selectPage(index: Int) {
        val maxIdx = ((uiState.value.lesson?.blocks?.size ?: 1) - 1).coerceAtLeast(0)
        pageIndexFlow.value = index.coerceIn(0, maxIdx)
    }

    fun previousPage() {
        selectPage(pageIndexFlow.value - 1)
    }

    fun nextPage() {
        selectPage(pageIndexFlow.value + 1)
    }
}

data class QuizUiState(
    val isLoading: Boolean = true,
    val lesson: LessonDetailData? = null,
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isChecked: Boolean = false,
    val selectedAnswers: Map<String, Int> = emptyMap(),
    val isSubmitting: Boolean = false
) {
    val totalQuestions: Int
        get() = lesson?.quiz?.size ?: 0

    val currentQuestion
        get() = lesson?.quiz?.getOrNull(currentQuestionIndex)

    val isLastQuestion: Boolean
        get() = totalQuestions > 0 && currentQuestionIndex >= totalQuestions - 1

    val isCurrentSelectionCorrect: Boolean
        get() {
            val q = currentQuestion ?: return false
            return isChecked && QuizValidator.isCorrect(q, selectedOptionIndex)
        }
}

class QuizViewModel(
    private val academyRepository: AcademyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _quizCompletedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val quizCompletedEvent: SharedFlow<String> = _quizCompletedEvent.asSharedFlow()

    private var loadedLessonId: String? = null

    fun loadLesson(lessonId: String) {
        if (loadedLessonId == lessonId && _uiState.value.lesson != null) return
        loadedLessonId = lessonId
        _uiState.value = QuizUiState(isLoading = true)
        viewModelScope.launch {
            academyRepository.observeLesson(lessonId).collect { lesson ->
                _uiState.update { current ->
                    if (current.lesson?.id == lesson?.id && current.lesson != null) {
                        current.copy(isLoading = false, lesson = lesson)
                    } else {
                        QuizUiState(
                            isLoading = false,
                            lesson = lesson
                        )
                    }
                }
            }
        }
    }

    fun selectOption(optionIndex: Int) {
        _uiState.update { state ->
            if (state.isChecked || state.isSubmitting) state
            else state.copy(selectedOptionIndex = optionIndex)
        }
    }

    fun checkAnswer() {
        _uiState.update { state ->
            val question = state.currentQuestion ?: return@update state
            val selected = state.selectedOptionIndex ?: return@update state
            if (state.isChecked || state.isSubmitting) return@update state
            state.copy(
                isChecked = true,
                selectedAnswers = state.selectedAnswers + (question.id to selected)
            )
        }
    }

    fun advanceOrFinish() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        if (!state.isChecked || state.isSubmitting) return

        if (!state.isLastQuestion) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = it.currentQuestionIndex + 1,
                    selectedOptionIndex = null,
                    isChecked = false
                )
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            runCatching {
                academyRepository.completeLesson(
                    lessonId = lesson.id,
                    selectedAnswers = state.selectedAnswers
                )
            }.onSuccess { outcome ->
                _uiState.update { it.copy(isSubmitting = false) }
                _quizCompletedEvent.tryEmit(outcome.lessonId)
            }.onFailure {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}

data class CompletionUiState(
    val isLoading: Boolean = true,
    val outcome: LessonCompletionOutcome? = null
)

class CompletionViewModel(
    private val academyRepository: AcademyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompletionUiState())
    val uiState: StateFlow<CompletionUiState> = _uiState.asStateFlow()

    private var loadedLessonId: String? = null

    fun loadCompletion(lessonId: String) {
        if (loadedLessonId == lessonId && !_uiState.value.isLoading) return
        loadedLessonId = lessonId
        _uiState.value = CompletionUiState(isLoading = true)
        viewModelScope.launch {
            val outcome = academyRepository.getCompletionOutcome(lessonId)
            _uiState.value = CompletionUiState(
                isLoading = false,
                outcome = outcome
            )
        }
    }
}
