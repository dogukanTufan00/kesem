package io.github.dogukantufan00.kesem.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.BackupSummary
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.ThemeMode
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.security.LocalAppLockController
import io.github.dogukantufan00.kesem.ui.security.PinSetupMode
import io.github.dogukantufan00.kesem.ui.security.PinSetupSheet
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.BiometricHelper
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.time.LocalDate

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onMonthStartDayChanged: (Int) -> Unit,
    onHideBalancesChanged: (Boolean) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onUnarchiveAccount: (Long) -> Unit,
    onEnablePinLock: (String) -> Unit,
    onChangePin: (String) -> Unit,
    onDisablePinLock: () -> Unit,
    onBiometricChanged: (Boolean) -> Unit,
    onExportJsonBackup: (Uri) -> Unit,
    onPreviewJsonRestore: (Uri) -> Unit,
    onConfirmJsonRestore: () -> Unit,
    onCancelJsonRestore: () -> Unit,
    onExportCsv: (Uri) -> Unit,
    onDismissStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kesemColors = LocalKesemColors.current
    val appLockController = LocalAppLockController.current

    var showMonthDayDialog by remember { mutableStateOf(false) }
    var pinSetupMode by remember { mutableStateOf<PinSetupMode?>(null) }
    var biometricError by remember { mutableStateOf<String?>(null) }

    val biometricAvailable = remember(context) { BiometricHelper.isBiometricAvailable(context) }

    val jsonBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportJsonBackup(uri)
    }

    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onPreviewJsonRestore(uri)
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) onExportCsv(uri)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        val bannerMessage = biometricError ?: uiState.statusMessage
        if (bannerMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bannerMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                biometricError = null
                                onDismissStatus()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Kapat",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dönem ve Görünüm
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Dönem ve Görünüm",
                    style = MaterialTheme.typography.labelLarge,
                    color = kesemColors.subtleText,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Tema",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (uiState.themeMode) {
                                        ThemeMode.LIGHT -> "Her zaman açık tema"
                                        ThemeMode.DARK -> "Her zaman koyu tema"
                                        ThemeMode.SYSTEM -> "Cihaz ayarını takip et"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = kesemColors.subtleText
                                )
                            }
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val themeOptions = listOf(
                                ThemeMode.LIGHT to "Açık",
                                ThemeMode.DARK to "Koyu",
                                ThemeMode.SYSTEM to "Sistem"
                            )
                            themeOptions.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = uiState.themeMode == mode,
                                    onClick = { onThemeModeChanged(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = themeOptions.size
                                    )
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = kesemColors.cardBorder
                    )

                    SettingsActionRow(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "Ay Başlangıç Günü",
                        subtitle = if (uiState.monthStartDay == 1) {
                            "Ayın 1'i (Varsayılan takvim ayı)"
                        } else {
                            "Her ayın ${uiState.monthStartDay}'i"
                        },
                        onClick = { showMonthDayDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = kesemColors.cardBorder
                    )

                    SettingsSwitchRow(
                        icon = if (uiState.hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        title = "Bakiyeleri Gizle",
                        subtitle = if (uiState.hideBalances) "Tutarlar şu an gizli" else "Tutarlar şu an görünür",
                        checked = uiState.hideBalances,
                        onCheckedChange = onHideBalancesChanged
                    )
                }
            }
        }

        // Planlama ve Kategoriler
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Planlama ve Kategoriler",
                    style = MaterialTheme.typography.labelLarge,
                    color = kesemColors.subtleText,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    SettingsActionRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "Bütçe Limitleri",
                        subtitle = if (uiState.activeBudgetsCount == 0) {
                            "Genel veya kategori bazlı limit belirle"
                        } else {
                            "${uiState.activeBudgetsCount} aktif bütçe izleniyor"
                        },
                        onClick = onNavigateToBudgets
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = kesemColors.cardBorder
                    )
                    val expenseCount = uiState.categories.count { it.type == CategoryType.EXPENSE }
                    val incomeCount = uiState.categories.count { it.type == CategoryType.INCOME }
                    SettingsActionRow(
                        icon = Icons.Rounded.Category,
                        title = "Kategori Yönetimi",
                        subtitle = "$expenseCount gider, $incomeCount gelir kategorisi",
                        onClick = onNavigateToCategories
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = kesemColors.cardBorder
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.Autorenew,
                        title = "Düzenli İşlemler & Abonelikler",
                        subtitle = "Maaş, kira, fatura ve abonelikleri yönet",
                        onClick = onNavigateToRecurring
                    )
                }
            }
        }

        // Güvenlik
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Güvenlik",
                    style = MaterialTheme.typography.labelLarge,
                    color = kesemColors.subtleText,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    val pinEnabled = uiState.appLockConfig.isPinEnabled
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Lock,
                        title = "Uygulama Kilidi (PIN)",
                        subtitle = if (pinEnabled) {
                            "4 haneli PIN koruması aktif"
                        } else {
                            "Açılışta 4 haneli PIN ile koru"
                        },
                        checked = pinEnabled,
                        onCheckedChange = { wantEnabled ->
                            pinSetupMode = if (wantEnabled) PinSetupMode.CREATE else PinSetupMode.DISABLE
                        }
                    )

                    if (pinEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = kesemColors.cardBorder
                        )
                        SettingsActionRow(
                            icon = Icons.Rounded.Password,
                            title = "PIN Değiştir",
                            subtitle = "Mevcut 4 haneli PIN'i güncelle",
                            onClick = { pinSetupMode = PinSetupMode.CHANGE }
                        )

                        if (biometricAvailable) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = kesemColors.cardBorder
                            )
                            SettingsSwitchRow(
                                icon = Icons.Rounded.Fingerprint,
                                title = "Parmak İzi / Yüz Tanıma",
                                subtitle = "PIN yerine hızlı biyometrik açılış",
                                checked = uiState.appLockConfig.isBiometricEnabled,
                                onCheckedChange = { enable ->
                                    if (!enable) {
                                        onBiometricChanged(false)
                                    } else {
                                        BiometricHelper.authenticate(
                                            context = context,
                                            title = "Biyometrik Kilidi Etkinleştir",
                                            subtitle = "Parmak izi veya yüz tanıma ile doğrula",
                                            onSuccess = {
                                                biometricError = null
                                                onBiometricChanged(true)
                                            },
                                            onError = { err ->
                                                if (!err.isNullOrBlank()) {
                                                    biometricError = err
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = kesemColors.cardBorder
                        )
                        SettingsActionRow(
                            icon = Icons.Rounded.Key,
                            title = "Uygulamayı Şimdi Kilitle",
                            subtitle = "Hemen PIN kilit ekranına geç",
                            onClick = { appLockController.lockNow() }
                        )
                    }
                }
            }
        }

        // Yedekleme ve Dışa Aktarma
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yedekleme ve Dışa Aktarma",
                        style = MaterialTheme.typography.labelLarge,
                        color = kesemColors.subtleText
                    )
                    if (uiState.isBackupBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    val todayTag = LocalDate.now().toString()
                    SettingsActionRow(
                        icon = Icons.Rounded.Backup,
                        title = "Tam Yedek Al (JSON)",
                        subtitle = "Hesaplar, işlemler, yatırımlar, bütçeler ve borçlar",
                        enabled = !uiState.isBackupBusy,
                        onClick = {
                            appLockController.suppressNextRelock()
                            jsonBackupLauncher.launch("kesem-yedek-$todayTag.json")
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = kesemColors.cardBorder
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.Restore,
                        title = "Yedekten Geri Yükle (JSON)",
                        subtitle = "Önceden alınan JSON yedeğini geri yükle",
                        enabled = !uiState.isBackupBusy,
                        onClick = {
                            appLockController.suppressNextRelock()
                            jsonRestoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = kesemColors.cardBorder
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.TableView,
                        title = "İşlemleri CSV Olarak Dışa Aktar",
                        subtitle = "Excel ve Google Sheets uyumlu (UTF-8 / ;)",
                        enabled = !uiState.isBackupBusy,
                        onClick = {
                            appLockController.suppressNextRelock()
                            csvExportLauncher.launch("kesem-islemler-$todayTag.csv")
                        }
                    )
                }
            }
        }

        // Arşivlenen Hesaplar
        if (uiState.archivedAccounts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Arşivlenen Hesaplar",
                        style = MaterialTheme.typography.labelLarge,
                        color = kesemColors.subtleText,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    KesemCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        uiState.archivedAccounts.forEachIndexed { index, archived ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = archived.account.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    AmountText(
                                        amountKurus = archived.currentBalanceKurus,
                                        hidden = uiState.hideBalances,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = kesemColors.subtleText
                                    )
                                }
                                TextButton(onClick = { onUnarchiveAccount(archived.account.id) }) {
                                    Text("Geri Al")
                                }
                            }
                            if (index < uiState.archivedAccounts.lastIndex) {
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

        // Hakkında & Gizlilik
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Hakkında",
                    style = MaterialTheme.typography.labelLarge,
                    color = kesemColors.subtleText,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                KesemCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kesem v1.0.0 (Faz 4)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = kesemColors.subtleText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tüm hesap, harcama, bütçe, yatırım ve fiş fotoğraflarınız yalnızca bu cihazda saklanır; piyasa kurları yalnızca açık kaynaklı fiyat servislerinden okunur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = kesemColors.subtleText
                        )
                    }
                }
            }
        }
    }

    if (showMonthDayDialog) {
        MonthStartDayDialog(
            selectedDay = uiState.monthStartDay,
            onDismiss = { showMonthDayDialog = false },
            onSelect = { day ->
                onMonthStartDayChanged(day)
                showMonthDayDialog = false
            }
        )
    }

    if (pinSetupMode != null) {
        val activeMode = pinSetupMode!!
        PinSetupSheet(
            mode = activeMode,
            currentPinSalt = uiState.appLockConfig.pinSalt,
            currentPinHash = uiState.appLockConfig.pinHash,
            onPinConfigured = { newPin ->
                if (activeMode == PinSetupMode.CREATE) {
                    onEnablePinLock(newPin)
                } else {
                    onChangePin(newPin)
                }
                pinSetupMode = null
            },
            onPinDisabled = {
                onDisablePinLock()
                pinSetupMode = null
            },
            onDismiss = { pinSetupMode = null }
        )
    }

    if (uiState.pendingRestoreSummary != null) {
        RestoreConfirmDialog(
            summary = uiState.pendingRestoreSummary,
            onConfirm = onConfirmJsonRestore,
            onDismiss = onCancelJsonRestore
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LocalKesemColors.current.subtleText
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = LocalKesemColors.current.subtleText
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LocalKesemColors.current.subtleText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun RestoreConfirmDialog(
    summary: BackupSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val exportDate = MoneyFormatter.formatShortDate(summary.exportedAtMillis)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yedekten geri yükle?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tarih: $exportDate (Şema v${summary.schemaVersion})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• ${summary.accountsCount} hesap, ${summary.categoriesCount} kategori\n" +
                        "• ${summary.transactionsCount} işlem, ${summary.budgetsCount} bütçe\n" +
                        "• ${summary.investmentsCount} yatırım, ${summary.recurringRulesCount} düzenli işlem\n" +
                        "• ${summary.debtsCount} borç/alacak (${summary.debtPaymentsCount} ödeme)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Onayladığında cihazdaki mevcut veriler bu yedekle değiştirilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Geri Yükle", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthStartDayDialog(
    selectedDay: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ay Başlangıç Günü") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Aylık özet ve analizlerin hangi gün sıfırlanacağını seç (ör. maaş günü).",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalKesemColors.current.subtleText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..28).forEach { day ->
                        val isSelected = day == selectedDay
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { onSelect(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
