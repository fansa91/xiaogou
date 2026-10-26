package com.xiaogou.bookkeeping.domain

import java.util.Calendar

/** 周期下一次时间，口径与旧版 RecurringRecordHelper.computeNextTime 一致。 */
fun computeNextTime(fromMs: Long, cycleType: Int, cycleValue: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = fromMs }
    when (cycleType) {
        0 -> cal.add(Calendar.DAY_OF_MONTH, 1)
        1 -> {
            val target = if (cycleValue in 1..7) cycleValue else Calendar.MONDAY
            do { cal.add(Calendar.DAY_OF_MONTH, 1) } while (cal.get(Calendar.DAY_OF_WEEK) != target)
        }
        2 -> {
            val day = cycleValue.coerceIn(1, 28)
            cal.add(Calendar.MONTH, 1)
            cal.set(Calendar.DAY_OF_MONTH, minOf(day, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        3 -> {
            var month = cycleValue / 100
            val day = cycleValue % 100
            if (month !in 1..12) month = 1
            cal.add(Calendar.YEAR, 1)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, minOf(day.coerceAtLeast(1), cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        else -> cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return cal.timeInMillis
}

/** 首次执行时间：当天或下一个满足日的 09:00。 */
fun firstOccurrence(cycleType: Int, cycleValue: Int): Long {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    // 若今天 09:00 已过，从明天起找；computeNextTime 从基准往后推，循环到满足条件
    var candidate = cal.timeInMillis
    if (candidate <= now.timeInMillis) {
        candidate = computeNextTime(candidate, 0, 0)
        // 对周/月/年规则，computeNextTime 从“明天 09:00”起推到下一个满足日
        // 简化处理：按日推进直到周期规则满足（周：星期几；月：几号；年：月日）
        var guard = 0
        while (guard++ < 400) {
            val c = Calendar.getInstance().apply { timeInMillis = candidate }
            val ok = when (cycleType) {
                1 -> c.get(Calendar.DAY_OF_WEEK) == cycleValue
                2 -> c.get(Calendar.DAY_OF_MONTH) == cycleValue.coerceIn(1, 28)
                3 -> (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH) == cycleValue
                else -> true
            }
            if (ok) break
            candidate = computeNextTime(candidate, 0, 0)
        }
    } else {
        // 今天 09:00 未到，但周/月/年规则今天不一定满足，同样按日推进校验
        if (cycleType != 0) {
            var guard = 0
            while (guard++ < 400) {
                val c = Calendar.getInstance().apply { timeInMillis = candidate }
                val ok = when (cycleType) {
                    1 -> c.get(Calendar.DAY_OF_WEEK) == cycleValue
                    2 -> c.get(Calendar.DAY_OF_MONTH) == cycleValue.coerceIn(1, 28)
                    3 -> (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH) == cycleValue
                    else -> true
                }
                if (ok) break
                candidate = computeNextTime(candidate, 0, 0)
            }
        }
    }
    return candidate
}

/** 当月预算键：yyyyMM，如 202610 */
fun currentMonthKey(): Long {
    val cal = Calendar.getInstance()
    return (cal.get(Calendar.YEAR) * 100 + (cal.get(Calendar.MONTH) + 1)).toLong()
}

fun monthKey(year: Int, monthZeroBased: Int): Long = (year * 100 + (monthZeroBased + 1)).toLong()
