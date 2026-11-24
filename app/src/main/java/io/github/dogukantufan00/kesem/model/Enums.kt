package io.github.dogukantufan00.kesem.model

enum class AccountType {
    BANK,
    CASH,
    CREDIT_CARD,
    INVESTMENT
}

enum class CategoryType {
    EXPENSE,
    INCOME
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT
}

enum class DebtType {
    LENT,
    BORROWED
}

enum class RecurrenceFrequency {
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class InvestmentAssetType {
    CURRENCY,
    GOLD,
    BIST_STOCK,
    US_STOCK,
    TEFAS_FUND,
    CRYPTO,
    TERM_DEPOSIT
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class BudgetStatus {
    OK,
    WARNING,
    EXCEEDED
}
