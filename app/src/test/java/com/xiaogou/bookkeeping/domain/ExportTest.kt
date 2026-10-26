package com.xiaogou.bookkeeping.domain

import com.xiaogou.bookkeeping.data.entity.Assets
import com.xiaogou.bookkeeping.data.entity.Record
import com.xiaogou.bookkeeping.data.entity.RecordType
import java.util.Calendar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** CSV 导出的 JVM 单元测试：表头、收支标签、缺失分类/账户、字段转义。 */
class ExportTest {
    @Before
    fun resetConfig() {
        DisplayConfig.currencySymbol = false
    }

    @After
    fun restoreConfig() {
        DisplayConfig.currencySymbol = false
    }

    private fun assets(id: Long, name: String) = Assets(
        id = id, name = name, imgName = "", type = 0, state = 1,
        remark = "", createTime = 0, money = 0, ranking = 1, initMoney = 0,
    )

    private fun type(id: Long, name: String, type: Int) = RecordType(
        id = id, name = name, imgName = "", type = type,
        ranking = 1, state = 1, parentId = -1,
    )

    private fun record(id: Long, money: Long, remark: String?, typeId: Long, assetId: Long?) =
        Record(
            id = id, money = money, remark = remark,
            time = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 26, 12, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis,
            createTime = 0, recordTypeId = typeId, assetsId = assetId,
        )

    @Test
    fun headerAndLabels() {
        val csv = buildBillsCsv(
            listOf(
                record(1, 2500, "午饭", 1, 1),
                record(2, 800000, "工资", 2, 2),
            ),
            mapOf(1L to type(1, "餐饮", 0), 2L to type(2, "工资", 1)),
            mapOf(1L to assets(1, "现金"), 2L to assets(2, "银行卡")),
        )
        val lines = csv.split("\n")
        assertEquals("日期,类型,分类,账户,金额（元）,备注", lines[0])
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains(",支出,餐饮,现金,25.00,午饭"))
        assertTrue(lines[2].contains(",收入,工资,银行卡,8000.00,工资"))
    }

    @Test
    fun missingTypeAndAsset_becomeEmpty() {
        val csv = buildBillsCsv(
            listOf(record(1, 0, null, 99, null)),
            emptyMap(), emptyMap(),
        )
        val row = csv.split("\n")[1]
        // 类型未知时按支出，分类和账户为空，金额 0.00，备注为空。
        assertTrue(row.contains(",支出,,,0.00,"))
    }

    @Test
    fun quoting_commaQuoteNewline() {
        val csv = buildBillsCsv(
            listOf(
                record(1, 100, "午饭,加菜", 1, 1),
                record(2, 100, "收到\"红包\"", 1, 1),
                record(3, 100, "第一行\n第二行", 1, 1),
            ),
            mapOf(1L to type(1, "餐饮", 0)),
            mapOf(1L to assets(1, "现金")),
        )
        assertTrue(csv.contains("\"午饭,加菜\""))
        assertTrue(csv.contains("\"收到\"\"红包\"\"\""))
        assertTrue(csv.contains("\"第一行\n第二行\""))
    }
}
