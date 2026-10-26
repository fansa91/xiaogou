package com.xiaogou.bookkeeping.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import com.xiaogou.bookkeeping.MainActivity
import com.xiaogou.bookkeeping.R
import com.xiaogou.bookkeeping.data.AppDatabase
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.currentMonthKey
import com.xiaogou.bookkeeping.domain.monthRange
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 桌面小部件（旧版 WidgetFast / WidgetToday / WidgetRecord /
 * WidgetBudget / WidgetPie 口径，RemoteViews 实现，Compose 画不出来）：
 * - 快捷记账：点开直接进记账页。
 * - 今日：当天支出 / 收入 / 结余。
 * - 最新一笔：最近一条记录的分类、金额、账户。
 * - 预算：本月第一条预算的已用 / 剩余 / 进度。
 * - 饼图：本月支出前 5 分类的环形图（Canvas 画成 Bitmap）+ 图例。
 * 旧版 Today / Pie 各有一个配置页，本版不做配置页，统一用默认口径
 * （今日、本月支出），避免多两个 Activity 只为选一项。
 * 刷新：系统每天触发一次 onUpdate；应用回到前台时 MainActivity
 * 也会主动发一次更新广播，记完账回桌面就能看到新数。
 */
private fun openAppIntent(context: Context, route: String, requestCode: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("route", route)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        context, requestCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun todayRange(): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
    return start.timeInMillis to end.timeInMillis
}

private abstract class XiaogouWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.open(context)
                val views = buildViews(context, db)
                ids.forEach { id -> manager.updateAppWidget(id, views) }
            } catch (_: Exception) {
                // 小部件读库失败时保持旧画面，不崩桌面。
            } finally {
                pending.finish()
            }
        }
    }

    protected abstract suspend fun buildViews(context: Context, db: AppDatabase): RemoteViews
}

class WidgetFastProvider : XiaogouWidgetProvider() {
    override suspend fun buildViews(context: Context, db: AppDatabase): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_fast).apply {
            setOnClickPendingIntent(R.id.widget_fast_root, openAppIntent(context, "record_add", 1))
        }
    }
}

class WidgetTodayProvider : XiaogouWidgetProvider() {
    override suspend fun buildViews(context: Context, db: AppDatabase): RemoteViews {
        val (s, e) = todayRange()
        val sums = db.recordDao().sumsByType(s, e).first()
        val types = db.recordTypeDao().all().first().associateBy { it.id }
        var expense = 0L
        var income = 0L
        sums.forEach { sum ->
            if (types[sum.recordTypeId]?.type == 1) income += sum.total else expense += sum.total
        }
        return RemoteViews(context.packageName, R.layout.widget_today).apply {
            setTextViewText(R.id.widget_today_expense, "支出 ${centsToYuan(expense)}")
            setTextViewText(R.id.widget_today_income, "收入 ${centsToYuan(income)}")
            setTextViewText(R.id.widget_today_balance, "结余 ${centsToYuan(income - expense)}")
            setOnClickPendingIntent(R.id.widget_today_root, openAppIntent(context, "home", 2))
        }
    }
}

class WidgetRecordProvider : XiaogouWidgetProvider() {
    override suspend fun buildViews(context: Context, db: AppDatabase): RemoteViews {
        val now = System.currentTimeMillis()
        val latest = db.recordDao().recordsInRange(0, now).first().firstOrNull()
        val types = db.recordTypeDao().all().first().associateBy { it.id }
        val assets = db.assetsDao().all().first().associateBy { it.id }
        return RemoteViews(context.packageName, R.layout.widget_record).apply {
            if (latest == null) {
                setTextViewText(R.id.widget_record_type, "暂无记录")
                setTextViewText(R.id.widget_record_amount, "")
                setTextViewText(R.id.widget_record_asset, "")
            } else {
                setTextViewText(R.id.widget_record_type, types[latest.recordTypeId]?.name ?: "")
                setTextViewText(R.id.widget_record_amount, centsToYuan(latest.money ?: 0))
                setTextViewText(R.id.widget_record_asset, assets[latest.assetsId]?.name ?: "")
            }
            setOnClickPendingIntent(R.id.widget_record_root, openAppIntent(context, "record_add", 3))
        }
    }
}

