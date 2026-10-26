package com.xiaogou.bookkeeping.domain

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 周期推算的 JVM 单元测试：日期用固定基准构造，不依赖“今天是几号”。 */
class RecurringTest {
    private fun ms(year: Int, monthZeroBased: Int, day: Int, hour: Int = 9): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun fields(ms: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = ms }

    @Test
    fun daily_addsOneDayKeepingTime() {
        val next = fields(computeNextTime(ms(2026, 9, 26), 0, 0))
        assertEquals(2026, next.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, next.get(Calendar.MONTH))
        assertEquals(27, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun weekly_nextMatchingWeekday() {
        // 2026-10-26 是星期一，下一个星期三是 2026-10-28。
        val next = fields(computeNextTime(ms(2026, 9, 26), 1, Calendar.WEDNESDAY))
        assertEquals(28, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.WEDNESDAY, next.get(Calendar.DAY_OF_WEEK))
        // 当天就是目标星期几时，应推到下周同一天，而不是当天。
        val sameDay = fields(computeNextTime(ms(2026, 9, 26), 1, Calendar.MONDAY))
        assertEquals(2026, sameDay.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, sameDay.get(Calendar.MONTH))
        assertEquals(2, sameDay.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun weekly_invalidValueDefaultsToMonday() {
        val next = fields(computeNextTime(ms(2026, 9, 26), 1, 99))
        assertEquals(Calendar.MONDAY, next.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun monthly_clampsDayTo28() {
        // 31 号规则从 1 月 31 日出发，2 月只有 28 天，应落在 2 月 28 日。
        val next = fields(computeNextTime(ms(2026, 0, 31), 2, 31))
        assertEquals(Calendar.FEBRUARY, next.get(Calendar.MONTH))
        assertEquals(28, next.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun yearly_leapDayClampsInCommonYear() {
        // 闰日规则：2024-02-29 出发，下一年是平年，应落在 2025-02-28。
        val next = fields(computeNextTime(ms(2024, 1, 29), 3, 229))
        assertEquals(2025, next.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, next.get(Calendar.MONTH))
        assertEquals(28, next.get(Calendar.DAY_OF_MONTH))
        // 2023-02-28 出发，下一年是闰年，应落在 2024-02-29。
        val leap = fields(computeNextTime(ms(2023, 1, 28), 3, 229))
        assertEquals(2024, leap.get(Calendar.YEAR))
        assertEquals(29, leap.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun unknownCycleType_defaultsToDaily() {
        val next = fields(computeNextTime(ms(2026, 9, 26), 99, 0))
        assertEquals(27, next.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun firstOccurrence_dailyIsAtNineWithinADay() {
        val first = firstOccurrence(0, 0)
        val f = fields(first)
        assertEquals(9, f.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, f.get(Calendar.MINUTE))
        assertTrue(first > System.currentTimeMillis())
        assertTrue(first - System.currentTimeMillis() <= 24L * 60 * 60 * 1000)
    }
}
