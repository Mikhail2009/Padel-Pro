package com.padelpro.app.core.common

/**
 * Pure level progression math per §12.5:
 * Total XP threshold for level L: T(L) = 25 * (L - 1) * (L + 2)
 */
object LevelUtils {
    private val levelTitles = listOf(
        "ROOKIE",
        "CHALLENGER",
        "COMPETITOR",
        "PLAYMAKER",
        "STRATEGIST",
        "ACE",
        "PRO",
        "ELITE",
        "MASTER",
        "LEGEND"
    )

    fun totalXpForLevel(level: Int): Int {
        val clamped = level.coerceAtLeast(1)
        return 25 * (clamped - 1) * (clamped + 2)
    }

    fun calculateLevel(totalXp: Int): Int {
        val xp = totalXp.coerceAtLeast(0)
        var level = 1
        while (level < levelTitles.size && xp >= totalXpForLevel(level + 1)) {
            level++
        }
        return level
    }

    fun levelTitle(level: Int): String {
        return levelTitles[(level - 1).coerceIn(0, levelTitles.lastIndex)]
    }

    fun nextLevelXp(level: Int): Int {
        val next = (level + 1).coerceAtMost(levelTitles.size)
        return if (level >= levelTitles.size) {
            totalXpForLevel(levelTitles.size)
        } else {
            totalXpForLevel(next)
        }
    }

    fun levelProgressFraction(totalXp: Int): Float {
        val level = calculateLevel(totalXp)
        if (level >= levelTitles.size) return 1f
        val currentFloor = totalXpForLevel(level)
        val nextFloor = totalXpForLevel(level + 1)
        val span = (nextFloor - currentFloor).coerceAtLeast(1)
        return ((totalXp - currentFloor).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }
}
