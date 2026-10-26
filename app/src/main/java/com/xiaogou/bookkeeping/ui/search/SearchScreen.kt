package com.xiaogou.bookkeeping.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.formatDate
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.SearchViewModel
import com.xiaogou.bookkeeping.ui.theme.WalkDimens

/**
 * 搜索页（fragment_search 口径）：40dp 深色圆角搜索框 + 主题色搜索按钮，
 * 搜索按备注或账户名命中，结果行带账户名。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(factory: AppViewModelFactory) {
    val vm: SearchViewModel = viewModel(factory = factory)
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val types by vm.types().collectAsState(initial = emptyList())
    val assets by vm.assets().collectAsState(initial = emptyList())
    val typeById = types.associateBy { it.id }
    val assetById = assets.associateBy { it.id }
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(topBar = { TopAppBar(title = { Text("搜索记录") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = query,
                    onValueChange = { vm.setQuery(it) },
                    placeholder = { Text("按备注或账户搜索", color = Color(0xFFBBBBBB)) },
                    modifier = Modifier.weight(1f).height(WalkDimens.searchBoxHeight)
                        .background(Color(0xFF333333), RoundedCornerShape(12.dp)),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                Button(
                    onClick = { keyboard?.hide() },
                    modifier = Modifier.height(WalkDimens.searchBoxHeight),
                ) { Text("搜索") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(results) { r ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${typeById[r.recordTypeId]?.name ?: ""} · ${r.remark ?: ""}")
                                Text("${formatDate(r.time)} · ${assetById[r.assetsId]?.name ?: "无账户"}")
                            }
                            Text(centsToYuan(r.money ?: 0), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (query.isNotBlank() && results.isEmpty()) {
                    item { Text("没有找到匹配的记录") }
                }
            }
        }
    }
}
