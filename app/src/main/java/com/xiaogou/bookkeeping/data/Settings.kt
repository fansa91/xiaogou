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
