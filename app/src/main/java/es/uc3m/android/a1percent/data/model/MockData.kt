package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import es.uc3m.android.a1percent.data.model.enums.MissionFeedback
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.data.model.enums.TaskType

/**
 * Hardcoded fake data for UI previews, prototyping and early feature development.
 * Next step: move this object to a dedicated mock/fixtures package if the amount of sample data grows.
 */
object MockData {

    val mockUser = UserProfile(
        id = "user-1",
        name = "Charlie Kirk",
        email = "charlie@kirk.com",
        password = "password123",
        level = 8,
        currentXp = 340,
        xpToNextLevel = 500,
        avatarUrl = null,
        streakDays = 12,
        totalTasksCompleted = 47
    )

    val mockUser2 = UserProfile(
        id = "user-2",
        name = "Jane Doe",
        email = "jane@doe.com",
        password = "securepass456",
        level = 12,
        currentXp = 150,
        xpToNextLevel = 1000,
        avatarUrl = null,
        streakDays = 30,
        totalTasksCompleted = 120
    )

    val mockUser3 = UserProfile(
        id = "user-3",
        name = "Admin User",
        email = "admin@1percent.com",
        password = "admin",
        level = 99,
        currentXp = 0,
        xpToNextLevel = 1,
        avatarUrl = null,
        streakDays = 365,
        totalTasksCompleted = 10000
    )

    val quickUser = UserProfile(
        id = "user-quick",
        name = "Quick Tester",
        email = "",
        password = "",
        level = 1,
        currentXp = 0,
        xpToNextLevel = 100,
        avatarUrl = null,
        streakDays = 0,
        totalTasksCompleted = 0
    )

    val allMockUsers = listOf(mockUser, mockUser2, mockUser3, quickUser)

    val mockGoal = Goal(
        id = "goal-1",
        title = "Learn Android Development",
        description = "Build a solid foundation in Kotlin, Compose and Android architecture.",
        category = Category.STUDY,
        difficulty = 4,
        xp = 250,
        deadline = 1772409600000,
        status = GoalStatus.ACTIVE,
        progress = 40,
        createdAt = 1741737600000
    )

    val mockTasks: List<Task> = listOf(
        Task(
            id = "task-1",
            title = "Review Kotlin flashcards",
            description = "Spend 20 minutes reviewing syntax and common Compose patterns.",
            type = TaskType.DAILY,
            difficulty = 2,
            xp = 15,
            energyCost = 2,
            deadline = 1741824000000,
            status = TaskStatus.COMPLETED,
            goalId = null,
            isAiGenerated = false,
            order = null,
            microfeedback = MissionFeedback.GOOD,
            createdAt = 1741737600000
        ),
        Task(
            id = "task-2",
            title = "Plan weekly workout routine",
            description = "Organize three gym sessions and one mobility session for the week.",
            type = TaskType.WEEKLY,
            difficulty = 3,
            xp = 25,
            energyCost = 4,
            deadline = 1742083200000,
            status = TaskStatus.PENDING,
            goalId = null,
            isAiGenerated = false,
            order = null,
            microfeedback = null,
            createdAt = 1741737600000
        ),
        Task(
            id = "task-3",
            title = "Complete Android Basics unit",
            description = "Finish the next training unit and take notes on key concepts.",
            type = TaskType.ONE_TIME,
            difficulty = 3,
            xp = 40,
            energyCost = 5,
            deadline = 1741910400000,
            status = TaskStatus.COMPLETED,
            goalId = "goal-1",
            isAiGenerated = true,
            order = 1,
            microfeedback = MissionFeedback.GOOD,
            createdAt = 1741737600000
        ),
        Task(
            id = "task-4",
            title = "Build a simple Compose screen",
            description = "Create a small screen with text fields, buttons and state handling.",
            type = TaskType.ONE_TIME,
            difficulty = 4,
            xp = 60,
            energyCost = 6,
            deadline = 1742169600000,
            status = TaskStatus.PENDING,
            goalId = "goal-1",
            isAiGenerated = true,
            order = 2,
            microfeedback = null,
            createdAt = 1741737600000
        ),
        Task(
            id = "task-5",
            title = "Connect navigation between app screens",
            description = "Add top-level routes and make the flow work end to end.",
            type = TaskType.ONE_TIME,
            difficulty = 5,
            xp = 80,
            energyCost = 7,
            deadline = 1742428800000,
            status = TaskStatus.PENDING,
            goalId = "goal-1",
            isAiGenerated = true,
            order = 3,
            microfeedback = null,
            createdAt = 1741737600000
        )
    )
}
