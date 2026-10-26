package com.xiaogou.bookkeeping.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机接收器（RECEIVE_BOOT_COMPLETED 口径）：设备重启后重新调度
 * 每日 09:00 任务。WorkManager 的周期任务本身重启后会恢复，
 * 这里是显式兜底：开机就按当前时间重新算一次下个 09:00 的延迟。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleDailyWork(context)
        }
    }
}
