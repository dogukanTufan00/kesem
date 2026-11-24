package io.github.dogukantufan00.kesem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.dogukantufan00.kesem.AppContainer
import io.github.dogukantufan00.kesem.ui.accounts.AccountsViewModel
import io.github.dogukantufan00.kesem.ui.analytics.AnalyticsViewModel
import io.github.dogukantufan00.kesem.ui.budgets.BudgetsViewModel
import io.github.dogukantufan00.kesem.ui.debts.DebtsViewModel
import io.github.dogukantufan00.kesem.ui.investments.InvestmentsViewModel
import io.github.dogukantufan00.kesem.ui.recurring.RecurringViewModel
import io.github.dogukantufan00.kesem.ui.settings.SettingsViewModel
import io.github.dogukantufan00.kesem.ui.summary.SummaryViewModel
import io.github.dogukantufan00.kesem.ui.transaction.TransactionViewModel

class KesemViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SummaryViewModel::class.java) -> {
                SummaryViewModel(
                    walletRepository = container.walletRepository,
                    settingsRepository = container.settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(AccountsViewModel::class.java) -> {
                AccountsViewModel(
                    walletRepository = container.walletRepository,
                    settingsRepository = container.settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(TransactionViewModel::class.java) -> {
                TransactionViewModel(
                    walletRepository = container.walletRepository,
                    settingsRepository = container.settingsRepository,
                    receiptStorage = container.receiptStorage
                ) as T
            }
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> {
                AnalyticsViewModel(
                    walletRepository = container.walletRepository,
                    settingsRepository = container.settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    walletRepository = container.walletRepository,
                    settingsRepository = container.settingsRepository,
                    backupManager = container.backupManager
                ) as T
            }
            modelClass.isAssignableFrom(BudgetsViewModel::class.java) -> {
                BudgetsViewModel(
                    walletRepository = container.walletRepository,
                    settingsRepository = container.settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(DebtsViewModel::class.java) -> {
                DebtsViewModel(
                    walletRepository = container.walletRepository,
                    settingsRepository = container.settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(RecurringViewModel::class.java) -> {
                RecurringViewModel(
                    walletRepository = container.walletRepository,
                    settingsRepository = container.settingsRepository
                ) as T
            }
            modelClass.isAssignableFrom(InvestmentsViewModel::class.java) -> {
                InvestmentsViewModel(
                    walletRepository = container.walletRepository,
                    settingsRepository = container.settingsRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı: ${modelClass.name}")
        }
    }
}
