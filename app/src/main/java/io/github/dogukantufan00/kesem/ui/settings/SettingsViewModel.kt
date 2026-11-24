package io.github.dogukantufan00.kesem.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dogukantufan00.kesem.data.backup.BackupManager
import io.github.dogukantufan00.kesem.data.backup.KesemBackupSnapshot
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.AppLockConfig
import io.github.dogukantufan00.kesem.model.BackupSummary
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val monthStartDay: Int = 1,
    val hideBalances: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val categories: List<CategoryEntity> = emptyList(),
    val archivedAccounts: List<AccountWithBalance> = emptyList(),
    val appLockConfig: AppLockConfig = AppLockConfig(),
    val activeBudgetsCount: Int = 0,
    val isBackupBusy: Boolean = false,
    val statusMessage: String? = null,
    val pendingRestoreSummary: BackupSummary? = null
)

class SettingsViewModel(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val isBackupBusy = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)
    private val pendingRestoreSummary = MutableStateFlow<BackupSummary?>(null)
    private var pendingRestoreSnapshot: KesemBackupSnapshot? = null

    private val basePrefsFlow = combine(
        settingsRepository.monthStartDay,
        settingsRepository.hideBalances,
        settingsRepository.themeMode,
        settingsRepository.appLockConfig
    ) { monthStartDay, hideBalances, themeMode, lockConfig ->
        BasePrefs(monthStartDay, hideBalances, themeMode, lockConfig)
    }

    private val dataFlow = combine(
        walletRepository.observeAllCategories(),
        walletRepository.observeArchivedAccounts(),
        walletRepository.observeBudgets()
    ) { categories, archivedAccounts, budgets ->
        SettingsData(
            categories = categories,
            archivedAccounts = archivedAccounts,
            activeBudgetsCount = budgets.count { it.isActive }
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        basePrefsFlow,
        dataFlow,
        isBackupBusy,
        statusMessage,
        pendingRestoreSummary
    ) { prefs, data, busy, status, restoreSummary ->
        SettingsUiState(
            monthStartDay = prefs.monthStartDay,
            hideBalances = prefs.hideBalances,
            themeMode = prefs.themeMode,
            categories = data.categories,
            archivedAccounts = data.archivedAccounts,
            appLockConfig = prefs.appLockConfig,
            activeBudgetsCount = data.activeBudgetsCount,
            isBackupBusy = busy,
            statusMessage = status,
            pendingRestoreSummary = restoreSummary
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setMonthStartDay(day: Int) {
        viewModelScope.launch {
            settingsRepository.setMonthStartDay(day)
        }
    }

    fun setHideBalances(hide: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideBalances(hide)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun enablePinLock(pin: String) {
        viewModelScope.launch {
            settingsRepository.enablePinLock(pin)
            statusMessage.value = "PIN kilidi etkinleştirildi"
        }
    }

    fun changePin(newPin: String) {
        viewModelScope.launch {
            settingsRepository.changePin(newPin)
            statusMessage.value = "PIN güncellendi"
        }
    }

    fun disablePinLock() {
        viewModelScope.launch {
            settingsRepository.disablePinLock()
            statusMessage.value = "Uygulama kilidi kapatıldı"
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

    fun exportJsonBackup(uri: Uri) {
        if (isBackupBusy.value) return
        viewModelScope.launch {
            isBackupBusy.value = true
            runCatching {
                backupManager.exportJsonToUri(uri)
            }.onSuccess { summary ->
                statusMessage.value =
                    "Yedek alındı: ${summary.accountsCount} hesap, ${summary.transactionsCount} işlem"
            }.onFailure { err ->
                statusMessage.value = "Yedekleme başarısız: ${err.message ?: "Bilinmeyen hata"}"
            }
            isBackupBusy.value = false
        }
    }

    fun previewJsonRestore(uri: Uri) {
        if (isBackupBusy.value) return
        viewModelScope.launch {
            isBackupBusy.value = true
            runCatching {
                backupManager.readAndParseBackupUri(uri)
            }.onSuccess { (snapshot, summary) ->
                pendingRestoreSnapshot = snapshot
                pendingRestoreSummary.value = summary
            }.onFailure { err ->
                pendingRestoreSnapshot = null
                pendingRestoreSummary.value = null
                statusMessage.value = err.message ?: "Yedek dosyası okunamadı"
            }
            isBackupBusy.value = false
        }
    }

    fun confirmJsonRestore() {
        val snapshot = pendingRestoreSnapshot ?: return
        if (isBackupBusy.value) return
        viewModelScope.launch {
            isBackupBusy.value = true
            pendingRestoreSummary.value = null
            pendingRestoreSnapshot = null
            runCatching {
                backupManager.restoreSnapshot(snapshot)
            }.onSuccess { summary ->
                statusMessage.value =
                    "Geri yüklendi: ${summary.accountsCount} hesap, ${summary.transactionsCount} işlem"
            }.onFailure { err ->
                statusMessage.value = "Geri yükleme hatası: ${err.message ?: "Bilinmeyen hata"}"
            }
            isBackupBusy.value = false
        }
    }

    fun cancelJsonRestore() {
        pendingRestoreSnapshot = null
        pendingRestoreSummary.value = null
    }

    fun exportCsv(uri: Uri) {
        if (isBackupBusy.value) return
        viewModelScope.launch {
            isBackupBusy.value = true
            runCatching {
                backupManager.exportCsvToUri(uri)
            }.onSuccess { count ->
                statusMessage.value = "CSV dışa aktarıldı ($count işlem)"
            }.onFailure { err ->
                statusMessage.value = "CSV aktarım hatası: ${err.message ?: "Bilinmeyen hata"}"
            }
            isBackupBusy.value = false
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    fun saveCategory(
        editingId: Long?,
        name: String,
        type: CategoryType,
        iconKey: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@launch
            if (editingId == null) {
                walletRepository.insertCategory(
                    CategoryEntity(
                        name = trimmed,
                        type = type,
                        iconKey = iconKey,
                        colorHex = colorHex,
                        isDefault = false,
                        sortOrder = 99
                    )
                )
            } else {
                val existing = walletRepository.getCategoryById(editingId) ?: return@launch
                walletRepository.updateCategory(
                    existing.copy(
                        name = trimmed,
                        type = type,
                        iconKey = iconKey,
                        colorHex = colorHex
                    )
                )
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            walletRepository.deleteCategory(categoryId)
        }
    }

    fun unarchiveAccount(accountId: Long) {
        viewModelScope.launch {
            walletRepository.setAccountArchived(accountId, archived = false)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            walletRepository.deleteAccount(accountId)
        }
    }

    private data class BasePrefs(
        val monthStartDay: Int,
        val hideBalances: Boolean,
        val themeMode: ThemeMode,
        val appLockConfig: AppLockConfig
    )

    private data class SettingsData(
        val categories: List<CategoryEntity>,
        val archivedAccounts: List<AccountWithBalance>,
        val activeBudgetsCount: Int
    )
}
