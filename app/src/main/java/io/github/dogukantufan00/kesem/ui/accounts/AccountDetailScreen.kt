package io.github.dogukantufan00.kesem.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.DateGroupHeader
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.components.PeriodHeader
import io.github.dogukantufan00.kesem.ui.components.TransactionRow
import io.github.dogukantufan00.kesem.ui.components.groupTransactionsByDate
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.ui.theme.adaptiveBadgeBackground
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    uiState: AccountDetailUiState,
    onBack: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onEditAccount: (AccountWithBalance) -> Unit,
    onAdjustBalance: (Long, Long) -> Unit,
    onArchiveAccount: (Long, Boolean) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onAddTransactionForAccount: (Long) -> Unit,
    onPayCreditCard: (cardId: Long, debtKurus: Long) -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit
) {
    val accountWithBalance = uiState.accountWithBalance
    val kesemColors = LocalKesemColors.current

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                        text = accountWithBalance?.account?.name ?: "Hesap Detayı",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    if (accountWithBalance != null) {
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Seçenekler"
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Düzenle") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onEditAccount(accountWithBalance)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (accountWithBalance.account.type == AccountType.CREDIT_CARD) {
                                                "Borcu Düzelt"
                                            } else {
                                                "Bakiyeyi Düzelt"
                                            }
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showAdjustDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (accountWithBalance.account.isArchived) {
                                                "Arşivden Çıkar"
                                            } else {
                                                "Arşivle"
                                            }
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showArchiveConfirm = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Sil",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (accountWithBalance == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hesap bulunamadı",
                    color = kesemColors.subtleText
                )
            }
            return@Scaffold
        }

        val account = accountWithBalance.account
        val isCreditCard = account.type == AccountType.CREDIT_CARD
        val rawAccent = Color(account.colorHex)
        val accent = rawAccent.adaptiveAccent(kesemColors.isDark)
        val grouped = groupTransactionsByDate(uiState.transactions)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Balance / Credit Card Summary Header
            item {
                KesemCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(rawAccent.adaptiveBadgeBackground(kesemColors.isDark)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconRegistry.iconFor(account.iconKey),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = when (account.type) {
                                    AccountType.BANK -> account.bankName ?: "Banka Hesabı"
                                    AccountType.CASH -> "Nakit Hesap"
                                    AccountType.CREDIT_CARD -> account.bankName ?: "Kredi Kartı"
                                    else -> account.name
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = kesemColors.subtleText
                            )
                            if (!account.includeInTotal) {
                                Text(
                                    text = "Toplama dahil edilmiyor",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val cardMetrics = uiState.creditCardMetrics
                    if (isCreditCard && cardMetrics != null) {
                        Text(
                            text = "Toplam Kart Borcu",
                            style = MaterialTheme.typography.labelMedium,
                            color = kesemColors.subtleText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AmountText(
                            amountKurus = cardMetrics.totalDebtKurus,
                            hidden = uiState.hideBalances,
                            style = MaterialTheme.typography.displaySmall,
                            color = if (cardMetrics.totalDebtKurus > 0L) {
                                kesemColors.expense
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (cardMetrics.limitKurus > 0L) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val usage = (cardMetrics.totalDebtKurus.toDouble() / cardMetrics.limitKurus.toDouble())
                                .toFloat()
                                .coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (usage > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(usage.coerceAtLeast(0.02f))
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(if (usage > 0.85f) kesemColors.expense else accent)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Kullanılabilir: ${
                                        MoneyFormatter.formatKurus(
                                            cardMetrics.availableLimitKurus,
                                            hidden = uiState.hideBalances
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = kesemColors.subtleText
                                )
                                Text(
                                    text = "Limit: ${
                                        MoneyFormatter.formatKurus(
                                            cardMetrics.limitKurus,
                                            hidden = uiState.hideBalances
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = kesemColors.subtleText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = kesemColors.cardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Dönem İçi Harcama",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = kesemColors.subtleText
                                )
                                AmountText(
                                    amountKurus = cardMetrics.statementPeriodSpendKurus,
                                    hidden = uiState.hideBalances,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (account.statementDay != null || account.paymentDueDay != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Kesim / Son Ödeme",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = kesemColors.subtleText
                                    )
                                    Text(
                                        text = "Ayın ${account.statementDay ?: "—"}'i / ${account.paymentDueDay ?: "—"}'i",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        AmountText(
                            amountKurus = accountWithBalance.currentBalanceKurus,
                            hidden = uiState.hideBalances,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (account.initialBalanceKurus != 0L) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Açılış bakiyesi: ${
                                    MoneyFormatter.formatKurus(
                                        account.initialBalanceKurus,
                                        hidden = uiState.hideBalances
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = kesemColors.subtleText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAdjustDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isCreditCard) "Borcu Düzelt" else "Bakiye Düzelt")
                        }
                        OutlinedButton(
                            onClick = { onEditAccount(accountWithBalance) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Düzenle")
                        }
                    }

                    if (isCreditCard && cardMetrics != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onPayCreditCard(account.id, cardMetrics.totalDebtKurus)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Payment,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kart Borcu Öde")
                            }
                            FilledTonalButton(
                                onClick = { onAddTransactionForAccount(account.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Harcama")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = { onAddTransactionForAccount(account.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bu Hesaba İşlem Ekle")
                        }
                    }
                }
            }

            // Upcoming Installments (for Credit Cards or accounts with future installments)
            if (uiState.upcomingInstallments.isNotEmpty()) {
                val futureSum = uiState.upcomingInstallments.sumOf { it.transaction.amountKurus }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gelecek Taksitler (${uiState.upcomingInstallments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        AmountText(
                            amountKurus = futureSum,
                            hidden = uiState.hideBalances,
                            color = kesemColors.subtleText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                item {
                    KesemCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        uiState.upcomingInstallments.forEachIndexed { index, inst ->
                            Column {
                                Text(
                                    text = MoneyFormatter.formatShortDate(inst.transaction.dateMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = kesemColors.subtleText,
                                    modifier = Modifier.padding(start = 16.dp, top = 10.dp)
                                )
                                TransactionRow(
                                    item = inst,
                                    hideBalances = uiState.hideBalances,
                                    onClick = { onTransactionClick(inst) }
                                )
                            }
                            if (index < uiState.upcomingInstallments.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = kesemColors.cardBorder
                                )
                            }
                        }
                    }
                }
            }

            // Period Inflow / Outflow Card
            item {
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    PeriodHeader(
                        label = uiState.periodRange.label,
                        onPrevious = onPreviousPeriod,
                        onNext = onNextPeriod
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isCreditCard) "Dönem Ödemesi" else "Dönem Girişi",
                                style = MaterialTheme.typography.labelSmall,
                                color = kesemColors.subtleText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AmountText(
                                amountKurus = uiState.periodTotals.inflowKurus,
                                hidden = uiState.hideBalances,
                                showPlusSign = true,
                                color = kesemColors.income,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isCreditCard) "Dönem Harcaması" else "Dönem Çıkışı",
                                style = MaterialTheme.typography.labelSmall,
                                color = kesemColors.subtleText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AmountText(
                                amountKurus = -uiState.periodTotals.outflowKurus,
                                hidden = uiState.hideBalances,
                                color = kesemColors.expense,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Hesap Hareketleri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (grouped.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = "Bu hesapta işlem yok",
                        subtitle = "Yaptığın gelir, gider ve transferler burada listelenecek."
                    )
                }
            } else {
                grouped.forEach { (dateLabel, txs) ->
                    item(key = "acc_hdr_$dateLabel") {
                        DateGroupHeader(title = dateLabel)
                    }
                    item(key = "acc_grp_$dateLabel") {
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

    if (showAdjustDialog && accountWithBalance != null) {
        val isCreditCard = accountWithBalance.account.type == AccountType.CREDIT_CARD
        val displayInitial = if (isCreditCard) {
            (-accountWithBalance.currentBalanceKurus).coerceAtLeast(0L)
        } else {
            accountWithBalance.currentBalanceKurus
        }
        AdjustBalanceDialog(
            isCreditCard = isCreditCard,
            currentDisplayKurus = displayInitial,
            onDismiss = { showAdjustDialog = false },
            onConfirm = { enteredKurus ->
                val storedTarget = if (isCreditCard) -kotlin.math.abs(enteredKurus) else enteredKurus
                onAdjustBalance(accountWithBalance.account.id, storedTarget)
                showAdjustDialog = false
            }
        )
    }

    if (showArchiveConfirm && accountWithBalance != null) {
        val archived = accountWithBalance.account.isArchived
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text(if (archived) "Arşivden Çıkar" else "Hesabı Arşivle") },
            text = {
                Text(
                    if (archived) {
                        "Bu hesap tekrar aktif hesaplar listesinde görünecek."
                    } else {
                        "Arşivlenen hesap aktif listede gizlenir ama geçmiş işlemleri korunur. Ayarlar'dan geri alabilirsin."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveConfirm = false
                        onArchiveAccount(accountWithBalance.account.id, !archived)
                        onBack()
                    }
                ) {
                    Text(if (archived) "Geri Al" else "Arşivle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    if (showDeleteConfirm && accountWithBalance != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hesabı Sil") },
            text = {
                Text("\"${accountWithBalance.account.name}\" hesabı ve bu hesaptan yapılan tüm işlemler kalıcı olarak silinecek.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount(accountWithBalance.account.id)
                        onBack()
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun AdjustBalanceDialog(
    isCreditCard: Boolean,
    currentDisplayKurus: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var input by remember {
        mutableStateOf(
            if (isCreditCard) {
                MoneyFormatter.formatInputFromKurus(currentDisplayKurus)
            } else {
                MoneyFormatter.formatSignedInputFromKurus(currentDisplayKurus)
            }
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isCreditCard) "Güncel Kart Borcunu Düzelt" else "Güncel Bakiyeyi Düzelt")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isCreditCard) {
                        "Kartındaki toplam güncel borcu yaz; işlem geçmişi varsa fark görünür bir 'Bakiye Düzeltme' hareketi olarak eklenir."
                    } else {
                        "Hesabındaki gerçek bakiyeyi yaz; işlem geçmişi varsa fark görünür bir 'Bakiye Düzeltme' hareketi olarak eklenir (gelir/gider özetini etkilemez)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalKesemColors.current.subtleText
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = { Text(if (isCreditCard) "Yeni Borç (₺)" else "Yeni Bakiye (₺)") },
                    suffix = { Text("₺") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                FilterChip(
                    selected = input.trim() == "0",
                    onClick = {
                        input = "0"
                        error = null
                    },
                    label = { Text("0 ₺ yap") }
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = if (input.isBlank()) {
                        0L
                    } else if (isCreditCard) {
                        MoneyFormatter.parseAmountToKurus(input, allowZero = true)
                    } else {
                        MoneyFormatter.parseSignedAmountToKurus(input)
                    }
                    if (parsed == null) {
                        error = "Geçerli bir tutar girin."
                    } else {
                        onConfirm(parsed)
                    }
                }
            ) {
                Text("Güncelle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
