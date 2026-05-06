package es.uc3m.android.a1percent.data.ai

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.Category
import org.junit.Assert.*
import org.junit.Test

class IntensityCalculationTest {

    private fun goal(
        deadline: Long? = null,
        createdAt: Long = System.currentTimeMillis(),
        currentIntensity: Float = 3.0f,
        difficulty: Int = 3
    ) = Goal(
        title = "Test",
        category = Category.FITNESS,
        difficulty = difficulty,
        xp = 100,
        deadline = deadline,
        createdAt = createdAt,
        currentIntensity = currentIntensity
    )

    @Test
    fun `infinite goal caps at difficulty x 1_5`() {
        val infiniteGoal = goal(difficulty = 4, currentIntensity = 5.9f)
        val result = AICoachService.calculateNewIntensity(
            goal = infiniteGoal,
            epicPassed = true,
            feedback = "SOBRADO"
        )
        assertTrue(result <= 4 * 1.5f)
    }

    @Test
    fun `finite goal caps at difficulty x 2_0`() {
        val fourWeeks = System.currentTimeMillis() + 4 * 7 * 24 * 3600 * 1000L
        val finiteGoal = goal(deadline = fourWeeks, difficulty = 3, currentIntensity = 5.8f)
        val result = AICoachService.calculateNewIntensity(
            goal = finiteGoal,
            epicPassed = true,
            feedback = "SOBRADO"
        )
        assertTrue(result <= 3 * 2.0f)
    }

    @Test
    fun `sprint final accelerates growth for finite goals with less than 4 weeks`() {
        val twoWeeks = System.currentTimeMillis() + 2 * 7 * 24 * 3600 * 1000L
        val finiteGoal = goal(deadline = twoWeeks, difficulty = 5, currentIntensity = 3.0f)
        val normalGrowth = AICoachService.calculateNewIntensity(
            goal = goal(deadline = System.currentTimeMillis() + 20 * 7 * 24 * 3600 * 1000L, difficulty = 5, currentIntensity = 3.0f),
            epicPassed = true,
            feedback = "PERFECTO"
        )
        val sprintGrowth = AICoachService.calculateNewIntensity(
            goal = finiteGoal,
            epicPassed = true,
            feedback = "PERFECTO"
        )
        assertTrue(sprintGrowth > normalGrowth)
    }

    @Test
    fun `agotado feedback reduces intensity`() {
        val infiniteGoal = goal(currentIntensity = 3.0f)
        val normal = AICoachService.calculateNewIntensity(
            goal = infiniteGoal, epicPassed = true, feedback = "PERFECTO"
        )
        val agotado = AICoachService.calculateNewIntensity(
            goal = infiniteGoal, epicPassed = true, feedback = "AGOTADO"
        )
        assertTrue(agotado < normal)
    }

    @Test
    fun `no change when epic not passed`() {
        val g = goal(currentIntensity = 3.0f)
        val result = AICoachService.calculateNewIntensity(
            goal = g, epicPassed = false, feedback = "SOBRADO"
        )
        assertEquals(3.0f, result, 0.001f)
    }
}
