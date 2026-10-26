package com.xiaogou.bookkeeping.data

import androidx.room.withTransaction
import com.xiaogou.bookkeeping.data.entity.Assets
import com.xiaogou.bookkeeping.data.entity.AssetsModifyRecord
import com.xiaogou.bookkeeping.data.entity.AssetsTransferRecord
import com.xiaogou.bookkeeping.data.entity.Budget
import com.xiaogou.bookkeeping.data.entity.Record
import com.xiaogou.bookkeeping.data.entity.RecordType
import com.xiaogou.bookkeeping.data.entity.RecurringRecord
import com.xiaogou.bookkeeping.data.entity.Reimburse
import com.xiaogou.bookkeeping.domain.computeNextTime
import kotlinx.coroutines.flow.Flow

/**
 * Phase 1 业务口径：
 * - 记账与余额同步在同一个事务里：支出扣账户、收入加账户，同时写一条
 *   AssetsModifyRecord（money_before 为变动前余额）。
 * - 账户为空（assetsId == null）时只记账，不动任何账户。
 * - 删除记录时按原方向反向冲回余额（支出删除 → 加回，收入删除 → 扣回）。
 * - 转账在同一个事务里：转出扣、转入加、写转账记录、两边各写一条变动记录。
 * - 默认分类在首次打开空库时播种（Phase 1 播种清单，可在分类管理里改名/开关，
 *   不是旧版内置清单的逐一复现，旧库有数据时不播种）。
 */
class BookkeepingRepository(private val db: AppDatabase) {
    fun recordsInRange(start: Long, end: Long) = db.recordDao().recordsInRange(start, end)
    fun recordsByType(typeId: Long, start: Long, end: Long) = db.recordDao().recordsByType(typeId, start, end)
    fun searchByRemark(query: String) = db.recordDao().searchByRemark(query)
    fun searchByRemarkOrAsset(query: String) = db.recordDao().searchByRemarkOrAsset(query)
    fun sumsByType(start: Long, end: Long) = db.recordDao().sumsByType(start, end)
    fun allTypes(): Flow<List<RecordType>> = db.recordTypeDao().all()
    fun enabledTypes(type: Int): Flow<List<RecordType>> = db.recordTypeDao().enabledByType(type)
    fun allAssets(): Flow<List<Assets>> = db.assetsDao().all()
    fun allTransfers() = db.assetsTransferDao().all()
    fun modifyRecords(assetsId: Long) = db.assetsModifyDao().byAssets(assetsId)

    suspend fun typeById(id: Long) = db.recordTypeDao().getById(id)
    suspend fun assetsById(id: Long) = db.assetsDao().getById(id)

