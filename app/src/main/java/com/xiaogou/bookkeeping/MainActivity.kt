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
import com.xiaogou.bookkeeping.ui.phase2.SettingsScreen
import com.xiaogou.bookkeeping.ui.phase2.StatisticsAdvancedScreen
import com.xiaogou.bookkeeping.ui.phase2.SubtypeScreen
import com.xiaogou.bookkeeping.ui.record.RecordAddScreen
import com.xiaogou.bookkeeping.ui.search.SearchScreen
import com.xiaogou.bookkeeping.ui.statistics.TypeDetailScreen
import com.xiaogou.bookkeeping.ui.theme.WalkTheme
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
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.open(applicationContext)
        val repo = BookkeepingRepository(db)
        val factory = AppViewModelFactory(repo)
        val settings = Settings.from(applicationContext)
        scheduleDailyWork(applicationContext)
        val start = if (settings.lockEnabled && !settings.pinHash.isNullOrBlank()) "lock" else "home"
        setContent {
            WalkTheme {
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
                        )
                    }
                    composable("record_add") {
                        RecordAddScreen(factory = factory, onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
                    }
                    composable("assets") {
                        AssetsScreen(
                            factory = factory,
                            onAdd = { nav.navigate("assets_add") },
                            onDetail = { id -> nav.navigate("assets_detail/$id") },
                            onTransfer = { nav.navigate("assets_transfer") },
                        )
                    }
                    composable("assets_add") {
                        AssetsAddScreen(factory = factory, onDone = { nav.popBackStack() })
                    }
                    composable("assets_detail/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                        AssetsDetailScreen(factory = factory, assetsId = id)
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
                        StatisticsAdvancedScreen(factory = factory, onSubtype = { id -> nav.navigate("subtype/$id") })
                    }
                    composable("subtype/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                        SubtypeScreen(factory = factory, parentId = id)
                    }
                    composable("settings") {
                        SettingsScreen(
                            settings = settings,
                            onBudget = { nav.navigate("budget") },
                            onRecurring = { nav.navigate("recurring") },
                            onReimburse = { nav.navigate("reimburse") },
                            onStatistics = { nav.navigate("statistics_adv") },
                            onLockSetup = { nav.navigate("lock_setup") },
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
