package com.xiaogou.bookkeeping.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaogou.bookkeeping.domain.centsToYuan
import com.xiaogou.bookkeeping.domain.formatMonth
import com.xiaogou.bookkeeping.ui.AppViewModelFactory
import com.xiaogou.bookkeeping.ui.HomeViewModel
import com.xiaogou.bookkeeping.ui.home.RecordRow

/** 分类明细：本月该分类的全部记录与合计。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeDetailScreen(factory: AppViewModelFactory, typeId: Long) {
    val vm: HomeViewModel = viewModel(factory = factory)
    val yearMonth by vm.yearMonth.collectAsState()
    val types by vm.types.collectAsState()
    val type = types.firstOrNull { it.id == typeId }
    // 这里通过 HomeViewModel 的当月 records 过滤，避免再开一个 ViewModel；
    // 若后续需要跨月明细，再把 recordsByType 接到独立 ViewModel。
    val records by vm.records.collectAsState()
    val filtered = records.filter { it.recordTypeId == typeId }
    Scaffold(topBar = { TopAppBar(title = { Text(type?.name ?: "分类明细") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("${formatMonth(yearMonth.first, yearMonth.second)} · 合计 ${centsToYuan(filtered.sumOf { it.money ?: 0 })} · ${filtered.size} 条")
                // start/end 保留为月份口径的显式依据，避免后续误改月份计算
                Text("统计口径：时间在当月范围内")
            }
            items(filtered) { r ->
                RecordRow(r, type?.name ?: "")
            }
        }
    }
}
