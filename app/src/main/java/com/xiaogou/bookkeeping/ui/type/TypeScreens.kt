package com.xiaogou.bookkeeping.ui.type

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.TypeViewModel
import com.xiaogou.bookkeeping.ui.theme.WalkDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeManageScreen(factory: AppViewModelFactory, onAdd: () -> Unit) {
    val vm: TypeViewModel = viewModel(factory = factory)
    val types by vm.types.collectAsState()
    var showIncome by remember { mutableStateOf(false) }
    // 改名 / 换图标弹窗：点“改名”打开，改名与图标一起保存。
    var editing by remember { mutableStateOf<com.xiaogou.bookkeeping.data.entity.RecordType?>(null) }
    var editName by remember { mutableStateOf("") }
    var editIcon by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<com.xiaogou.bookkeeping.data.entity.RecordType?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("分类管理") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showIncome, onClick = { showIncome = false }, label = { Text("支出") })
                    FilterChip(selected = showIncome, onClick = { showIncome = true }, label = { Text("收入") })
                    Button(onClick = onAdd) { Text("新增分类") }
                }
            }
            val shown = types.filter { it.type == if (showIncome) 1 else 0 }
            items(shown) { t ->
                val index = shown.indexOf(t)
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(WalkDimens.rowCorner)) {
                    // 右边 5 个控件默认内边距加起来就超 270dp，左边名字再一长，
                    // 开关直接被挤出屏外。左边给 weight + 单行省略，右边按钮
                    // 内边距收窄到 4dp、间距收小，整行在 360dp 屏上也放得下。
                    val smallButtonPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(WalkDimens.iconBox)) {
                                Text(if (!t.imgName.isNullOrBlank()) t.imgName else (t.name?.firstOrNull()?.toString() ?: "?"))
                            }
                            Text(
                                t.name ?: "",
                                modifier = Modifier.padding(start = 8.dp),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    editing = t
                                    editName = t.name ?: ""
                                    editIcon = t.imgName ?: ""
                                },
                                contentPadding = smallButtonPadding,
                            ) { Text("改名") }
                            androidx.compose.material3.TextButton(
                                onClick = { deleting = t },
                                contentPadding = smallButtonPadding,
                            ) { Text("删除") }
                            // 排序：与同组相邻项交换 ranking，列表按 ranking 排。
                            androidx.compose.material3.TextButton(
                                enabled = index > 0,
                                onClick = { vm.swapRanking(t, shown[index - 1]) },
                                contentPadding = smallButtonPadding,
                            ) { Text("↑") }
                            androidx.compose.material3.TextButton(
                                enabled = index < shown.size - 1,
                                onClick = { vm.swapRanking(t, shown[index + 1]) },
                                contentPadding = smallButtonPadding,
                            ) { Text("↓") }
                            Switch(checked = t.state == 1, onCheckedChange = { vm.setEnabled(t, it) })
                        }
                    }
                }
            }
            deleteError?.let { item { Text(it) } }
        }
    }
    val editingType = editing
    if (editingType != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("编辑分类") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("分类名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("图标")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.xiaogou.bookkeeping.ui.assets.AssetIcons.forEach { ic ->
                            androidx.compose.material3.OutlinedButton(onClick = { editIcon = ic }, modifier = Modifier.size(48.dp)) { Text(ic) }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        vm.rename(editingType, editName)
                        vm.setIcon(editingType, editIcon)
                    }
                    editing = null
                }) { Text("保存") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { editing = null }) { Text("取消") }
            },
        )
    }
    deleting?.let { t ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除分类") },
            text = { Text("确定删除“${t.name}”吗？该分类下有记录或子类型时不能删除。") },
            confirmButton = {
                Button(onClick = {
                    vm.deleteType(t) { ok ->
                        if (!ok) deleteError = "该分类下有记录或子类型，不能删除。"
                    }
                    deleting = null
                }) { Text("删除") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleting = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeAddScreen(factory: AppViewModelFactory, onDone: () -> Unit) {
    val vm: TypeViewModel = viewModel(factory = factory)
    var name by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var icon by remember { mutableStateOf(com.xiaogou.bookkeeping.ui.assets.AssetIcons.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("新增分类") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("支出") })
                FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("收入") })
            }
            Text("图标")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.xiaogou.bookkeeping.ui.assets.AssetIcons.forEach { ic ->
                    androidx.compose.material3.OutlinedButton(onClick = { icon = ic }, modifier = Modifier.size(48.dp)) { Text(ic) }
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("分类名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it) }
            Button(onClick = {
                if (name.isBlank()) error = "请填写分类名"
                else vm.add(name, if (isIncome) 1 else 0, icon, onDone)
            }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        }
    }
}
