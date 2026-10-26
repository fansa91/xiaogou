package com.xiaogou.bookkeeping.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Phase 1 实体：字段、表名、外键口径与 docs/DATA_DICTIONARY.md 一致。
 * 金额一律 Long（分），时间一律 Long（毫秒时间戳）。
 * 数据库版本 5，与旧版迁移链 1→2→3→4→5 对应（见 AppDatabase）。
 */
@Entity(
    tableName = "Record",
    foreignKeys = [
        ForeignKey(
            entity = RecordType::class,
            parentColumns = ["id"],
            childColumns = ["record_type_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = Assets::class,
            parentColumns = ["id"],
            childColumns = ["assets_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
)
data class Record(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "money") val money: Long?,
    @ColumnInfo(name = "remark") val remark: String?,
    @ColumnInfo(name = "time") val time: Long?,
    @ColumnInfo(name = "create_time") val createTime: Long?,
    @ColumnInfo(name = "record_type_id") val recordTypeId: Long,
    @ColumnInfo(name = "assets_id") val assetsId: Long?,
)

@Entity(tableName = "RecordType")
data class RecordType(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "img_name") val imgName: String?,
    /** 0 支出 / 1 收入 */
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "ranking") val ranking: Int,
    /** 启用状态：旧版口径，1 为启用（Phase 1 按此约定，管理页可开关） */
    @ColumnInfo(name = "state") val state: Int,
    /** 顶级分类为 -1，子类型存父 id */
    @ColumnInfo(name = "parent_id") val parentId: Long,
)

@Entity(tableName = "Assets")
data class Assets(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "img_name") val imgName: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "state") val state: Int,
    @ColumnInfo(name = "remark") val remark: String,
    @ColumnInfo(name = "create_time") val createTime: Long,
    /** 当前余额（分） */
    @ColumnInfo(name = "money") val money: Long,
    @ColumnInfo(name = "ranking") val ranking: Int?,
    /** 初始金额（分） */
    @ColumnInfo(name = "init_money") val initMoney: Long,
)

@Entity(
    tableName = "AssetsModifyRecord",
    foreignKeys = [
        ForeignKey(
            entity = Assets::class,
            parentColumns = ["id"],
            childColumns = ["assets_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AssetsModifyRecord(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "assets_id") val assetsId: Long,
    @ColumnInfo(name = "money") val money: Long,
    @ColumnInfo(name = "money_before") val moneyBefore: Long,
    @ColumnInfo(name = "state") val state: Int,
    @ColumnInfo(name = "create_time") val createTime: Long,
)

@Entity(tableName = "AssetsSummaryRecord")
data class AssetsSummaryRecord(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "time") val time: Long,
    @ColumnInfo(name = "assets_id") val assetsId: Long,
    @ColumnInfo(name = "assets_money") val assetsMoney: Long,
    @ColumnInfo(name = "liabilities_money") val liabilitiesMoney: Long,
)

@Entity(
    tableName = "AssetsTransferRecord",
    foreignKeys = [
        ForeignKey(
            entity = Assets::class,
            parentColumns = ["id"],
            childColumns = ["assets_id_form"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Assets::class,
            parentColumns = ["id"],
            childColumns = ["assets_id_to"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AssetsTransferRecord(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "state") val state: Int,
    @ColumnInfo(name = "create_time") val createTime: Long,
    @ColumnInfo(name = "time") val time: Long,
    @ColumnInfo(name = "assets_id_form") val assetsIdForm: Long,
    @ColumnInfo(name = "assets_id_to") val assetsIdTo: Long,
    @ColumnInfo(name = "remark") val remark: String,
    @ColumnInfo(name = "money") val money: Long,
)

@Entity(tableName = "Budget")
data class Budget(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "record_type_id") val recordTypeId: Long,
    @ColumnInfo(name = "remark") val remark: String,
    @ColumnInfo(name = "month") val month: Long?,
    @ColumnInfo(name = "money") val money: Long,
)

@Entity(
    tableName = "Reimburse",
    foreignKeys = [
        ForeignKey(
            entity = RecordType::class,
            parentColumns = ["id"],
            childColumns = ["record_type_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = Assets::class,
            parentColumns = ["id"],
            childColumns = ["assets_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
)
data class Reimburse(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "money") val money: Long,
    @ColumnInfo(name = "reimburse_money") val reimburseMoney: Long,
    @ColumnInfo(name = "remark") val remark: String?,
    @ColumnInfo(name = "time") val time: Long?,
    @ColumnInfo(name = "create_time") val createTime: Long?,
    @ColumnInfo(name = "record_type_id") val recordTypeId: Long,
    @ColumnInfo(name = "reimburse_state") val reimburseState: Int,
    @ColumnInfo(name = "assets_id") val assetsId: Long,
)
