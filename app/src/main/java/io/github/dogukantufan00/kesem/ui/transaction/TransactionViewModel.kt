package io.github.dogukantufan00.kesem.ui.transaction

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.data.local.entity.TransactionEntity
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.data.storage.ReceiptStorage
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class TransactionFormDependencies(
    val activeAccounts: List<AccountWithBalance> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val suggestedTags: List<String> = emptyList()
)

data class AllTransactionsUiState(
    val transactions: List<TransactionWithDetails> = emptyList(),
    val activeAccounts: List<AccountWithBalance> = emptyList(),
    val hideBalances: Boolean = false
)

class TransactionViewModel(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository,
    private val receiptStorage: ReceiptStorage
) : ViewModel() {

    val formDependencies: StateFlow<TransactionFormDependencies> = combine(
        walletRepository.observeActiveAccounts(),
        walletRepository.observeCategories(CategoryType.EXPENSE),
        walletRepository.observeCategories(CategoryType.INCOME),
        walletRepository.observeDistinctTagsCsv()
    ) { accounts, expenseCats, incomeCats, tagsCsvList ->
        val parsedTags = tagsCsvList
            .flatMap { MoneyFormatter.parseTags(it) }
            .distinct()
            .take(16)
        TransactionFormDependencies(
            activeAccounts = accounts,
            expenseCategories = expenseCats,
            incomeCategories = incomeCats,
            suggestedTags = parsedTags
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionFormDependencies()
    )

    val allTransactionsState: StateFlow<AllTransactionsUiState> = combine(
        walletRepository.observeAllTransactions(),
        walletRepository.observeActiveAccounts(),
        settingsRepository.hideBalances
    ) { transactions, accounts, hideBalances ->
        AllTransactionsUiState(
            transactions = transactions,
            activeAccounts = accounts,
            hideBalances = hideBalances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AllTransactionsUiState()
    )

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        walletRepository.getTransactionById(id)

    suspend fun importReceiptFromUri(uri: Uri): String? =
        receiptStorage.saveFromUri(uri)

    fun createCameraTempUri(): Pair<Uri, File> =
        receiptStorage.createTempCameraUri()

    fun resolveReceiptFile(relativePath: String?): File? =
        receiptStorage.resolveFile(relativePath)

    fun saveTransaction(
        editingId: Long?,
        type: TransactionType,
        amountKurus: Long,
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long?,
        dateMillis: Long,
        note: String,
        tags: List<String>,
        receiptPath: String?,
        installmentCount: Int = 1,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (editingId == null && type == TransactionType.EXPENSE && installmentCount > 1) {
                walletRepository.saveInstallmentExpense(
                    totalAmountKurus = amountKurus,
                    installmentCount = installmentCount,
                    accountId = accountId,
                    categoryId = categoryId,
                    startDateMillis = dateMillis,
                    note = note,
                    tags = tags,
                    receiptPath = receiptPath
                )
            } else {
                walletRepository.saveTransaction(
                    TransactionEntity(
                        id = editingId ?: 0L,
                        type = type,
                        amountKurus = amountKurus,
                        accountId = accountId,
                        toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                        categoryId = if (type == TransactionType.TRANSFER) null else categoryId,
                        dateMillis = dateMillis,
                        note = note.trim(),
                        tagsCsv = MoneyFormatter.encodeTags(tags),
                        receiptPath = receiptPath
                    )
                )
            }
            onSaved()
        }
    }

    fun deleteTransaction(id: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            walletRepository.deleteTransaction(id)
            onDeleted()
        }
    }

    fun deleteInstallmentGroup(groupId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            walletRepository.deleteInstallmentGroup(groupId)
            onDeleted()
        }
    }

    fun createQuickCategory(
        name: String,
        type: CategoryType,
        iconKey: String,
        colorHex: Long,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val newId = walletRepository.insertCategory(
                CategoryEntity(
                    name = name.trim(),
                    type = type,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    isDefault = false,
                    sortOrder = 99
                )
            )
            onCreated(newId)
        }
    }
}
