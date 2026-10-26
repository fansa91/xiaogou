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
    Scaffold(topBar = { TopAppBar(title = { Text("分类管理") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showIncome, onClick = { showIncome = false }, label = { Text("支出") })
                    FilterChip(selected = showIncome, onClick = { showIncome = true }, label = { Text("收入") })
                    Button(onClick = onAdd) { Text("新增分类") }
                }
            }
            items(types.filter { it.type == if (showIncome) 1 else 0 }) { t ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(WalkDimens.rowCorner)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row {
                            Box(modifier = Modifier.size(WalkDimens.iconBox))
                            Text(t.name ?: "", modifier = Modifier.padding(start = 8.dp))
                        }
                        Switch(checked = t.state == 1, onCheckedChange = { vm.setEnabled(t, it) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeAddScreen(factory: AppViewModelFactory, onDone: () -> Unit) {
    val vm: TypeViewModel = viewModel(factory = factory)
    var name by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("新增分类") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("支出") })
                FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("收入") })
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("分类名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it) }
            Button(onClick = {
                if (name.isBlank()) error = "请填写分类名"
                else vm.add(name, if (isIncome) 1 else 0, onDone)
            }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        }
    }
}
