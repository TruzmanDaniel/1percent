package es.uc3m.android.a1percent.data.ai

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.Category
import org.junit.Assert.*
import org.junit.Test

class IntensityCalculationTest {

    private fun goal(
        deadline: Long = System.currentTimeMillis() + 20 * 7 * 24 * 3600 * 1000L,
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
    fun `caps at difficulty x 2_0`() {
        val fourWeeks = System.currentTimeMillis() + 4 * 7 * 24 * 3600 * 1000L
        val g = goal(deadline = fourWeeks, difficulty = 3, currentIntensity = 5.8f)
        val result = AICoachService.calculateNewIntensity(
            goal = g,
            epicPassed = true,
            feedback = "SOBRADO"
        )
        assertTrue(result <= 3 * 2.0f)
    }

    @Test
    fun `sprint final accelerates growth for goals with less than 4 weeks`() {
        val twoWeeks = System.currentTimeMillis() + 2 * 7 * 24 * 3600 * 1000L
        val sprintGoal = goal(deadline = twoWeeks, difficulty = 5, currentIntensity = 3.0f)
        val normalGoal = goal(difficulty = 5, currentIntensity = 3.0f)
        val normalGrowth = AICoachService.calculateNewIntensity(
            goal = normalGoal,
            epicPassed = true,
            feedback = "PERFECTO"
        )
        val sprintGrowth = AICoachService.calculateNewIntensity(
            goal = sprintGoal,
            epicPassed = true,
            feedback = "PERFECTO"
        )
        assertTrue(sprintGrowth > normalGrowth)
    }

    @Test
    fun `agotado feedback reduces intensity`() {
        val g = goal(currentIntensity = 3.0f)
        val normal = AICoachService.calculateNewIntensity(
            goal = g, epicPassed = true, feedback = "PERFECTO"
        )
        val agotado = AICoachService.calculateNewIntensity(
            goal = g, epicPassed = true, feedback = "AGOTADO"
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
