package com.budgetquest.app.domain

/**
 * Aggregated monthly budget state for the progress dashboard.
 */
data class MonthlyBudgetSummary(
    val totalSpent: Double,
    val monthlyMin: Double,
    val monthlyMax: Double,
    val expenseCount: Int,
    val categoryCount: Int,
    val overLimitCategories: List<CategorySpendSummary>,
    val status: BudgetStatus
)

data class CategorySpendSummary(
    val categoryId: Long,
    val name: String,
    val emoji: String,
    val spent: Double,
    val limit: Double
)

enum class BudgetStatus {
    NO_GOALS,
    UNDER_MIN,
    ON_TRACK,
    OVER_MAX
}
