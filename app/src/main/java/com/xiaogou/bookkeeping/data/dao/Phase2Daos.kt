package com.xiaogou.bookkeeping.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xiaogou.bookkeeping.data.entity.Budget
import com.xiaogou.bookkeeping.data.entity.RecurringRecord
import com.xiaogou.bookkeeping.data.entity.Reimburse
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM Budget WHERE month = :month ORDER BY id ASC")
    fun byMonth(month: Long): Flow<List<Budget>>

    @Query("SELECT * FROM Budget WHERE month = :month")
    suspend fun listByMonth(month: Long): List<Budget>

    @Insert
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}

@Dao
interface ReimburseDao {
    @Query("SELECT * FROM Reimburse ORDER BY time DESC, id DESC")
    fun all(): Flow<List<Reimburse>>

    @Insert
    suspend fun insert(reimburse: Reimburse): Long

    @Update
    suspend fun update(reimburse: Reimburse)

    @Delete
    suspend fun delete(reimburse: Reimburse)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM RecurringRecord ORDER BY next_time ASC")
    fun all(): Flow<List<RecurringRecord>>

    @Query("SELECT * FROM RecurringRecord WHERE state = 0 AND next_time <= :now AND (end_time IS NULL OR end_time >= :now) ORDER BY next_time ASC")
    suspend fun due(now: Long): List<RecurringRecord>

    @Insert
    suspend fun insert(rule: RecurringRecord): Long

    @Update
    suspend fun update(rule: RecurringRecord)

    @Delete
    suspend fun delete(rule: RecurringRecord)
}
