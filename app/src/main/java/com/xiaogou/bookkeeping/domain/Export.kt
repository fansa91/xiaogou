package com.xiaogou.bookkeeping.domain

import com.xiaogou.bookkeeping.data.entity.Assets
import com.xiaogou.bookkeeping.data.entity.Record
import com.xiaogou.bookkeeping.data.entity.RecordType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** CSV 字段转义：含逗号 / 引号 / 换行时加引号并双写引号。 */
private fun csvCell(value: String): String {
    return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}

/**
 * 账单导出（旧版 setting_export_bills 口径）：列为日期、类型、分类、
 * 账户、金额（元）、备注；金额按分转元，与界面显示一致。
 */
fun buildBillsCsv(
    records: List<Record>,
    typeById: Map<Long, RecordType>,
    assetById: Map<Long, Assets>,
): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    val lines = mutableListOf("日期,类型,分类,账户,金额（元）,备注")
    records.forEach { r ->
        val type = typeById[r.recordTypeId]
        lines.add(
            listOf(
                dateFormat.format(Date(r.time ?: 0)),
                if (type?.type == 1) "收入" else "支出",
                type?.name ?: "",
                assetById[r.assetsId]?.name ?: "",
                centsToYuan(r.money ?: 0),
                r.remark ?: "",
            ).joinToString(",") { csvCell(it) },
        )
    }
    return lines.joinToString("\n")
}
