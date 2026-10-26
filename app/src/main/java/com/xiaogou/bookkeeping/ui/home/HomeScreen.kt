package com.xiaogou.bookkeeping.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.R
import com.xiaogou.bookkeeping.data.entity.Record
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.formatDate
import com.xiaogou.bookkeeping.domain.formatMonth
import com.xiaogou.bookkeeping.domain.weekdayText
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.AssetsViewModel
import com.xiaogou.bookkeeping.ui.BudgetViewModel
import com.xiaogou.bookkeeping.ui.HomeViewModel

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
            // 背景图本身也是白色：旧版 ThemeIcon 会按 color_mode 着色
            // 再叠 0.07 透明度当水印；不着色的话浅色底上白对白，
            // 等于没有这张图。着文字色、保持 0.07 透明度。
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
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
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
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
                            // 这批图标 PNG 本身是纯白：旧版 ThemeIcon 按
                            // color_mode 着色后才可见；新版直接显示就是
                            // 白对白，浅色主题下看不见。左边跟金额同色。
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
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
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
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
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
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
    var deletingRecord by remember { mutableStateOf<Record?>(null) }
    var deletingTransfer by remember { mutableStateOf<com.xiaogou.bookkeeping.data.entity.AssetsTransferRecord?>(null) }
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
    // 按天分组：yyyy-MM-dd → 当天记录（时间倒序，数据库已按时间排）；
    // 缓存起来，免得每次重组都对当月记录重分一次组。
    val byDay = remember(records) { records.groupBy { formatDate(it.time).take(10) } }

    // 底部栏已提到 MainActivity 全局：这里不再自己放栏，内容底部
    // 由外层 Scaffold 的 padding 留出，不用再额外留 92dp。
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
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
                // 日期头按备份 layout_record_header 排布：整条 40dp 高；
                // 左边月日小标签（22dp 高、6dp 圆角，月和日分两段），
                // 后面跟 32×22dp 的星期小标签，右边是当天收入 / 支出，
                // 两段之间隔 10dp，不再只写一个合计。
                item {
                    val firstTime = dayRecords.firstOrNull()?.time
                    val dayIncome = dayRecords.filter { typeById[it.recordTypeId]?.type == 1 }.sumOf { it.money ?: 0 }
                    val dayExpense = dayRecords.filter { typeById[it.recordTypeId]?.type != 1 }.sumOf { it.money ?: 0 }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(start = 6.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${day.substring(5, 7).toInt()}月",
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "${day.substring(8, 10).toInt()}日",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        Box(
                            modifier = Modifier.padding(start = 6.dp)
                                .size(32.dp, 22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(weekdayText(firstTime), fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("收 ${centsToYuan(dayIncome)}", fontSize = 16.sp)
                            Text(
                                "支 ${centsToYuan(dayExpense)}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                }
                items(dayRecords) { record ->
                    val type = typeById[record.recordTypeId]
                    RecordRow(
                        record, type?.name ?: "",
                        imgName = type?.imgName,
                        assetName = assetById[record.assetsId]?.name,
                        onDelete = { deletingRecord = record },
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
                                TextButton(onClick = { deletingTransfer = t }) { Text("删除") }
                            }
                        }
                    }
                }
            }
        }
    }
    // 删记录确认：以前列表行和详情弹窗都是点一下直接删，误删找不回；
    // 现在统一先弹确认（转账删除本来就有确认，这里只是补齐记录一侧）。
    deletingRecord?.let { r ->
        AlertDialog(
            onDismissRequest = { deletingRecord = null },
            title = { Text("删除记录") },
            text = { Text("确定删除这条记录吗？删除后对应账户余额会冲回。") },
            confirmButton = {
                TextButton(onClick = { vm.deleteRecord(r); deletingRecord = null }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletingRecord = null }) { Text("取消") }
            },
        )
    }
    deletingTransfer?.let { t ->
        AlertDialog(
            onDismissRequest = { deletingTransfer = null },
            title = { Text("删除转账") },
            text = { Text("确定删除这条转账记录吗？删除后转出账户加回、转入账户扣回。") },
            confirmButton = {
                TextButton(onClick = { assetsVm.deleteTransfer(t); deletingTransfer = null }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletingTransfer = null }) { Text("取消") }
            },
        )
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
                    // 详情里的删除也走同一个确认弹窗：先关详情，再弹确认，
                    // 避免两个弹窗叠在一起。
                    TextButton(onClick = { detailRecord = null; deletingRecord = r }) { Text("删除") }
                    TextButton(onClick = { detailRecord = null }) { Text("关闭") }
                }
            },
        )
    }
}

@Composable
fun RecordRow(
    record: Record,
    typeName: String,
    imgName: String? = null,
    assetName: String? = null,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    // 记录行按备份 layout_record_item 排布，不再套卡片：
    // 左边是时间（16sp）+ 6×12dp 主题色指示条，内容行在时间
    // 右边、并往下错 16dp（备份里那块空 divider 的高度）；
    // 内容是 40dp 圆角 12dp 的图标位 + 分类名（14sp 单行）
    // + 备注（12sp，隔 6dp），右边是金额（16sp）。
    // 删除键是现在这版已有的操作，备份里没有，保留在金额下方。
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatDate(record.time).takeLast(5), fontSize = 16.sp)
            Box(
                modifier = Modifier.padding(start = 6.dp)
                    .size(6.dp, 12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Row(
            modifier = Modifier.weight(1f).padding(start = 10.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (!imgName.isNullOrBlank()) imgName else (typeName.firstOrNull() ?: "?").toString(),
                    fontSize = 18.sp,
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = 16.dp, end = 10.dp),
            ) {
                Text(typeName, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val second = listOfNotNull(
                    record.remark?.takeIf { it.isNotBlank() },
                    assetName?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (second.isNotBlank()) {
                    Text(second, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(centsToYuan(record.money ?: 0), fontSize = 16.sp)
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
        }
    }
}
