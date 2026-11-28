package com.padelpro.app.core.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Academy : AppRoute

    @Serializable
    data class CourseDetail(val courseId: String) : AppRoute

    @Serializable
    data class Lesson(val lessonId: String) : AppRoute

    @Serializable
    data class Quiz(val lessonId: String) : AppRoute

    @Serializable
    data class Completion(val lessonId: String) : AppRoute

    @Serializable
    data object Rulebook : AppRoute

    @Serializable
    data object MatchCenter : AppRoute

    @Serializable
    data class MatchSetup(val format: String = "DOUBLES") : AppRoute

    @Serializable
    data object LiveMatch : AppRoute

    @Serializable
    data class MatchResults(val matchId: String) : AppRoute

    @Serializable
    data object MatchHistory : AppRoute

    @Serializable
    data class MatchDetails(val matchId: String) : AppRoute

    @Serializable
    data class MatchInsights(val matchId: String) : AppRoute

    @Serializable
    data object Performance : AppRoute

    @Serializable
    data object Profile : AppRoute
}
