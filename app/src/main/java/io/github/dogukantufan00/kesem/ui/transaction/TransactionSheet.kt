package io.github.dogukantufan00.kesem.ui.transaction

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.ui.components.ReceiptThumbnail
import io.github.dogukantufan00.kesem.ui.components.ReceiptViewerDialog
import io.github.dogukantufan00.kesem.ui.security.LocalAppLockController
import io.github.dogukantufan00.kesem.ui.settings.CategoryFormDialog
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionSheet(
    dependencies: TransactionFormDependencies,
    editingTransaction: TransactionWithDetails?,
    preselectedAccountId: Long?,
    preselectedType: TransactionType? = null,
    preselectedToAccountId: Long? = null,
    preselectedAmountKurus: Long? = null,
    resolveReceiptFile: (String?) -> File?,
    onImportReceipt: suspend (Uri) -> String?,
    onCreateCameraUri: () -> Pair<Uri, File>,
    onCreateQuickCategory: (name: String, type: CategoryType, iconKey: String, colorHex: Long, onCreated: (Long) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
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
        installmentCount: Int
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kesemColors = LocalKesemColors.current
    val appLockController = LocalAppLockController.current
    val scope = rememberCoroutineScope()
    val amountFocusRequester = remember { FocusRequester() }

    val existing = editingTransaction?.transaction
    val isEditing = existing != null

    var txType by remember(existing, preselectedType) {
        mutableStateOf(existing?.type ?: preselectedType ?: TransactionType.EXPENSE)
    }
    var amountText by remember(existing, preselectedAmountKurus) {
        mutableStateOf(
            when {
                existing != null -> MoneyFormatter.formatInputFromKurus(existing.amountKurus)
                preselectedAmountKurus != null && preselectedAmountKurus > 0L ->
                    MoneyFormatter.formatInputFromKurus(preselectedAmountKurus)
                else -> ""
            }
        )
    }

    val accounts = dependencies.activeAccounts
    var selectedAccountId by remember(existing, preselectedAccountId, preselectedToAccountId, accounts) {
        val fallback = if (preselectedToAccountId != null) {
            accounts.firstOrNull {
                it.account.id != preselectedToAccountId && it.account.type != AccountType.CREDIT_CARD
            }?.account?.id
                ?: accounts.firstOrNull { it.account.id != preselectedToAccountId }?.account?.id
                ?: 0L
        } else {
            accounts.firstOrNull()?.account?.id ?: 0L
        }
        mutableLongStateOf(existing?.accountId ?: preselectedAccountId ?: fallback)
    }
    var selectedToAccountId by remember(existing, preselectedToAccountId, accounts) {
        val defaultTarget = preselectedToAccountId
            ?: accounts.firstOrNull { it.account.id != selectedAccountId }?.account?.id
            ?: 0L
        mutableLongStateOf(existing?.toAccountId ?: defaultTarget)
    }

    val selectedAccount = accounts.firstOrNull { it.account.id == selectedAccountId }?.account
    var installmentCount by remember(existing) { mutableIntStateOf(1) }

    val categoriesForType = if (txType == TransactionType.INCOME) {
        dependencies.incomeCategories
    } else {
        dependencies.expenseCategories
    }

    var selectedCategoryId by remember(existing) {
        mutableStateOf(existing?.categoryId)
    }

    LaunchedEffect(txType, categoriesForType) {
        if (txType != TransactionType.TRANSFER) {
            if (selectedCategoryId == null || categoriesForType.none { it.id == selectedCategoryId }) {
                selectedCategoryId = categoriesForType.firstOrNull()?.id
            }
        }
        if (txType == TransactionType.TRANSFER) {
            if (selectedToAccountId == 0L || selectedToAccountId == selectedAccountId) {
                selectedToAccountId = accounts.firstOrNull { it.account.id != selectedAccountId }?.account?.id ?: 0L
            }
        }
    }

    LaunchedEffect(selectedAccountId, txType) {
        if (selectedAccount?.type != AccountType.CREDIT_CARD || txType != TransactionType.EXPENSE) {
            installmentCount = 1
        }
    }

    LaunchedEffect(isEditing) {
        if (!isEditing) {
            runCatching { amountFocusRequester.requestFocus() }
        }
    }

    var selectedDateMillis by remember(existing) {
        mutableLongStateOf(existing?.dateMillis ?: System.currentTimeMillis())
    }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var selectedTags by remember(existing) {
        mutableStateOf(
            if (existing != null) MoneyFormatter.parseTags(existing.tagsCsv) else emptyList()
        )
    }
    var tagInput by remember { mutableStateOf("") }
    var receiptPath by remember(existing) { mutableStateOf(existing?.receiptPath) }
    var isProcessingReceipt by remember { mutableStateOf(false) }

    var showDetailsSection by remember(existing) {
        mutableStateOf(
            existing != null && (
                existing.note.isNotBlank() ||
                    existing.tagsCsv.isNotBlank() ||
                    !existing.receiptPath.isNullOrBlank()
                )
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showQuickCategoryDialog by remember { mutableStateOf(false) }
    var previewReceiptFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingReceipt = true
            scope.launch {
                val saved = onImportReceipt(uri)
                if (saved != null) {
                    receiptPath = saved
                } else {
                    errorMessage = "Fiş fotoğrafı okunamadı."
                }
                isProcessingReceipt = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            isProcessingReceipt = true
            scope.launch {
                val saved = onImportReceipt(uri)
                if (saved != null) {
                    receiptPath = saved
                } else {
                    errorMessage = "Çekilen fotoğraf kaydedilemedi."
                }
                isProcessingReceipt = false
            }
        }
    }

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
                text = if (isEditing) "İşlemi Düzenle" else "Hızlı İşlem",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Type tabs
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(
                    TransactionType.INCOME to "Gelir",
                    TransactionType.EXPENSE to "Gider",
                    TransactionType.TRANSFER to "Transfer"
                )
                tabs.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = txType == type,
                        onClick = {
                            txType = type
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size)
                    ) {
                        Text(label)
                    }
                }
            }

            // Focused Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Tutar (₺)") },
                placeholder = { Text("0,00") },
                suffix = { Text("₺", style = MaterialTheme.typography.titleMedium) },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester),
                shape = RoundedCornerShape(14.dp)
            )

            // Account Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (txType == TransactionType.TRANSFER) "Kaynak Hesap" else "Hesap",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.account.id }) { item ->
                        val acc = item.account
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = {
                                selectedAccountId = acc.id
                                if (txType == TransactionType.TRANSFER && selectedToAccountId == acc.id) {
                                    selectedToAccountId = accounts.firstOrNull { it.account.id != acc.id }?.account?.id ?: 0L
                                }
                                errorMessage = null
                            },
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

            // Installment selector for Credit Card expenses when creating
            if (!isEditing && txType == TransactionType.EXPENSE && selectedAccount?.type == AccountType.CREDIT_CARD) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Taksit",
                        style = MaterialTheme.typography.labelMedium,
                        color = kesemColors.subtleText
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val options = listOf(1, 2, 3, 4, 6, 9, 12)
                        items(options) { count ->
                            FilterChip(
                                selected = installmentCount == count,
                                onClick = { installmentCount = count },
                                label = { Text(if (count == 1) "Peşin" else "$count Taksit") }
                            )
                        }
                    }
                    if (installmentCount > 1) {
                        val previewTotal = MoneyFormatter.parseAmountToKurus(amountText, allowZero = false)
                        val previewText = if (previewTotal != null) {
                            val perMonth = previewTotal / installmentCount
                            "$installmentCount taksit × ${MoneyFormatter.formatKurus(perMonth)} / ay"
                        } else {
                            "Tutar girildiğinde $installmentCount aya bölünecek"
                        }
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Destination Account (Transfer only)
            if (txType == TransactionType.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Hedef Hesap",
                        style = MaterialTheme.typography.labelMedium,
                        color = kesemColors.subtleText
                    )
                    if (accounts.size < 2) {
                        Text(
                            text = "Transfer yapmak için en az 2 aktif hesap ekleyin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                accounts.filter { it.account.id != selectedAccountId },
                                key = { "to_${it.account.id}" }
                            ) { item ->
                                val acc = item.account
                                FilterChip(
                                    selected = selectedToAccountId == acc.id,
                                    onClick = {
                                        selectedToAccountId = acc.id
                                        errorMessage = null
                                    },
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
            } else {
                // Category Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.labelMedium,
                        color = kesemColors.subtleText
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categoriesForType.forEach { cat ->
                            val selected = selectedCategoryId == cat.id
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategoryId = cat.id },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconRegistry.iconFor(cat.iconKey),
                                        contentDescription = null,
                                        tint = Color(cat.colorHex).adaptiveAccent(kesemColors.isDark),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(cat.name) }
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { showQuickCategoryDialog = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Yeni") }
                        )
                    }
                }
            }

            // Quick Date Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val currentSelectedDate = Instant.ofEpochMilli(selectedDateMillis).atZone(zone).toLocalDate()
                val isToday = currentSelectedDate == today
                val isYesterday = currentSelectedDate == today.minusDays(1)

                Text(
                    text = "Tarih",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isToday,
                        onClick = { selectedDateMillis = System.currentTimeMillis() },
                        label = { Text("Bugün") }
                    )
                    FilterChip(
                        selected = isYesterday,
                        onClick = {
                            selectedDateMillis = today.minusDays(1)
                                .atTime(12, 0)
                                .atZone(zone)
                                .toInstant()
                                .toEpochMilli()
                        },
                        label = { Text("Dün") }
                    )
                    FilterChip(
                        selected = !isToday && !isYesterday,
                        onClick = { showDatePicker = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = {
                            Text(
                                if (!isToday && !isYesterday) {
                                    MoneyFormatter.formatShortDate(selectedDateMillis, today = today)
                                } else {
                                    "Tarih Seç"
                                }
                            )
                        }
                    )
                }
            }

            // Expandable details: Note, Tags, Receipt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showDetailsSection = !showDetailsSection }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Not, etiket veya fiş ekle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (showDetailsSection) {
                        Icons.Rounded.ExpandLess
                    } else {
                        Icons.Rounded.ExpandMore
                    },
                    contentDescription = null,
                    tint = kesemColors.subtleText
                )
            }

            AnimatedVisibility(visible = showDetailsSection) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Not (İsteğe bağlı)") },
                        placeholder = { Text("Örn. Akşam market alışverişi") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Tags
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Etiketler",
                            style = MaterialTheme.typography.labelMedium,
                            color = kesemColors.subtleText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { tagInput = it },
                                placeholder = { Text("Etiket yaz (örn. tatil)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            TextButton(
                                onClick = {
                                    val clean = tagInput.trim().removePrefix("#")
                                    if (clean.isNotEmpty() && !selectedTags.contains(clean)) {
                                        selectedTags = selectedTags + clean
                                        tagInput = ""
                                    }
                                }
                            ) {
                                Text("Ekle")
                            }
                        }

                        if (selectedTags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                selectedTags.forEach { tag ->
                                    InputChip(
                                        selected = true,
                                        onClick = { selectedTags = selectedTags - tag },
                                        label = { Text("#$tag") },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Kaldır",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        val availableSuggestions = dependencies.suggestedTags
                            .filterNot { selectedTags.contains(it) }
                        if (availableSuggestions.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(availableSuggestions) { suggestion ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { selectedTags = selectedTags + suggestion },
                                        label = { Text("#$suggestion") }
                                    )
                                }
                            }
                        }
                    }

                    // Receipt Photo
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Fiş Fotoğrafı",
                            style = MaterialTheme.typography.labelMedium,
                            color = kesemColors.subtleText
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    appLockController.suppressNextRelock()
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isProcessingReceipt
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galeri")
                            }
                            OutlinedButton(
                                onClick = {
                                    appLockController.suppressNextRelock()
                                    val (uri, _) = onCreateCameraUri()
                                    pendingCameraUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                enabled = !isProcessingReceipt
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kamera")
                            }
                            if (isProcessingReceipt) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }

                        val resolvedFile = resolveReceiptFile(receiptPath)
                        if (resolvedFile != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ReceiptThumbnail(
                                    file = resolvedFile,
                                    onClick = { previewReceiptFile = resolvedFile },
                                    modifier = Modifier.size(64.dp)
                                )
                                TextButton(onClick = { previewReceiptFile = resolvedFile }) {
                                    Text("Görüntüle")
                                }
                                IconButton(onClick = { receiptPath = null }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Fişi kaldır",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
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

            Button(
                onClick = {
                    val parsedKurus = MoneyFormatter.parseAmountToKurus(amountText, allowZero = false)
                    if (parsedKurus == null) {
                        errorMessage = "Lütfen sıfırdan büyük geçerli bir tutar girin."
                        return@Button
                    }
                    if (selectedAccountId == 0L) {
                        errorMessage = "Lütfen bir hesap seçin."
                        return@Button
                    }
                    if (txType == TransactionType.TRANSFER) {
                        if (selectedToAccountId == 0L || selectedToAccountId == selectedAccountId) {
                            errorMessage = "Transfer için farklı bir hedef hesap seçin."
                            return@Button
                        }
                    }
                    val finalTags = if (tagInput.isNotBlank()) {
                        val extra = tagInput.trim().removePrefix("#")
                        if (extra.isNotEmpty() && !selectedTags.contains(extra)) {
                            selectedTags + extra
                        } else {
                            selectedTags
                        }
                    } else {
                        selectedTags
                    }

                    onSave(
                        existing?.id,
                        txType,
                        parsedKurus,
                        selectedAccountId,
                        if (txType == TransactionType.TRANSFER) selectedToAccountId else null,
                        if (txType == TransactionType.TRANSFER) null else selectedCategoryId,
                        selectedDateMillis,
                        note,
                        finalTags,
                        receiptPath,
                        if (!isEditing && txType == TransactionType.EXPENSE && selectedAccount?.type == AccountType.CREDIT_CARD) {
                            installmentCount
                        } else {
                            1
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isEditing) "Güncelle" else "Kaydet",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showDatePicker) {
        val zone = ZoneId.systemDefault()
        val initialLocal = Instant.ofEpochMilli(selectedDateMillis).atZone(zone).toLocalDate()
        val initialUtcMillis = initialLocal.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedUtc = pickerState.selectedDateMillis
                        if (pickedUtc != null) {
                            val pickedLocal = Instant.ofEpochMilli(pickedUtc)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            selectedDateMillis = pickedLocal
                                .atTime(12, 0)
                                .atZone(zone)
                                .toInstant()
                                .toEpochMilli()
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

    if (showQuickCategoryDialog) {
        val quickType = if (txType == TransactionType.INCOME) {
            CategoryType.INCOME
        } else {
            CategoryType.EXPENSE
        }
        CategoryFormDialog(
            initialCategory = null,
            defaultType = quickType,
            onDismiss = { showQuickCategoryDialog = false },
            onSave = { name, type, iconKey, colorHex ->
                onCreateQuickCategory(name, type, iconKey, colorHex) { createdId ->
                    selectedCategoryId = createdId
                }
                showQuickCategoryDialog = false
            }
        )
    }

    if (previewReceiptFile != null) {
        ReceiptViewerDialog(
            receiptFile = previewReceiptFile!!,
            onDismiss = { previewReceiptFile = null }
        )
    }
}
