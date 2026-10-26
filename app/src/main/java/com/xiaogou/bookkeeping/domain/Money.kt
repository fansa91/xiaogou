package com.xiaogou.bookkeeping.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 显示口径（单 Activity 应用内的全局开关，由 MainActivity 从设置里同步）：
 * - currencySymbol：金额前加 ¥（源码 setting_currency 口径）
 * - monthBeginDay：每月起始日（1–28，源码 setting_month_begin 口径），
 *   月份窗口从该日算起，而不是每月 1 日；为 1 时与旧行为完全一致。
 */
object DisplayConfig {
    var currencySymbol: Boolean = false
    var monthBeginDay: Int = 1
}

/** 分 → 元显示，如 12345 分 → "123.45"，开货币符号时为 "¥123.45"。 */
fun centsToYuan(cents: Long): String {
    val value = BigDecimal(cents).divide(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP).toPlainString()
    return if (DisplayConfig.currencySymbol) "¥$value" else value
}

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

/** 当月起止（毫秒，本地时区，左闭右开）：[start, end)，起始日取 DisplayConfig。 */
fun monthRange(year: Int, monthZeroBased: Int): Pair<Long, Long> {
    val beginDay = DisplayConfig.monthBeginDay.coerceIn(1, 28)
    val startCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, beginDay)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    return startCal.timeInMillis to endCal.timeInMillis
}

fun formatMonth(year: Int, monthZeroBased: Int): String = "%04d-%02d".format(year, monthZeroBased + 1)

/** 星期文案：备份 layout_record_header 里星期小标签的口径（周日–周六）。 */
fun weekdayText(ms: Long?): String {
    if (ms == null) return ""
    val day = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_WEEK)
    return listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[day - 1]
}

fun formatDate(ms: Long?): String {
    if (ms == null) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
}

fun currentYearMonth(): Pair<Int, Int> {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
}
