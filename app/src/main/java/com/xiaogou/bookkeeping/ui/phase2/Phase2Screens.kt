package com.xiaogou.bookkeeping.ui.phase2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.data.Settings
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.currentMonthKey
import com.xiaogou.bookkeeping.domain.formatDate
import com.xiaogou.bookkeeping.domain.yuanToCents
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.BudgetViewModel
import com.xiaogou.bookkeeping.ui.HomeViewModel
import com.xiaogou.bookkeeping.ui.RecurringViewModel
import com.xiaogou.bookkeeping.ui.ReimburseViewModel
import com.xiaogou.bookkeeping.ui.StatisticsViewModel
import com.xiaogou.bookkeeping.ui.home.RecordRow
import com.xiaogou.bookkeeping.ui.theme.PieChart
import com.xiaogou.bookkeeping.ui.theme.SettingsRow
import com.xiaogou.bookkeeping.ui.theme.WalkDimens

// ---------- 预算 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(factory: AppViewModelFactory, homeFactoryVm: HomeViewModel, onAdd: () -> Unit) {
    // homeFactoryVm 复用首页的当月汇总（按分类合计），与预算对比算比例
    val vm: BudgetViewModel = viewModel(factory = factory)
    val budgets by vm.budgets.collectAsState()
    val sums by homeFactoryVm.sums.collectAsState()
    val types by homeFactoryVm.types.collectAsState()
    val typeById = types.associateBy { it.id }
    val spentByType = sums.associate { it.recordTypeId to it.total }
    var detailBudget by remember { mutableStateOf<com.xiaogou.bookkeeping.data.entity.Budget?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("预算") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("新增本月预算") } }
            items(budgets) { b ->
                val spent = spentByType[b.recordTypeId] ?: 0L
                val ratio = if (b.money > 0) spent.toFloat() / b.money.toFloat() else 0f
                val status = when {
                    ratio >= 1f -> "超支"
                    ratio >= 0.8f -> "即将用尽"
                    else -> "正常"
                }
                Card(modifier = Modifier.fillMaxWidth().clickable { detailBudget = b }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${typeById[b.recordTypeId]?.name ?: ""} · $status")
                        Text("已用 ${centsToYuan(spent)} / 预算 ${centsToYuan(b.money)}")
                        LinearProgressIndicator(progress = { ratio.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        Button(onClick = { vm.delete(b) }) { Text("删除") }
                    }
                }
            }
            if (budgets.isEmpty()) item { Text("本月暂无预算，点上面新增。提醒口径：80% 即将用尽、100% 超支，同月同预算只提醒一次。") }
        }
    }
    detailBudget?.let { b ->
        val spent = spentByType[b.recordTypeId] ?: 0L
        val remaining = b.money - spent
        val cal = java.util.Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val daysLeft = (daysInMonth - cal.get(java.util.Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
        val daily = remaining / daysLeft
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { detailBudget = null },
            title = { Text("${typeById[b.recordTypeId]?.name ?: ""} · 预算详情") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("预算总额 ${centsToYuan(b.money)}")
                    Text("已用 ${centsToYuan(spent)}")
                    Text("剩余 ${centsToYuan(remaining)}")
                    Text("可日均 ${centsToYuan(daily)}")
                    Text("剩余天数 $daysLeft 天")
                }
            },
            confirmButton = {
                TextButton(onClick = { detailBudget = null }) { Text("知道了") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetAddScreen(factory: AppViewModelFactory, onDone: () -> Unit) {
    val vm: BudgetViewModel = viewModel(factory = factory)
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val types by homeVm.types.collectAsState()
    val expenseTypes = types.filter { it.type == 0 && it.state == 1 }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    if (selectedId == null && expenseTypes.isNotEmpty()) selectedId = expenseTypes.first().id
    Scaffold(topBar = { TopAppBar(title = { Text("新增预算") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("月份：${currentMonthKey()}（本月）")
            expenseTypes.forEach { t ->
                Text(t.name ?: "", modifier = Modifier.fillMaxWidth().clickable { selectedId = t.id }.padding(8.dp))
            }
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("预算金额（元）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it) }
            Button(onClick = {
                val cents = yuanToCents(amount)
                val id = selectedId
                when {
                    id == null -> error = "请先选择分类"
                    cents == null || cents <= 0 -> error = "请填写大于 0 的金额"
                    else -> vm.add(id, cents, remark, onDone)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        }
    }
}

// ---------- 周期记账 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(factory: AppViewModelFactory, onAdd: () -> Unit) {
    val vm: RecurringViewModel = viewModel(factory = factory)
    val rules by vm.rules.collectAsState()
    var result by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("周期记账") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("新增规则") }
                Button(onClick = { vm.generateDue { n -> result = "已生成 $n 条到期记录" } }, modifier = Modifier.fillMaxWidth()) { Text("立即生成到期记录") }
                result?.let { Text(it) }
            }
            items(rules) { r ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${r.remark ?: ""} · ${centsToYuan(r.money)} · 周期 ${r.cycleType} / ${r.cycleValue}")
                        Text("下次 ${formatDate(r.nextTime)} · ${if (r.state == 0) "启用" else "暂停"}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.toggle(r) }) { Text(if (r.state == 0) "暂停" else "启用") }
                            Button(onClick = { vm.delete(r) }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringAddScreen(factory: AppViewModelFactory, onDone: () -> Unit) {
    val vm: RecurringViewModel = viewModel(factory = factory)
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val types by homeVm.types.collectAsState()
    val assets by homeVm.assets.collectAsState()
    var isIncome by remember { mutableStateOf(false) }
    val filtered = types.filter { it.type == (if (isIncome) 1 else 0) && it.state == 1 }
    var typeId by remember { mutableStateOf<Long?>(null) }
    var assetsId by remember { mutableStateOf<Long>(-1) }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var cycleType by remember { mutableStateOf(0) }
    var cycleValue by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    if (typeId == null && filtered.isNotEmpty()) typeId = filtered.first().id
    Scaffold(topBar = { TopAppBar(title = { Text("新增周期规则") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isIncome, onClick = { isIncome = false; typeId = null }, label = { Text("支出") })
                FilterChip(selected = isIncome, onClick = { isIncome = true; typeId = null }, label = { Text("收入") })
            }
            filtered.forEach { t ->
                Text(t.name ?: "", modifier = Modifier.fillMaxWidth().clickable { typeId = t.id }.padding(8.dp))
            }
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额（元）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("周期：0 每天 / 1 每周（1=周日…7=周六）/ 2 每月（几号）/ 3 每年（Mdd，如 1225）")
            OutlinedTextField(value = cycleType.toString(), onValueChange = { cycleType = it.toIntOrNull() ?: 0 }, label = { Text("周期类型") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = cycleValue, onValueChange = { cycleValue = it }, label = { Text("周期值") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (assetsId >= 0) assets.firstOrNull { it.id == assetsId }?.name ?: "选择账户" else "不计入账户")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("不计入账户") }, onClick = { assetsId = -1; menu = false })
                assets.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { assetsId = a.id; menu = false }) }
            }
            error?.let { Text(it) }
            Button(onClick = {
                val cents = yuanToCents(amount)
                val tid = typeId
                val cv = cycleValue.toIntOrNull()
                when {
                    tid == null -> error = "请先选择分类"
                    cents == null || cents <= 0 -> error = "请填写大于 0 的金额"
                    cv == null -> error = "周期值格式不正确"
                    else -> vm.add(cents, tid, if (isIncome) 1 else 0, assetsId, remark, cycleType, cv, null, onDone)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存（首次执行为下一个满足日的 09:00）") }
        }
    }
}

// ---------- 报销 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReimburseScreen(factory: AppViewModelFactory, onAdd: () -> Unit) {
    val vm: ReimburseViewModel = viewModel(factory = factory)
    val items by vm.items.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("报销") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("新增报销") } }
            items(items) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${item.remark ?: ""} · 金额 ${centsToYuan(item.money)} · 已报 ${centsToYuan(item.reimburseMoney)}")
                        Text(if (item.reimburseState == 1) "已报销" else "未报销")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.reimburseState == 0) Button(onClick = { vm.markReimbursed(item) }) { Text("标记已报销") }
                            if (item.reimburseState == 1) Button(onClick = { vm.undoReimbursed(item) }) { Text("撤销已报销") }
                            Button(onClick = { vm.delete(item) }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReimburseAddScreen(factory: AppViewModelFactory, onDone: () -> Unit) {
    val vm: ReimburseViewModel = viewModel(factory = factory)
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val types by homeVm.types.collectAsState()
    val assets by homeVm.assets.collectAsState()
    val expenseTypes = types.filter { it.type == 0 && it.state == 1 }
    var typeId by remember { mutableStateOf<Long?>(null) }
    var assetsId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    if (typeId == null && expenseTypes.isNotEmpty()) typeId = expenseTypes.first().id
    if (assetsId == null && assets.isNotEmpty()) assetsId = assets.first().id
    Scaffold(topBar = { TopAppBar(title = { Text("新增报销") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            expenseTypes.forEach { t -> Text(t.name ?: "", modifier = Modifier.fillMaxWidth().clickable { typeId = t.id }.padding(8.dp)) }
            assets.forEach { a -> Text("账户：${a.name}", modifier = Modifier.fillMaxWidth().clickable { assetsId = a.id }.padding(8.dp)) }
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额（元）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it) }
            Button(onClick = {
                val cents = yuanToCents(amount)
                val tid = typeId
                val aid = assetsId
                when {
                    tid == null || aid == null -> error = "请先选择分类与账户"
                    cents == null || cents <= 0 -> error = "请填写大于 0 的金额"
                    else -> vm.add(cents, tid, aid, remark, onDone)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        }
    }
}

// ---------- 统计：区块列表（fragment_statistics 口径） ----------
// 不是一张图，而是一列区块：饼图+排行、柱状图、折线图、全部类型。
// 饼图 120dp 环形（外 60 / 内 20），柱状/折线 118dp、底部 18dp 轴区；
// 柱状与折线都有支出/收入切换（源码里两个容器叠在同一位置，点切换只显示其一）。
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsAdvancedScreen(
    factory: AppViewModelFactory,
    onSubtype: (Long) -> Unit,
    onAllTypes: () -> Unit = {},
    onHist: () -> Unit = {},
    showPie: Boolean = true,
) {
    val vm: StatisticsViewModel = viewModel(factory = factory)
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val records by vm.recentRecords.collectAsState(initial = emptyList())
    val types by homeVm.types.collectAsState()
    val typeById = types.associateBy { it.id }
    var histIncome by remember { mutableStateOf(false) }
    var curveIncome by remember { mutableStateOf(false) }
    var showRankSheet by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }
    // 年月选择器：默认当月，可前后翻月；饼图按所选月份算，不再只算当月。
    val nowCal = java.util.Calendar.getInstance()
    var selYear by remember { mutableStateOf(nowCal.get(java.util.Calendar.YEAR)) }
    var selMonth by remember { mutableStateOf(nowCal.get(java.util.Calendar.MONTH)) }
    fun shiftMonth(delta: Int) {
        val cal = java.util.Calendar.getInstance().apply { set(selYear, selMonth, 1); add(java.util.Calendar.MONTH, delta) }
        selYear = cal.get(java.util.Calendar.YEAR)
        selMonth = cal.get(java.util.Calendar.MONTH)
    }
    val pieColors = listOf(
        androidx.compose.ui.graphics.Color(0xFFFEAD23),
        androidx.compose.ui.graphics.Color(0xFF0F9D58),
        androidx.compose.ui.graphics.Color(0xFFFE2542),
        androidx.compose.ui.graphics.Color(0xFF2581FE),
        androidx.compose.ui.graphics.Color(0xFF65C3AC),
    )
    // 按年月分组：yyyy-MM → 支出/收入
    // 性能：这些派生数据以前每次重组都对近 6 个月记录全量 groupBy /
    // filter / sum，点切换、滑屏都重算；现在按输入缓存，只在记录、
    // 分类、所选月份变时重算。
    val byMonth = remember(records) {
        records.groupBy {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.time ?: 0 }
            "%04d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
        }.toSortedMap()
    }
    val monthKeys = remember(byMonth) { byMonth.keys.toList() }
    val monthExpense = remember(byMonth, typeById) {
        monthKeys.map { k ->
            byMonth[k]?.filter { typeById[it.recordTypeId]?.type != 1 }?.sumOf { it.money ?: 0 } ?: 0L
        }
    }
    val monthIncome = remember(byMonth, typeById) {
        monthKeys.map { k ->
            byMonth[k]?.filter { typeById[it.recordTypeId]?.type == 1 }?.sumOf { it.money ?: 0 } ?: 0L
        }
    }
    val thisMonth = "%04d%02d".format(selYear, selMonth + 1)
    val monthSums = remember(records, typeById, thisMonth) {
        records.filter {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.time ?: 0 }
            "%04d%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1) == thisMonth
        }.groupBy { it.recordTypeId }.map { (id, list) ->
            (typeById[id]?.name ?: "") to list.sumOf { it.money ?: 0 }
        }.filter { it.second > 0 }
    }
    val monthTotal = remember(monthSums) { monthSums.sumOf { it.second }.takeIf { it > 0 } ?: 1L }
    Scaffold(topBar = { TopAppBar(title = { Text("统计") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(40.dp)) {
            item {
                com.xiaogou.bookkeeping.ui.theme.StatisticsBlockHeader("分类占比")
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { shiftMonth(-1) }) { Text("‹ 上月") }
                    TextButton(onClick = { showYearDialog = true }) { Text("${selYear}年${selMonth + 1}月 ▾") }
                    TextButton(onClick = { shiftMonth(1) }) { Text("下月 ›") }
                }
                if (showPie) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        if (monthSums.isNotEmpty()) {
                            PieChart(values = monthSums, colors = pieColors)
                            Column(modifier = Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                monthSums.forEachIndexed { i, (name, v) ->
                                    Text("$name ${v * 100 / monthTotal}% · ${centsToYuan(v)}")
                                }
                            }
                        } else {
                            Text("所选月份暂无记录")
                        }
                    }
                }
                monthSums.forEach { (name, v) ->
                    com.xiaogou.bookkeeping.ui.theme.RankRow(name = name, value = v, total = monthTotal, label = centsToYuan(v))
                }
                Button(onClick = { showRankSheet = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("查看排行（底部表）")
                }
            }
            item {
                com.xiaogou.bookkeeping.ui.theme.StatisticsBlockHeader("收支柱状", onMore = onHist)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { histIncome = false }) { Text("支出") }
                    OutlinedButton(onClick = { histIncome = true }) { Text("收入") }
                }
                if (monthKeys.isNotEmpty()) {
                    com.xiaogou.bookkeeping.ui.theme.HistBarChart(
                        modifier = Modifier.padding(top = 16.dp),
                        values = if (histIncome) monthIncome else monthExpense,
                        color = if (histIncome) androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        monthKeys.forEach { Text(it.takeLast(2), fontSize = 12.sp) }
                    }
                } else {
                    Text("暂无趋势数据")
                }
            }
            item {
                com.xiaogou.bookkeeping.ui.theme.StatisticsBlockHeader("收支曲线")
                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { curveIncome = false }) { Text("支出") }
                    OutlinedButton(onClick = { curveIncome = true }) { Text("收入") }
                }
                if (monthKeys.size >= 2) {
                    com.xiaogou.bookkeeping.ui.theme.CurveChart(
                        modifier = Modifier.padding(top = 16.dp),
                        values = if (curveIncome) monthIncome else monthExpense,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    )
                    // 曲线以前没有横轴，点对应哪个月只能猜；补上月份轴，
                    // 与柱状图下面那行月份标签同一口径（取后两位）。
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        monthKeys.forEach { Text(it.takeLast(2), fontSize = 12.sp) }
                    }
                } else {
                    Text("至少 2 个月才有曲线")
                }
            }
            // 趋势行原来每行都是 LazyColumn 的一个 item，区块间距 40dp
            // 会直接落在行与行之间、显得过散；收进一个 item 里，
            // 行间距单独用 12dp，区块之间还是 40dp。
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("最近 6 个月趋势")
                    byMonth.forEach { (month, list) ->
                        var exp = 0L
                        var inc = 0L
                        list.forEach { r ->
                            if (typeById[r.recordTypeId]?.type == 1) inc += r.money ?: 0 else exp += r.money ?: 0
                        }
                        Text("$month · 支出 ${centsToYuan(exp)} · 收入 ${centsToYuan(inc)}")
                    }
                }
            }
            item {
                Button(onClick = onAllTypes, modifier = Modifier.fillMaxWidth()) { Text("全部类型（按支出 / 收入分组）") }
            }
        }
    }
    if (showYearDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showYearDialog = false },
            title = { Text("选择年份") },
            text = {
                Column {
                    val years = (records.mapNotNull {
                        java.util.Calendar.getInstance().apply { timeInMillis = it.time ?: 0 }.get(java.util.Calendar.YEAR)
                    } + nowCal.get(java.util.Calendar.YEAR)).distinct().sorted()
                    years.forEach { y ->
                        Text(
                            "$y 年",
                            modifier = Modifier.fillMaxWidth().clickable { selYear = y; showYearDialog = false }.padding(vertical = 12.dp),
                        )
                    }
                    if (years.isEmpty()) Text("${nowCal.get(java.util.Calendar.YEAR)} 年")
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showYearDialog = false }) { Text("取消") }
            },
        )
    }
    if (showRankSheet) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showRankSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, bottom = 30.dp)) {
                Text("排行", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                monthSums.forEach { (name, v) ->
                    com.xiaogou.bookkeeping.ui.theme.RankRow(name = name, value = v, total = monthTotal, label = centsToYuan(v))
                }
                if (monthSums.isEmpty()) Text("所选月份暂无记录")
            }
        }
    }
}

