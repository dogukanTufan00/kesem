package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.model.BudgetStatus
import io.github.dogukantufan00.kesem.model.CategorySpendSummary
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.util.BudgetCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCalculatorTest {

    @Test
    fun `evaluate assigns OK WARNING and EXCEEDED statuses properly`() {
        val budget = BudgetEntity(
            id = 1L,
            categoryId = 10L,
            monthlyLimitKurus = 100_000L, // 1.000 TL
            alertThresholdPercent = 80
        )

        val ok = BudgetCalculator.evaluate(budget, 79_000L, "Market", "shopping_cart", 0xFF16A34AL)
        assertEquals(BudgetStatus.OK, ok.status)
        assertEquals(79, ok.usagePercent)
        assertEquals(21_000L, ok.remainingKurus)

        val warning = BudgetCalculator.evaluate(budget, 80_000L, "Market", "shopping_cart", 0xFF16A34AL)
        assertEquals(BudgetStatus.WARNING, warning.status)
        assertEquals(80, warning.usagePercent)

        val exceeded = BudgetCalculator.evaluate(budget, 125_000L, "Market", "shopping_cart", 0xFF16A34AL)
        assertEquals(BudgetStatus.EXCEEDED, exceeded.status)
        assertEquals(125, exceeded.usagePercent)
        assertEquals(-25_000L, exceeded.remainingKurus)
    }

    @Test
    fun `evaluateAll includes zero spending categories and sorts overall first then usage descending`() {
        val categories = listOf(
            CategoryEntity(id = 1L, name = "Market", type = CategoryType.EXPENSE, iconKey = "shopping_cart", colorHex = 0xFF16A34A),
            CategoryEntity(id = 2L, name = "Ulaşım", type = CategoryType.EXPENSE, iconKey = "directions_bus", colorHex = 0xFF2563EB),
            CategoryEntity(id = 3L, name = "Eğlence", type = CategoryType.EXPENSE, iconKey = "movie", colorHex = 0xFF9333EA)
        )
        val budgets = listOf(
            BudgetEntity(id = 11L, categoryId = 1L, monthlyLimitKurus = 200_000L, alertThresholdPercent = 80, isActive = true),
            BudgetEntity(id = 12L, categoryId = 2L, monthlyLimitKurus = 100_000L, alertThresholdPercent = 80, isActive = true),
            BudgetEntity(id = 13L, categoryId = 3L, monthlyLimitKurus = 50_000L, alertThresholdPercent = 80, isActive = false),
            BudgetEntity(id = 14L, categoryId = null, monthlyLimitKurus = 500_000L, alertThresholdPercent = 80, isActive = true)
        )
        val breakdown = listOf(
            CategorySpendSummary(1L, "Market", "shopping_cart", 0xFF16A34AL, 100_000L, 4), // 50%
            CategorySpendSummary(2L, "Ulaşım", "directions_bus", 0xFF2563EBL, 90_000L, 6) // 90%
        )

        val activeOnly = BudgetCalculator.evaluateAll(
            budgets = budgets,
            categories = categories,
            expenseBreakdown = breakdown,
            totalExpenseKurus = 190_000L,
            includeInactive = false
        )

        assertEquals(3, activeOnly.size)
        // Overall budget is always first
        assertEquals(null, activeOnly[0].budget.categoryId)
        assertEquals(190_000L, activeOnly[0].spentKurus)
        assertEquals(38, activeOnly[0].usagePercent)
        // Highest usage category second (Ulaşım 90%)
        assertEquals(2L, activeOnly[1].budget.categoryId)
        assertEquals(BudgetStatus.WARNING, activeOnly[1].status)
        // Lower usage category third (Market 50%)
        assertEquals(1L, activeOnly[2].budget.categoryId)
        assertEquals(BudgetStatus.OK, activeOnly[2].status)

        val withInactive = BudgetCalculator.evaluateAll(
            budgets = budgets,
            categories = categories,
            expenseBreakdown = breakdown,
            totalExpenseKurus = 190_000L,
            includeInactive = true
        )
        assertEquals(4, withInactive.size)
        val inactiveItem = withInactive.first { it.budget.id == 13L }
        assertEquals(0L, inactiveItem.spentKurus)
        assertEquals(BudgetStatus.OK, inactiveItem.status)
    }
}
