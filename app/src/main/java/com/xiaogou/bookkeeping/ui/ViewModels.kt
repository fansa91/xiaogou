package com.xiaogou.bookkeeping.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaogou.bookkeeping.data.BookkeepingRepository
import com.xiaogou.bookkeeping.data.dao.TypeSum
import com.xiaogou.bookkeeping.data.entity.Assets
import com.xiaogou.bookkeeping.data.entity.Record
import com.xiaogou.bookkeeping.data.entity.RecordType
import com.xiaogou.bookkeeping.domain.currentYearMonth
import com.xiaogou.bookkeeping.domain.monthRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthSummary(val incomeCents: Long, val expenseCents: Long, val totalAssetsCents: Long)

class HomeViewModel(private val repo: BookkeepingRepository) : ViewModel() {
    private val _yearMonth = MutableStateFlow(currentYearMonth())
    val yearMonth: StateFlow<Pair<Int, Int>> = _yearMonth

    @OptIn(ExperimentalCoroutinesApi::class)
    val records: StateFlow<List<Record>> = _yearMonth.flatMapLatest { (y, m) ->
        val (s, e) = monthRange(y, m)
        repo.recordsInRange(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sums: StateFlow<List<TypeSum>> = _yearMonth.flatMapLatest { (y, m) ->
        val (s, e) = monthRange(y, m)
        repo.sumsByType(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val types: StateFlow<List<RecordType>> = repo.allTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assets: StateFlow<List<Assets>> = repo.allAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<MonthSummary> = kotlinx.coroutines.flow.combine(records, sums, types, assets) { recs, sumsList, typesList, assetsList ->
        val byId = typesList.associateBy { it.id }
        var income = 0L
        var expense = 0L
        sumsList.forEach { s ->
            if (byId[s.recordTypeId]?.type == 1) income += s.total else expense += s.total
        }
        // sums 为空时（无记录）直接为 0；records 未在 summary 里重复算，避免双计
        MonthSummary(income, expense, assetsList.sumOf { it.money })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthSummary(0, 0, 0))

    init {
        viewModelScope.launch { repo.seedIfEmpty() }
    }

    fun prevMonth() {
        val (y, m) = _yearMonth.value
        val cal = Calendar.getInstance().apply { set(y, m, 1); add(Calendar.MONTH, -1) }
        _yearMonth.value = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun nextMonth() {
        val (y, m) = _yearMonth.value
        val cal = Calendar.getInstance().apply { set(y, m, 1); add(Calendar.MONTH, 1) }
        _yearMonth.value = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun deleteRecord(record: Record) = viewModelScope.launch { repo.deleteRecord(record) }
}

class RecordViewModel(private val repo: BookkeepingRepository) : ViewModel() {
    fun expenseTypes(): Flow<List<RecordType>> = repo.enabledTypes(0)
    fun incomeTypes(): Flow<List<RecordType>> = repo.enabledTypes(1)
    fun assets(): Flow<List<Assets>> = repo.allAssets()
    fun allTypes(): Flow<List<RecordType>> = repo.allTypes()
    suspend fun getRecord(id: Long) = repo.getRecord(id)

    fun save(moneyCents: Long, typeId: Long, type: Int, assetsId: Long?, remark: String, onDone: () -> Unit) {
        save(moneyCents, typeId, type, assetsId, remark, reimburse = false, onDone)
    }

    fun save(moneyCents: Long, typeId: Long, type: Int, assetsId: Long?, remark: String, reimburse: Boolean, onDone: () -> Unit) {
        save(moneyCents, typeId, type, assetsId, remark, reimburse, System.currentTimeMillis(), onDone)
    }

    fun save(moneyCents: Long, typeId: Long, type: Int, assetsId: Long?, remark: String, reimburse: Boolean, timeMs: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.addRecord(moneyCents, typeId, type, assetsId, remark, timeMs)
            if (reimburse) {
                repo.addReimburse(
                    com.xiaogou.bookkeeping.data.entity.Reimburse(
                        money = moneyCents, reimburseMoney = 0, remark = remark, time = now,
                        createTime = now, recordTypeId = typeId, reimburseState = 0,
                        assetsId = assetsId ?: -1,
                    ),
                )
            }
            onDone()
        }
    }

    fun updateRecord(
        old: com.xiaogou.bookkeeping.data.entity.Record,
        moneyCents: Long, typeId: Long, type: Int, assetsId: Long?, remark: String, timeMs: Long, onDone: () -> Unit,
    ) {
        viewModelScope.launch { repo.updateRecord(old, moneyCents, typeId, type, assetsId, remark, timeMs); onDone() }
    }
}

class AssetsViewModel(private val repo: BookkeepingRepository) : ViewModel() {
    val assets: StateFlow<List<Assets>> = repo.allAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun modifyRecords(assetsId: Long) = repo.modifyRecords(assetsId)

    fun add(name: String, type: Int, initMoneyCents: Long, remark: String, onDone: () -> Unit) {
        add(name, type, initMoneyCents, remark, "", onDone)
    }

    fun add(name: String, type: Int, initMoneyCents: Long, remark: String, imgName: String, onDone: () -> Unit) {
        viewModelScope.launch { repo.addAssets(name, type, initMoneyCents, remark, imgName); onDone() }
    }

    fun transfer(fromId: Long, toId: Long, moneyCents: Long, remark: String, onDone: () -> Unit) {
        viewModelScope.launch { repo.transfer(fromId, toId, moneyCents, remark); onDone() }
    }

    val transfers = repo.allTransfers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTransfer(record: com.xiaogou.bookkeeping.data.entity.AssetsTransferRecord) {
        viewModelScope.launch { repo.deleteTransfer(record) }
    }
}

class TypeViewModel(private val repo: BookkeepingRepository) : ViewModel() {
    val types: StateFlow<List<RecordType>> = repo.allTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, type: Int, onDone: () -> Unit) {
        add(name, type, "", onDone)
    }

    fun add(name: String, type: Int, imgName: String, onDone: () -> Unit) {
        viewModelScope.launch { repo.addType(name, type, imgName = imgName); onDone() }
    }

    fun setIcon(type: RecordType, imgName: String) {
        viewModelScope.launch { repo.setTypeIcon(type, imgName) }
    }

    fun setEnabled(type: RecordType, enabled: Boolean) {
        viewModelScope.launch { repo.setTypeEnabled(type, enabled) }
    }

    fun rename(type: RecordType, newName: String) {
        viewModelScope.launch { repo.renameType(type, newName) }
    }

    fun addSubtype(parentId: Long, parentType: Int, name: String, onDone: () -> Unit) {
        viewModelScope.launch { repo.addSubtype(parentId, name, parentType); onDone() }
    }
}

class SearchViewModel(private val repo: BookkeepingRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Record>> = _query.flatMapLatest { q ->
        if (q.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList()) else repo.searchByRemarkOrAsset(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun types() = repo.allTypes()
    fun assets() = repo.allAssets()
}

@Suppress("UNCHECKED_CAST")
class AppViewModelFactory(private val repo: BookkeepingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repo) as T
            modelClass.isAssignableFrom(RecordViewModel::class.java) -> RecordViewModel(repo) as T
            modelClass.isAssignableFrom(AssetsViewModel::class.java) -> AssetsViewModel(repo) as T
            modelClass.isAssignableFrom(TypeViewModel::class.java) -> TypeViewModel(repo) as T
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(repo) as T
            modelClass.isAssignableFrom(BudgetViewModel::class.java) -> BudgetViewModel(repo) as T
            modelClass.isAssignableFrom(RecurringViewModel::class.java) -> RecurringViewModel(repo) as T
            modelClass.isAssignableFrom(ReimburseViewModel::class.java) -> ReimburseViewModel(repo) as T
            modelClass.isAssignableFrom(StatisticsViewModel::class.java) -> StatisticsViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
