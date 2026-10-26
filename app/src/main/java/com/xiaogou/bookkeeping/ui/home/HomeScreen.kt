package com.xiaogou.bookkeeping.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.R
import com.xiaogou.bookkeeping.data.entity.Record
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.formatDate
import com.xiaogou.bookkeeping.domain.formatMonth
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.AssetsViewModel
import com.xiaogou.bookkeeping.ui.BudgetViewModel
import com.xiaogou.bookkeeping.ui.HomeViewModel
import com.xiaogou.bookkeeping.ui.theme.FloatingBottomBar

/**
 * 首页（banner_record 口径）：
 * - 顶部不是系统 Toolbar，而是 banner 本身：32sp 大标题“记账”，
 *   左列本月支出（28sp 主题色）+ 第二行结余（16sp + 14dp 图标），
 *   右列本月收入（28sp）。背景 img_banner 透明度 0.07，顶部留 62dp。
 * - 右上角悬浮预算按钮：32dp 图标位 + 60×24dp 进度条（8dp 圆角）
 *   + 10sp 半透明百分比标签，进度取本月第一个预算的已用占比。
 * - 记录按天分组：先是日期头（年-月-日 + 当天合计），下面是该天记录，
 *   点分类合计进分类明细，点记录行可删除（与之前一致）。
 */
