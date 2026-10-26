package com.xiaogou.bookkeeping.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaogou.bookkeeping.data.BookkeepingRepository
import com.xiaogou.bookkeeping.data.entity.Budget
import com.xiaogou.bookkeeping.data.entity.RecurringRecord
import com.xiaogou.bookkeeping.data.entity.Reimburse
import com.xiaogou.bookkeeping.domain.currentMonthKey
import com.xiaogou.bookkeeping.domain.firstOccurrence
import com.xiaogou.bookkeeping.domain.monthRange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetViewModel(private val repo: BookkeepingRepository) : ViewModel() {
    val monthKey = currentMonthKey()
    val budgets: StateFlow<List<Budget>> = repo.budgetsByMonth(monthKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(typeId: Long, moneyCents: Long, remark: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.addBudget(Budget(recordTypeId = typeId, remark = remark, month = monthKey, money = moneyCents))
            onDone()
        }
    }

    fun delete(budget: Budget) = viewModelScope.launch { repo.deleteBudget(budget) }
}

class RecurringViewModel(private val repo: BookkeepingRepository) : ViewModel() {
    val rules: StateFlow<List<RecurringRecord>> = repo.allRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(moneyCents: Long, typeId: Long, type: Int, assetsId: Long, remark: String, cycleType: Int, cycleValue: Int, endTime: Long?, onDone: () -> Unit) {
        viewModelScope.launch {
            val next = firstOccurrence(cycleType, cycleValue)
            repo.addRecurring(
                RecurringRecord(
                    money = moneyCents, remark = remark, recordTypeId = typeId, recordType = type,
                    assetsId = assetsId, cycleType = cycleType, cycleValue = cycleValue,
                    startTime = System.currentTimeMillis(), endTime = endTime, nextTime = next,
                    lastTime = null, state = 0, createTime = System.currentTimeMillis(),
                ),
            )
            onDone()
        }
    }

    fun toggle(rule: RecurringRecord) {
        viewModelScope.launch { repo.updateRecurring(rule.copy(state = if (rule.state == 0) 1 else 0)) }
    }

    fun delete(rule: RecurringRecord) = viewModelScope.launch { repo.deleteRecurring(rule) }

    fun generateDue(onResult: (Int) -> Unit) {
        viewModelScope.launch { onResult(repo.generateDue(System.currentTimeMillis())) }
    }
}

class ReimburseViewModel(private val repo: BookkeepingRepository) : ViewModel() {
    val items: StateFlow<List<Reimburse>> = repo.allReimburses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(moneyCents: Long, typeId: Long, assetsId: Long, remark: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.addReimburse(
                Reimburse(money = moneyCents, reimburseMoney = 0, remark = remark, time = now, createTime = now, recordTypeId = typeId, reimburseState = 0, assetsId = assetsId),
            )
            onDone()
        }
    }

    fun markReimbursed(item: Reimburse) {
        viewModelScope.launch { repo.updateReimburse(item.copy(reimburseState = 1, reimburseMoney = item.money)) }
    }

    /** 撤销已报销：回到未报销状态（旧版 reimburse_dialog_undo 口径）。 */
    fun undoReimbursed(item: Reimburse) {
        viewModelScope.launch { repo.updateReimburse(item.copy(reimburseState = 0, reimburseMoney = 0)) }
    }

    fun delete(item: Reimburse) = viewModelScope.launch { repo.deleteReimburse(item) }
}

class StatisticsViewModel(private val repo: BookkeepingRepository) : ViewModel() {
    /** 最近 6 个月记录（Flow），界面按年月分组算收支，口径与首页当月一致。 */
    val recentRecords = run {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -5); set(Calendar.DAY_OF_MONTH, 1) }
        val (start, _) = monthRange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        val (_, end) = monthRange(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH))
        // end 是当月结束，取当月范围的 end 需再推一个月，monthRange 已返回 [当月1日, 下月1日)
        repo.recordsInRange(start, end)
    }
}
