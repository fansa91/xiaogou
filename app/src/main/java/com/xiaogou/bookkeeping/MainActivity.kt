package com.xiaogou.bookkeeping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xiaogou.bookkeeping.data.AppDatabase
import com.xiaogou.bookkeeping.data.BookkeepingRepository
import com.xiaogou.bookkeeping.data.Settings
import com.xiaogou.bookkeeping.domain.DisplayConfig
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.HomeViewModel
import com.xiaogou.bookkeeping.ui.assets.AssetsAddScreen
import com.xiaogou.bookkeeping.ui.assets.AssetsDetailScreen
import com.xiaogou.bookkeeping.ui.assets.AssetsScreen
import com.xiaogou.bookkeeping.ui.assets.TransferScreen
import com.xiaogou.bookkeeping.ui.home.HomeScreen
import com.xiaogou.bookkeeping.ui.phase2.BudgetAddScreen
import com.xiaogou.bookkeeping.ui.phase2.BudgetScreen
import com.xiaogou.bookkeeping.ui.phase2.LockScreen
import com.xiaogou.bookkeeping.ui.phase2.LockSetupScreen
import com.xiaogou.bookkeeping.ui.phase2.RecurringAddScreen
import com.xiaogou.bookkeeping.ui.phase2.RecurringScreen
import com.xiaogou.bookkeeping.ui.phase2.ReimburseAddScreen
import com.xiaogou.bookkeeping.ui.phase2.ReimburseScreen
import com.xiaogou.bookkeeping.ui.phase2.AllTypesScreen
import com.xiaogou.bookkeeping.ui.phase2.SettingsScreen
import com.xiaogou.bookkeeping.ui.phase2.StatisticsAdvancedScreen
import com.xiaogou.bookkeeping.ui.phase2.StatisticsHistScreen
import com.xiaogou.bookkeeping.ui.phase2.SubtypeScreen
import com.xiaogou.bookkeeping.ui.record.RecordAddScreen
import com.xiaogou.bookkeeping.ui.record.RecordEditScreen
import com.xiaogou.bookkeeping.ui.search.SearchScreen
import com.xiaogou.bookkeeping.ui.statistics.TypeDetailScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xiaogou.bookkeeping.ui.theme.NightMode
import com.xiaogou.bookkeeping.ui.theme.WalkTheme
import com.xiaogou.bookkeeping.ui.theme.WalkThemeId
import com.xiaogou.bookkeeping.ui.type.TypeAddScreen
import com.xiaogou.bookkeeping.ui.type.TypeManageScreen
import com.xiaogou.bookkeeping.worker.scheduleDailyWork
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Phase 2：单 Activity + Navigation。
 * 新增路由：budget / budget_add / recurring / recurring_add / reimburse /
 * reimburse_add / statistics_adv / subtype/{id} / settings / lock_setup / lock。
 * 应用锁：Settings 里开启且已设 PIN 时，启动先走 lock，解锁后进 home；
 * 每日任务在启动时调度一次（WorkManager，每日 09:00 前后）。
 */
