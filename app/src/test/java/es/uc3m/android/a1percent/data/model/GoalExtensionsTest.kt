package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.Category
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class GoalExtensionsTest {

    private fun goal(
        deadline: Long = System.currentTimeMillis() + 52 * 7 * 24 * 3600 * 1000L,
        progress: Int = 35,
        extensionCount: Int = 0
    ) = Goal(
        title = "Marathon",
        category = Category.FITNESS,
        difficulty = 3,
        xp = 100,
        deadline = deadline,
        progress = progress,
        extensionCount = extensionCount
    )

    @Test
    fun `weeksRemaining returns weeks until deadline`() {
        val fourWeeksFromNow = System.currentTimeMillis() + 4 * 7 * 24 * 3600 * 1000L
        val g = goal(deadline = fourWeeksFromNow)
        val remaining = g.weeksRemaining()
        assertTrue(remaining in 3..5)
    }

    @Test
    fun `weeksRemaining returns 0 when deadline has passed`() {
        val pastDeadline = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
        assertEquals(0, goal(deadline = pastDeadline).weeksRemaining())
    }

    @Test
    fun `totalWeeks calculates from creation to deadline`() {
        val now = System.currentTimeMillis()
        val g = Goal(
            title = "Test",
            category = Category.FITNESS,
            difficulty = 3,
            xp = 100,
            deadline = now + 10 * 7 * 24 * 3600 * 1000L,
            createdAt = now
        )
        assertEquals(10, g.totalWeeks())
    }

    @Test
    fun `totalWeeks returns at least 1`() {
        val now = System.currentTimeMillis()
        val g = Goal(
            title = "Test",
            category = Category.FITNESS,
            difficulty = 3,
            xp = 100,
            deadline = now + 1000L,
            createdAt = now
        )
        assertEquals(1, g.totalWeeks())
    }

    @Test
    fun `weekLabel shows current week and total`() {
        assertEquals("Semana 8 de 52", goal().weekLabel(8))
    }

    @Test
    fun `isDeadlineWeek returns true when deadline is this week`() {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        val midWeek = cal.timeInMillis
        assertTrue(goal(deadline = midWeek).isDeadlineWeek())
    }

    @Test
    fun `isDeadlineWeek returns false when deadline is next week`() {
        val nextWeek = System.currentTimeMillis() + 10 * 24 * 3600 * 1000L
        assertFalse(goal(deadline = nextWeek).isDeadlineWeek())
    }
}
