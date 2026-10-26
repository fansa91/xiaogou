package com.xiaogou.bookkeeping.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xiaogou.bookkeeping.data.dao.AssetsDao
import com.xiaogou.bookkeeping.data.dao.AssetsModifyDao
import com.xiaogou.bookkeeping.data.dao.AssetsTransferDao
import com.xiaogou.bookkeeping.data.dao.BudgetDao
import com.xiaogou.bookkeeping.data.dao.RecordDao
import com.xiaogou.bookkeeping.data.dao.RecordTypeDao
import com.xiaogou.bookkeeping.data.dao.RecurringDao
import com.xiaogou.bookkeeping.data.dao.ReimburseDao
import com.xiaogou.bookkeeping.data.entity.Assets
import com.xiaogou.bookkeeping.data.entity.AssetsModifyRecord
import com.xiaogou.bookkeeping.data.entity.AssetsSummaryRecord
import com.xiaogou.bookkeeping.data.entity.AssetsTransferRecord
import com.xiaogou.bookkeeping.data.entity.Budget
import com.xiaogou.bookkeeping.data.entity.Record
import com.xiaogou.bookkeeping.data.entity.RecordType
import com.xiaogou.bookkeeping.data.entity.RecurringRecord
import com.xiaogou.bookkeeping.data.entity.Reimburse

/**
 * MoneyKeeper.db，版本 6（Phase 2 起）。
 * 迁移链与旧版一致：1→2 加 RecordType.parent_id；2→3 建 Budget；
 * 3→4 建 Reimburse；4→5 建 AssetsSummaryRecord；5→6 建 RecurringRecord
 * （用旧版直接 SQL 的同一条 CREATE TABLE IF NOT EXISTS 语句，旧库里
 * 已有的表不会被重建）。
 *
 * 旧库打开风险说明（Phase 1 验收必测项，CI 无法自动测）：
 * Room 打开旧库时会校验 room_master_table 的 identity_hash。
 * 本版实体已按 docs/DATA_DICTIONARY.md 的 CREATE 口径对齐，
 * 但若旧库来自不同 Room 版本导致 hash 不一致，打开会抛异常。
 * 正式替换主线前，必须用一份真实旧版 MoneyKeeper.db 副本实测
 * “打开 + 记一笔 + 统计”；若 hash 不匹配，再按实测的 TableInfo
 * 调整实体空性/外键，而不是用破坏性迁移清数据。
 */
@Database(
    entities = [
        Record::class,
        RecordType::class,
        Assets::class,
        AssetsModifyRecord::class,
        AssetsSummaryRecord::class,
        AssetsTransferRecord::class,
        Budget::class,
        Reimburse::class,
        RecurringRecord::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun recordTypeDao(): RecordTypeDao
    abstract fun assetsDao(): AssetsDao
    abstract fun assetsModifyDao(): AssetsModifyDao
    abstract fun assetsTransferDao(): AssetsTransferDao
    abstract fun budgetDao(): BudgetDao
    abstract fun reimburseDao(): ReimburseDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE RecordType ADD COLUMN parent_id INTEGER NOT NULL DEFAULT -1")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS Budget (id INTEGER PRIMARY KEY AUTOINCREMENT, record_type_id INTEGER NOT NULL, remark TEXT NOT NULL, month INTEGER, money INTEGER NOT NULL)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS Reimburse (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, money INTEGER NOT NULL, reimburse_money INTEGER NOT NULL, remark TEXT, time INTEGER, create_time INTEGER, record_type_id INTEGER NOT NULL, reimburse_state INTEGER NOT NULL, assets_id INTEGER NOT NULL, FOREIGN KEY (record_type_id) REFERENCES RecordType (id), FOREIGN KEY (assets_id) REFERENCES Assets (id))")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS AssetsSummaryRecord (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, time INTEGER NOT NULL, assets_id INTEGER NOT NULL, assets_money INTEGER NOT NULL, liabilities_money INTEGER NOT NULL)")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `RecurringRecord` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `money` INTEGER NOT NULL, `remark` TEXT, `record_type_id` INTEGER NOT NULL, `record_type` INTEGER NOT NULL, `assets_id` INTEGER NOT NULL, `cycle_type` INTEGER NOT NULL, `cycle_value` INTEGER NOT NULL, `start_time` INTEGER, `end_time` INTEGER, `next_time` INTEGER NOT NULL, `last_time` INTEGER, `state` INTEGER NOT NULL, `create_time` INTEGER)")
            }
        }

        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "MoneyKeeper.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
    }
}