    suspend fun seedIfEmpty() {
        if (db.recordTypeDao().count() > 0) return
        val expense = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "教育", "其他")
        val income = listOf("工资", "兼职", "投资", "红包", "其他")
        expense.forEachIndexed { i, name ->
            db.recordTypeDao().insert(RecordType(name = name, imgName = "", type = 0, ranking = i, state = 1, parentId = -1))
        }
        income.forEachIndexed { i, name ->
            db.recordTypeDao().insert(RecordType(name = name, imgName = "", type = 1, ranking = i, state = 1, parentId = -1))
        }
    }

    suspend fun addRecord(moneyCents: Long, typeId: Long, type: Int, assetsId: Long?, remark: String, timeMs: Long) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            db.recordDao().insert(
                Record(money = moneyCents, remark = remark, time = timeMs, createTime = now, recordTypeId = typeId, assetsId = assetsId),
            )
            if (assetsId != null) {
                val assets = db.assetsDao().getById(assetsId) ?: return@withTransaction
                val before = assets.money
                // 支出扣、收入加
                val after = if (type == 0) before - moneyCents else before + moneyCents
                db.assetsDao().updateMoney(assetsId, after)
                db.assetsModifyDao().insert(
                    AssetsModifyRecord(assetsId = assetsId, money = after, moneyBefore = before, state = 1, createTime = now),
                )
            }
        }
    }

    suspend fun getRecord(id: Long) = db.recordDao().getById(id)

    /**
     * 编辑记录：先按旧记录反向冲回旧账户，再按新值重新记账，两步各写一条
     * 变动记录；账户不变时余额净效果就是新旧金额之差，不会双计。
     */
    suspend fun updateRecord(old: Record, moneyCents: Long, typeId: Long, type: Int, assetsId: Long?, remark: String, timeMs: Long) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val oldType = db.recordTypeDao().getById(old.recordTypeId)?.type ?: 0
            val oldMoney = old.money ?: 0L
            old.assetsId?.let { id ->
                val a = db.assetsDao().getById(id) ?: return@let
                val after = if (oldType == 0) a.money + oldMoney else a.money - oldMoney
                db.assetsDao().updateMoney(id, after)
                db.assetsModifyDao().insert(AssetsModifyRecord(assetsId = id, money = after, moneyBefore = a.money, state = 1, createTime = now))
            }
            db.recordDao().update(old.copy(money = moneyCents, recordTypeId = typeId, assetsId = assetsId, remark = remark, time = timeMs))
            assetsId?.let { id ->
                val a = db.assetsDao().getById(id) ?: return@let
                val after = if (type == 0) a.money - moneyCents else a.money + moneyCents
                db.assetsDao().updateMoney(id, after)
                db.assetsModifyDao().insert(AssetsModifyRecord(assetsId = id, money = after, moneyBefore = a.money, state = 1, createTime = now))
            }
        }
    }

    /** 删除转账：转出加回、转入扣回，再删转账记录。 */
    suspend fun deleteTransfer(record: AssetsTransferRecord) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val from = db.assetsDao().getById(record.assetsIdForm)
            val to = db.assetsDao().getById(record.assetsIdTo)
            if (from != null) {
                db.assetsDao().updateMoney(from.id, from.money + record.money)
                db.assetsModifyDao().insert(AssetsModifyRecord(assetsId = from.id, money = from.money + record.money, moneyBefore = from.money, state = 1, createTime = now))
            }
            if (to != null) {
                db.assetsDao().updateMoney(to.id, to.money - record.money)
                db.assetsModifyDao().insert(AssetsModifyRecord(assetsId = to.id, money = to.money - record.money, moneyBefore = to.money, state = 1, createTime = now))
            }
            db.assetsTransferDao().delete(record)
        }
    }

    suspend fun deleteRecord(record: Record) {
        db.withTransaction {
            db.recordDao().delete(record)
            val assetsId = record.assetsId
            val money = record.money ?: return@withTransaction
            if (assetsId != null) {
                val assets = db.assetsDao().getById(assetsId) ?: return@withTransaction
                val type = db.recordTypeDao().getById(record.recordTypeId)?.type ?: return@withTransaction
                val before = assets.money
                // 反向冲回：支出删除加回，收入删除扣回
                val after = if (type == 0) before + money else before - money
                db.assetsDao().updateMoney(assetsId, after)
                db.assetsModifyDao().insert(
                    AssetsModifyRecord(assetsId = assetsId, money = after, moneyBefore = before, state = 1, createTime = System.currentTimeMillis()),
                )
            }
        }
    }

    suspend fun addAssets(name: String, type: Int, initMoneyCents: Long, remark: String, imgName: String = ""): Long {
        val now = System.currentTimeMillis()
        val id = db.assetsDao().insert(
            Assets(name = name, imgName = imgName, type = type, state = 1, remark = remark, createTime = now, money = initMoneyCents, ranking = 0, initMoney = initMoneyCents),
        )
        db.assetsModifyDao().insert(
            AssetsModifyRecord(assetsId = id, money = initMoneyCents, moneyBefore = 0, state = 1, createTime = now),
        )
        return id
    }

    suspend fun transfer(fromId: Long, toId: Long, moneyCents: Long, remark: String) {
        require(fromId != toId) { "转出与转入账户不能相同" }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val from = db.assetsDao().getById(fromId) ?: return@withTransaction
            val to = db.assetsDao().getById(toId) ?: return@withTransaction
            db.assetsDao().updateMoney(fromId, from.money - moneyCents)
            db.assetsDao().updateMoney(toId, to.money + moneyCents)
            db.assetsModifyDao().insert(AssetsModifyRecord(assetsId = fromId, money = from.money - moneyCents, moneyBefore = from.money, state = 1, createTime = now))
            db.assetsModifyDao().insert(AssetsModifyRecord(assetsId = toId, money = to.money + moneyCents, moneyBefore = to.money, state = 1, createTime = now))
            db.assetsTransferDao().insert(
                AssetsTransferRecord(state = 1, createTime = now, time = now, assetsIdForm = fromId, assetsIdTo = toId, remark = remark, money = moneyCents),
            )
        }
    }

    suspend fun addType(name: String, type: Int, ranking: Int = 99, imgName: String = "") {
        db.recordTypeDao().insert(RecordType(name = name, imgName = imgName, type = type, ranking = ranking, state = 1, parentId = -1))
    }

    suspend fun setTypeIcon(type: RecordType, imgName: String) {
        db.recordTypeDao().update(type.copy(imgName = imgName))
    }

    /** 删分类：有记录或有子类型时返回 false（不删），由界面提示。 */
    suspend fun deleteType(type: RecordType): Boolean {
        if (db.recordDao().countByType(type.id) > 0) return false
        if (db.recordTypeDao().countByParent(type.id) > 0) return false
        db.recordTypeDao().delete(type)
        return true
    }

    /** 交换两个分类的排序值（分类管理上移 / 下移用）。 */
    suspend fun swapTypeRanking(a: RecordType, b: RecordType) {
        db.recordTypeDao().update(a.copy(ranking = b.ranking))
        db.recordTypeDao().update(b.copy(ranking = a.ranking))
    }

    fun recordsByAsset(assetsId: Long) = db.recordDao().recordsByAsset(assetsId)

    /** 删账户：名下有记账记录时返回 false（外键会挡住，界面先提示）；否则删账户，变动/转账记录按外键级联删除。 */
    suspend fun deleteAssets(asset: Assets): Boolean {
        if (db.recordDao().countByAsset(asset.id) > 0) return false
        db.assetsDao().delete(asset)
        return true
    }

    suspend fun deleteModifyRecord(record: AssetsModifyRecord) {
        db.assetsModifyDao().delete(record)
    }

    suspend fun setTypeEnabled(type: RecordType, enabled: Boolean) {
        db.recordTypeDao().update(type.copy(state = if (enabled) 1 else 0))
    }

    suspend fun renameType(type: RecordType, newName: String) {
        db.recordTypeDao().update(type.copy(name = newName))
    }

    // ---------- Phase 2：预算 ----------
    fun budgetsByMonth(month: Long) = db.budgetDao().byMonth(month)
    suspend fun budgetsListByMonth(month: Long) = db.budgetDao().listByMonth(month)
    suspend fun addBudget(budget: Budget) = db.budgetDao().insert(budget)
    suspend fun deleteBudget(budget: Budget) = db.budgetDao().delete(budget)

    // ---------- Phase 2：报销 ----------
    fun allReimburses() = db.reimburseDao().all()
    suspend fun addReimburse(reimburse: Reimburse) = db.reimburseDao().insert(reimburse)
    suspend fun updateReimburse(reimburse: Reimburse) = db.reimburseDao().update(reimburse)
    suspend fun deleteReimburse(reimburse: Reimburse) = db.reimburseDao().delete(reimburse)

    // ---------- Phase 2：周期记账 ----------
    fun allRecurring() = db.recurringDao().all()
    suspend fun addRecurring(rule: RecurringRecord) = db.recurringDao().insert(rule)
    suspend fun updateRecurring(rule: RecurringRecord) = db.recurringDao().update(rule)
    suspend fun deleteRecurring(rule: RecurringRecord) = db.recurringDao().delete(rule)

    /**
     * 生成所有到期规则，补跑上限 100 次/规则（旧版 MAX_CATCHUP 口径）。
     * 每条生成记录复用 addRecord 的余额同步逻辑，时间取规则当次执行时间。
     * 返回生成条数。
     */
    suspend fun generateDue(nowMs: Long): Int {
        var generated = 0
        val due = db.recurringDao().due(nowMs)
        for (rule in due) {
            var next = rule.nextTime
            var guard = 0
            var last: Long? = rule.lastTime
            var currentNext = next
            while (next <= nowMs && guard < 100) {
                val end = rule.endTime
                if (end != null && next > end) break
                // 直接记账：不走 addRecord 的“当前时间”版本，时间用规则执行时间
                db.withTransaction {
                    val now = System.currentTimeMillis()
                    db.recordDao().insert(
                        Record(
                            money = rule.money, remark = rule.remark, time = next,
                            createTime = now, recordTypeId = rule.recordTypeId,
                            assetsId = if (rule.assetsId >= 0) rule.assetsId else null,
                        ),
                    )
                    if (rule.assetsId >= 0) {
                        val assets = db.assetsDao().getById(rule.assetsId)
                        if (assets != null) {
                            val before = assets.money
                            val after = if (rule.recordType == 0) before - rule.money else before + rule.money
                            db.assetsDao().updateMoney(rule.assetsId, after)
                            db.assetsModifyDao().insert(
                                AssetsModifyRecord(assetsId = rule.assetsId, money = after, moneyBefore = before, state = 1, createTime = now),
                            )
                        }
                    }
                }
                last = next
                generated++
                guard++
                next = computeNextTime(next, rule.cycleType, rule.cycleValue)
            }
            currentNext = next
            db.recurringDao().update(rule.copy(nextTime = currentNext, lastTime = last))
        }
        return generated
    }

    // ---------- Phase 2：子类型 ----------
    suspend fun addSubtype(parentId: Long, name: String, type: Int) {
        db.recordTypeDao().insert(RecordType(name = name, imgName = "", type = type, ranking = 99, state = 1, parentId = parentId))
    }

}
