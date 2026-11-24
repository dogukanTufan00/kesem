package io.github.dogukantufan00.kesem.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtPaymentEntity
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.model.DebtWithSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebtsUiState(
    val debts: List<DebtWithSummary> = emptyList(),
    val activeAccounts: List<AccountWithBalance> = emptyList(),
    val totalReceivablesKurus: Long = 0L,
    val totalBorrowedKurus: Long = 0L,
    val hideBalances: Boolean = false
)

class DebtsViewModel(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<DebtsUiState> = combine(
        walletRepository.observeDebts(),
        walletRepository.observeActiveAccounts(),
        settingsRepository.hideBalances
    ) { debts, accounts, hideBalances ->
        val openDebts = debts.filter { !it.debt.isSettled }
        val receivables = openDebts
            .filter { it.debt.type == DebtType.LENT }
            .sumOf { it.remainingAmountKurus }
        val borrowed = openDebts
            .filter { it.debt.type == DebtType.BORROWED }
            .sumOf { it.remainingAmountKurus }

        DebtsUiState(
            debts = debts,
            activeAccounts = accounts.filter {
                it.account.type == AccountType.BANK || it.account.type == AccountType.CASH
            },
            totalReceivablesKurus = receivables,
            totalBorrowedKurus = borrowed,
            hideBalances = hideBalances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtsUiState()
    )

    fun observePayments(debtId: Long): Flow<List<DebtPaymentEntity>> =
        walletRepository.observeDebtPayments(debtId)

    fun saveDebt(
        editingDebt: DebtEntity?,
        personName: String,
        type: DebtType,
        totalAmountKurus: Long,
        dueDateMillis: Long?,
        note: String,
        reflectAccountId: Long?
    ) {
        viewModelScope.launch {
            val entity = if (editingDebt == null) {
                DebtEntity(
                    personName = personName.trim(),
                    type = type,
                    totalAmountKurus = totalAmountKurus,
                    dueDateMillis = dueDateMillis,
                    note = note.trim()
                )
            } else {
                editingDebt.copy(
                    personName = personName.trim(),
                    type = type,
                    totalAmountKurus = totalAmountKurus,
                    dueDateMillis = dueDateMillis,
                    note = note.trim()
                )
            }
            walletRepository.saveDebt(
                debt = entity,
                reflectAccountId = if (editingDebt == null) reflectAccountId else null
            )
        }
    }

    fun recordPayment(
        debtId: Long,
        amountKurus: Long,
        dateMillis: Long,
        accountId: Long?,
        note: String
    ) {
        viewModelScope.launch {
            walletRepository.recordDebtPayment(
                debtId = debtId,
                amountKurus = amountKurus,
                dateMillis = dateMillis,
                accountId = accountId,
                note = note
            )
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            walletRepository.deleteDebtPayment(paymentId)
        }
    }

    fun setSettled(debtId: Long, settled: Boolean) {
        viewModelScope.launch {
            walletRepository.setDebtSettled(debtId, settled)
        }
    }

    fun deleteDebt(debtId: Long) {
        viewModelScope.launch {
            walletRepository.deleteDebt(debtId)
        }
    }
}
