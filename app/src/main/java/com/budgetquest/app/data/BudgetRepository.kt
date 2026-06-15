package com.budgetquest.app.data

import androidx.lifecycle.LiveData
import com.budgetquest.app.data.db.*
import com.budgetquest.app.domain.CategorySpendSummary
import com.budgetquest.app.domain.GameLogic
import com.budgetquest.app.domain.MonthlyBudgetSummary
import java.time.LocalDate
import timber.log.Timber

/**
 * Central repository coordinating Room reads/writes and gamification updates.
 */
class BudgetRepository(private val db: AppDatabase) {
    private val users = db.userDao()
    private val categories = db.categoryDao()
    private val expenses = db.expenseDao()
    private val goals = db.budgetGoalDao()
    private val limits = db.categoryLimitDao()
    private val badges = db.badgeDao()

    suspend fun registerUser(user: UserEntity): Long = users.insert(user)
    suspend fun findUserByUsername(username: String) = users.findByUsername(username)
    suspend fun findUserById(userId: Long) = users.findById(userId)
    suspend fun updateUser(user: UserEntity) = users.update(user)

    fun categoriesLive(userId: Long): LiveData<List<CategoryEntity>> = categories.getByUser(userId)
    suspend fun categoriesNow(userId: Long) = categories.getByUserNow(userId)
    suspend fun addCategory(category: CategoryEntity) = categories.insert(category)
    suspend fun updateCategory(category: CategoryEntity) = categories.update(category)
    suspend fun deleteCategory(category: CategoryEntity) = categories.delete(category)

    fun expensesByRange(userId: Long, start: String, end: String): LiveData<List<ExpenseEntity>> =
        expenses.getByDateRange(userId, start, end)

    suspend fun expenseById(expenseId: Long): ExpenseEntity? = expenses.getById(expenseId)

    suspend fun saveExpenseWithXp(expense: ExpenseEntity, isUpdate: Boolean = false) {
        Timber.d("saveExpenseWithXp user=%s update=%s", expense.userId, isUpdate)
        if (isUpdate) expenses.update(expense) else expenses.insert(expense)

        val user = users.findById(expense.userId) ?: return
        val newXp = user.xp + if (isUpdate) 0 else 10
        users.update(user.copy(xp = newXp, level = GameLogic.levelFromXp(newXp)))

        evaluateBadges(expense.userId)
    }

    suspend fun saveMonthlyGoals(goal: BudgetGoalEntity, categoryLimits: List<CategoryLimitEntity>) {
        goals.insert(goal)
        categoryLimits.forEach { limits.insert(it) }
    }

    suspend fun goalByMonth(userId: Long, month: String) = goals.getByMonth(userId, month)
    suspend fun limitsByMonth(userId: Long, month: String) = limits.getByMonth(userId, month)
    suspend fun monthExpenses(userId: Long, month: String) = expenses.getByMonth(userId, month)

    suspend fun evaluateBadges(userId: Long) {
        val total = expenses.countByUser(userId)
        if (total == 1) badges.insert(BadgeEntity(userId = userId, badgeKey = GameLogic.BADGE_FIRST_ENTRY))

        val dates = expenses.distinctDates(userId)
        if (GameLogic.hasSevenDayStreak(dates, LocalDate.now())) {
            badges.insert(BadgeEntity(userId = userId, badgeKey = GameLogic.BADGE_WEEK_WARRIOR))
        }

        val month = LocalDate.now().toString().substring(0, 7)
        val monthExpenses = expenses.getByMonth(userId, month)
        val monthLimits = limits.getByMonth(userId, month).associateBy { it.categoryId }
        val overspent = monthExpenses.groupBy { it.categoryId }.any { (categoryId, exps) ->
            val limit = monthLimits[categoryId]?.limitAmount ?: Double.MAX_VALUE
            exps.sumOf { it.amountZar } > limit
        }
        if (!overspent && monthExpenses.isNotEmpty()) {
            badges.insert(BadgeEntity(userId = userId, badgeKey = GameLogic.BADGE_BUDGET_HERO))
        }
    }

    suspend fun badges(userId: Long) = badges.getByUser(userId).map { it.badgeKey }.toSet()

    suspend fun buildMonthlySummary(userId: Long, month: String): MonthlyBudgetSummary {
        val expenses = expenses.getByMonth(userId, month)
        val goal = goals.getByMonth(userId, month)
        val limits = limits.getByMonth(userId, month).associateBy { it.categoryId }
        val categories = categories.getByUserNow(userId).associateBy { it.id }
        val total = expenses.sumOf { it.amountZar }
        val monthlyMin = goal?.monthlyMin ?: 0.0
        val monthlyMax = goal?.monthlyMax ?: 0.0
        val overLimit = expenses.groupBy { it.categoryId }.mapNotNull { (categoryId, list) ->
            val limit = limits[categoryId]?.limitAmount ?: return@mapNotNull null
            val spent = list.sumOf { it.amountZar }
            if (spent <= limit) return@mapNotNull null
            val cat = categories[categoryId] ?: return@mapNotNull null
            CategorySpendSummary(categoryId, cat.name, cat.emoji, spent, limit)
        }
        return MonthlyBudgetSummary(
            totalSpent = total,
            monthlyMin = monthlyMin,
            monthlyMax = monthlyMax,
            expenseCount = expenses.size,
            categoryCount = categories.size,
            overLimitCategories = overLimit,
            status = GameLogic.budgetStatus(total, monthlyMin, monthlyMax)
        )
    }

    suspend fun categoryTotalsForRange(
        userId: Long,
        startDate: String,
        endDate: String,
        month: String
    ): List<CategorySpendSummary> {
        val categories = categories.getByUserNow(userId)
        val limits = limits.getByMonth(userId, month).associateBy { it.categoryId }
        return categories.map { cat ->
            val spent = expenses.getByCategoryAndDateRange(userId, cat.id, startDate, endDate)
                .sumOf { it.amountZar }
            CategorySpendSummary(
                categoryId = cat.id,
                name = cat.name,
                emoji = cat.emoji,
                spent = spent,
                limit = limits[cat.id]?.limitAmount ?: 0.0
            )
        }.filter { it.spent > 0.0 || it.limit > 0.0 }
            .sortedByDescending { it.spent }
    }
}
