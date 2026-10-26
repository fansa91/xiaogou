package com.xiaogou.bookkeeping.data

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * 设置存储（SharedPreferences，键名与旧版口径一致）：
 * - key_app_lock_enabled：应用锁开关（Phase 2 新增，旧版锁是生物识别，
 *   本版用 PIN + 生物识别留待后续，PIN 哈希存 key_app_lock_pin_hash）
 * - key_budget_notified_<id>_<yyyyMM>：预算同月去重键
 * - key_web_dav_auto_backup 等 WebDAV 键在 Phase 3 接入时复用同一文件。
 */
class Settings(private val prefs: SharedPreferences) {
    companion object {
        fun from(context: Context): Settings =
            Settings(context.getSharedPreferences("xiaogou_settings", Context.MODE_PRIVATE))

        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    /** 主题色键，值如 theme_yellow / theme_green / theme_red / theme_blue / theme_cyan。 */
    var themeKey: String
        get() = prefs.getString("key_theme_name", "theme_yellow") ?: "theme_yellow"
        set(v) { prefs.edit().putString("key_theme_name", v).apply() }

    /** 夜间开关（源码 KEY_NIGHT_MODE 口径），默认开 + 跟随系统 = 当前行为不变。 */
    var nightMode: Boolean
        get() = prefs.getBoolean("key_night_mode", true)
        set(v) { prefs.edit().putBoolean("key_night_mode", v).apply() }

    /** 是否跟随系统（源码 KEY_NIGHT_MODE_BY_SYSTEM 口径）。 */
    var nightBySystem: Boolean
        get() = prefs.getBoolean("key_night_mode_by_system", true)
        set(v) { prefs.edit().putBoolean("key_night_mode_by_system", v).apply() }

    /** 夜间三档：0 跟随系统 / 1 白天 / 2 夜间（由上面两个键换算）。 */
    fun nightModeOption(): Int = when {
        nightBySystem -> 0
        !nightMode -> 1
        else -> 2
    }

    fun setNightModeOption(option: Int) {
        when (option) {
            0 -> { nightMode = true; nightBySystem = true }
            1 -> { nightMode = false; nightBySystem = false }
            2 -> { nightMode = true; nightBySystem = false }
        }
    }

    /** 夜间是否生效：开启夜间 且（不跟随系统 或 系统本身是夜间）。 */
    fun isNight(systemDark: Boolean): Boolean =
        nightMode && (!nightBySystem || systemDark)

    /** 金额前显示货币符号（源码 setting_currency 口径）。 */
    var currencySymbol: Boolean
        get() = prefs.getBoolean("key_currency_symbol", false)
        set(v) { prefs.edit().putBoolean("key_currency_symbol", v).apply() }

    /** 每月起始日（1–28，源码 setting_month_begin 口径）。 */
    var monthBeginDay: Int
        get() = prefs.getInt("key_month_begin_day", 1).coerceIn(1, 28)
        set(v) { prefs.edit().putInt("key_month_begin_day", v.coerceIn(1, 28)).apply() }

    /** 资产页显示饼图（源码 setting_assets_pie 口径）。 */
    var assetsPie: Boolean
        get() = prefs.getBoolean("key_assets_pie", true)
        set(v) { prefs.edit().putBoolean("key_assets_pie", v).apply() }

    /** 统计页显示饼图（源码 setting_pie 口径）。 */
    var statisticsPie: Boolean
        get() = prefs.getBoolean("key_statistics_pie", true)
        set(v) { prefs.edit().putBoolean("key_statistics_pie", v).apply() }

    var lockEnabled: Boolean
        get() = prefs.getBoolean("key_app_lock_enabled", false)
        set(v) { prefs.edit().putBoolean("key_app_lock_enabled", v).apply() }

    var pinHash: String?
        get() = prefs.getString("key_app_lock_pin_hash", null)
        set(v) { prefs.edit().putString("key_app_lock_pin_hash", v).apply() }

    fun isBudgetNotified(budgetId: Long, monthKey: Long): Boolean =
        prefs.getBoolean("key_budget_notified_${budgetId}_$monthKey", false)

    fun markBudgetNotified(budgetId: Long, monthKey: Long) {
        prefs.edit().putBoolean("key_budget_notified_${budgetId}_$monthKey", true).apply()
    }
}
