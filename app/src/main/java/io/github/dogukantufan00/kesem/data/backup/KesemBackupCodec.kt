package io.github.dogukantufan00.kesem.data.backup

import io.github.dogukantufan00.kesem.data.local.entity.AccountEntity
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtPaymentEntity
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.data.local.entity.TransactionEntity
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.BackupSummary
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.model.RecurrenceFrequency
import io.github.dogukantufan00.kesem.model.ThemeMode
import io.github.dogukantufan00.kesem.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject

data class BackupSettings(
    val monthStartDay: Int = 1,
    val hideBalances: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

data class KesemBackupSnapshot(
    val schemaVersion: Int = 4,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val settings: BackupSettings = BackupSettings(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val debtPayments: List<DebtPaymentEntity> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList()
)

object KesemBackupCodec {

    private const val APP_HEADER = "Kesem"
    const val CURRENT_SCHEMA_VERSION = 4

    fun summarize(snapshot: KesemBackupSnapshot): BackupSummary = BackupSummary(
        exportedAtMillis = snapshot.exportedAtMillis,
        schemaVersion = snapshot.schemaVersion,
        accountsCount = snapshot.accounts.size,
        categoriesCount = snapshot.categories.size,
        transactionsCount = snapshot.transactions.size,
        debtsCount = snapshot.debts.size,
        debtPaymentsCount = snapshot.debtPayments.size,
        recurringRulesCount = snapshot.recurringRules.size,
        investmentsCount = snapshot.investments.size,
        budgetsCount = snapshot.budgets.size
    )

    fun encode(snapshot: KesemBackupSnapshot): String {
        val root = JSONObject().apply {
            put("app", APP_HEADER)
            put("schemaVersion", snapshot.schemaVersion)
            put("exportedAtMillis", snapshot.exportedAtMillis)
            put("appVersion", snapshot.appVersion)
            put("settings", encodeSettings(snapshot.settings))
            put("accounts", JSONArray().apply { snapshot.accounts.forEach { put(encodeAccount(it)) } })
            put("categories", JSONArray().apply { snapshot.categories.forEach { put(encodeCategory(it)) } })
            put("transactions", JSONArray().apply { snapshot.transactions.forEach { put(encodeTransaction(it)) } })
            put("debts", JSONArray().apply { snapshot.debts.forEach { put(encodeDebt(it)) } })
            put("debtPayments", JSONArray().apply { snapshot.debtPayments.forEach { put(encodeDebtPayment(it)) } })
            put("recurringRules", JSONArray().apply { snapshot.recurringRules.forEach { put(encodeRecurringRule(it)) } })
            put("investments", JSONArray().apply { snapshot.investments.forEach { put(encodeInvestment(it)) } })
            put("budgets", JSONArray().apply { snapshot.budgets.forEach { put(encodeBudget(it)) } })
        }
        return root.toString(2)
    }

    fun decode(rawJson: String): KesemBackupSnapshot {
        val root = runCatching { JSONObject(rawJson) }.getOrElse {
            throw IllegalArgumentException("Geçersiz JSON dosyası")
        }
        val appName = root.optNullableString("app")
        if (appName != APP_HEADER) {
            throw IllegalArgumentException("Bu dosya geçerli bir Kesem yedeği değil")
        }
        val schemaVersion = root.optInt("schemaVersion", 0)
        if (schemaVersion <= 0 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException("Desteklenmeyen yedek sürümü: $schemaVersion")
        }

        val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())
        val appVersion = root.optNullableString("appVersion") ?: "1.0.0"

        val settingsObj = root.optJSONObject("settings")
        val settings = if (settingsObj != null) decodeSettings(settingsObj) else BackupSettings()

        val accounts = decodeList(root, "accounts", required = true) { decodeAccount(it) }
        val categories = decodeList(root, "categories", required = true) { decodeCategory(it) }
        val transactions = decodeList(root, "transactions", required = true) { decodeTransaction(it) }
        val debts = decodeList(root, "debts", required = false) { decodeDebt(it) }
        val debtPayments = decodeList(root, "debtPayments", required = false) { decodeDebtPayment(it) }
        val recurringRules = decodeList(root, "recurringRules", required = false) { decodeRecurringRule(it) }
        val investments = decodeList(root, "investments", required = false) { decodeInvestment(it) }
        val budgets = decodeList(root, "budgets", required = false) { decodeBudget(it) }

        return KesemBackupSnapshot(
            schemaVersion = schemaVersion,
            exportedAtMillis = exportedAtMillis,
            appVersion = appVersion,
            settings = settings,
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            debts = debts,
            debtPayments = debtPayments,
            recurringRules = recurringRules,
            investments = investments,
            budgets = budgets
        )
    }

    private fun <T> decodeList(
        root: JSONObject,
        key: String,
        required: Boolean,
        mapper: (JSONObject) -> T
    ): List<T> {
        val arr = root.optJSONArray(key)
        if (arr == null) {
            if (required) throw IllegalArgumentException("Yedek dosyasında '$key' bölümü eksik")
            return emptyList()
        }
        return List(arr.length()) { index ->
            val obj = arr.optJSONObject(index)
                ?: throw IllegalArgumentException("Yedek dosyasında bozuk kayıt ($key#$index)")
            runCatching { mapper(obj) }.getOrElse { cause ->
                throw IllegalArgumentException("Bozuk yedek kaydı ($key#$index): ${cause.message}", cause)
            }
        }
    }

    private fun encodeSettings(s: BackupSettings): JSONObject = JSONObject().apply {
        put("monthStartDay", s.monthStartDay)
        put("hideBalances", s.hideBalances)
        put("themeMode", s.themeMode.name)
    }

    private fun decodeSettings(obj: JSONObject): BackupSettings = BackupSettings(
        monthStartDay = obj.optInt("monthStartDay", 1).coerceIn(1, 28),
        hideBalances = obj.optBoolean("hideBalances", false),
        themeMode = obj.optNullableString("themeMode")?.let { raw ->
            runCatching { ThemeMode.valueOf(raw) }.getOrNull()
        } ?: ThemeMode.SYSTEM
    )

    private fun encodeAccount(a: AccountEntity): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("name", a.name)
        put("type", a.type.name)
        put("bankName", a.bankName ?: JSONObject.NULL)
        put("currency", a.currency)
        put("initialBalanceKurus", a.initialBalanceKurus)
        put("colorHex", a.colorHex)
        put("iconKey", a.iconKey)
        put("includeInTotal", a.includeInTotal)
        put("isArchived", a.isArchived)
        put("sortOrder", a.sortOrder)
        put("createdAt", a.createdAt)
        put("creditLimitKurus", a.creditLimitKurus ?: JSONObject.NULL)
        put("statementDay", a.statementDay ?: JSONObject.NULL)
        put("paymentDueDay", a.paymentDueDay ?: JSONObject.NULL)
    }

    private fun decodeAccount(obj: JSONObject): AccountEntity = AccountEntity(
        id = obj.getLong("id"),
        name = obj.getString("name"),
        type = AccountType.valueOf(obj.getString("type")),
        bankName = obj.optNullableString("bankName"),
        currency = obj.optNullableString("currency") ?: "TRY",
        initialBalanceKurus = obj.getLong("initialBalanceKurus"),
        colorHex = obj.getLong("colorHex"),
        iconKey = obj.optNullableString("iconKey") ?: "wallet",
        includeInTotal = obj.optBoolean("includeInTotal", true),
        isArchived = obj.optBoolean("isArchived", false),
        sortOrder = obj.optInt("sortOrder", 0),
        createdAt = obj.optLong("createdAt", 0L),
        creditLimitKurus = obj.optNullableLong("creditLimitKurus"),
        statementDay = obj.optNullableInt("statementDay"),
        paymentDueDay = obj.optNullableInt("paymentDueDay")
    )

    private fun encodeCategory(c: CategoryEntity): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("type", c.type.name)
        put("iconKey", c.iconKey)
        put("colorHex", c.colorHex)
        put("isDefault", c.isDefault)
        put("sortOrder", c.sortOrder)
    }

    private fun decodeCategory(obj: JSONObject): CategoryEntity = CategoryEntity(
        id = obj.getLong("id"),
        name = obj.getString("name"),
        type = CategoryType.valueOf(obj.getString("type")),
        iconKey = obj.optNullableString("iconKey") ?: "more_horiz",
        colorHex = obj.getLong("colorHex"),
        isDefault = obj.optBoolean("isDefault", false),
        sortOrder = obj.optInt("sortOrder", 0)
    )

    private fun encodeTransaction(t: TransactionEntity): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("type", t.type.name)
        put("amountKurus", t.amountKurus)
        put("accountId", t.accountId)
        put("toAccountId", t.toAccountId ?: JSONObject.NULL)
        put("categoryId", t.categoryId ?: JSONObject.NULL)
        put("dateMillis", t.dateMillis)
        put("note", t.note)
        put("tagsCsv", t.tagsCsv)
        put("receiptPath", t.receiptPath ?: JSONObject.NULL)
        put("createdAt", t.createdAt)
        put("updatedAt", t.updatedAt)
        put("installmentGroupId", t.installmentGroupId ?: JSONObject.NULL)
        put("installmentIndex", t.installmentIndex ?: JSONObject.NULL)
        put("installmentCount", t.installmentCount ?: JSONObject.NULL)
    }

    private fun decodeTransaction(obj: JSONObject): TransactionEntity = TransactionEntity(
        id = obj.getLong("id"),
        type = TransactionType.valueOf(obj.getString("type")),
        amountKurus = obj.getLong("amountKurus"),
        accountId = obj.getLong("accountId"),
        toAccountId = obj.optNullableLong("toAccountId"),
        categoryId = obj.optNullableLong("categoryId"),
        dateMillis = obj.getLong("dateMillis"),
        note = obj.optNullableString("note") ?: "",
        tagsCsv = obj.optNullableString("tagsCsv") ?: "",
        receiptPath = obj.optNullableString("receiptPath"),
        createdAt = obj.optLong("createdAt", 0L),
        updatedAt = obj.optLong("updatedAt", 0L),
        installmentGroupId = obj.optNullableString("installmentGroupId"),
        installmentIndex = obj.optNullableInt("installmentIndex"),
        installmentCount = obj.optNullableInt("installmentCount")
    )

    private fun encodeDebt(d: DebtEntity): JSONObject = JSONObject().apply {
        put("id", d.id)
        put("personName", d.personName)
        put("type", d.type.name)
        put("totalAmountKurus", d.totalAmountKurus)
        put("dueDateMillis", d.dueDateMillis ?: JSONObject.NULL)
        put("note", d.note)
        put("isSettled", d.isSettled)
        put("createdAt", d.createdAt)
    }

    private fun decodeDebt(obj: JSONObject): DebtEntity = DebtEntity(
        id = obj.getLong("id"),
        personName = obj.getString("personName"),
        type = DebtType.valueOf(obj.getString("type")),
        totalAmountKurus = obj.getLong("totalAmountKurus"),
        dueDateMillis = obj.optNullableLong("dueDateMillis"),
        note = obj.optNullableString("note") ?: "",
        isSettled = obj.optBoolean("isSettled", false),
        createdAt = obj.optLong("createdAt", 0L)
    )

    private fun encodeDebtPayment(p: DebtPaymentEntity): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("debtId", p.debtId)
        put("amountKurus", p.amountKurus)
        put("dateMillis", p.dateMillis)
        put("accountId", p.accountId ?: JSONObject.NULL)
        put("linkedTransactionId", p.linkedTransactionId ?: JSONObject.NULL)
        put("note", p.note)
    }

    private fun decodeDebtPayment(obj: JSONObject): DebtPaymentEntity = DebtPaymentEntity(
        id = obj.getLong("id"),
        debtId = obj.getLong("debtId"),
        amountKurus = obj.getLong("amountKurus"),
        dateMillis = obj.getLong("dateMillis"),
        accountId = obj.optNullableLong("accountId"),
        linkedTransactionId = obj.optNullableLong("linkedTransactionId"),
        note = obj.optNullableString("note") ?: ""
    )

    private fun encodeRecurringRule(r: RecurringRuleEntity): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("title", r.title)
        put("type", r.type.name)
        put("amountKurus", r.amountKurus)
        put("accountId", r.accountId)
        put("categoryId", r.categoryId ?: JSONObject.NULL)
        put("frequency", r.frequency.name)
        put("dayOfMonth", r.dayOfMonth)
        put("nextDueDateEpochDay", r.nextDueDateEpochDay)
        put("isActive", r.isActive)
        put("note", r.note)
        put("createdAt", r.createdAt)
    }

    private fun decodeRecurringRule(obj: JSONObject): RecurringRuleEntity = RecurringRuleEntity(
        id = obj.getLong("id"),
        title = obj.getString("title"),
        type = TransactionType.valueOf(obj.getString("type")),
        amountKurus = obj.getLong("amountKurus"),
        accountId = obj.getLong("accountId"),
        categoryId = obj.optNullableLong("categoryId"),
        frequency = RecurrenceFrequency.valueOf(obj.getString("frequency")),
        dayOfMonth = obj.getInt("dayOfMonth"),
        nextDueDateEpochDay = obj.getLong("nextDueDateEpochDay"),
        isActive = obj.optBoolean("isActive", true),
        note = obj.optNullableString("note") ?: "",
        createdAt = obj.optLong("createdAt", 0L)
    )

    private fun encodeInvestment(i: InvestmentEntity): JSONObject = JSONObject().apply {
        put("id", i.id)
        put("assetType", i.assetType.name)
        put("symbol", i.symbol)
        put("name", i.name)
        put("quantity", i.quantity)
        put("averageCostTry", i.averageCostTry)
        put("lastPriceTry", i.lastPriceTry)
        put("dailyChangePercent", i.dailyChangePercent ?: JSONObject.NULL)
        put("lastUpdatedMillis", i.lastUpdatedMillis ?: JSONObject.NULL)
        put("priceSource", i.priceSource)
        put("isManualPrice", i.isManualPrice)
        put("includeInTotal", i.includeInTotal)
        put("annualInterestRate", i.annualInterestRate ?: JSONObject.NULL)
        put("withholdingTaxRate", i.withholdingTaxRate ?: JSONObject.NULL)
        put("startEpochDay", i.startEpochDay ?: JSONObject.NULL)
        put("maturityEpochDay", i.maturityEpochDay ?: JSONObject.NULL)
        put("note", i.note)
        put("createdAt", i.createdAt)
    }

    private fun decodeInvestment(obj: JSONObject): InvestmentEntity = InvestmentEntity(
        id = obj.getLong("id"),
        assetType = InvestmentAssetType.valueOf(obj.getString("assetType")),
        symbol = obj.getString("symbol"),
        name = obj.getString("name"),
        quantity = obj.getDouble("quantity"),
        averageCostTry = obj.getDouble("averageCostTry"),
        lastPriceTry = obj.getDouble("lastPriceTry"),
        dailyChangePercent = obj.optNullableDouble("dailyChangePercent"),
        lastUpdatedMillis = obj.optNullableLong("lastUpdatedMillis"),
        priceSource = obj.optNullableString("priceSource") ?: "Manuel",
        isManualPrice = obj.optBoolean("isManualPrice", false),
        includeInTotal = obj.optBoolean("includeInTotal", true),
        annualInterestRate = obj.optNullableDouble("annualInterestRate"),
        withholdingTaxRate = obj.optNullableDouble("withholdingTaxRate"),
        startEpochDay = obj.optNullableLong("startEpochDay"),
        maturityEpochDay = obj.optNullableLong("maturityEpochDay"),
        note = obj.optNullableString("note") ?: "",
        createdAt = obj.optLong("createdAt", 0L)
    )

    private fun encodeBudget(b: BudgetEntity): JSONObject = JSONObject().apply {
        put("id", b.id)
        put("categoryId", b.categoryId ?: JSONObject.NULL)
        put("monthlyLimitKurus", b.monthlyLimitKurus)
        put("alertThresholdPercent", b.alertThresholdPercent)
        put("isActive", b.isActive)
        put("createdAt", b.createdAt)
    }

    private fun decodeBudget(obj: JSONObject): BudgetEntity = BudgetEntity(
        id = obj.getLong("id"),
        categoryId = obj.optNullableLong("categoryId"),
        monthlyLimitKurus = obj.getLong("monthlyLimitKurus"),
        alertThresholdPercent = obj.optInt("alertThresholdPercent", 80).coerceIn(50, 100),
        isActive = obj.optBoolean("isActive", true),
        createdAt = obj.optLong("createdAt", 0L)
    )

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else getLong(key)

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else getInt(key)

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = getDouble(key)
        return if (value.isNaN() || value.isInfinite()) null else value
    }
}
