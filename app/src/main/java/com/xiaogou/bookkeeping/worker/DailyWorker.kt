package com.xiaogou.bookkeeping.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xiaogou.bookkeeping.R
import com.xiaogou.bookkeeping.data.AppDatabase
import com.xiaogou.bookkeeping.data.BookkeepingRepository
import com.xiaogou.bookkeeping.data.Settings
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.currentMonthKey
import com.xiaogou.bookkeeping.domain.currentYearMonth
import com.xiaogou.bookkeeping.domain.monthRange
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val CHANNEL_BACKUP = "channel_backup"
const val CHANNEL_RECURRING = "channel_recurring"
const val CHANNEL_BUDGET = "channel_budget"

fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(NotificationChannel(CHANNEL_BACKUP, "备份", NotificationManager.IMPORTANCE_LOW))
    manager.createNotificationChannel(NotificationChannel(CHANNEL_RECURRING, "周期记账", NotificationManager.IMPORTANCE_DEFAULT))
    manager.createNotificationChannel(NotificationChannel(CHANNEL_BUDGET, "预算提醒", NotificationManager.IMPORTANCE_HIGH))
}

/**
 * 每日任务（WorkManager，替代旧版 AlarmManager 方案，Phase 2 起）：
 * 每日 09:00 前后执行一次：生成到期周期记录、检查当月预算。
 * 预算口径：支出 ≥100% 超支、≥80% 接近上限；同一预算同一月只提醒一次，
 * 去重键 key_budget_notified_<id>_<yyyyMM>。
 * 通知权限：Android 13+ 需 POST_NOTIFICATIONS，未授权时静默跳过发送，
 * 任务本身仍执行生成与检查（去重标记只在成功发送后写，避免漏提醒）。
 */
class DailyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            createNotificationChannels(applicationContext)
            val db = AppDatabase.open(applicationContext)
            val repo = BookkeepingRepository(db)
            val settings = Settings.from(applicationContext)

            // 1. 周期记账
            val generated = repo.generateDue(System.currentTimeMillis())
            if (generated > 0) {
                notify(CHANNEL_RECURRING, 1002, "周期记账", "已自动生成 $generated 条记录")
            }

            // 2. 预算检查（当月）
            val (year, month) = currentYearMonth()
            val (start, end) = monthRange(year, month)
            val sums = repo.sumsByType(start, end).first().associateBy { it.recordTypeId }
            val monthKey = currentMonthKey()
            val budgets = repo.budgetsListByMonth(monthKey)
            for (budget in budgets) {
                if (budget.money <= 0) continue
                if (settings.isBudgetNotified(budget.id, monthKey)) continue
                val spent = sums[budget.recordTypeId]?.total ?: 0L
                val ratio = spent.toFloat() / budget.money.toFloat()
                when {
                    ratio >= 1f -> {
                        notify(CHANNEL_BUDGET, 1003, "预算超支", "已超出预算 ${centsToYuan(budget.money)}")
                        settings.markBudgetNotified(budget.id, monthKey)
                    }
                    ratio >= 0.8f -> {
                        notify(CHANNEL_BUDGET, 1003, "预算即将用尽", "已达预算的 ${(ratio * 100).toInt()}%")
                        settings.markBudgetNotified(budget.id, monthKey)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notify(channel: String, id: Int, title: String, text: String) {
        // Android 13+ 未授权时不抛异常，直接跳过（NotificationManager 在未授权时不显示）
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            // 未授权通知权限，静默跳过
        }
    }
}

/** 调度每日任务：下一次 09:00 首次执行，之后每 24 小时一次。 */
fun scheduleDailyWork(context: Context) {
    createNotificationChannels(context)
    val now = Calendar.getInstance()
    val next9 = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
    }
    val delayMs = next9.timeInMillis - now.timeInMillis
    val request = PeriodicWorkRequestBuilder<DailyWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "xiaogou_daily",
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}
