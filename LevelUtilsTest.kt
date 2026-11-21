package com.padelpro.app

import com.padelpro.app.core.common.LevelUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelUtilsTest {

    @Test
    fun `level thresholds match T(L) formula`() {
        assertEquals(0, LevelUtils.totalXpForLevel(1))
        assertEquals(100, LevelUtils.totalXpForLevel(2))
        assertEquals(250, LevelUtils.totalXpForLevel(3))
        assertEquals(450, LevelUtils.totalXpForLevel(4))
        assertEquals(700, LevelUtils.totalXpForLevel(5))
        assertEquals(1000, LevelUtils.totalXpForLevel(6))
        assertEquals(1350, LevelUtils.totalXpForLevel(7))
        assertEquals(1750, LevelUtils.totalXpForLevel(8))
        assertEquals(2200, LevelUtils.totalXpForLevel(9))
        assertEquals(2700, LevelUtils.totalXpForLevel(10))
    }

    @Test
    fun `level calculation handles boundary values`() {
        assertEquals(1, LevelUtils.calculateLevel(0))
        assertEquals(1, LevelUtils.calculateLevel(99))
        assertEquals(2, LevelUtils.calculateLevel(100))
        assertEquals(2, LevelUtils.calculateLevel(249))
        assertEquals(3, LevelUtils.calculateLevel(250))
        assertEquals(10, LevelUtils.calculateLevel(5000))
    }

    @Test
    fun `level titles match specification`() {
        assertEquals("ROOKIE", LevelUtils.levelTitle(1))
        assertEquals("PLAYMAKER", LevelUtils.levelTitle(4))
        assertEquals("LEGEND", LevelUtils.levelTitle(10))
    }
}