// ---------- 全部类型（AllTypeActivity 口径）：支出 / 收入分组，点进子类型 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTypesScreen(factory: AppViewModelFactory, onSubtype: (Long) -> Unit) {
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val types by homeVm.types.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("全部类型") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(0 to "支出", 1 to "收入").forEach { (type, label) ->
                item { Text(label, fontSize = 20.sp) }
                items(types.filter { it.parentId == -1L && it.type == type }) { t ->
                    Text(t.name ?: "", modifier = Modifier.fillMaxWidth().clickable { onSubtype(t.id) }.padding(8.dp))
                }
            }
        }
    }
}

// ---------- 柱状全屏页（StatisticsHistActivity 口径）：月份柱状 + 支出/收入切换 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsHistScreen(factory: AppViewModelFactory) {
    val vm: StatisticsViewModel = viewModel(factory = factory)
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val records by vm.recentRecords.collectAsState(initial = emptyList())
    val types by homeVm.types.collectAsState()
    val typeById = types.associateBy { it.id }
    var histIncome by remember { mutableStateOf(false) }
    val byMonth = records.groupBy {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.time ?: 0 }
        "%04d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
    }.toSortedMap()
    val monthKeys = byMonth.keys.toList()
    val monthExpense = monthKeys.map { k ->
        byMonth[k]?.filter { typeById[it.recordTypeId]?.type != 1 }?.sumOf { it.money ?: 0 } ?: 0L
    }
    val monthIncome = monthKeys.map { k ->
        byMonth[k]?.filter { typeById[it.recordTypeId]?.type == 1 }?.sumOf { it.money ?: 0 } ?: 0L
    }
    Scaffold(topBar = { TopAppBar(title = { Text("收支柱状") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { histIncome = false }) { Text("支出") }
                OutlinedButton(onClick = { histIncome = true }) { Text("收入") }
            }
            if (monthKeys.isNotEmpty()) {
                com.xiaogou.bookkeeping.ui.theme.HistBarChart(
                    values = if (histIncome) monthIncome else monthExpense,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                )
                monthKeys.forEachIndexed { i, k ->
                    Text("$k · ${(if (histIncome) monthIncome else monthExpense)[i].let { centsToYuan(it) }}")
                }
            } else {
                Text("暂无趋势数据")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtypeScreen(factory: AppViewModelFactory, parentId: Long) {
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val typeVm: com.xiaogou.bookkeeping.ui.TypeViewModel = viewModel(factory = factory)
    val types by homeVm.types.collectAsState()
    val parent = types.firstOrNull { it.id == parentId }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var added by remember { mutableStateOf(false) }
    val sums by homeVm.sums.collectAsState()
    val subtypeIds = types.filter { it.parentId == parentId }.map { it.id }.toSet()
    val subtypeSums = sums.filter { it.recordTypeId in subtypeIds }.map { s ->
        (types.firstOrNull { it.id == s.recordTypeId }?.name ?: "") to s.total
    }.filter { it.second > 0 }
    val subtypeTotal = subtypeSums.sumOf { it.second }
    Scaffold(topBar = { TopAppBar(title = { Text("${parent?.name ?: ""} · 子类型") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                // 子类型饼图（statistics_subtype_pie 口径）：120dp 环形饼 + 右下合计行。
                if (subtypeSums.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PieChart(
                            values = subtypeSums,
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFFFEAD23),
                                androidx.compose.ui.graphics.Color(0xFF0F9D58),
                                androidx.compose.ui.graphics.Color(0xFFFE2542),
                                androidx.compose.ui.graphics.Color(0xFF2581FE),
                                androidx.compose.ui.graphics.Color(0xFF65C3AC),
                            ),
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                            subtypeSums.forEach { (n, v) ->
                                Text("$n ${v * 100 / subtypeTotal.coerceAtLeast(1)}% · ${centsToYuan(v)}")
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("合计 ${centsToYuan(subtypeTotal)}")
                    }
                } else {
                    Text("本月该分类下暂无子类型记录")
                }
            }
            item {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("子类型名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                error?.let { Text(it) }
                Button(onClick = {
                    val p = parent
                    when {
                        p == null -> error = "父分类不存在"
                        name.isBlank() -> error = "请填写子类型名"
                        else -> typeVm.addSubtype(parentId, p.type, name) { added = true; name = "" }
                    }
                }) { Text("新增子类型") }
                if (added) Text("已添加")
            }
            items(types.filter { it.parentId == parentId }) { t ->
                Text(t.name ?: "")
            }
        }
    }
}

// ---------- 设置与应用锁 ----------
// 源码骨架（setting_item_* 口径）：每行先是一条 1dp 分割线（左缩进 50dp，
// 上下各 8dp），再是 40dp 深色圆角图标位 + 标题 + 12sp 副标题 + 右侧控件。
// 主题在行内直接选（setting_item_theme：一行 5 个 40dp 色点）；
// 夜间模式是弹窗三选（dialog_dark_mode：白天 / 夜间 / 跟随系统）。
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    themeKey: String = "theme_yellow",
    nightOption: Int = 0,
    onThemeChange: (String) -> Unit = {},
    onNightOptionChange: (Int) -> Unit = {},
    onBudget: () -> Unit,
    onRecurring: () -> Unit,
    onReimburse: () -> Unit,
    onStatistics: () -> Unit,
    onLockSetup: () -> Unit,
    onTypes: () -> Unit = {},
    currencySymbol: Boolean = false,
    onCurrencyChange: (Boolean) -> Unit = {},
    monthBeginDay: Int = 1,
    onMonthBeginChange: (Int) -> Unit = {},
    assetsPie: Boolean = true,
    onAssetsPieChange: (Boolean) -> Unit = {},
    statisticsPie: Boolean = true,
    onStatisticsPieChange: (Boolean) -> Unit = {},
    factory: com.xiaogou.bookkeeping.ui.AppViewModelFactory? = null,
) {
    var lockOn by remember { mutableStateOf(settings.lockEnabled) }
    var showNightDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showMonthDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var monthInput by remember { mutableStateOf(monthBeginDay.toString()) }
    val themeLabel = com.xiaogou.bookkeeping.ui.theme.WalkThemeId.fromKey(themeKey).label
    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                // banner_setting 口径：标题区固定 166dp 高，只放 32sp 大标题。
                Box(modifier = Modifier.fillMaxWidth().height(166.dp), contentAlignment = Alignment.TopStart) {
                    Text("设置", fontSize = 32.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
            item {
                SettingsRow(icon = { Text("色") }, title = "主题颜色", subtitle = themeLabel, onClick = {})
                com.xiaogou.bookkeeping.ui.theme.ThemeColorPicker(selectedKey = themeKey, onSelect = onThemeChange)
                SettingsRow(
                    icon = { Text("夜") },
                    title = "夜间模式",
                    subtitle = com.xiaogou.bookkeeping.ui.theme.NightMode.labels.getOrElse(nightOption) { "跟随系统" },
                    onClick = { showNightDialog = true },
                )
            }
            item {
                SettingsRow(icon = { Text("预") }, title = "预算", subtitle = "本月预算与提醒", onClick = onBudget)
                SettingsRow(icon = { Text("周") }, title = "周期记账", subtitle = "每天 / 每周 / 每月 / 每年自动记", onClick = onRecurring)
                SettingsRow(icon = { Text("报") }, title = "报销", subtitle = "待报销与已报销", onClick = onReimburse)
                SettingsRow(icon = { Text("统") }, title = "统计", subtitle = "分类占比、柱状、曲线", onClick = onStatistics)
                SettingsRow(icon = { Text("类") }, title = "分类管理", subtitle = "新增、改名、开关分类", onClick = onTypes)
            }
            item {
                com.xiaogou.bookkeeping.ui.theme.SettingsSwitchRow(
                    icon = { Text("¥") },
                    title = "货币符号",
                    subtitle = "金额前显示 ¥",
                    checked = currencySymbol,
                    onCheckedChange = onCurrencyChange,
                )
                SettingsRow(
                    icon = { Text("月") },
                    title = "每月起始日",
                    subtitle = "每月 $monthBeginDay 日起算（1–28）",
                    onClick = { monthInput = monthBeginDay.toString(); showMonthDialog = true },
                )
                com.xiaogou.bookkeeping.ui.theme.SettingsSwitchRow(
                    icon = { Text("饼") },
                    title = "资产饼图",
                    subtitle = "资产页显示饼图",
                    checked = assetsPie,
                    onCheckedChange = onAssetsPieChange,
                )
                com.xiaogou.bookkeeping.ui.theme.SettingsSwitchRow(
                    icon = { Text("图") },
                    title = "统计饼图",
                    subtitle = "统计页显示饼图",
                    checked = statisticsPie,
                    onCheckedChange = onStatisticsPieChange,
                )
            }
            item {
                if (factory != null) {
                    SettingsRow(icon = { Text("导") }, title = "账单导出", subtitle = "导出为 CSV 文件并发送", onClick = { showExportDialog = true })
                }
                SettingsRow(icon = { Text("关") }, title = "关于", subtitle = "小狗记账 0.4", onClick = { showAboutDialog = true })
                SettingsRow(icon = { Text("私") }, title = "隐私政策", subtitle = "数据只存在本机", onClick = { showPrivacyDialog = true })
            }
            item {
                com.xiaogou.bookkeeping.ui.theme.SettingsSwitchRow(
                    icon = { Text("锁") },
                    title = "应用锁（PIN）",
                    subtitle = "开启后每次打开应用需输入 PIN",
                    checked = lockOn,
                    onCheckedChange = {
                        if (it && settings.pinHash.isNullOrBlank()) {
                            onLockSetup()
                        } else {
                            lockOn = it
                            settings.lockEnabled = it
                        }
                    },
                )
                Button(onClick = onLockSetup, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("设置 / 修改 PIN") }
                Text(
                    "开启后每次打开应用需输入 PIN；失败超过 3 次会提示。PIN 只存 SHA-256 哈希，不存明文。",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
    if (showNightDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNightDialog = false },
            title = { Text("夜间模式") },
            text = {
                Column {
                    com.xiaogou.bookkeeping.ui.theme.NightMode.labels.forEachIndexed { i, label ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onNightOptionChange(i)
                                showNightDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = nightOption == i,
                                onClick = {
                                    onNightOptionChange(i)
                                    showNightDialog = false
                                },
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNightDialog = false }) { Text("取消") }
            },
        )
    }
    if (showMonthDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showMonthDialog = false },
            title = { Text("每月起始日") },
            text = {
                Column {
                    Text("输入 1–28 之间的日期，月份窗口从该日起算。")
                    OutlinedTextField(
                        value = monthInput,
                        onValueChange = { monthInput = it },
                        label = { Text("起始日") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val day = monthInput.toIntOrNull()?.coerceIn(1, 28)
                    if (day != null) {
                        onMonthBeginChange(day)
                        showMonthDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showMonthDialog = false }) { Text("取消") }
            },
        )
    }
    if (showAboutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于小狗记账") },
            text = { Text("小狗记账 0.4\n记一笔收支、管理多个账户、按分类做月预算、看月度与分类统计；支持周期记账与预算提醒。") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("知道了") }
            },
        )
    }
    if (showPrivacyDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("隐私政策") },
            text = {
                Text(
                    "小狗记账的账本数据只保存在本机数据库中，不上传到任何服务器；" +
                        "应用锁 PIN 只以哈希形式存在本机；未来若开启 WebDAV 备份，" +
                        "备份文件在上传前会先在本机加密。",
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("知道了") }
            },
        )
    }
    if (showExportDialog && factory != null) {
        ExportDialog(factory = factory, onDismiss = { showExportDialog = false })
    }
}

/**
 * 账单导出弹窗（dialog_export_mode / dialog_export_time 口径）：
 * 全部导出，或按起止日期导出；生成 CSV 写缓存后用系统分享发送，
 * 文件本身不上传、不进相册。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDialog(factory: com.xiaogou.bookkeeping.ui.AppViewModelFactory, onDismiss: () -> Unit) {
    val vm: com.xiaogou.bookkeeping.ui.ExportViewModel = viewModel(factory = factory)
    val records by vm.allRecords.collectAsState()
    val types by vm.types.collectAsState()
    val assets by vm.assets.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var byTime by remember { mutableStateOf(false) }
    var startMs by remember {
        mutableStateOf(
            java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1) }.timeInMillis,
        )
    }
    var endMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var picking by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("账单导出") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { byTime = false }) { Text("导出全部") }
                    OutlinedButton(onClick = { byTime = true }) { Text("按时间导出") }
                }
                if (byTime) {
                    OutlinedButton(onClick = { picking = "start" }, modifier = Modifier.fillMaxWidth()) {
                        Text("开始 ${com.xiaogou.bookkeeping.domain.formatDate(startMs)}")
                    }
                    OutlinedButton(onClick = { picking = "end" }, modifier = Modifier.fillMaxWidth()) {
                        Text("结束 ${com.xiaogou.bookkeeping.domain.formatDate(endMs)}")
                    }
                }
                result?.let { Text(it) }
                if (records.isEmpty()) Text("暂无可导出的账单。")
            }
        },
        confirmButton = {
            Button(onClick = {
                val filtered = if (byTime) {
                    records.filter { (it.time ?: 0) in startMs..endMs }
                } else {
                    records
                }
                if (filtered.isEmpty()) {
                    result = "所选范围内没有账单。"
                    return@Button
                }
                val csv = com.xiaogou.bookkeeping.domain.buildBillsCsv(
                    filtered,
                    types.associateBy { it.id },
                    assets.associateBy { it.id },
                )
                val file = java.io.File(context.cacheDir, "xiaogou_bills.csv")
                file.writeText(csv, Charsets.UTF_8)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file,
                )
                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(share, "发送账单 CSV"))
                result = "已生成 ${filtered.size} 条，正在打开分享。"
            }) { Text("导出并发送") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
    picking?.let { which ->
        com.xiaogou.bookkeeping.ui.record.CalendarPickerDialog(
            initialMs = if (which == "start") startMs else endMs,
            onDismiss = { picking = null },
            onDatePicked = { dayMs ->
                picking = null
                if (which == "start") startMs = dayMs else endMs = dayMs
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockSetupScreen(settings: Settings, onDone: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("设置 PIN") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN（4-6 位数字）") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("确认 PIN") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
            error?.let { Text(it) }
            Button(onClick = {
                when {
                    pin.length !in 4..6 || !pin.all { it.isDigit() } -> error = "PIN 需为 4-6 位数字"
                    pin != confirm -> error = "两次输入不一致"
                    else -> {
                        settings.pinHash = Settings.hashPin(pin)
                        settings.lockEnabled = true
                        onDone()
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存并开启应用锁") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(settings: Settings, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var failures by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    // 生物识别可用时才显示按钮：设备已录入指纹/面容且系统允许。
    val biometricAvailable = remember {
        val manager = androidx.biometric.BiometricManager.from(context)
        manager.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK,
        ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
    Scaffold(topBar = { TopAppBar(title = { Text("应用锁") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(modifier = Modifier.height(WalkDimens.lockTopSpace))
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.xiaogou.bookkeeping.R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(WalkDimens.lockLogoSize).align(androidx.compose.ui.Alignment.CenterHorizontally),
            )
            Text("请输入 PIN 解锁小狗记账")
            OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
            error?.let { Text(it) }
            Button(onClick = {
                if (Settings.hashPin(pin) == settings.pinHash) {
                    onUnlocked()
                } else {
                    failures++
                    error = if (failures > 3) "解锁失败，请稍后再试" else "PIN 不正确"
                    pin = ""
                }
            }, modifier = Modifier.fillMaxWidth().height(WalkDimens.lockBottomButtonHeight)) { Text("解锁") }
            if (biometricAvailable) {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        if (activity == null) {
                            error = "当前界面不支持生物识别，请用 PIN 解锁"
                            return@OutlinedButton
                        }
                        val prompt = androidx.biometric.BiometricPrompt(
                            activity,
                            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                    onUnlocked()
                                }
                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    error = errString.toString()
                                }
                            },
                        )
                        prompt.authenticate(
                            androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                .setTitle("解锁小狗记账")
                                .setSubtitle("使用指纹或面容解锁")
                                .setNegativeButtonText("用 PIN 解锁")
                                .build(),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("指纹 / 面容解锁") }
            }
        }
    }
}
