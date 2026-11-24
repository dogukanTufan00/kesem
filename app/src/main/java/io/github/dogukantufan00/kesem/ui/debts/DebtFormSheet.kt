package io.github.dogukantufan00.kesem.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormSheet(
    editingDebt: DebtEntity?,
    activeAccounts: List<AccountWithBalance>,
    onDismiss: () -> Unit,
    onSave: (
        editingDebt: DebtEntity?,
        personName: String,
        type: DebtType,
        totalAmountKurus: Long,
        dueDateMillis: Long?,
        note: String,
        reflectAccountId: Long?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kesemColors = LocalKesemColors.current
    val isEditing = editingDebt != null

    var debtType by remember(editingDebt) {
        mutableStateOf(editingDebt?.type ?: DebtType.LENT)
    }
    var personName by remember(editingDebt) {
        mutableStateOf(editingDebt?.personName ?: "")
    }
    var amountText by remember(editingDebt) {
        mutableStateOf(
            if (editingDebt != null) MoneyFormatter.formatInputFromKurus(editingDebt.totalAmountKurus) else ""
        )
    }
    var dueDateMillis by remember(editingDebt) {
        mutableStateOf(editingDebt?.dueDateMillis)
    }
    var note by remember(editingDebt) {
        mutableStateOf(editingDebt?.note ?: "")
    }
    var reflectInAccount by remember(editingDebt) { mutableStateOf(false) }
    var selectedAccountId by remember(activeAccounts) {
        mutableLongStateOf(activeAccounts.firstOrNull()?.account?.id ?: 0L)
    }
    var showDatePicker by remember { mutableStateOf(false) }
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
                text = if (isEditing) "Kaydı Düzenle" else "Yeni Borç / Alacak",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    DebtType.LENT to "Alacak (Verdim)",
                    DebtType.BORROWED to "Borç (Aldım)"
                )
                options.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = debtType == type,
                        onClick = { debtType = type },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    errorMessage = null
                },
                label = { Text("Kişi / Kurum Adı") },
                placeholder = { Text("Örn. Ahmet, Ev Sahibi") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Toplam Tutar (₺)") },
                placeholder = { Text("0,00") },
                suffix = { Text("₺") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Optional Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterChip(
                    selected = dueDateMillis != null,
                    onClick = { showDatePicker = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (dueDateMillis != null) {
                                "Vade: ${MoneyFormatter.formatShortDate(dueDateMillis!!)}"
                            } else {
                                "Vade Tarihi Ekle (İsteğe bağlı)"
                            }
                        )
                    }
                )
                if (dueDateMillis != null) {
                    IconButton(onClick = { dueDateMillis = null }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Vadeyi kaldır"
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Not (İsteğe bağlı)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (!isEditing && activeAccounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                text = "Hesap bakiyesine yansıt",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (debtType == DebtType.LENT) {
                                    "Seçilen hesaptan gider işlemi oluşturur"
                                } else {
                                    "Seçilen hesaba gelir işlemi oluşturur"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = kesemColors.subtleText
                            )
                        }
                        Switch(
                            checked = reflectInAccount,
                            onCheckedChange = { reflectInAccount = it }
                        )
                    }

                    if (reflectInAccount) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activeAccounts, key = { it.account.id }) { item ->
                                val acc = item.account
                                FilterChip(
                                    selected = selectedAccountId == acc.id,
                                    onClick = { selectedAccountId = acc.id },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(acc.colorHex))
                                        )
                                    },
                                    label = { Text(acc.name) }
                                )
                            }
                        }
                    }
                }
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
                    val trimmedPerson = personName.trim()
                    if (trimmedPerson.isEmpty()) {
                        errorMessage = "Lütfen kişi veya kurum adını girin."
                        return@Button
                    }
                    val parsedKurus = MoneyFormatter.parseAmountToKurus(amountText, allowZero = false)
                    if (parsedKurus == null) {
                        errorMessage = "Sıfırdan büyük geçerli bir tutar girin."
                        return@Button
                    }
                    val accountToReflect = if (!isEditing && reflectInAccount && selectedAccountId != 0L) {
                        selectedAccountId
                    } else {
                        null
                    }
                    onSave(
                        editingDebt,
                        trimmedPerson,
                        debtType,
                        parsedKurus,
                        dueDateMillis,
                        note,
                        accountToReflect
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isEditing) "Güncelle" else "Kaydet")
            }
        }
    }

    if (showDatePicker) {
        val zone = ZoneId.systemDefault()
        val initialLocal = if (dueDateMillis != null) {
            Instant.ofEpochMilli(dueDateMillis!!).atZone(zone).toLocalDate()
        } else {
            LocalDate.now(zone)
        }
        val initialUtcMillis = initialLocal.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val utc = pickerState.selectedDateMillis
                        if (utc != null) {
                            val local = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                            dueDateMillis = local.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Seç")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
