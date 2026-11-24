package io.github.dogukantufan00.kesem.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.data.local.SeedData
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    editingAccount: AccountWithBalance?,
    defaultType: AccountType = AccountType.BANK,
    onDismiss: () -> Unit,
    onSave: (
        editingId: Long?,
        name: String,
        type: AccountType,
        bankName: String?,
        balanceKurus: Long,
        colorHex: Long,
        includeInTotal: Boolean,
        creditLimitKurus: Long?,
        statementDay: Int?,
        paymentDueDay: Int?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = editingAccount != null

    var accountType by remember(editingAccount, defaultType) {
        mutableStateOf(editingAccount?.account?.type ?: defaultType)
    }
    var selectedBank by remember(editingAccount) {
        mutableStateOf(editingAccount?.account?.bankName ?: "")
    }
    var name by remember(editingAccount) {
        mutableStateOf(editingAccount?.account?.name ?: "")
    }
    var balanceText by remember(editingAccount) {
        mutableStateOf(
            if (editingAccount != null) {
                if (editingAccount.account.type == AccountType.CREDIT_CARD) {
                    val debtKurus = (-editingAccount.currentBalanceKurus).coerceAtLeast(0L)
                    MoneyFormatter.formatInputFromKurus(debtKurus)
                } else {
                    MoneyFormatter.formatSignedInputFromKurus(editingAccount.currentBalanceKurus)
                }
            } else {
                ""
            }
        )
    }
    var limitText by remember(editingAccount) {
        mutableStateOf(
            editingAccount?.account?.creditLimitKurus?.let {
                MoneyFormatter.formatInputFromKurus(it)
            } ?: ""
        )
    }
    var statementDayText by remember(editingAccount) {
        mutableStateOf(editingAccount?.account?.statementDay?.toString() ?: "")
    }
    var paymentDueDayText by remember(editingAccount) {
        mutableStateOf(editingAccount?.account?.paymentDueDay?.toString() ?: "")
    }

    var selectedColor by remember(editingAccount) {
        mutableLongStateOf(
            editingAccount?.account?.colorHex ?: CategoryIconRegistry.colorPalette.first()
        )
    }
    var includeInTotal by remember(editingAccount) {
        mutableStateOf(editingAccount?.account?.includeInTotal ?: true)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isEditing) "Hesabı Düzenle" else "Yeni Hesap",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    AccountType.BANK to "Banka",
                    AccountType.CASH to "Nakit",
                    AccountType.CREDIT_CARD to "Kredi Kartı"
                )
                options.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = accountType == type,
                        onClick = {
                            accountType = type
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        )
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            if (accountType == AccountType.BANK || accountType == AccountType.CREDIT_CARD) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Banka Seç (İsteğe bağlı)",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalKesemColors.current.subtleText
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(SeedData.POPULAR_TR_BANKS) { bank ->
                            val isSelected = selectedBank == bank
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val prevBank = selectedBank
                                    if (isSelected) {
                                        selectedBank = ""
                                    } else {
                                        selectedBank = bank
                                        val suggestedName = when {
                                            bank == "Diğer" -> ""
                                            accountType == AccountType.CREDIT_CARD -> "$bank Kart"
                                            else -> bank
                                        }
                                        val prevSuggested = when {
                                            prevBank.isBlank() || prevBank == "Diğer" -> ""
                                            accountType == AccountType.CREDIT_CARD -> "$prevBank Kart"
                                            else -> prevBank
                                        }
                                        if (name.isBlank() || name == prevBank || name == prevSuggested) {
                                            name = suggestedName
                                        }
                                    }
                                },
                                label = { Text(bank) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = {
                    Text(if (accountType == AccountType.CREDIT_CARD) "Kart Adı" else "Hesap Adı")
                },
                placeholder = {
                    Text(
                        when (accountType) {
                            AccountType.BANK -> "Örn. Garanti Maaş, Enpara"
                            AccountType.CASH -> "Örn. Cüzdan, Evdeki Nakit"
                            AccountType.CREDIT_CARD -> "Örn. Bonus Kart, Maximum"
                            else -> "Hesap adı"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (accountType == AccountType.CREDIT_CARD) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = it
                        errorMessage = null
                    },
                    label = { Text("Kart Limiti (₺)") },
                    placeholder = { Text("Örn. 50.000") },
                    suffix = { Text("₺") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = balanceText,
                onValueChange = {
                    balanceText = it
                    errorMessage = null
                },
                label = {
                    Text(
                        when {
                            accountType == AccountType.CREDIT_CARD -> "Mevcut Kart Borcu (₺)"
                            isEditing -> "Güncel Bakiye (₺)"
                            else -> "Başlangıç Bakiyesi (₺)"
                        }
                    )
                },
                placeholder = { Text("0,00") },
                suffix = { Text("₺") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (accountType == AccountType.CREDIT_CARD) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = statementDayText,
                        onValueChange = {
                            if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                                statementDayText = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Kesim Günü (1–28)") },
                        placeholder = { Text("Örn. 15") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = paymentDueDayText,
                        onValueChange = {
                            if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                                paymentDueDayText = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Son Ödeme (1–28)") },
                        placeholder = { Text("Örn. 25") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Renk",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalKesemColors.current.subtleText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CategoryIconRegistry.colorPalette.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.5.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Seçili",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Toplama dahil et",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (accountType == AccountType.CREDIT_CARD) {
                            "Net varlıktaki toplam borç hesabına katılır"
                        } else {
                            "Net varlık ve likit toplamında hesaplanır"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalKesemColors.current.subtleText
                    )
                }
                Switch(
                    checked = includeInTotal,
                    onCheckedChange = { includeInTotal = it }
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        errorMessage = "Lütfen bir ad girin."
                        return@Button
                    }
                    val parsedBalance = if (balanceText.isBlank()) {
                        0L
                    } else if (accountType == AccountType.CREDIT_CARD) {
                        MoneyFormatter.parseAmountToKurus(balanceText, allowZero = true)
                    } else {
                        MoneyFormatter.parseSignedAmountToKurus(balanceText)
                    }
                    if (parsedBalance == null) {
                        errorMessage = "Geçerli bir tutar girin (örn. 1250,50)."
                        return@Button
                    }

                    var parsedLimit: Long? = null
                    var parsedStatement: Int? = null
                    var parsedDue: Int? = null

                    if (accountType == AccountType.CREDIT_CARD) {
                        if (limitText.isNotBlank()) {
                            val limit = MoneyFormatter.parseAmountToKurus(limitText, allowZero = false)
                            if (limit == null) {
                                errorMessage = "Geçerli bir kart limiti girin."
                                return@Button
                            }
                            parsedLimit = limit
                        }
                        if (statementDayText.isNotBlank()) {
                            val day = statementDayText.toIntOrNull()
                            if (day == null || day !in 1..28) {
                                errorMessage = "Hesap kesim günü 1 ile 28 arasında olmalı."
                                return@Button
                            }
                            parsedStatement = day
                        }
                        if (paymentDueDayText.isNotBlank()) {
                            val day = paymentDueDayText.toIntOrNull()
                            if (day == null || day !in 1..28) {
                                errorMessage = "Son ödeme günü 1 ile 28 arasında olmalı."
                                return@Button
                            }
                            parsedDue = day
                        }
                    }

                    onSave(
                        editingAccount?.account?.id,
                        trimmedName,
                        accountType,
                        selectedBank.takeIf {
                            (accountType == AccountType.BANK || accountType == AccountType.CREDIT_CARD) &&
                                it.isNotBlank()
                        },
                        parsedBalance,
                        selectedColor,
                        includeInTotal,
                        parsedLimit,
                        parsedStatement,
                        parsedDue
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isEditing) "Değişiklikleri Kaydet" else "Hesabı Kaydet",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
