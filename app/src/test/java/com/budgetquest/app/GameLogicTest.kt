package com.budgetquest.app

import com.budgetquest.app.domain.BudgetStatus
import com.budgetquest.app.domain.GameLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GameLogicTest {
    @Test
    fun xpToLevel_isCalculatedCorrectly() {
        assertEquals(1, GameLogic.levelFromXp(100))
        assertEquals(2, GameLogic.levelFromXp(250))
        assertEquals(3, GameLogic.levelFromXp(700))
        assertEquals(4, GameLogic.levelFromXp(1500))
        assertEquals(5, GameLogic.levelFromXp(2200))
    }

    @Test
    fun budgetPercentage_calculationWorks() {
        assertEquals(46, GameLogic.budgetPercent(2300.0, 5000.0))
        assertEquals(0, GameLogic.budgetPercent(200.0, 0.0))
    }

    @Test
    fun weekWarrior_requiresSevenConsecutiveDates() {
        val today = LocalDate.of(2026, 4, 26)
        val dates = (0..6).map { today.minusDays(it.toLong()).toString() }
        assertTrue(GameLogic.hasSevenDayStreak(dates, today))
    }

    @Test
    fun budgetStatus_reflectsMinAndMaxGoals() {
        assertEquals(BudgetStatus.ON_TRACK, GameLogic.budgetStatus(2500.0, 1000.0, 5000.0))
        assertEquals(BudgetStatus.UNDER_MIN, GameLogic.budgetStatus(500.0, 1000.0, 5000.0))
        assertEquals(BudgetStatus.OVER_MAX, GameLogic.budgetStatus(6000.0, 1000.0, 5000.0))
        assertEquals(BudgetStatus.NO_GOALS, GameLogic.budgetStatus(100.0, 0.0, 0.0))
    }

    @Test
    fun categoryOverLimit_detectsOverspend() {
        assertTrue(GameLogic.isCategoryOverLimit(900.0, 800.0))
        assertFalse(GameLogic.isCategoryOverLimit(700.0, 800.0))
    }
}