class WidgetBudgetProvider : XiaogouWidgetProvider() {
    override suspend fun buildViews(context: Context, db: AppDatabase): RemoteViews {
        val cal = Calendar.getInstance()
        val (s, e) = monthRange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        val sums = db.recordDao().sumsByType(s, e).first()
        val spentByType = sums.associate { it.recordTypeId to it.total }
        val budget = db.budgetDao().listByMonth(currentMonthKey()).firstOrNull()
        val spent = budget?.let { spentByType[it.recordTypeId] ?: 0L } ?: 0L
        val total = budget?.money ?: 0L
        val remain = total - spent
        val percent = if (total > 0) (spent * 100 / total).toInt().coerceIn(0, 100) else 0
        return RemoteViews(context.packageName, R.layout.widget_budget).apply {
            setTextViewText(R.id.widget_budget_title, "本月预算")
            setTextViewText(R.id.widget_budget_spent, "已用 ${centsToYuan(spent)}")
            setTextViewText(R.id.widget_budget_remain, "剩余 ${centsToYuan(remain)}")
            setProgressBar(R.id.widget_budget_progress, 100, percent, false)
            setTextViewText(R.id.widget_budget_percent, "$percent%")
            setOnClickPendingIntent(R.id.widget_budget_root, openAppIntent(context, "budget", 4))
        }
    }
}

private val PIE_COLORS = listOf(
    Color.parseColor("#FEAD23"), Color.parseColor("#0F9D58"),
    Color.parseColor("#FE2542"), Color.parseColor("#2581FE"),
    Color.parseColor("#65C3AC"),
)

private fun drawDonut(values: List<Long>, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = size / 4f }
    val rect = RectF(size / 4f, size / 4f, size * 3 / 4f, size * 3 / 4f)
    val total = values.sum().takeIf { it > 0 } ?: 1L
    var start = -90f
    values.forEachIndexed { i, v ->
        paint.color = PIE_COLORS[i % PIE_COLORS.size]
        val sweep = v * 360f / total
        canvas.drawArc(rect, start, sweep, false, paint)
        start += sweep
    }
    return bitmap
}

class WidgetPieProvider : XiaogouWidgetProvider() {
    override suspend fun buildViews(context: Context, db: AppDatabase): RemoteViews {
        val cal = Calendar.getInstance()
        val (s, e) = monthRange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        val sums = db.recordDao().sumsByType(s, e).first()
        val types = db.recordTypeDao().all().first().associateBy { it.id }
        val top = sums.filter { types[it.recordTypeId]?.type != 1 }
            .sortedByDescending { it.total }.take(5)
        val total = top.sumOf { it.total }.takeIf { it > 0 } ?: 1L
        val rowIds = listOf(
            R.id.widget_pie_row1, R.id.widget_pie_row2, R.id.widget_pie_row3,
            R.id.widget_pie_row4, R.id.widget_pie_row5,
        )
        return RemoteViews(context.packageName, R.layout.widget_pie).apply {
            setImageViewBitmap(R.id.widget_pie_image, drawDonut(top.map { it.total }, 220))
            rowIds.forEachIndexed { i, id ->
                val item = top.getOrNull(i)
                setTextViewText(
                    id,
                    if (item == null) "" else
                        "${types[item.recordTypeId]?.name ?: ""} ${item.total * 100 / total}%",
                )
            }
            setOnClickPendingIntent(R.id.widget_pie_root, openAppIntent(context, "statistics_adv", 5))
        }
    }
}

/** 应用回到前台时刷新全部小部件（记完账回桌面就能看到新数）。 */
fun refreshWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val providers = listOf(
        WidgetFastProvider::class.java, WidgetTodayProvider::class.java,
        WidgetRecordProvider::class.java, WidgetBudgetProvider::class.java,
        WidgetPieProvider::class.java,
    )
    providers.forEach { clazz ->
        val ids = manager.getAppWidgetIds(ComponentName(context, clazz))
        if (ids.isNotEmpty()) {
            context.sendBroadcast(
                Intent(context, clazz).apply { action = AppWidgetManager.ACTION_APPWIDGET_UPDATE; putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids) },
            )
        }
    }
}
