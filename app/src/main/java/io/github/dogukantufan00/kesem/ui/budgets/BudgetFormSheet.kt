package io.github.dogukantufan00.kesem.ui.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.util.BudgetCalculator
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetFormSheet(
    editingBudget: BudgetEntity?,
    expenseCategories: List<CategoryEntity>,
    existingCategoryIds: Set<Long?>,
    onDismiss: () -> Unit,
    onSave: (
        editingBudget: BudgetEntity?,
        categoryId: Long?,
        monthlyLimitKurus: Long,
        alertThresholdPercent: Int,
        isActive: Boolean
    ) -> Unit,
    onDelete: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kesemColors = LocalKesemColors.current
    val isEditing = editingBudget != null

    var selectedCategoryId by remember(editingBudget) {
        mutableStateOf(
            editingBudget?.categoryId
                ?: if (null !in existingCategoryIds) null else expenseCategories.firstOrNull { it.id !in existingCategoryIds }?.id
        )
    }
    var limitText by remember(editingBudget) {
        mutableStateOf(
            if (editingBudget != null) {
                MoneyFormatter.formatInputFromKurus(editingBudget.monthlyLimitKurus)
            } else {
                ""
            }
        )
    }
    var thresholdPercent by remember(editingBudget) {
        mutableIntStateOf(editingBudget?.alertThresholdPercent ?: 80)
    }
    var isActive by remember(editingBudget) {
        mutableStateOf(editingBudget?.isActive ?: true)
    }
    var limitError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isEditing) "Bütçeyi Düzenle" else "Bütçe Ekle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Target scope
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Kapsam",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                if (editingBudget != null) {
                    val existingName = if (editingBudget.categoryId == null) {
                        BudgetCalculator.OVERALL_BUDGET_TITLE
                    } else {
                        expenseCategories.firstOrNull { it.id == editingBudget.categoryId }?.name
                            ?: "Seçili Kategori"
                    }
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text(existingName) }
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val hasOverall = null in existingCategoryIds
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = {
                                Text(
                                    if (hasOverall) "Genel Aylık (Güncelle)"
                                    else BudgetCalculator.OVERALL_BUDGET_TITLE
                                )
                            }
                        )
                        expenseCategories.forEach { cat ->
                            val hasBudget = cat.id in existingCategoryIds
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconRegistry.iconFor(cat.iconKey),
                                        contentDescription = null,
                                        tint = Color(cat.colorHex).adaptiveAccent(kesemColors.isDark),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(if (hasBudget) "${cat.name} (Güncelle)" else cat.name)
                                }
                            )
                        }
                    }
                }
            }

            // Limit Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = it
                        limitError = null
                    },
                    label = { Text("Aylık Limit") },
                    placeholder = { Text("0,00") },
                    suffix = { Text("₺") },
                    isError = limitError != null,
                    supportingText = limitError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(2500L, 5000L, 10000L, 25000L).forEach { presetTl ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                limitText = presetTl.toString()
                                limitError = null
                            },
                            label = { Text("$presetTl ₺") }
                        )
                    }
                }
            }

            // Threshold Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Uyarı Eşiği",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                val thresholds = listOf(70, 80, 90, 100)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    thresholds.forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = thresholdPercent == value,
                            onClick = { thresholdPercent = value },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = thresholds.size
                            )
                        ) {
                            Text("%$value")
                        }
                    }
                }
            }

            // Active Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aktif Bütçe",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Pasif bütçeler Özet uyarılarında gösterilmez",
                        style = MaterialTheme.typography.bodySmall,
                        color = kesemColors.subtleText
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }

            Button(
                onClick = {
                    val limitKurus = MoneyFormatter.parseAmountToKurus(limitText)
                    if (limitKurus == null || limitKurus <= 0L) {
                        limitError = "Geçerli bir limit gir"
                        return@Button
                    }
                    onSave(
                        editingBudget,
                        selectedCategoryId,
                        limitKurus,
                        thresholdPercent,
                        isActive
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isEditing) "Güncelle" else "Kaydet")
            }

            if (editingBudget != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Bütçeyi Sil")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    if (showDeleteConfirm && editingBudget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Bütçeyi sil?") },
            text = { Text("Bu bütçe limiti kalıcı olarak kaldırılacak. İşlemlerin silinmez.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(editingBudget.id)
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
