package io.github.dogukantufan00.kesem.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.dogukantufan00.kesem.AppContainer
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.ui.KesemViewModelFactory
import io.github.dogukantufan00.kesem.ui.accounts.AccountDetailScreen
import io.github.dogukantufan00.kesem.ui.accounts.AccountDetailUiState
import io.github.dogukantufan00.kesem.ui.accounts.AccountFormSheet
import io.github.dogukantufan00.kesem.ui.accounts.AccountsScreen
import io.github.dogukantufan00.kesem.ui.accounts.AccountsViewModel
import io.github.dogukantufan00.kesem.ui.analytics.AnalyticsScreen
import io.github.dogukantufan00.kesem.ui.analytics.AnalyticsViewModel
import io.github.dogukantufan00.kesem.ui.budgets.BudgetFormSheet
import io.github.dogukantufan00.kesem.ui.budgets.BudgetsScreen
import io.github.dogukantufan00.kesem.ui.budgets.BudgetsViewModel
import io.github.dogukantufan00.kesem.ui.components.ReceiptViewerDialog
import io.github.dogukantufan00.kesem.ui.debts.DebtDetailSheet
import io.github.dogukantufan00.kesem.ui.debts.DebtFormSheet
import io.github.dogukantufan00.kesem.ui.debts.DebtsScreen
import io.github.dogukantufan00.kesem.ui.debts.DebtsViewModel
import io.github.dogukantufan00.kesem.ui.investments.InvestmentDetailSheet
import io.github.dogukantufan00.kesem.ui.investments.InvestmentFormSheet
import io.github.dogukantufan00.kesem.ui.investments.InvestmentsScreen
import io.github.dogukantufan00.kesem.ui.investments.InvestmentsViewModel
import io.github.dogukantufan00.kesem.ui.recurring.RecurringFormSheet
import io.github.dogukantufan00.kesem.ui.recurring.RecurringScreen
import io.github.dogukantufan00.kesem.ui.recurring.RecurringViewModel
import io.github.dogukantufan00.kesem.ui.settings.CategoriesScreen
import io.github.dogukantufan00.kesem.ui.settings.SettingsScreen
import io.github.dogukantufan00.kesem.ui.settings.SettingsViewModel
import io.github.dogukantufan00.kesem.ui.summary.SummaryScreen
import io.github.dogukantufan00.kesem.ui.summary.SummaryViewModel
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.transaction.AllTransactionsScreen
import io.github.dogukantufan00.kesem.ui.transaction.TransactionDetailSheet
import io.github.dogukantufan00.kesem.ui.transaction.TransactionSheet
import io.github.dogukantufan00.kesem.ui.transaction.TransactionViewModel
import java.io.File

private object Routes {
    const val SUMMARY = "summary"
    const val ACCOUNTS = "accounts"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
    const val ACCOUNT_DETAIL = "account/{accountId}"
    const val ALL_TRANSACTIONS = "transactions"
    const val CATEGORIES = "categories"
    const val DEBTS = "debts"
    const val RECURRING = "recurring"
    const val INVESTMENTS = "investments"
    const val BUDGETS = "budgets"

