package com.xiaogou.bookkeeping.ui.assets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.formatDate
import com.xiaogou.bookkeeping.domain.yuanToCents
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.AssetsViewModel
import com.xiaogou.bookkeeping.ui.home.WalkBanner
import com.xiaogou.bookkeeping.ui.theme.CurveChart
import com.xiaogou.bookkeeping.ui.theme.WalkDimens

/**
 * 资产页（fragment_assets + assets_item + banner_assets 口径）：
 * - 顶部是资产 banner：左列负债（负余额账户绝对值之和，28sp 主题色）
 *   + 第二行净资产（资产 - 负债）；右列资产总额（正余额账户之和）。
 *   账户类型字段在现有库里未区分资产/负债口径，所以用余额正负划分，
 *   并在代码里写明这一点，不假装有类型映射。
 * - 列表行 62dp、圆角 18dp、上下 8dp、左右 16dp，左侧 50dp 图标位
 *   （12dp 圆角），名左、金额右、备注在下。
 * - 右下角 50dp 悬浮按钮（16dp 圆角）向上展开：转账在上、新增在下，
 *   展开时先点哪个就是哪个；底部留 92dp 给悬浮 tab。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(factory: AppViewModelFactory, onAdd: () -> Unit, onDetail: (Long) -> Unit, onTransfer: () -> Unit) {
    val vm: AssetsViewModel = viewModel(factory = factory)
    val assets by vm.assets.collectAsState()
    var fabOpen by remember { mutableStateOf(false) }
    val positive = assets.filter { it.money >= 0 }.sumOf { it.money }
    val negative = assets.filter { it.money < 0 }.sumOf { -it.money }
    val net = assets.sumOf { it.money }
    var pieLiabilities by remember { mutableStateOf(false) }
    val pieValues = if (pieLiabilities) {
        assets.filter { it.money < 0 }.map { (it.name) to -it.money }
    } else {
        assets.filter { it.money >= 0 }.map { (it.name) to it.money }
    }.filter { it.second > 0 }
    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (fabOpen) {
                    Button(onClick = { fabOpen = false; onTransfer() }) { Text("转账") }
                    Button(onClick = { fabOpen = false; onAdd() }) { Text("新增账户") }
                }
                FloatingActionButton(
                    onClick = { fabOpen = !fabOpen },
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(if (fabOpen) "×" else "+", color = MaterialTheme.colorScheme.onPrimary) }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                WalkBanner(
                    title = "资产",
                    leftTitle = "负债",
                    leftValue = centsToYuan(negative),
                    leftSecond = centsToYuan(net),
                    rightTitle = "资产",
                    rightValue = centsToYuan(positive),
                    leftIconRes = com.xiaogou.bookkeeping.R.drawable.icon_balance,
                    rightIconRes = com.xiaogou.bookkeeping.R.drawable.icon_balance,
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("总额 ${centsToYuan(net)}")
                    Text("${assets.size} 个账户")
                }
            }
            item {
                // 资产饼图（assets_pie 口径）：资产 / 负债切换，120dp 环形饼 + 图例。
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { pieLiabilities = false }) { Text("资产") }
                    OutlinedButton(onClick = { pieLiabilities = true }) { Text("负债") }
                }
                if (pieValues.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        com.xiaogou.bookkeeping.ui.theme.PieChart(
                            values = pieValues,
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFFFEAD23),
                                androidx.compose.ui.graphics.Color(0xFF0F9D58),
                                androidx.compose.ui.graphics.Color(0xFFFE2542),
                                androidx.compose.ui.graphics.Color(0xFF2581FE),
                                androidx.compose.ui.graphics.Color(0xFF65C3AC),
                            ),
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                            val total = pieValues.sumOf { it.second }.takeIf { it > 0 } ?: 1L
                            pieValues.forEach { (name, v) ->
                                Text("$name ${v * 100 / total}% · ${centsToYuan(v)}")
                            }
                        }
                    }
                } else {
                    Text(if (pieLiabilities) "暂无负债账户" else "暂无资产账户", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            items(assets) { a ->
                Card(
                    modifier = Modifier.fillMaxWidth().height(WalkDimens.assetRowHeight)
                        .padding(horizontal = 16.dp).clickable { onDetail(a.id) },
                    shape = RoundedCornerShape(WalkDimens.listCorner),
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(WalkDimens.iconBoxLarge)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(0.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text((a.name.firstOrNull() ?: "?").toString()) }
                        Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                            Text(a.name)
                            if (a.remark.isNotBlank()) Text(a.remark, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(centsToYuan(a.money))
                    }
                }
            }
            if (assets.isEmpty()) item { Text("暂无账户，点右下角 + 新增。", modifier = Modifier.padding(horizontal = 16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsAddScreen(factory: AppViewModelFactory, onDone: () -> Unit) {
    val vm: AssetsViewModel = viewModel(factory = factory)
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("新增账户") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("账户名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("初始金额（元）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f).height(48.dp)) { Text("取消") }
                Button(onClick = {
                    val cents = yuanToCents(amount)
                    when {
                        name.isBlank() -> error = "请填写账户名"
                        cents == null || cents < 0 -> error = "初始金额格式不正确"
                        else -> vm.add(name, 0, cents, remark, onDone)
                    }
                }, modifier = Modifier.weight(1f).height(48.dp)) { Text("确定") }
            }
        }
    }
}

/**
 * 资产详情（activity_assets_detail 口径）：头部（名 / 余额 / 初始 / 备注 /
 * 创建时间）+ 分段切换（变动记录 / 曲线），曲线是变动后余额的折线。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsDetailScreen(factory: AppViewModelFactory, assetsId: Long) {
    val vm: AssetsViewModel = viewModel(factory = factory)
    val assets by vm.assets.collectAsState()
    val asset = assets.firstOrNull { it.id == assetsId }
    val modifies by vm.modifyRecords(assetsId).collectAsState(initial = emptyList())
    var tab by remember { mutableStateOf(0) }
    Scaffold(topBar = { TopAppBar(title = { Text(asset?.name ?: "账户详情") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                if (asset != null) {
                    Text(asset.name)
                    Text("余额 ${centsToYuan(asset.money)}")
                    Text("初始 ${centsToYuan(asset.initMoney)}")
                    if (asset.remark.isNotBlank()) Text(asset.remark)
                    Text("创建 ${formatDate(asset.createTime)}")
                } else {
                    Text("账户不存在")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { tab = 0 }) { Text("变动记录") }
                    OutlinedButton(onClick = { tab = 1 }) { Text("曲线") }
                }
            }
            if (tab == 1) {
                item {
                    if (modifies.size >= 2) {
                        CurveChart(values = modifies.reversed().map { it.money }, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("变动记录不足 2 条，暂不画曲线。")
                    }
                }
            } else {
                items(modifies) { m ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${centsToYuan(m.moneyBefore)} → ${centsToYuan(m.money)}")
                            Text("变动后 ${centsToYuan(m.money)}（变动前 ${centsToYuan(m.moneyBefore)}）")
                            Text(formatDate(m.createTime))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(factory: AppViewModelFactory, onDone: () -> Unit) {
    val vm: AssetsViewModel = viewModel(factory = factory)
    val assets by vm.assets.collectAsState()
    var fromId by remember { mutableStateOf<Long?>(null) }
    var toId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf<String?>(null) }

    @Composable
    fun picker(label: String, current: Long?, onPick: (Long) -> Unit) {
        OutlinedButton(onClick = { menu = label }, modifier = Modifier.fillMaxWidth()) {
            Text(assets.firstOrNull { it.id == current }?.name ?: label)
        }
        DropdownMenu(expanded = menu == label, onDismissRequest = { menu = null }) {
            assets.forEach { a ->
                DropdownMenuItem(text = { Text(a.name) }, onClick = { onPick(a.id); menu = null })
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("账户转账") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("转出账户")
            picker("选择转出账户", fromId) { fromId = it }
            Text("转入账户")
            picker("选择转入账户", toId) { toId = it }
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额（元）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it) }
            Button(onClick = {
                val cents = yuanToCents(amount)
                val f = fromId
                val t = toId
                when {
                    f == null || t == null -> error = "请先选择转出与转入账户"
                    f == t -> error = "转出与转入账户不能相同"
                    cents == null || cents <= 0 -> error = "请填写大于 0 的金额"
                    else -> vm.transfer(f, t, cents, remark, onDone)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("确认转账") }
        }
    }
}