class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onResume() {
        super.onResume()
        // 回到前台时刷新小部件，桌面上的今日 / 预算 / 饼图跟上最新账本。
        com.xiaogou.bookkeeping.ui.widget.refreshWidgets(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.open(applicationContext)
        val repo = BookkeepingRepository(db)
        val factory = AppViewModelFactory(repo)
        val settings = Settings.from(applicationContext)
        // 小部件点开时带 route（如 record_add），锁开启时仍先走锁，安全优先。
        // 通知权限（Android 13+）：预算 / 周期提醒要靠它，不申请就静默发不出。
        // 只在未授权时申请一次意图，系统弹窗由用户决定，拒绝也不影响记账。
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001,
            )
        }
        scheduleDailyWork(applicationContext)
        val widgetRoute = intent?.getStringExtra("route")
        val knownRoutes = setOf("home", "record_add", "assets", "budget", "statistics_adv")
        val start = if (settings.lockEnabled && !settings.pinHash.isNullOrBlank()) {
            "lock"
        } else if (widgetRoute in knownRoutes) {
            widgetRoute!!
        } else {
            "home"
        }
        setContent {
            // 主题状态提升到 Activity：设置页换色 / 换夜间模式后整棵树立即生效。
            var themeKey by remember { mutableStateOf(settings.themeKey) }
            var nightOption by remember { mutableStateOf(settings.nightModeOption()) }
            var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
            var monthBeginDay by remember { mutableStateOf(settings.monthBeginDay) }
            var assetsPie by remember { mutableStateOf(settings.assetsPie) }
            var statisticsPie by remember { mutableStateOf(settings.statisticsPie) }
            // 显示口径同步到全局（金额符号、月份起始日），首页/统计的月份窗口据此算。
            DisplayConfig.currencySymbol = currencySymbol
            DisplayConfig.monthBeginDay = monthBeginDay
            val darkTheme = when (nightOption) {
                NightMode.DAY -> false
                NightMode.NIGHT -> true
                else -> isSystemInDarkTheme()
            }
            WalkTheme(themeId = WalkThemeId.fromKey(themeKey), darkTheme = darkTheme) {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = start) {
                    composable("lock") {
                        LockScreen(settings = settings, onUnlocked = {
                            nav.navigate("home") { popUpTo("lock") { inclusive = true } }
                        })
                    }
                    composable("home") {
                        HomeScreen(
                            factory = factory,
                            onRecord = { nav.navigate("record_add") },
                            onAssets = { nav.navigate("assets") },
                            onTypes = { nav.navigate("types") },
                            onSearch = { nav.navigate("search") },
                            onTypeDetail = { id -> nav.navigate("type_detail/$id") },
                            onBudget = { nav.navigate("budget") },
                            onStatistics = { nav.navigate("statistics_adv") },
                            onSettings = { nav.navigate("settings") },
                            onEditRecord = { id -> nav.navigate("record_edit/$id") },
                        )
                    }
                    composable("record_add") {
                        RecordAddScreen(factory = factory, onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
                    }
                    composable("record_edit/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                        RecordEditScreen(factory = factory, recordId = id, onDone = { nav.popBackStack() })
                    }
                    composable("assets") {
                        AssetsScreen(
                            factory = factory,
                            onAdd = { nav.navigate("assets_add") },
                            onDetail = { id -> nav.navigate("assets_detail/$id") },
                            onTransfer = { nav.navigate("assets_transfer") },
                            showPie = assetsPie,
                        )
                    }
                    composable("assets_add") {
                        AssetsAddScreen(factory = factory, onDone = { nav.popBackStack() })
                    }
                    composable("assets_detail/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                        AssetsDetailScreen(factory = factory, assetsId = id, onDeleted = { nav.popBackStack() })
                    }
                    composable("assets_transfer") {
                        TransferScreen(factory = factory, onDone = { nav.popBackStack() })
                    }
                    composable("types") {
                        TypeManageScreen(factory = factory, onAdd = { nav.navigate("type_add") })
                    }
                    composable("type_add") {
                        TypeAddScreen(factory = factory, onDone = { nav.popBackStack() })
                    }
                    composable("search") {
                        SearchScreen(factory = factory)
                    }
                    composable("type_detail/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                        TypeDetailScreen(factory = factory, typeId = id)
                    }
                    composable("budget") {
                        val homeVm: HomeViewModel = viewModel(factory = factory)
                        BudgetScreen(factory = factory, homeFactoryVm = homeVm, onAdd = { nav.navigate("budget_add") })
                    }
                    composable("budget_add") {
                        BudgetAddScreen(factory = factory, onDone = { nav.popBackStack() })
                    }
                    composable("recurring") {
                        RecurringScreen(factory = factory, onAdd = { nav.navigate("recurring_add") })
                    }
                    composable("recurring_add") {
                        RecurringAddScreen(factory = factory, onDone = { nav.popBackStack() })
                    }
                    composable("reimburse") {
                        ReimburseScreen(factory = factory, onAdd = { nav.navigate("reimburse_add") })
                    }
                    composable("reimburse_add") {
                        ReimburseAddScreen(factory = factory, onDone = { nav.popBackStack() })
                    }
                    composable("statistics_adv") {
                        StatisticsAdvancedScreen(
                            factory = factory,
                            onSubtype = { id -> nav.navigate("subtype/$id") },
                            onAllTypes = { nav.navigate("all_types") },
                            onHist = { nav.navigate("statistics_hist") },
                            showPie = statisticsPie,
                        )
                    }
                    composable("all_types") {
                        AllTypesScreen(factory = factory, onSubtype = { id -> nav.navigate("subtype/$id") })
                    }
                    composable("statistics_hist") {
                        StatisticsHistScreen(factory = factory)
                    }
                    composable("subtype/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                        SubtypeScreen(factory = factory, parentId = id)
                    }
                    composable("settings") {
                        SettingsScreen(
                            settings = settings,
                            themeKey = themeKey,
                            nightOption = nightOption,
                            onThemeChange = { key ->
                                settings.themeKey = key
                                themeKey = key
                            },
                            onNightOptionChange = { option ->
                                settings.setNightModeOption(option)
                                nightOption = option
                            },
                            onBudget = { nav.navigate("budget") },
                            onRecurring = { nav.navigate("recurring") },
                            onReimburse = { nav.navigate("reimburse") },
                            onStatistics = { nav.navigate("statistics_adv") },
                            onLockSetup = { nav.navigate("lock_setup") },
                            onTypes = { nav.navigate("types") },
                            currencySymbol = currencySymbol,
                            onCurrencyChange = { v ->
                                settings.currencySymbol = v
                                currencySymbol = v
                            },
                            monthBeginDay = monthBeginDay,
                            onMonthBeginChange = { day ->
                                settings.monthBeginDay = day
                                monthBeginDay = day
                            },
                            assetsPie = assetsPie,
                            onAssetsPieChange = { v ->
                                settings.assetsPie = v
                                assetsPie = v
                            },
                            statisticsPie = statisticsPie,
                            onStatisticsPieChange = { v ->
                                settings.statisticsPie = v
                                statisticsPie = v
                            },
                            factory = factory,
                        )
                    }
                    composable("lock_setup") {
                        LockSetupScreen(settings = settings, onDone = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
