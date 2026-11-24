package io.github.dogukantufan00.kesem

import android.content.Context
import io.github.dogukantufan00.kesem.data.backup.BackupManager
import io.github.dogukantufan00.kesem.data.local.KesemDatabase
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.remote.MarketDataService
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.data.storage.ReceiptStorage

class AppContainer(context: Context) {
    val database: KesemDatabase by lazy { KesemDatabase.build(context) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }
    val receiptStorage: ReceiptStorage by lazy { ReceiptStorage(context) }
    val marketDataService: MarketDataService by lazy { MarketDataService() }
    val walletRepository: WalletRepository by lazy {
        WalletRepository(
            database = database,
            receiptStorage = receiptStorage,
            marketDataService = marketDataService
        )
    }
    val backupManager: BackupManager by lazy {
        BackupManager(
            context = context,
            database = database,
            settingsRepository = settingsRepository
        )
    }
}
