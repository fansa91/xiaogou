package com.xiaogou.bookkeeping.domain

import java.util.Calendar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * 金额与月份窗口的 JVM 单元测试。
 * DisplayConfig 是全局可变开关，每个用例前后都复位，避免用例互相影响。
 * 月份断言只看日历字段（年/月/日/时），不写死毫秒数，避免 CI 时区不同导致误报。
 */
class MoneyTest {
    @Before
    fun resetConfig() {
        DisplayConfig.currencySymbol = false
        DisplayConfig.monthBeginDay = 1
    }

    @After
    fun restoreConfig() {
        DisplayConfig.currencySymbol = false
        DisplayConfig.monthBeginDay = 1
    }

    @Test
    fun centsToYuan_formats() {
        assertEquals("0.00", centsToYuan(0))
        assertEquals("0.01", centsToYuan(1))
        assertEquals("123.45", centsToYuan(12345))
        assertEquals("-2.50", centsToYuan(-250))
    }

    @Test
    fun centsToYuan_withSymbol() {
        DisplayConfig.currencySymbol = true
        assertEquals("¥123.45", centsToYuan(12345))
    }

    @Test
    fun yuanToCents_validAndRounding() {
        assertEquals(1000L, yuanToCents("10"))
        assertEquals(1050L, yuanToCents("10.5"))
        assertEquals(1001L, yuanToCents("10.005"))
        assertEquals(-525L, yuanToCents("-5.25"))
        assertEquals(0L, yuanToCents("0"))
    }

    @Test
    fun yuanToCents_invalidReturnsNull() {
        assertNull(yuanToCents(""))
        assertNull(yuanToCents("   "))
        assertNull(yuanToCents("abc"))
        assertNull(yuanToCents("10.5.2"))
    }

    private fun fields(ms: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = ms }

    @Test
    fun monthRange_defaultStartsOnFirst() {
        val (start, end) = monthRange(2026, 9) // 2026 年 10 月
        val s = fields(start)
        assertEquals(2026, s.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, s.get(Calendar.MONTH))
        assertEquals(1, s.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, s.get(Calendar.HOUR_OF_DAY))
        val e = fields(end)
        assertEquals(2026, e.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, e.get(Calendar.MONTH))
        assertEquals(1, e.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun monthRange_beginDayAndYearCrossing() {
        DisplayConfig.monthBeginDay = 15
        val (start, end) = monthRange(2026, 11) // 2026 年 12 月
        assertEquals(15, fields(start).get(Calendar.DAY_OF_MONTH))
        val e = fields(end)
        assertEquals(2027, e.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, e.get(Calendar.MONTH))
        assertEquals(15, e.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun monthRange_beginDayClampedTo1To28() {
        DisplayConfig.monthBeginDay = 99
        assertEquals(28, fields(monthRange(2026, 9).first).get(Calendar.DAY_OF_MONTH))
        DisplayConfig.monthBeginDay = 0
        assertEquals(1, fields(monthRange(2026, 9).first).get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun formatMonth_andMonthKey() {
        assertEquals("2026-10", formatMonth(2026, 9))
        assertEquals("2026-01", formatMonth(2026, 0))
        assertEquals(202610L, monthKey(2026, 9))
        assertEquals(202601L, monthKey(2026, 0))
    }

    @Test
    fun formatDate_nullIsEmpty() {
        assertEquals("", formatDate(null))
    }
}
