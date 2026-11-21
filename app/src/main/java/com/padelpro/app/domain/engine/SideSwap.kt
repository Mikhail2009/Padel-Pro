package com.padelpro.app.domain.engine

import com.padelpro.app.domain.model.MatchStatus

/**
 * Смена сторон — производная от состояния матча, поэтому Undo откатывает и стороны.
 * Правило FIP: смена после каждого нечётного гейма сета, каждые 6 очков тай-брейка,
 * и на стыке сета при нечётной сумме геймов (что уже учтено формулой ceil(G/2)).
 */
object SideSwap {

    fun autoChanges(state: MatchEngineState): Int {
        var n = 0
        state.completedSets.forEach { s ->
            n += (s.gamesA + s.gamesB + 1) / 2
            val ta = s.tbA
            val tb = s.tbB
            if (ta != null && tb != null) n += (ta + tb) / 6
        }
        if (state.status == MatchStatus.ACTIVE) {
            n += (state.gamesA + state.gamesB + 1) / 2
            if (state.isTieBreak) n += (state.tbPointsA + state.tbPointsB) / 6
        }
        return n
    }

    fun isSwapped(state: MatchEngineState, autoEnabled: Boolean, manualTaps: Int): Boolean {
        val auto = if (autoEnabled) autoChanges(state) else 0
        return (auto + manualTaps) % 2 == 1
    }
}