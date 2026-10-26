package com.xiaogou.bookkeeping.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.SearchViewModel
import com.xiaogou.bookkeeping.ui.home.RecordRow
import com.xiaogou.bookkeeping.ui.theme.WalkDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(factory: AppViewModelFactory) {
    val vm: SearchViewModel = viewModel(factory = factory)
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val types by vm.types().collectAsState(initial = emptyList())
    val typeById = types.associateBy { it.id }
    Scaffold(topBar = { TopAppBar(title = { Text("搜索记录") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { vm.setQuery(it) },
                label = { Text("按备注搜索") },
                modifier = Modifier.fillMaxWidth().height(WalkDimens.searchBoxHeight),
                singleLine = true,
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(results) { r ->
                    RecordRow(r, typeById[r.recordTypeId]?.name ?: "")
                }
                if (query.isNotBlank() && results.isEmpty()) {
                    item { Text("没有找到匹配的记录") }
                }
            }
        }
    }
}