@Composable
fun WalkBanner(
    title: String,
    leftTitle: String,
    leftValue: String,
    leftSecond: String,
    rightTitle: String,
    rightValue: String,
    budgetProgress: Float = 0f,
    budgetLabel: String = "预算",
    onBudget: (() -> Unit)? = null,
    leftIconRes: Int = R.drawable.icon_expense,
    rightIconRes: Int = R.drawable.icon_income,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.img_banner),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().alpha(0.07f),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.fillMaxWidth().padding(top = 62.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontSize = 32.sp)
                if (onBudget != null) {
                    Button(onClick = onBudget) {
                        Image(
                            painter = painterResource(R.drawable.icon_budget),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                        Box(modifier = Modifier.size(60.dp, 24.dp), contentAlignment = Alignment.Center) {
                            LinearProgressIndicator(
                                progress = { budgetProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.size(60.dp, 24.dp),
                            )
                            Text(budgetLabel, fontSize = 10.sp, modifier = Modifier.alpha(0.7f))
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 16.dp, end = 16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(leftTitle, fontSize = 16.sp)
                        Image(
                            painter = painterResource(leftIconRes),
                            contentDescription = null,
                            modifier = Modifier.padding(start = 2.dp).size(16.dp),
                        )
                    }
                    Text(
                        leftValue,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(leftSecond, fontSize = 16.sp)
                        Image(
                            painter = painterResource(R.drawable.icon_balance),
                            contentDescription = null,
                            modifier = Modifier.padding(start = 2.dp).size(14.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(rightTitle, fontSize = 16.sp)
                        Image(
                            painter = painterResource(rightIconRes),
                            contentDescription = null,
                            modifier = Modifier.padding(start = 2.dp).size(16.dp),
                        )
                    }
                    Text(rightValue, fontSize = 28.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    factory: AppViewModelFactory,
    onRecord: () -> Unit,
    onAssets: () -> Unit,
    onTypes: () -> Unit,
    onSearch: () -> Unit,
    onTypeDetail: (Long) -> Unit,
    onBudget: () -> Unit,
    onStatistics: () -> Unit,
    onSettings: () -> Unit,
    onEditRecord: (Long) -> Unit = {},
) {
    val vm: HomeViewModel = viewModel(factory = factory)
    val budgetVm: BudgetViewModel = viewModel(factory = factory)
    val assetsVm: AssetsViewModel = viewModel(factory = factory)
    val transfers by assetsVm.transfers.collectAsState()
    val assetsList by vm.assets.collectAsState()
    val assetById = assetsList.associateBy { it.id }
    var detailRecord by remember { mutableStateOf<Record?>(null) }
    val yearMonth by vm.yearMonth.collectAsState()
    val records by vm.records.collectAsState()
    val sums by vm.sums.collectAsState()
    val types by vm.types.collectAsState()
    val summary by vm.summary.collectAsState()
    val budgets by budgetVm.budgets.collectAsState()
    val typeById = types.associateBy { it.id }
    val spentByType = sums.associate { it.recordTypeId to it.total }
    val firstBudget = budgets.firstOrNull()
    val budgetProgress = if (firstBudget != null && firstBudget.money > 0) {
        (spentByType[firstBudget.recordTypeId] ?: 0L).toFloat() / firstBudget.money.toFloat()
    } else 0f
    val budgetLabel = if (firstBudget != null) "${(budgetProgress * 100).toInt()}%" else "预算"
    // 按天分组：yyyy-MM-dd → 当天记录（时间倒序，数据库已按时间排）
    val byDay = records.groupBy { formatDate(it.time).take(10) }

    Scaffold(
        bottomBar = {
            FloatingBottomBar(
                tabs = listOf("首页" to {}, "资产" to onAssets, "统计" to onStatistics, "设置" to onSettings),
                current = 0,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WalkBanner(
                    title = "记账",
                    leftTitle = "支出",
                    leftValue = centsToYuan(summary.expenseCents),
                    leftSecond = centsToYuan(summary.totalAssetsCents),
                    rightTitle = "收入",
                    rightValue = centsToYuan(summary.incomeCents),
                    budgetProgress = budgetProgress,
                    budgetLabel = budgetLabel,
                    onBudget = onBudget,
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { vm.prevMonth() }) { Text("‹ 上月") }
                    Text(formatMonth(yearMonth.first, yearMonth.second))
                    TextButton(onClick = { vm.nextMonth() }) { Text("下月 ›") }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRecord, modifier = Modifier.weight(1f)) { Text("记一笔") }
                    Button(onClick = onTypes, modifier = Modifier.weight(1f)) { Text("分类") }
                    Button(onClick = onSearch, modifier = Modifier.weight(1f)) { Text("搜索") }
                }
            }
            if (sums.isNotEmpty()) {
                item { Text("按分类统计", modifier = Modifier.padding(horizontal = 16.dp)) }
                items(sums) { sum ->
                    val t = typeById[sum.recordTypeId]
                    if (t != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onTypeDetail(t.id) }.padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("${t.name}（${if (t.type == 1) "收入" else "支出"}）")
                            Text(centsToYuan(sum.total))
                        }
                    }
                }
            }
            item { Text("本月记录（${records.size} 条）", modifier = Modifier.padding(horizontal = 16.dp)) }
            byDay.forEach { (day, dayRecords) ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(day)
                        Text("合计 ${centsToYuan(dayRecords.sumOf { it.money ?: 0 })}")
                    }
                }
                items(dayRecords) { record ->
                    RecordRow(
                        record, typeById[record.recordTypeId]?.name ?: "",
                        onDelete = { vm.deleteRecord(record) },
                        onClick = { detailRecord = record },
                    )
                }
            }
            if (transfers.isNotEmpty()) {
                item { Text("转账记录（${transfers.size} 条）", modifier = Modifier.padding(horizontal = 16.dp)) }
                items(transfers) { t ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${assetById[t.assetsIdForm]?.name ?: ""} → ${assetById[t.assetsIdTo]?.name ?: ""}")
                                Text("${t.remark} · ${formatDate(t.time)}")
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(centsToYuan(t.money))
                                TextButton(onClick = { assetsVm.deleteTransfer(t) }) { Text("删除") }
                            }
                        }
                    }
                }
            }
        }
    }
    // 记录详情弹窗（源码 dialog_record_content 口径）：分类 / 金额 / 账户 /
    // 备注 / 时间，点编辑进编辑页，点删除直接删。
    detailRecord?.let { r ->
        AlertDialog(
            onDismissRequest = { detailRecord = null },
            title = { Text(typeById[r.recordTypeId]?.name ?: "记录详情") },
            text = {
                Column {
                    Text("金额 ${centsToYuan(r.money ?: 0)}")
                    Text("账户 ${assetById[r.assetsId]?.name ?: "不计入账户"}")
                    Text("备注 ${r.remark ?: ""}")
                    Text("时间 ${formatDate(r.time)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { detailRecord = null; onEditRecord(r.id) }) { Text("编辑") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.deleteRecord(r); detailRecord = null }) { Text("删除") }
                    TextButton(onClick = { detailRecord = null }) { Text("关闭") }
                }
            },
        )
    }
}

@Composable
fun RecordRow(record: Record, typeName: String, onDelete: (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$typeName · ${record.remark ?: ""}")
                Text(formatDate(record.time))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(centsToYuan(record.money ?: 0))
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
        }
    }
}
