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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.domain.yuanToCents
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.RecordViewModel
import com.xiaogou.bookkeeping.ui.theme.WalkDimens
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            else -> vm.save(cents, typeId, if (isIncome) 1 else 0, selectedAssetsId, remark, reimburse) {
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
                Text(today)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("报销")
                    Switch(checked = reimburse, onCheckedChange = { reimburse = it })
                }
                OutlinedButton(onClick = { doSave(true) }) { Text("再记一笔") }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }
        }
    }
}
