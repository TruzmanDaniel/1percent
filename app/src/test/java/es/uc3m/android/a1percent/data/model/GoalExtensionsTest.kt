package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.GoalType
import org.junit.Assert.*
import org.junit.Test

class GoalExtensionsTest {

    private fun finiteGoal(
        deadline: Long = System.currentTimeMillis() + 52 * 7 * 24 * 3600 * 1000L,
        weeklyStreak: Int = 0,
        currentIntensity: Float = 3.0f,
        progress: Int = 35,
        extensionCount: Int = 0
    ) = Goal(
        title = "Marathon",
        category = Category.FITNESS,
        difficulty = 3,
        xp = 100,
        deadline = deadline,
        progress = progress,
        currentIntensity = currentIntensity,
        weeklyStreak = weeklyStreak,
        extensionCount = extensionCount
    )

    private fun infiniteGoal(
        weeklyStreak: Int = 0,
        currentIntensity: Float = 3.5f
    ) = Goal(
        title = "Run better",
        category = Category.FITNESS,
        difficulty = 3,
        xp = 100,
        deadline = null,
        currentIntensity = currentIntensity,
        weeklyStreak = weeklyStreak
    )

    @Test
    fun `finite goal has FINITE type`() {
        assertEquals(GoalType.FINITE, finiteGoal().goalType)
        assertTrue(finiteGoal().isFinite)
        assertFalse(finiteGoal().isInfinite)
    }

    @Test
    fun `infinite goal has INFINITE type`() {
        assertEquals(GoalType.INFINITE, infiniteGoal().goalType)
        assertTrue(infiniteGoal().isInfinite)
        assertFalse(infiniteGoal().isFinite)
    }

    @Test
    fun `weekLabel shows total weeks for finite goals`() {
        assertEquals("Semana 8 de 52", finiteGoal().weekLabel(8))
    }

    @Test
    fun `weekLabel omits total for infinite goals`() {
        assertEquals("Semana 8", infiniteGoal().weekLabel(8))
    }

    @Test
    fun `progressDisplay returns value for finite, null for infinite`() {
        assertEquals(35, finiteGoal(progress = 35).progressDisplay())
        assertNull(infiniteGoal().progressDisplay())
    }

    @Test
    fun `intensityDisplay returns formatted string for infinite, null for finite`() {
        assertEquals("3.5", infiniteGoal(currentIntensity = 3.5f).intensityDisplay())
        assertNull(finiteGoal().intensityDisplay())
    }

    @Test
    fun `streakDisplay returns formatted string for infinite, null for finite`() {
        assertEquals("12 sem", infiniteGoal(weeklyStreak = 12).streakDisplay())
        assertNull(finiteGoal().streakDisplay())
    }

    @Test
    fun `weeksRemaining returns weeks for finite, null for infinite`() {
        val fourWeeksFromNow = System.currentTimeMillis() + 4 * 7 * 24 * 3600 * 1000L
        val goal = finiteGoal(deadline = fourWeeksFromNow)
        val remaining = goal.weeksRemaining()
        assertNotNull(remaining)
        assertTrue(remaining!! in 3..5)
        assertNull(infiniteGoal().weeksRemaining())
    }

    @Test
    fun `nextMilestone returns correct next milestone for infinite goals`() {
        assertEquals(4, infiniteGoal(weeklyStreak = 0).nextMilestone())
        assertEquals(4, infiniteGoal(weeklyStreak = 2).nextMilestone())
        assertEquals(12, infiniteGoal(weeklyStreak = 4).nextMilestone())
        assertEquals(12, infiniteGoal(weeklyStreak = 8).nextMilestone())
        assertEquals(26, infiniteGoal(weeklyStreak = 12).nextMilestone())
        assertEquals(52, infiniteGoal(weeklyStreak = 26).nextMilestone())
        assertEquals(56, infiniteGoal(weeklyStreak = 52).nextMilestone())
        assertNull(finiteGoal().nextMilestone())
    }

    @Test
    fun `justReachedMilestone detects correct milestone`() {
        assertNull(infiniteGoal(weeklyStreak = 3).justReachedMilestone())
        assertEquals(4, infiniteGoal(weeklyStreak = 4).justReachedMilestone())
        assertEquals(12, infiniteGoal(weeklyStreak = 12).justReachedMilestone())
        assertEquals(26, infiniteGoal(weeklyStreak = 26).justReachedMilestone())
        assertEquals(52, infiniteGoal(weeklyStreak = 52).justReachedMilestone())
    }

    @Test
    fun `justReachedMilestone prioritizes larger milestone`() {
        assertEquals(52, infiniteGoal(weeklyStreak = 52).justReachedMilestone())
        assertEquals(12, infiniteGoal(weeklyStreak = 12).justReachedMilestone())
    }

    @Test
    fun `justReachedMilestone works in second cycle`() {
        assertEquals(4, infiniteGoal(weeklyStreak = 56).justReachedMilestone())
        assertEquals(12, infiniteGoal(weeklyStreak = 60).justReachedMilestone())
    }

    @Test
    fun `justReachedMilestone returns null for finite goals`() {
        assertNull(finiteGoal(weeklyStreak = 4).justReachedMilestone())
    }

    @Test
    fun `justReachedMilestone returns null for zero streak`() {
        assertNull(infiniteGoal(weeklyStreak = 0).justReachedMilestone())
    }
}
