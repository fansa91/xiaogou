package com.xiaogou.bookkeeping.ui.assets

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.sp
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
 *   展开时先点哪个就是哪个；底部栏由 MainActivity 全局悬浮提供，
 *   内容底部留白由外层 Scaffold 的 padding 给，不在这里另留。
 */
/** 资产图标候选（源码图标选择器的 Compose 版：用符号代替原图标资源）。 */
val AssetIcons = listOf("💰", "🏦", "💳", "💵", "🏠", "🚗", "📈", "💎")

/** 账户类型（旧版 assets_type_* 口径）：下标即 Assets.type 的取值。 */
val AssetTypeNames = listOf("现金", "银行卡", "信用卡", "支付宝", "微信", "股票", "债券", "基金", "京东白条", "转账", "其他")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(factory: AppViewModelFactory, onAdd: () -> Unit, onDetail: (Long) -> Unit, onTransfer: () -> Unit, showPie: Boolean = true) {
    val vm: AssetsViewModel = viewModel(factory = factory)
    val assets by vm.assets.collectAsState()
    var fabOpen by remember { mutableStateOf(false) }
    val positive = assets.filter { it.money >= 0 }.sumOf { it.money }
    val negative = assets.filter { it.money < 0 }.sumOf { -it.money }
    val net = assets.sumOf { it.money }
    var pieLiabilities by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }
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
        // 内容底部由 MainActivity 外层 Scaffold 的 padding 留出（底部栏高），
        // 这里不再额外留 92dp，否则底部会空出一大截。
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
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
                    androidx.compose.material3.TextButton(onClick = { showGrid = !showGrid }) {
                        Text(if (showGrid) "列表" else "网格")
                    }
                }
            }
            if (showPie) item {
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
            if (showGrid) {
                // 网格视图（assets_show_gride 口径）：每行 2 张小卡片，图标 + 名 + 余额。
                // 不用 LazyVerticalGrid：它和 LazyColumn 的 items 扩展同名会冲突，按行排即可。
                items(assets.chunked(2)) { rowAssets ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowAssets.forEach { a ->
                            Card(
                                modifier = Modifier.weight(1f).clickable { onDetail(a.id) },
                                shape = RoundedCornerShape(WalkDimens.listCorner),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(if (a.imgName.isNotBlank()) a.imgName else (a.name.firstOrNull() ?: "?").toString())
                                    Text(a.name)
                                    Text(centsToYuan(a.money))
                                }
                            }
                        }
                        if (rowAssets.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else items(assets) { a ->
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
                        ) { Text(if (a.imgName.isNotBlank()) a.imgName else (a.name.firstOrNull() ?: "?").toString()) }
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
    var icon by remember { mutableStateOf(AssetIcons.first()) }
    var assetType by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("新增账户") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("账户类型")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetTypeNames.forEachIndexed { i, label ->
                    androidx.compose.material3.FilterChip(
                        selected = assetType == i,
                        onClick = { assetType = i },
                        label = { Text(label) },
                    )
                }
            }
            Text("图标")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetIcons.forEach { ic ->
                    OutlinedButton(onClick = { icon = ic }, modifier = Modifier.size(48.dp)) { Text(ic) }
                }
            }
            Text("已选：$icon")
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
                        else -> vm.add(name, assetType, cents, remark, icon, onDone)
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
fun AssetsDetailScreen(factory: AppViewModelFactory, assetsId: Long, onDeleted: () -> Unit = {}) {
    val vm: AssetsViewModel = viewModel(factory = factory)
    val assets by vm.assets.collectAsState()
    val asset = assets.firstOrNull { it.id == assetsId }
    val modifies by vm.modifyRecords(assetsId).collectAsState(initial = emptyList())
    val bills by vm.recordsByAsset(assetsId).collectAsState(initial = emptyList())
    val types by vm.types().collectAsState(initial = emptyList())
    val typeById = types.associateBy { it.id }
    var tab by remember { mutableStateOf(0) }
    // 曲线范围（dialog_curve_style 口径）：周 / 月 / 季 / 半年 / 年 / 2–6 年，按变动时间过滤。
    var curveDays by remember { mutableStateOf(365) }
    val curveRanges = listOf("周" to 7, "月" to 30, "季" to 90, "半年" to 180, "年" to 365, "2年" to 730, "3年" to 1095, "4年" to 1460, "5年" to 1825, "6年" to 2190)
    val curveModifies = modifies.filter { it.createTime >= System.currentTimeMillis() - curveDays * 24L * 60 * 60 * 1000 }
    var modifyDetail by remember { mutableStateOf<com.xiaogou.bookkeeping.data.entity.AssetsModifyRecord?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text(asset?.name ?: "账户详情") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                if (asset != null) {
                    Text(asset.name)
                    Text("类型 ${AssetTypeNames.getOrNull(asset.type) ?: "其他"}")
                    Text("余额 ${centsToYuan(asset.money)}")
                    Text("初始 ${centsToYuan(asset.initMoney)}")
                    if (asset.remark.isNotBlank()) Text(asset.remark)
                    Text("创建 ${formatDate(asset.createTime)}")
                    Button(onClick = { showDeleteConfirm = true }) { Text("删除账户") }
                    deleteError?.let { Text(it) }
                } else {
                    Text("账户不存在")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { tab = 0 }) { Text("账单") }
                    OutlinedButton(onClick = { tab = 1 }) { Text("变动记录") }
                    OutlinedButton(onClick = { tab = 2 }) { Text("曲线") }
                }
            }
            when (tab) {
                0 -> {
                    if (bills.isEmpty()) item { Text("该账户下暂无账单。") }
                    items(bills) { r ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${typeById[r.recordTypeId]?.name ?: ""} · ${r.remark ?: ""}")
                                Text("${formatDate(r.time)} · ${centsToYuan(r.money ?: 0)}")
                            }
                        }
                    }
                }
                1 -> {
                    if (modifies.isEmpty()) item { Text("暂无变动记录。") }
                    items(modifies) { m ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { modifyDetail = m }) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${centsToYuan(m.moneyBefore)} → ${centsToYuan(m.money)}")
                                Text("变动后 ${centsToYuan(m.money)}（变动前 ${centsToYuan(m.moneyBefore)}）")
                                Text(formatDate(m.createTime))
                            }
                        }
                    }
                }
                else -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            curveRanges.forEach { (label, days) ->
                                androidx.compose.material3.FilterChip(
                                    selected = curveDays == days,
                                    onClick = { curveDays = days },
                                    label = { Text(label) },
                                )
                            }
                        }
                        if (curveModifies.size >= 2) {
                            val chronological = curveModifies.reversed()
                            CurveChart(values = chronological.map { it.money }, color = MaterialTheme.colorScheme.primary)
                            // 曲线光一条线读不出数值，补起止两点：时间 + 余额，
                            // 中间趋势看线，两头数值看这行。
                            Text(
                                "起点 ${formatDate(chronological.first().createTime)} · ${centsToYuan(chronological.first().money)}",
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                "终点 ${formatDate(chronological.last().createTime)} · ${centsToYuan(chronological.last().money)}",
                            )
                        } else {
                            Text("所选范围内变动记录不足 2 条，暂不画曲线。")
                        }
                    }
                }
            }
        }
    }
    modifyDetail?.let { m ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { modifyDetail = null },
            title = { Text("变动详情") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${centsToYuan(m.moneyBefore)} → ${centsToYuan(m.money)}")
                    Text("日期 ${formatDate(m.createTime)}")
                }
            },
            confirmButton = {
                Button(onClick = { vm.deleteModifyRecord(m); modifyDetail = null }) { Text("删除这条记录") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { modifyDetail = null }) { Text("关闭") }
            },
        )
    }
    if (showDeleteConfirm && asset != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除账户") },
            text = { Text("确定删除“${asset.name}”吗？名下有记账记录时不能删除。") },
            confirmButton = {
                Button(onClick = {
                    vm.deleteAssets(asset) { ok ->
                        if (ok) onDeleted() else deleteError = "该账户下有记账记录，不能删除。"
                    }
                    showDeleteConfirm = false
                }) { Text("删除") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
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

    // 转账用同一套自定义键盘（activity_assets_transfer 口径）：金额框 +
    // 备注框在上，数字键盘在下，不弹系统键盘；数字规则与记账键盘一致
    // （最多 9 位、小数后 2 位、00 键）。
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
        if (amount.contains(".") && amount.substringAfter(".").length >= 2) return
        amount += key
    }

    Scaffold(topBar = { TopAppBar(title = { Text("账户转账") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("转出账户")
            picker("选择转出账户", fromId) { fromId = it }
            Text("转入账户")
            picker("选择转入账户", toId) { toId = it }
            Text(if (amount.isEmpty()) "0.00" else amount, fontSize = 28.sp)
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 不用 LazyVerticalGrid：它和 LazyColumn 的 items 扩展同名，
                // 同时 import 会冲突；键盘固定 4 行 × 3 列，直接按行排。
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "00").chunked(3).forEach { rowKeys ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowKeys.forEach { k ->
                                OutlinedButton(
                                    onClick = { appendDigit(k) },
                                    modifier = Modifier.weight(1f).height(WalkDimens.keyboardRowHeight),
                                ) { Text(k) }
                            }
                        }
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
                    Button(
                        onClick = {
                            val cents = yuanToCents(amount)
                            val f = fromId
                            val t = toId
                            when {
                                f == null || t == null -> error = "请先选择转出与转入账户"
                                f == t -> error = "转出与转入账户不能相同"
                                cents == null || cents <= 0 -> error = "请填写大于 0 的金额"
                                else -> vm.transfer(f, t, cents, remark, onDone)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(WalkDimens.keyboardRowHeight),
                    ) { Text("确认") }
                }
            }
        }
    }
}
