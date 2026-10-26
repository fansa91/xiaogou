package com.xiaogou.bookkeeping.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 周期记账规则。表结构与旧版直接 SQL 建表语句一致：
 * money INTEGER NOT NULL（分），时间字段 INTEGER（毫秒，end/last 可空），
 * state 0 启用 / 1 暂停。Phase 2 起把它注册进 Room（版本 6），
 * 迁移 5→6 用同一条 CREATE TABLE IF NOT EXISTS 语句，旧库里已有的
 * 直接 SQL 建的表不会被重建。
 */
@Entity(tableName = "RecurringRecord")
data class RecurringRecord(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "money") val money: Long,
    @ColumnInfo(name = "remark") val remark: String?,
    @ColumnInfo(name = "record_type_id") val recordTypeId: Long,
    @ColumnInfo(name = "record_type") val recordType: Int,
    /** -1 = 不计入账户 */
    @ColumnInfo(name = "assets_id") val assetsId: Long,
    /** 0 每天 / 1 每周 / 2 每月 / 3 每年 */
    @ColumnInfo(name = "cycle_type") val cycleType: Int,
    /** 每周：Calendar.DAY_OF_WEEK（1=周日…7=周六）；每月：几号；每年：Mdd（如 1225） */
    @ColumnInfo(name = "cycle_value") val cycleValue: Int,
    @ColumnInfo(name = "start_time") val startTime: Long?,
    @ColumnInfo(name = "end_time") val endTime: Long?,
    @ColumnInfo(name = "next_time") val nextTime: Long,
    @ColumnInfo(name = "last_time") val lastTime: Long?,
    @ColumnInfo(name = "state") val state: Int,
    @ColumnInfo(name = "create_time") val createTime: Long?,
)