    fun accountDetail(accountId: Long) = "account/$accountId"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun KesemAppScaffold(
    container: AppContainer
) {
    val factory = remember(container) { KesemViewModelFactory(container) }
    val summaryViewModel: SummaryViewModel = viewModel(factory = factory)
    val accountsViewModel: AccountsViewModel = viewModel(factory = factory)
    val transactionViewModel: TransactionViewModel = viewModel(factory = factory)
    val analyticsViewModel: AnalyticsViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val debtsViewModel: DebtsViewModel = viewModel(factory = factory)
    val recurringViewModel: RecurringViewModel = viewModel(factory = factory)
    val investmentsViewModel: InvestmentsViewModel = viewModel(factory = factory)
    val budgetsViewModel: BudgetsViewModel = viewModel(factory = factory)

    val summaryState by summaryViewModel.uiState.collectAsStateWithLifecycle()
    val accountsState by accountsViewModel.uiState.collectAsStateWithLifecycle()
    val txFormDeps by transactionViewModel.formDependencies.collectAsStateWithLifecycle()
    val allTransactionsState by transactionViewModel.allTransactionsState.collectAsStateWithLifecycle()
    val analyticsState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val debtsState by debtsViewModel.uiState.collectAsStateWithLifecycle()
    val recurringState by recurringViewModel.uiState.collectAsStateWithLifecycle()
    val investmentsState by investmentsViewModel.uiState.collectAsStateWithLifecycle()
    val budgetsState by budgetsViewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomTabs = remember {
        listOf(
            BottomNavTab(Routes.SUMMARY, "Özet", Icons.Rounded.Home),
            BottomNavTab(Routes.ACCOUNTS, "Hesaplar", Icons.Rounded.AccountBalanceWallet),
            BottomNavTab(Routes.ANALYTICS, "Analiz", Icons.Rounded.PieChart),
            BottomNavTab(Routes.SETTINGS, "Ayarlar", Icons.Rounded.Settings)
        )
    }
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    // Shared transaction modal sheet states
    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var preselectedAccountId by remember { mutableStateOf<Long?>(null) }
    var preselectedTxType by remember { mutableStateOf<TransactionType?>(null) }
    var preselectedToAccountId by remember { mutableStateOf<Long?>(null) }
    var preselectedAmountKurus by remember { mutableStateOf<Long?>(null) }

    var detailTransaction by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var viewerReceiptFile by remember { mutableStateOf<File?>(null) }

    // Account modal sheet states
    var showAccountSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountWithBalance?>(null) }
    var defaultAccountType by remember { mutableStateOf(AccountType.BANK) }

    // Debt modal sheet states
    var showDebtFormSheet by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var detailDebtId by remember { mutableStateOf<Long?>(null) }
    var openDebtPaymentInitially by remember { mutableStateOf(false) }

    // Recurring modal sheet states
    var showRecurringFormSheet by remember { mutableStateOf(false) }
    var editingRecurringRule by remember { mutableStateOf<RecurringRuleEntity?>(null) }

