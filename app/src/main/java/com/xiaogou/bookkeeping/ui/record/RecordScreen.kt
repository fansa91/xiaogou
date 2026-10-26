package com.xiaogou.bookkeeping.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.formatDate
import com.xiaogou.bookkeeping.domain.yuanToCents
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.RecordViewModel
import com.xiaogou.bookkeeping.ui.theme.WalkDimens
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 只选时间（时分）的系统弹窗：日历视图选完日期后用它补时间。 */
fun showTimePicker(context: android.content.Context, initialMs: Long, onPick: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialMs }
    TimePickerDialog(
        context,
        { _, h, min ->
            val picked = Calendar.getInstance().apply {
                timeInMillis = initialMs
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onPick(picked.timeInMillis)
        },
        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true,
    ).show()
}

/**
 * Compose 月历视图（layout_calendar_* 口径）：月份标题 + 上月/下月切换 +
 * 7 列星期头 + 日期格，选中的日期用主题色底，今天描边；点某天后
 * 回调该天的毫秒时间戳（时分保留原值，调用方再弹时间弹窗补齐）。
 */
@Composable
fun CalendarPickerDialog(initialMs: Long, onDismiss: () -> Unit, onDatePicked: (Long) -> Unit) {
    val base = Calendar.getInstance().apply { timeInMillis = initialMs }
    var year by remember { mutableStateOf(base.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(base.get(Calendar.MONTH)) }
    val selectedDay = if (year == base.get(Calendar.YEAR) && month == base.get(Calendar.MONTH)) {
        base.get(Calendar.DAY_OF_MONTH)
    } else {
        -1
    }
    val first = Calendar.getInstance().apply {
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    // 星期日起算：firstDayOfWeek 在中文环境为周日，空白格 = 周几 - 1。
    val blanks = (first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceAtLeast(0)
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells: List<Int?> = List(blanks) { null } + (1..daysInMonth).toList()
    val today = Calendar.getInstance()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.TextButton(onClick = {
                        val c = Calendar.getInstance().apply { set(year, month, 1) }
                        c.add(Calendar.MONTH, -1)
                        year = c.get(Calendar.YEAR)
                        month = c.get(Calendar.MONTH)
                    }) { Text("‹ 上月") }
                    Text("${year}年${month + 1}月")
                    androidx.compose.material3.TextButton(onClick = {
                        val c = Calendar.getInstance().apply { set(year, month, 1) }
                        c.add(Calendar.MONTH, 1)
                        year = c.get(Calendar.YEAR)
                        month = c.get(Calendar.MONTH)
                    }) { Text("下月 ›") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach { Text(it) }
                }
                LazyVerticalGrid(columns = GridCells.Fixed(7)) {
                    items(cells) { day ->
                        if (day == null) {
                            Text("")
                        } else {
                            val isSelected = day == selectedDay
                            val isToday = year == today.get(Calendar.YEAR) &&
                                month == today.get(Calendar.MONTH) &&
                                day == today.get(Calendar.DAY_OF_MONTH)
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    val picked = Calendar.getInstance().apply {
                                        timeInMillis = initialMs
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, day)
                                    }
                                    onDatePicked(picked.timeInMillis)
                                },
                            ) {
                                Text(
                                    day.toString(),
                                    color = when {
                                        isSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                                        isToday -> androidx.compose.material3.MaterialTheme.colorScheme.secondary
                                        else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/**
 * 日期时间选择：系统日期弹窗 → 时间弹窗，两步选完回一个毫秒时间戳。
 * 源码的日期 / 月份选择器（dialog_date_picker / dialog_month_picker /
 * layout_calendar_*）在 Compose 里没有现成等价物，先用系统弹窗实现
 * 同样的“改日期”能力，年月日时分都能改。
 */
fun showDateTimePicker(context: android.content.Context, initialMs: Long, onPick: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialMs }
    DatePickerDialog(
        context,
        { _, y, m, d ->
            TimePickerDialog(
                context,
                { _, h, min ->
                    val picked = Calendar.getInstance().apply {
                        set(y, m, d, h, min, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPick(picked.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true,
            ).show()
        },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
    ).show()
}

/**
 * 记账页布局：
 * - 顶部居中支出 / 收入切换，中间分类网格。
 * - 金额框 + 备注框，下方是完整自定义数字键盘（不是系统键盘）：
 *   左侧 3 列数字键（1-9、.、0、00），右侧 64dp 功能列（删除、+、−、保存），
 *   底部一行：账户、今天日期、报销开关、再记一笔。
 * - + / − 是简单加减：先按 + 或 − 把当前金额计入累计，再输入下一笔，
 *   保存时累计 + 当前输入 = 最终金额；不按则直接保存当前输入。
 * - 再记一笔：保存后不退出，清空金额与备注，继续记下一笔。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordAddScreen(factory: AppViewModelFactory, onDone: () -> Unit, onBack: () -> Unit) {
    val vm: RecordViewModel = viewModel(factory = factory)
    var isIncome by remember { mutableStateOf(false) }
    val types by (if (isIncome) vm.incomeTypes() else vm.expenseTypes()).collectAsState(initial = emptyList())
    val assets by vm.assets().collectAsState(initial = emptyList())
    var selectedTypeId by remember { mutableStateOf<Long?>(null) }
    var selectedAssetsId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf(BigDecimal.ZERO) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var remark by remember { mutableStateOf("") }
    var reimburse by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var assetsMenu by remember { mutableStateOf(false) }
    var recordTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCalendar by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (selectedTypeId == null && types.isNotEmpty()) selectedTypeId = types.first().id
    val today = remember { SimpleDateFormat("今天 MM月dd日", Locale.CHINA).format(Date()) }

    fun appendDigit(key: String) {
        if (amount.replace(".", "").length >= 9) return
        if (key == ".") {
            if (!amount.contains(".")) amount = if (amount.isEmpty()) "0." else "$amount."
            return
        }
        if (key == "00") {
            if (amount.isEmpty()) return
            if (amount.contains(".")) {
                val afterDot = amount.substringAfter(".", "")
                if (afterDot.length >= 2) return
                amount += "0".repeat(2 - afterDot.length)
            } else {
                amount += "00"
            }
            return
        }
        val afterDot = amount.substringAfter(".", "")
        if (amount.contains(".") && afterDot.length >= 2) return
        amount += key
    }

    fun applyOp(op: String) {
        val cur = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        pending = when (pendingOp) {
            "+" -> pending + cur
            "−" -> pending - cur
            else -> cur
        }
        pendingOp = op
        amount = ""
    }

    fun finalAmount(): String {
        val cur = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val total = when (pendingOp) {
            "+" -> pending + cur
            "−" -> pending - cur
            else -> cur
        }
        return total.toPlainString()
    }

    fun doSave(again: Boolean) {
        val cents = yuanToCents(finalAmount())
        val typeId = selectedTypeId
        when {
            cents == null || cents <= 0 -> error = "请用下方键盘输入大于 0 的金额"
            typeId == null -> error = "请先选择分类"
            else -> vm.save(cents, typeId, if (isIncome) 1 else 0, selectedAssetsId, remark, reimburse, recordTimeMs) {
                if (again) {
                    amount = ""; pending = BigDecimal.ZERO; pendingOp = null; remark = ""; reimburse = false; error = null
                } else onDone()
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("记一笔") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilterChip(selected = !isIncome, onClick = { isIncome = false; selectedTypeId = null }, label = { Text("支出") })
                FilterChip(selected = isIncome, onClick = { isIncome = true; selectedTypeId = null }, label = { Text("收入") })
            }
            Text(
                if (amount.isEmpty() && pendingOp == null) "0.00" else finalAmount(),
                fontSize = 28.sp,
                modifier = Modifier.height(36.dp),
            )
            if (types.isEmpty()) Text("该类型下暂无启用的分类，请先去分类管理添加。")
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(types) { t ->
                    FilterChip(
                        selected = selectedTypeId == t.id,
                        onClick = { selectedTypeId = t.id },
                        label = { Text(t.name ?: "") },
                    )
                }
            }
            OutlinedTextField(
                value = remark, onValueChange = { remark = it }, label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            OutlinedButton(onClick = { assetsMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(assets.firstOrNull { it.id == selectedAssetsId }?.name ?: "不计入账户（可选）")
            }
            DropdownMenu(expanded = assetsMenu, onDismissRequest = { assetsMenu = false }) {
                DropdownMenuItem(text = { Text("不计入账户") }, onClick = { selectedAssetsId = null; assetsMenu = false })
                assets.forEach { a ->
                    DropdownMenuItem(text = { Text(a.name) }, onClick = { selectedAssetsId = a.id; assetsMenu = false })
                }
            }
            error?.let { Text(it) }
            // 完整自定义数字键盘
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "00")) { k ->
                        OutlinedButton(
                            onClick = { appendDigit(k) },
                            modifier = Modifier.fillMaxWidth().height(WalkDimens.keyboardRowHeight),
                        ) { Text(k) }
                    }
                }
                Column(
                    modifier = Modifier.width(WalkDimens.keyboardSideColumnWidth),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { if (amount.isNotEmpty()) amount = amount.dropLast(1) },
                        modifier = Modifier.fillMaxWidth().height(WalkDimens.keyboardRowHeight),
                    ) { Text("⌫") }
                    OutlinedButton(
                        onClick = { applyOp("+") },
                        modifier = Modifier.fillMaxWidth().height(WalkDimens.keyboardRowHeight),
                    ) { Text("+") }
                    OutlinedButton(
                        onClick = { applyOp("−") },
                        modifier = Modifier.fillMaxWidth().height(WalkDimens.keyboardRowHeight),
                    ) { Text("−") }
                    Button(
                        onClick = { doSave(false) },
                        modifier = Modifier.fillMaxWidth().height(WalkDimens.keyboardRowHeight),
                    ) { Text("保存") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { showCalendar = true }) {
                    Text(formatDate(recordTimeMs))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("报销")
                    Switch(checked = reimburse, onCheckedChange = { reimburse = it })
                }
                OutlinedButton(onClick = { doSave(true) }) { Text("再记一笔") }
            }
            Text(today, fontSize = 12.sp)
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }
        }
    }
    if (showCalendar) {
        CalendarPickerDialog(
            initialMs = recordTimeMs,
            onDismiss = { showCalendar = false },
            onDatePicked = { dayMs ->
                showCalendar = false
                showTimePicker(context, dayMs) { recordTimeMs = it }
            },
        )
    }
}

/**
 * 编辑记录（源码 dialog_record_content 的“改”能力）：
 * 金额 / 分类（按原收支方向）/ 账户 / 备注 / 日期时间都能改，
 * 保存时按旧记录反向冲回、按新值重新记账，余额不会双计。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditScreen(factory: AppViewModelFactory, recordId: Long, onDone: () -> Unit) {
    val vm: RecordViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    var old by remember { mutableStateOf<com.xiaogou.bookkeeping.data.entity.Record?>(null) }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var timeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedTypeId by remember { mutableStateOf<Long?>(null) }
    var selectedAssetsId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var assetsMenu by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    val types by vm.allTypes().collectAsState(initial = emptyList())
    val assets by vm.assets().collectAsState(initial = emptyList())

    LaunchedEffect(recordId) {
        val r = vm.getRecord(recordId)
        old = r
        if (r != null && !loaded) {
            amount = centsToYuan(r.money ?: 0L)
            remark = r.remark ?: ""
            timeMs = r.time ?: System.currentTimeMillis()
            selectedTypeId = r.recordTypeId
            selectedAssetsId = r.assetsId
            loaded = true
        }
    }
    val oldType = types.firstOrNull { it.id == old?.recordTypeId }?.type ?: 0
    val typeOptions = types.filter { it.type == oldType && it.state == 1 }

    Scaffold(topBar = { TopAppBar(title = { Text("编辑记录") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (old == null) {
                Text("记录不存在")
            } else {
                Text(if (oldType == 1) "收入" else "支出")
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额（元）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedButton(onClick = { assetsMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(assets.firstOrNull { it.id == selectedAssetsId }?.name ?: "不计入账户（可选）")
                }
                DropdownMenu(expanded = assetsMenu, onDismissRequest = { assetsMenu = false }) {
                    DropdownMenuItem(text = { Text("不计入账户") }, onClick = { selectedAssetsId = null; assetsMenu = false })
                    assets.forEach { a ->
                        DropdownMenuItem(text = { Text(a.name) }, onClick = { selectedAssetsId = a.id; assetsMenu = false })
                    }
                }
                Text("分类")
                typeOptions.forEach { t ->
                    FilterChip(
                        selected = selectedTypeId == t.id,
                        onClick = { selectedTypeId = t.id },
                        label = { Text(t.name ?: "") },
                    )
                }
                OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedButton(onClick = { showCalendar = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(formatDate(timeMs))
                }
                error?.let { Text(it) }
                Button(onClick = {
                    val cents = yuanToCents(amount)
                    val tid = selectedTypeId
                    val o = old
                    when {
                        o == null -> error = "记录不存在"
                        cents == null || cents <= 0 -> error = "请填写大于 0 的金额"
                        tid == null -> error = "请先选择分类"
                        else -> vm.updateRecord(o, cents, tid, oldType, selectedAssetsId, remark, timeMs, onDone)
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("保存修改") }
            }
        }
    }
    if (showCalendar) {
        CalendarPickerDialog(
            initialMs = timeMs,
            onDismiss = { showCalendar = false },
            onDatePicked = { dayMs ->
                showCalendar = false
                showTimePicker(context, dayMs) { timeMs = it }
            },
        )
    }
}
