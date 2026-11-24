package io.github.dogukantufan00.kesem.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.dogukantufan00.kesem.data.local.dao.AccountDao
import io.github.dogukantufan00.kesem.data.local.dao.BudgetDao
import io.github.dogukantufan00.kesem.data.local.dao.CategoryDao
import io.github.dogukantufan00.kesem.data.local.dao.DebtDao
import io.github.dogukantufan00.kesem.data.local.dao.InvestmentDao
import io.github.dogukantufan00.kesem.data.local.dao.RecurringRuleDao
import io.github.dogukantufan00.kesem.data.local.dao.TransactionDao
import io.github.dogukantufan00.kesem.data.local.entity.AccountEntity
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtPaymentEntity
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        RecurringRuleEntity::class,
        InvestmentEntity::class,
        BudgetEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KesemDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun debtDao(): DebtDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimitKurus INTEGER")
                db.execSQL("ALTER TABLE accounts ADD COLUMN statementDay INTEGER")
                db.execSQL("ALTER TABLE accounts ADD COLUMN paymentDueDay INTEGER")

                db.execSQL("ALTER TABLE transactions ADD COLUMN installmentGroupId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN installmentIndex INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN installmentCount INTEGER")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `debts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personName` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `totalAmountKurus` INTEGER NOT NULL,
                        `dueDateMillis` INTEGER,
                        `note` TEXT NOT NULL,
                        `isSettled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `debt_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `debtId` INTEGER NOT NULL,
                        `amountKurus` INTEGER NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `accountId` INTEGER,
                        `linkedTransactionId` INTEGER,
                        `note` TEXT NOT NULL,
                        FOREIGN KEY(`debtId`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`linkedTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debtId` ON `debt_payments` (`debtId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_linkedTransactionId` ON `debt_payments` (`linkedTransactionId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amountKurus` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `categoryId` INTEGER,
                        `frequency` TEXT NOT NULL,
                        `dayOfMonth` INTEGER NOT NULL,
                        `nextDueDateEpochDay` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_rules_accountId` ON `recurring_rules` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_rules_categoryId` ON `recurring_rules` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_rules_nextDueDateEpochDay` ON `recurring_rules` (`nextDueDateEpochDay`)")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `investments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assetType` TEXT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `averageCostTry` REAL NOT NULL,
                        `lastPriceTry` REAL NOT NULL,
                        `dailyChangePercent` REAL,
                        `lastUpdatedMillis` INTEGER,
                        `priceSource` TEXT NOT NULL,
                        `isManualPrice` INTEGER NOT NULL,
                        `includeInTotal` INTEGER NOT NULL,
                        `annualInterestRate` REAL,
                        `withholdingTaxRate` REAL,
                        `startEpochDay` INTEGER,
                        `maturityEpochDay` INTEGER,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_investments_assetType` ON `investments` (`assetType`)")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER,
                        `monthlyLimitKurus` INTEGER NOT NULL,
                        `alertThresholdPercent` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)")
            }
        }

        fun build(context: Context): KesemDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                KesemDatabase::class.java,
                "kesem_db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }
    }
}
