package com.xiaogou.bookkeeping.ui.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.formatDate
import com.xiaogou.bookkeeping.domain.yuanToCents
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.AssetsViewModel
import com.xiaogou.bookkeeping.ui.theme.WalkDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(factory: AppViewModelFactory, onAdd: () -> Unit, onDetail: (Long) -> Unit, onTransfer: () -> Unit) {
    val vm: AssetsViewModel = viewModel(factory = factory)
    val assets by vm.assets.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("资产") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("总额 ${centsToYuan(assets.sumOf { it.money })}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAdd) { Text("新增账户") }
                    Button(onClick = onTransfer) { Text("转账") }
                }
            }
            items(assets) { a ->
                Card(
                    modifier = Modifier.fillMaxWidth().height(WalkDimens.assetRowHeight).clickable { onDetail(a.id) },
                    shape = RoundedCornerShape(WalkDimens.listCorner),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(a.name)
                        Text(centsToYuan(a.money))
                        if (a.remark.isNotBlank()) Text(a.remark)
                    }
                }
            }
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
            Button(onClick = {
                val cents = yuanToCents(amount)
                when {
                    name.isBlank() -> error = "请填写账户名"
                    cents == null || cents < 0 -> error = "初始金额格式不正确"
                    else -> vm.add(name, 0, cents, remark, onDone)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsDetailScreen(factory: AppViewModelFactory, assetsId: Long) {
    val vm: AssetsViewModel = viewModel(factory = factory)
    val assets by vm.assets.collectAsState()
    val asset = assets.firstOrNull { it.id == assetsId }
    val modifies by vm.modifyRecords(assetsId).collectAsState(initial = emptyList())
    Scaffold(topBar = { TopAppBar(title = { Text(asset?.name ?: "账户详情") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                if (asset != null) {
                    Text("余额 ${centsToYuan(asset.money)}")
                    Text("初始 ${centsToYuan(asset.initMoney)}")
                    if (asset.remark.isNotBlank()) Text(asset.remark)
                } else {
                    Text("账户不存在")
                }
                Text("变动记录")
            }
            items(modifies) { m ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("变动后 ${centsToYuan(m.money)}（变动前 ${centsToYuan(m.moneyBefore)}）")
                        Text(formatDate(m.createTime))
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
            picker("选择转出账户", fromId) { fromId = it }
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
