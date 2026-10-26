package com.xiaogou.bookkeeping.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 分 → 元显示，如 12345 分 → "123.45" */
fun centsToYuan(cents: Long): String =
    BigDecimal(cents).divide(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP).toPlainString()

/** 元输入 → 分，非法/空返回 null。旧版口径：BigDecimal ×100。 */
fun yuanToCents(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    return try {
        BigDecimal(trimmed).multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP).longValueExact()
    } catch (e: Exception) {
        null
    }
}

/** 当月起止（毫秒，本地时区，左闭右开）：[start, end) */
fun monthRange(year: Int, monthZeroBased: Int): Pair<Long, Long> {
    val startCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    return startCal.timeInMillis to endCal.timeInMillis
}

fun formatMonth(year: Int, monthZeroBased: Int): String = "%04d-%02d".format(year, monthZeroBased + 1)

fun formatDate(ms: Long?): String {
    if (ms == null) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
}

fun currentYearMonth(): Pair<Int, Int> {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
}
