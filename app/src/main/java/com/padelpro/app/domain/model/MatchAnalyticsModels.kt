package com.padelpro.app.domain.model

/**
 * Единый результат расширенной аналитики матча.
 * Используется экранами Match Details / Match Results, будущими Match Insights
 * и Match Poster Renderer. Все значения вычислены из сохранённых событий
 * и доменных данных; выдуманные показатели отсутствуют.
 */
data class MatchAnalytics(
    val overview: MatchOverview,
    val pointStats: PointStats,
    val gameStats: GameStats,
    /** Точки графика Momentum: по одной на каждый реально сыгранный розыгрыш. */
    val momentum: List<MomentumPoint>,
    /** Только достоверно вычисленные достижения. Пустой список, если данных мало. */
    val highlights: List<MatchHighlight>,
    /** Совместимость с существующей карточкой статистики (null, если очков не было). */
    val eventStats: MatchEventStats?
)

/** Общая информация о матче (всё, что нужно шапке постера без повторных запросов). */
data class MatchOverview(
    val matchId: String,
    val format: MatchFormat,
    val deuceMode: DeuceMode,
    val bestOf: Int,
    val teamANames: List<String>,
    val teamBNames: List<String>,
    val startedAt: Long,
    val endedAt: Long?,
    val durationMs: Long?,
    val isCompleted: Boolean,
    /** Ручное завершение обозначено явно; победитель в этом случае не определяется. */
    val isManuallyCompleted: Boolean,
    val winnerTeam: TeamSide?,
    val setsWonA: Int,
    val setsWonB: Int,
    /** Только завершённые сеты (как в set_results). */
    val sets: List<SetScore>,
    val incompleteSet: IncompleteSetScore?,
    val totalPoints: Int,
    /** Очки, реально сыгранные в незаконченном сете (null, если сета нет). */
    val incompleteSetPointsPlayed: Int?,
    /** Игровое время в незаконченном сете (null, если сета или длительности нет). */
    val incompleteSetDurationMs: Long?
)

/** Статистика очков. Проценты null при нулевом total (защита от деления на ноль). */
data class PointStats(
    val totalPoints: Int,
    val teamAPoints: Int,
    val teamBPoints: Int,
    val teamAPointPercent: Float?,
    val teamBPointPercent: Float?,
    /** Разница очков со знаком: A − B. */
    val pointDifference: Int,
    val maxPointStreakA: Int,
    val maxPointStreakB: Int,
    /** Самая长 непрерывная серия розыгрышей, в течение которых команда удерживала лидерство по очкам. */
    val longestLeadSpanA: Int,
    val longestLeadSpanB: Int,
    /** Смены лидера по очкам (переходы A↔B через ничью включаются, ничья лидером не считается). */
    val pointLeadChanges: Int
)

/** Статистика геймов и сетов. Очки внутри гейма и выигранные геймы не смешиваются. */
data class GameStats(
    /** Все реально завершённые геймы, включая геймы незаконченного сета. */
    val gamesWonA: Int,
    val gamesWonB: Int,
    /** Только завершённые сеты. */
    val setsWonA: Int,
    val setsWonB: Int,
    val maxGameStreakA: Int,
    val maxGameStreakB: Int,
    val gamesLeadChanges: Int,
    /** Наибольшее преимущество по кумулятивным геймам за матч. */
    val biggestGamesLead: GamesLead?,
    /** Детализация каждого завершённого сета: очки и игровое время. */
    val setPerformances: List<SetPerformance>,
    val longestSet: SetPerformance?,
    val shortestSet: SetPerformance?,
    val mostDominantSet: SetScore?,
    val closestSet: SetScore?
)

data class GamesLead(
    val team: TeamSide,
    val lead: Int,
    /** Порядковый номер завершённого гейма (1-based), после которого зафиксировано преимущество. */
    val gameIndex: Int
)

data class SetPerformance(
    val setIndex: Int,
    val gamesA: Int,
    val gamesB: Int,
    val tbA: Int?,
    val tbB: Int?,
    val winnerTeam: TeamSide,
    val durationMs: Long,
    val pointsPlayed: Int
)

/** Одна точка Momentum-графика. */
data class MomentumPoint(
    /** Номер розыгрыша в матче (1-based), после Undo нумерация пересчитывается из событий. */
    val rallyIndex: Int,
    val winner: TeamSide,
    /** Разница выигранных очков после этого розыгрыша: A − B. */
    val pointDifference: Int,
    /** Кумулятивные завершённые геймы за матч после этого розыгрыша. */
    val gamesA: Int,
    val gamesB: Int,
    /** Сет, в котором был сыгран розыгрыш. */
    val setIndex: Int,
    val timestampMillis: Long
)

/**
 * Достоверные достижения матча. Каждый тип ссылается на реальные вычисленные данные;
 * заголовок/описание локализуется на UI-слое по типу достижения.
 */
sealed interface MatchHighlight {
    data class LongestPointStreak(
        val team: TeamSide,
        val length: Int,
        val startRallyIndex: Int,
        val endRallyIndex: Int
    ) : MatchHighlight

    data class BiggestPointLead(
        val team: TeamSide,
        val lead: Int,
        val rallyIndex: Int
    ) : MatchHighlight

    data class MostDominantSet(
        val setIndex: Int,
        val winnerTeam: TeamSide,
        val gamesWinner: Int,
        val gamesLoser: Int
    ) : MatchHighlight

    data class ClosestSet(
        val setIndex: Int,
        val gamesA: Int,
        val gamesB: Int,
        val hadTieBreak: Boolean
    ) : MatchHighlight

    data class LongestSet(
        val setIndex: Int,
        val durationMs: Long
    ) : MatchHighlight

    /** Самая长 серия выигранных геймов подряд (сквозная через границы сетов). */
    data class BestGameStreak(
        val team: TeamSide,
        val length: Int,
        val startGameIndex: Int,
        val endGameIndex: Int
    ) : MatchHighlight
}