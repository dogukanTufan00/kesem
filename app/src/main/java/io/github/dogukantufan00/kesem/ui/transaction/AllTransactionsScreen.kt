package io.github.dogukantufan00.kesem.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.ui.components.DateGroupHeader
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.components.TransactionRow
import io.github.dogukantufan00.kesem.ui.components.groupTransactionsByDate
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    uiState: AllTransactionsUiState,
    onBack: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit
) {
    val kesemColors = LocalKesemColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedAccountFilter by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(uiState.transactions, searchQuery, selectedTypeFilter, selectedAccountFilter) {
        val query = searchQuery.trim().lowercase()
        uiState.transactions.filter { item ->
            val tx = item.transaction
            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchesAccount = selectedAccountFilter == null ||
                tx.accountId == selectedAccountFilter ||
                tx.toAccountId == selectedAccountFilter
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                tx.note.lowercase().contains(query) ||
                    tx.tagsCsv.lowercase().contains(query) ||
                    (item.categoryName?.lowercase()?.contains(query) == true) ||
                    item.accountName.lowercase().contains(query) ||
                    (item.toAccountName?.lowercase()?.contains(query) == true)
            }
            matchesType && matchesAccount && matchesQuery
        }
    }

    val grouped = remember(filtered) { groupTransactionsByDate(filtered) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Tüm İşlemler",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Not, kategori, etiket veya hesap ara") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Temizle")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val typeOptions = listOf(
                    null to "Tümü",
                    TransactionType.INCOME to "Gelir",
                    TransactionType.EXPENSE to "Gider",
                    TransactionType.TRANSFER to "Transfer"
                )
                items(typeOptions) { (type, label) ->
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = type },
                        label = { Text(label) }
                    )
                }
            }

            if (uiState.activeAccounts.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAccountFilter == null,
                            onClick = { selectedAccountFilter = null },
                            label = { Text("Tüm Hesaplar") }
                        )
                    }
                    items(uiState.activeAccounts, key = { "filter_${it.account.id}" }) { acc ->
                        FilterChip(
                            selected = selectedAccountFilter == acc.account.id,
                            onClick = {
                                selectedAccountFilter =
                                    if (selectedAccountFilter == acc.account.id) null else acc.account.id
                            },
                            label = { Text(acc.account.name) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (grouped.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.SearchOff,
                            title = "Eşleşen işlem bulunamadı",
                            subtitle = "Arama veya filtreleri temizleyip tekrar deneyebilirsin."
                        )
                    }
                } else {
                    grouped.forEach { (dateLabel, txs) ->
                        item(key = "all_hdr_$dateLabel") {
                            DateGroupHeader(title = dateLabel)
                        }
                        item(key = "all_grp_$dateLabel") {
                            KesemCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                txs.forEachIndexed { index, tx ->
                                    TransactionRow(
                                        item = tx,
                                        hideBalances = uiState.hideBalances,
                                        onClick = { onTransactionClick(tx) }
                                    )
                                    if (index < txs.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = kesemColors.cardBorder
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
