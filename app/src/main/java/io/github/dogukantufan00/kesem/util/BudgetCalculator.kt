package io.github.dogukantufan00.kesem.util

import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.model.BudgetProgress
import io.github.dogukantufan00.kesem.model.BudgetStatus
import io.github.dogukantufan00.kesem.model.CategorySpendSummary
import kotlin.math.roundToInt

object BudgetCalculator {

    const val OVERALL_BUDGET_TITLE = "Genel Aylık Bütçe"
    const val OVERALL_BUDGET_ICON = "wallet"
    const val OVERALL_BUDGET_COLOR = 0xFFB34E2E

    fun evaluate(
        budget: BudgetEntity,
        spentKurus: Long,
        title: String,
        iconKey: String,
        colorHex: Long
    ): BudgetProgress {
        val limitKurus = budget.monthlyLimitKurus.coerceAtLeast(0L)
        val safeSpent = spentKurus.coerceAtLeast(0L)
        val remainingKurus = limitKurus - safeSpent
        val usageRatio = if (limitKurus <= 0L) {
            0f
        } else {
            (safeSpent.toDouble() / limitKurus.toDouble()).toFloat().coerceAtLeast(0f)
        }
        val usagePercent = if (limitKurus <= 0L) {
            0
        } else {
            ((safeSpent.toDouble() / limitKurus.toDouble()) * 100.0).roundToInt().coerceAtLeast(0)
        }
        val status = when {
            limitKurus <= 0L -> BudgetStatus.OK
            safeSpent > limitKurus -> BudgetStatus.EXCEEDED
            usagePercent >= budget.alertThresholdPercent -> BudgetStatus.WARNING
            else -> BudgetStatus.OK
        }
        return BudgetProgress(
            budget = budget,
            title = title,
            iconKey = iconKey,
            colorHex = colorHex,
            spentKurus = safeSpent,
            limitKurus = limitKurus,
            remainingKurus = remainingKurus,
            usageRatio = usageRatio,
            usagePercent = usagePercent,
            status = status
        )
    }

    fun evaluateAll(
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        expenseBreakdown: List<CategorySpendSummary>,
        totalExpenseKurus: Long,
        includeInactive: Boolean = false
    ): List<BudgetProgress> {
        val categoryMap = categories.associateBy { it.id }
        val spentByCategory = expenseBreakdown
            .filter { it.categoryId != null }
            .associate { it.categoryId!! to it.totalKurus }

        val targetBudgets = if (includeInactive) budgets else budgets.filter { it.isActive }
        return targetBudgets.mapNotNull { budget ->
            val catId = budget.categoryId
            if (catId == null) {
                evaluate(
                    budget = budget,
                    spentKurus = totalExpenseKurus,
                    title = OVERALL_BUDGET_TITLE,
                    iconKey = OVERALL_BUDGET_ICON,
                    colorHex = OVERALL_BUDGET_COLOR
                )
            } else {
                val category = categoryMap[catId] ?: return@mapNotNull null
                evaluate(
                    budget = budget,
                    spentKurus = spentByCategory[catId] ?: 0L,
                    title = category.name,
                    iconKey = category.iconKey,
                    colorHex = category.colorHex
                )
            }
        }.sortedWith(
            compareBy<BudgetProgress>(
                { it.budget.categoryId != null },
                { -it.usageRatio },
                { it.title }
            )
        )
    }
}