    // Investment modal sheet states
    var showInvestmentFormSheet by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<InvestmentEntity?>(null) }
    var detailInvestmentId by remember { mutableStateOf<Long?>(null) }

    // Budget modal sheet states
    var showBudgetFormSheet by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }

    fun resetTxPreselections() {
        editingTransaction = null
        preselectedAccountId = null
        preselectedTxType = null
        preselectedToAccountId = null
        preselectedAmountKurus = null
    }

    fun openNewTransaction(accountId: Long? = null) {
        resetTxPreselections()
        preselectedAccountId = accountId
        showTransactionSheet = true
    }

    fun openCreditCardPayment(cardId: Long, debtKurus: Long) {
        resetTxPreselections()
        preselectedTxType = TransactionType.TRANSFER
        preselectedToAccountId = cardId
        preselectedAmountKurus = debtKurus.takeIf { it > 0L }
        showTransactionSheet = true
    }

    fun openAccountForm(
        editing: AccountWithBalance? = null,
        type: AccountType = AccountType.BANK
    ) {
        editingAccount = editing
        defaultAccountType = type
        showAccountSheet = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (showBottomBar) {
            WindowInsets.statusBars
        } else {
            WindowInsets(0, 0, 0, 0)
        },
        bottomBar = {
            if (showBottomBar) {
                Box {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = LocalKesemColors.current.subtleText,
                                    unselectedTextColor = LocalKesemColors.current.subtleText
                                )
                            )
                        }
                    }
                    HorizontalDivider(color = LocalKesemColors.current.cardBorder)
                }
            }
        },
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(
                    onClick = { openNewTransaction(null) },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Yeni İşlem Ekle"
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SUMMARY,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.SUMMARY) {
                SummaryScreen(
                    uiState = summaryState,
                    onToggleHideBalances = summaryViewModel::toggleHideBalances,
                    onPreviousPeriod = summaryViewModel::previousPeriod,
                    onNextPeriod = summaryViewModel::nextPeriod,
                    onAccountClick = { accountId ->
                        navController.navigate(Routes.accountDetail(accountId))
                    },
                    onAddBankAccount = {
                        openAccountForm(editing = null, type = AccountType.BANK)
                    },
                    onNavigateToAccounts = {
                        navController.navigate(Routes.ACCOUNTS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToRecurring = {
                        navController.navigate(Routes.RECURRING)
                    },
                    onNavigateToBudgets = {
                        navController.navigate(Routes.BUDGETS)
                    },
                    onExecuteRecurringRule = summaryViewModel::executeRecurringRule,
                    onSkipRecurringRule = summaryViewModel::skipRecurringRule,
                    onTransactionClick = { detailTransaction = it },
                    onSeeAllTransactions = {
                        navController.navigate(Routes.ALL_TRANSACTIONS)
                    },
                    onQuickAddTransaction = { openNewTransaction(null) }
                )
            }

            composable(Routes.ACCOUNTS) {
                AccountsScreen(
                    uiState = accountsState,
                    onAddAccount = { type ->
                        openAccountForm(editing = null, type = type)
                    },
                    onAccountClick = { accountId ->
                        navController.navigate(Routes.accountDetail(accountId))
                    },
                    onNavigateToInvestments = {
                        navController.navigate(Routes.INVESTMENTS)
                    },
                    onNavigateToDebts = {
                        navController.navigate(Routes.DEBTS)
                    },
                    onNavigateToRecurring = {
                        navController.navigate(Routes.RECURRING)
                    }
                )
            }

            composable(Routes.ANALYTICS) {
                AnalyticsScreen(
                    uiState = analyticsState,
                    onPreviousPeriod = analyticsViewModel::previousPeriod,
                    onNextPeriod = analyticsViewModel::nextPeriod,
                    onSelectCategoryType = analyticsViewModel::selectCategoryType,
                    onNavigateToBudgets = {
                        navController.navigate(Routes.BUDGETS)
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    uiState = settingsState,
                    onMonthStartDayChanged = settingsViewModel::setMonthStartDay,
                    onHideBalancesChanged = settingsViewModel::setHideBalances,
                    onThemeModeChanged = settingsViewModel::setThemeMode,
                    onNavigateToCategories = {
                        navController.navigate(Routes.CATEGORIES)
                    },
                    onNavigateToRecurring = {
                        navController.navigate(Routes.RECURRING)
                    },
                    onNavigateToBudgets = {
                        navController.navigate(Routes.BUDGETS)
                    },
                    onUnarchiveAccount = settingsViewModel::unarchiveAccount,
                    onEnablePinLock = settingsViewModel::enablePinLock,
                    onChangePin = settingsViewModel::changePin,
                    onDisablePinLock = settingsViewModel::disablePinLock,
                    onBiometricChanged = settingsViewModel::setBiometricEnabled,
                    onExportJsonBackup = settingsViewModel::exportJsonBackup,
                    onPreviewJsonRestore = settingsViewModel::previewJsonRestore,
                    onConfirmJsonRestore = settingsViewModel::confirmJsonRestore,
                    onCancelJsonRestore = settingsViewModel::cancelJsonRestore,
                    onExportCsv = settingsViewModel::exportCsv,
                    onDismissStatus = settingsViewModel::clearStatusMessage
                )
            }

            composable(
                route = Routes.ACCOUNT_DETAIL,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { entry ->
                val accountId = entry.arguments?.getLong("accountId") ?: 0L
                LaunchedEffect(accountId) {
                    accountsViewModel.resetDetailPeriod()
                }
                val detailFlow = remember(accountId) {
                    accountsViewModel.observeAccountDetail(accountId)
                }
                val detailState by detailFlow.collectAsStateWithLifecycle(
                    initialValue = AccountDetailUiState()
                )

                AccountDetailScreen(
                    uiState = detailState,
                    onBack = { navController.popBackStack() },
                    onPreviousPeriod = accountsViewModel::previousDetailPeriod,
                    onNextPeriod = accountsViewModel::nextDetailPeriod,
                    onEditAccount = { acc ->
                        openAccountForm(editing = acc, type = acc.account.type)
                    },
                    onAdjustBalance = accountsViewModel::adjustBalance,
                    onArchiveAccount = accountsViewModel::archiveAccount,
                    onDeleteAccount = accountsViewModel::deleteAccount,
                    onAddTransactionForAccount = { accId ->
                        openNewTransaction(accountId = accId)
                    },
                    onPayCreditCard = { cardId, debtKurus ->
                        openCreditCardPayment(cardId = cardId, debtKurus = debtKurus)
                    },
                    onTransactionClick = { detailTransaction = it }
                )
            }

            composable(Routes.ALL_TRANSACTIONS) {
                AllTransactionsScreen(
                    uiState = allTransactionsState,
                    onBack = { navController.popBackStack() },
                    onTransactionClick = { detailTransaction = it }
                )
            }

            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    categories = settingsState.categories,
                    onBack = { navController.popBackStack() },
                    onSaveCategory = settingsViewModel::saveCategory,
                    onDeleteCategory = settingsViewModel::deleteCategory
                )
            }

            composable(Routes.DEBTS) {
                DebtsScreen(
                    uiState = debtsState,
                    onBack = { navController.popBackStack() },
                    onAddDebt = {
                        editingDebt = null
                        showDebtFormSheet = true
                    },
                    onDebtClick = { debt ->
                        openDebtPaymentInitially = false
                        detailDebtId = debt.debt.id
                    },
                    onQuickPaymentClick = { debt ->
                        openDebtPaymentInitially = true
                        detailDebtId = debt.debt.id
                    }
                )
            }

            composable(Routes.RECURRING) {
                RecurringScreen(
                    uiState = recurringState,
                    onBack = { navController.popBackStack() },
                    onAddRule = {
                        editingRecurringRule = null
                        showRecurringFormSheet = true
                    },
                    onEditRule = { rule ->
                        editingRecurringRule = rule
                        showRecurringFormSheet = true
                    },
                    onExecuteRule = recurringViewModel::executeRule,
                    onToggleActive = recurringViewModel::setRuleActive,
                    onDeleteRule = recurringViewModel::deleteRule
                )
            }

            composable(Routes.INVESTMENTS) {
                InvestmentsScreen(
                    uiState = investmentsState,
                    onBack = { navController.popBackStack() },
                    onRefresh = investmentsViewModel::refreshPrices,
                    onDismissStatus = investmentsViewModel::clearStatusMessage,
                    onAddInvestment = {
                        editingInvestment = null
                        showInvestmentFormSheet = true
                    },
                    onInvestmentClick = { item ->
                        detailInvestmentId = item.investment.id
                    }
                )
            }

            composable(Routes.BUDGETS) {
                BudgetsScreen(
                    uiState = budgetsState,
                    onBack = { navController.popBackStack() },
                    onPreviousPeriod = budgetsViewModel::previousPeriod,
                    onNextPeriod = budgetsViewModel::nextPeriod,
                    onAddBudget = {
                        editingBudget = null
                        showBudgetFormSheet = true
                    },
                    onEditBudget = { budget ->
                        editingBudget = budget
                        showBudgetFormSheet = true
                    },
                    onToggleActive = budgetsViewModel::setBudgetActive
                )
            }
        }
    }

    if (showTransactionSheet) {
        TransactionSheet(
            dependencies = txFormDeps,
            editingTransaction = editingTransaction,
            preselectedAccountId = preselectedAccountId,
            preselectedType = preselectedTxType,
            preselectedToAccountId = preselectedToAccountId,
            preselectedAmountKurus = preselectedAmountKurus,
            resolveReceiptFile = transactionViewModel::resolveReceiptFile,
            onImportReceipt = transactionViewModel::importReceiptFromUri,
            onCreateCameraUri = transactionViewModel::createCameraTempUri,
            onCreateQuickCategory = transactionViewModel::createQuickCategory,
            onDismiss = {
                showTransactionSheet = false
                resetTxPreselections()
            },
            onSave = { editingId, type, amountKurus, accountId, toAccountId, categoryId, dateMillis, note, tags, receiptPath, installmentCount ->
                transactionViewModel.saveTransaction(
                    editingId = editingId,
                    type = type,
                    amountKurus = amountKurus,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    dateMillis = dateMillis,
                    note = note,
                    tags = tags,
                    receiptPath = receiptPath,
                    installmentCount = installmentCount,
                    onSaved = {
                        showTransactionSheet = false
                        resetTxPreselections()
                    }
                )
            }
        )
    }

    if (detailTransaction != null) {
        val item = detailTransaction!!
        val receiptFile = transactionViewModel.resolveReceiptFile(item.transaction.receiptPath)
        TransactionDetailSheet(
            item = item,
            hideBalances = summaryState.hideBalances,
            receiptFile = receiptFile,
            onViewReceipt = { file -> viewerReceiptFile = file },
            onEdit = { tx ->
                detailTransaction = null
                resetTxPreselections()
                editingTransaction = tx
                showTransactionSheet = true
            },
            onDelete = { id ->
                transactionViewModel.deleteTransaction(id) {
                    detailTransaction = null
                }
            },
            onDeleteInstallmentGroup = { groupId ->
                transactionViewModel.deleteInstallmentGroup(groupId) {
                    detailTransaction = null
                }
            },
            onDismiss = { detailTransaction = null }
        )
    }

    if (showAccountSheet) {
        AccountFormSheet(
            editingAccount = editingAccount,
            defaultType = defaultAccountType,
            onDismiss = {
                showAccountSheet = false
                editingAccount = null
            },
            onSave = { editingId, name, type, bankName, balanceKurus, colorHex, includeInTotal, creditLimitKurus, statementDay, paymentDueDay ->
                accountsViewModel.saveAccount(
                    editingAccountId = editingId,
                    name = name,
                    type = type,
                    bankName = bankName,
                    balanceKurus = balanceKurus,
                    colorHex = colorHex,
                    includeInTotal = includeInTotal,
                    creditLimitKurus = creditLimitKurus,
                    statementDay = statementDay,
                    paymentDueDay = paymentDueDay
                )
                showAccountSheet = false
                editingAccount = null
            }
        )
    }

    if (detailDebtId != null) {
        val liveDebt = debtsState.debts.firstOrNull { it.debt.id == detailDebtId }
        if (liveDebt == null) {
            detailDebtId = null
        } else {
            val paymentsFlow = remember(liveDebt.debt.id) {
                debtsViewModel.observePayments(liveDebt.debt.id)
            }
            val payments by paymentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            DebtDetailSheet(
                item = liveDebt,
                payments = payments,
                activeAccounts = debtsState.activeAccounts,
                hideBalances = debtsState.hideBalances,
                openPaymentDialogInitially = openDebtPaymentInitially,
                onRecordPayment = { amountKurus, dateMillis, accountId, note ->
                    debtsViewModel.recordPayment(
                        debtId = liveDebt.debt.id,
                        amountKurus = amountKurus,
                        dateMillis = dateMillis,
                        accountId = accountId,
                        note = note
                    )
                },
                onDeletePayment = debtsViewModel::deletePayment,
                onEdit = { debt ->
                    detailDebtId = null
                    editingDebt = debt
                    showDebtFormSheet = true
                },
                onToggleSettled = debtsViewModel::setSettled,
                onDeleteDebt = { id ->
                    debtsViewModel.deleteDebt(id)
                    detailDebtId = null
                },
                onDismiss = {
                    detailDebtId = null
                    openDebtPaymentInitially = false
                }
            )
        }
    }

    if (showDebtFormSheet) {
        DebtFormSheet(
            editingDebt = editingDebt,
            activeAccounts = debtsState.activeAccounts,
            onDismiss = {
                showDebtFormSheet = false
                editingDebt = null
            },
            onSave = { existing, personName, type, totalAmountKurus, dueDateMillis, note, reflectAccountId ->
                debtsViewModel.saveDebt(
                    editingDebt = existing,
                    personName = personName,
                    type = type,
                    totalAmountKurus = totalAmountKurus,
                    dueDateMillis = dueDateMillis,
                    note = note,
                    reflectAccountId = reflectAccountId
                )
                showDebtFormSheet = false
                editingDebt = null
            }
        )
    }

    if (showRecurringFormSheet) {
        RecurringFormSheet(
            editingRule = editingRecurringRule,
            uiState = recurringState,
            onDismiss = {
                showRecurringFormSheet = false
                editingRecurringRule = null
            },
            onSave = { existing, title, type, amountKurus, accountId, categoryId, frequency, dayOfMonth, nextDueDateEpochDay, note ->
                recurringViewModel.saveRule(
                    editingRule = existing,
                    title = title,
                    type = type,
                    amountKurus = amountKurus,
                    accountId = accountId,
                    categoryId = categoryId,
                    frequency = frequency,
                    dayOfMonth = dayOfMonth,
                    nextDueDateEpochDay = nextDueDateEpochDay,
                    note = note
                )
                showRecurringFormSheet = false
                editingRecurringRule = null
            }
        )
    }

    if (detailInvestmentId != null) {
        val liveItem = investmentsState.items.firstOrNull { it.investment.id == detailInvestmentId }
        if (liveItem == null) {
            detailInvestmentId = null
        } else {
            InvestmentDetailSheet(
                item = liveItem,
                hideBalances = investmentsState.hideBalances,
                onAddBuyLot = investmentsViewModel::addBuyLot,
                onApplySellLot = investmentsViewModel::applySellLot,
                onUpdateManualPrice = investmentsViewModel::updateManualPrice,
                onEdit = { entity ->
                    detailInvestmentId = null
                    editingInvestment = entity
                    showInvestmentFormSheet = true
                },
                onDelete = { id ->
                    investmentsViewModel.deleteInvestment(id)
                    detailInvestmentId = null
                },
                onDismiss = { detailInvestmentId = null }
            )
        }
    }

    if (showInvestmentFormSheet) {
        InvestmentFormSheet(
            editingInvestment = editingInvestment,
            onDismiss = {
                showInvestmentFormSheet = false
                editingInvestment = null
            },
            onSave = { entity ->
                investmentsViewModel.saveInvestment(entity)
                showInvestmentFormSheet = false
                editingInvestment = null
            }
        )
    }

    if (viewerReceiptFile != null) {
        ReceiptViewerDialog(
            receiptFile = viewerReceiptFile!!,
            onDismiss = { viewerReceiptFile = null }
        )
    }

    if (showBudgetFormSheet) {
        BudgetFormSheet(
            editingBudget = editingBudget,
            expenseCategories = budgetsState.expenseCategories,
            existingCategoryIds = budgetsState.existingCategoryIds,
            onDismiss = {
                showBudgetFormSheet = false
                editingBudget = null
            },
            onSave = { editing, categoryId, monthlyLimitKurus, thresholdPercent, isActive ->
                budgetsViewModel.saveBudget(
                    editingBudget = editing,
                    categoryId = categoryId,
                    monthlyLimitKurus = monthlyLimitKurus,
                    alertThresholdPercent = thresholdPercent,
                    isActive = isActive
                )
                showBudgetFormSheet = false
                editingBudget = null
            },
            onDelete = { id ->
                budgetsViewModel.deleteBudget(id)
                showBudgetFormSheet = false
                editingBudget = null
            }
        )
    }
}
