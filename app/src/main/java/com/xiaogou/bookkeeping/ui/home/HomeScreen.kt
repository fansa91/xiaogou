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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.R
import com.xiaogou.bookkeeping.data.entity.Record
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.formatDate
import com.xiaogou.bookkeeping.domain.formatMonth
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.HomeViewModel
import com.xiaogou.bookkeeping.ui.theme.FloatingBottomBar
import com.xiaogou.bookkeeping.ui.theme.WalkDimens

/**
 * 首页布局：
 * - 顶部标题“记账”，左栏本月支出 + 结余，右栏本月收入；
 *   背景 img_banner 透明度 0.07。
 * - 底部悬浮 tab 条（见 FloatingBottomBar）。
 * - 顶部右侧悬浮预算按钮，点进预算页。
 */
@OptIn(ExperimentalMaterial3Api::class)
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
) {
    val vm: HomeViewModel = viewModel(factory = factory)
    val yearMonth by vm.yearMonth.collectAsState()
    val records by vm.records.collectAsState()
    val sums by vm.sums.collectAsState()
    val types by vm.types.collectAsState()
    val summary by vm.summary.collectAsState()
    val typeById = types.associateBy { it.id }

    Scaffold(
        topBar = { TopAppBar(title = { Text("小狗记账") }) },
        bottomBar = {
            FloatingBottomBar(
                tabs = listOf("首页" to {}, "资产" to onAssets, "统计" to onStatistics, "设置" to onSettings),
                current = 0,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // banner_record + img_banner 背景
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.img_banner),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().alpha(0.07f),
                        contentScale = ContentScale.Crop,
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("记账")
                            // 悬浮预算按钮：32dp 图标位 + 60×24dp 进度条 + 10sp 标签
                            Button(onClick = onBudget) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center,
                                ) {
                                    Image(painter = painterResource(R.drawable.icon_budget), contentDescription = null)
                                }
                                Column {
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { 0f },
                                        modifier = Modifier.size(60.dp, 24.dp),
                                    )
                                    Text("预算", fontSize = 10.sp)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row {
                                    Text("本月支出")
                                    Image(painter = painterResource(R.drawable.icon_expense), contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                                }
                                Text(centsToYuan(summary.expenseCents))
                                Row {
                                    Text(centsToYuan(summary.totalAssetsCents))
                                    Image(painter = painterResource(R.drawable.icon_balance), contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row {
                                    Text("本月收入")
                                    Image(painter = painterResource(R.drawable.icon_income), contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                                }
                                Text(centsToYuan(summary.incomeCents))
                            }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { vm.prevMonth() }) { Text("‹ 上月") }
                    Text(formatMonth(yearMonth.first, yearMonth.second))
                    TextButton(onClick = { vm.nextMonth() }) { Text("下月 ›") }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRecord, modifier = Modifier.weight(1f)) { Text("记一笔") }
                    Button(onClick = onTypes, modifier = Modifier.weight(1f)) { Text("分类") }
                    Button(onClick = onSearch, modifier = Modifier.weight(1f)) { Text("搜索") }
                }
            }
            if (sums.isNotEmpty()) {
                item { Text("按分类统计") }
                items(sums) { sum ->
                    val t = typeById[sum.recordTypeId]
                    if (t != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onTypeDetail(t.id) }.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("${t.name}（${if (t.type == 1) "收入" else "支出"}）")
                            Text(centsToYuan(sum.total))
                        }
                    }
                }
            }
            item { Text("本月记录（${records.size} 条）") }
            items(records) { record ->
                RecordRow(record, typeById[record.recordTypeId]?.name ?: "", onDelete = { vm.deleteRecord(record) })
            }
        }
    }
}

@Composable
fun RecordRow(record: Record, typeName: String, onDelete: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$typeName · ${record.remark ?: ""}")
                Text(formatDate(record.time))
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(centsToYuan(record.money ?: 0))
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
        }
    }
}
