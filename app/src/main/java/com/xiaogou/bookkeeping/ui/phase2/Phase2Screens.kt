package com.xiaogou.bookkeeping.ui.phase2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
                Card(modifier = Modifier.fillMaxWidth()) {
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

// ---------- 统计进阶：历史趋势 + 全部类型 + 子类型 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsAdvancedScreen(factory: AppViewModelFactory, onSubtype: (Long) -> Unit) {
    val vm: StatisticsViewModel = viewModel(factory = factory)
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val records by vm.recentRecords.collectAsState(initial = emptyList())
    val types by homeVm.types.collectAsState()
    val typeById = types.associateBy { it.id }
    // 按年月分组：yyyy-MM → 支出/收入
    val byMonth = records.groupBy {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.time ?: 0 }
        "%04d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
    }.toSortedMap()
    Scaffold(topBar = { TopAppBar(title = { Text("统计进阶") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("本月支出占比")
                val monthSums = records.filter {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.time ?: 0 }
                    "%04d%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1) ==
                        "%04d%02d".format(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR), java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)
                }.groupBy { it.recordTypeId }.map { (id, list) ->
                    (typeById[id]?.name ?: "") to list.sumOf { it.money ?: 0 }
                }.filter { it.second > 0 }
                if (monthSums.isNotEmpty()) {
                    PieChart(
                        values = monthSums,
                        colors = listOf(
                            androidx.compose.ui.graphics.Color(0xFFFEAD23),
                            androidx.compose.ui.graphics.Color(0xFF0F9D58),
                            androidx.compose.ui.graphics.Color(0xFFFE2542),
                            androidx.compose.ui.graphics.Color(0xFF2581FE),
                            androidx.compose.ui.graphics.Color(0xFF65C3AC),
                        ),
                    )
                }
            }
            item { Text("最近 6 个月趋势") }
            byMonth.forEach { (month, list) ->
                item {
                    var exp = 0L
                    var inc = 0L
                    list.forEach { r ->
                        if (typeById[r.recordTypeId]?.type == 1) inc += r.money ?: 0 else exp += r.money ?: 0
                    }
                    Text("$month · 支出 ${centsToYuan(exp)} · 收入 ${centsToYuan(inc)}")
                }
            }
            item { Text("全部类型（点进子类型）") }
            items(types.filter { it.parentId == -1L }) { t ->
                Text(t.name ?: "", modifier = Modifier.fillMaxWidth().clickable { onSubtype(t.id) }.padding(8.dp))
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
    Scaffold(topBar = { TopAppBar(title = { Text("${parent?.name ?: ""} · 子类型") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: Settings, onBudget: () -> Unit, onRecurring: () -> Unit, onReimburse: () -> Unit, onStatistics: () -> Unit, onLockSetup: () -> Unit) {
    var lockOn by remember { mutableStateOf(settings.lockEnabled) }
    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingsRow(icon = { Text("预") }, title = "预算", onClick = onBudget)
                SettingsRow(icon = { Text("周") }, title = "周期记账", onClick = onRecurring)
                SettingsRow(icon = { Text("报") }, title = "报销", onClick = onReimburse)
                SettingsRow(icon = { Text("统") }, title = "统计进阶", onClick = onStatistics)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("应用锁（PIN）")
                    Switch(checked = lockOn, onCheckedChange = {
                        if (it && settings.pinHash.isNullOrBlank()) {
                            onLockSetup()
                        } else {
                            lockOn = it
                            settings.lockEnabled = it
                        }
                    })
                }
                Button(onClick = onLockSetup, modifier = Modifier.fillMaxWidth()) { Text("设置 / 修改 PIN") }
                Text("开启后每次打开应用需输入 PIN；失败超过 3 次会提示。PIN 只存 SHA-256 哈希，不存明文。")
            }
        }
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
        }
    }
}
