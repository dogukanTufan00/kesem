package io.github.dogukantufan00.kesem.ui.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.model.InvestmentWithValuation
import io.github.dogukantufan00.kesem.util.InvestmentCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InvestmentsUiState(
    val items: List<InvestmentWithValuation> = emptyList(),
    val totalValueKurus: Long = 0L,
    val totalCostKurus: Long = 0L,
    val totalProfitLossKurus: Long = 0L,
    val totalProfitLossPercent: Double? = null,
    val lastUpdatedMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val statusMessage: String? = null,
    val hideBalances: Boolean = false
)

class InvestmentsViewModel(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InvestmentsUiState> = combine(
        walletRepository.observeInvestments(),
        settingsRepository.hideBalances,
        isRefreshing,
        statusMessage
    ) { entities, hideBalances, refreshing, message ->
        val valued = entities.map { entity ->
            InvestmentWithValuation(
                investment = entity,
                valuation = InvestmentCalculator.evaluate(entity)
            )
        }
        val included = valued.filter { it.investment.includeInTotal }
        val totalValue = included.sumOf { it.valuation.currentValueKurus }
        val totalCost = included.sumOf { it.valuation.costBasisKurus }
        val totalProfitLoss = totalValue - totalCost
        val totalPercent = if (totalCost > 0L) {
            (totalProfitLoss.toDouble() / totalCost.toDouble()) * 100.0
        } else {
            null
        }
        val lastUpdated = entities
            .filter { it.assetType != InvestmentAssetType.TERM_DEPOSIT }
            .mapNotNull { it.lastUpdatedMillis }
            .maxOrNull()

        InvestmentsUiState(
            items = valued,
            totalValueKurus = totalValue,
            totalCostKurus = totalCost,
            totalProfitLossKurus = totalProfitLoss,
            totalProfitLossPercent = totalPercent,
            lastUpdatedMillis = lastUpdated,
            isRefreshing = refreshing,
            statusMessage = message,
            hideBalances = hideBalances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InvestmentsUiState()
    )

    fun refreshPrices(force: Boolean) {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                val result = walletRepository.refreshInvestmentPrices(force = force)
                when {
                    result.failedSymbols.isNotEmpty() -> {
                        val failedText = result.failedSymbols.joinToString(", ")
                        statusMessage.value = if (result.updatedCount > 0) {
                            "${result.updatedCount} varlık güncellendi. Alınamayan ($failedText) manuel güncellenebilir."
                        } else {
                            "Alınamadı: $failedText (manuel güncellenebilir)"
                        }
                    }
                    force && result.updatedCount > 0 -> {
                        statusMessage.value = "${result.updatedCount} varlık fiyatı güncellendi."
                    }
                    force && result.updatedCount == 0 -> {
                        statusMessage.value = "Güncellenecek piyasa varlığı bulunmuyor."
                    }
                }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun saveInvestment(entity: InvestmentEntity) {
        viewModelScope.launch {
            val isNewMarketAsset = entity.id == 0L &&
                entity.assetType != InvestmentAssetType.TERM_DEPOSIT &&
                !entity.isManualPrice
            walletRepository.saveInvestment(entity)
            if (isNewMarketAsset) {
                refreshPrices(force = false)
            }
        }
    }

    fun addBuyLot(investmentId: Long, buyQty: Double, buyUnitPriceTry: Double) {
        viewModelScope.launch {
            walletRepository.addBuyLot(investmentId, buyQty, buyUnitPriceTry)
        }
    }

    fun applySellLot(investmentId: Long, sellQty: Double) {
        viewModelScope.launch {
            walletRepository.applySellLot(investmentId, sellQty)
        }
    }

    fun updateManualPrice(investmentId: Long, newPriceTry: Double) {
        viewModelScope.launch {
            walletRepository.updateManualPrice(investmentId, newPriceTry)
        }
    }

    fun deleteInvestment(id: Long) {
        viewModelScope.launch {
            walletRepository.deleteInvestment(id)
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }
}
