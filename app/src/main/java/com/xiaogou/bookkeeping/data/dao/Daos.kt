package com.xiaogou.bookkeeping.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xiaogou.bookkeeping.data.entity.Assets
import com.xiaogou.bookkeeping.data.entity.AssetsModifyRecord
import com.xiaogou.bookkeeping.data.entity.AssetsTransferRecord
import com.xiaogou.bookkeeping.data.entity.Record
import com.xiaogou.bookkeeping.data.entity.RecordType
import kotlinx.coroutines.flow.Flow

/** 按分类汇总：typeId + 当月合计（分） */
data class TypeSum(val recordTypeId: Long, val total: Long)

@Dao
interface RecordDao {
    @Query("SELECT * FROM `Record` WHERE time BETWEEN :start AND :end ORDER BY time DESC, id DESC")
    fun recordsInRange(start: Long, end: Long): Flow<List<Record>>

    @Query("SELECT * FROM `Record` WHERE record_type_id = :typeId AND time BETWEEN :start AND :end ORDER BY time DESC, id DESC")
    fun recordsByType(typeId: Long, start: Long, end: Long): Flow<List<Record>>

    @Query("SELECT * FROM `Record` WHERE remark LIKE '%' || :query || '%' ORDER BY time DESC, id DESC LIMIT 200")
    fun searchByRemark(query: String): Flow<List<Record>>

    @Query("SELECT record_type_id AS recordTypeId, COALESCE(SUM(money), 0) AS total FROM `Record` WHERE time BETWEEN :start AND :end GROUP BY record_type_id")
    fun sumsByType(start: Long, end: Long): Flow<List<TypeSum>>

    @Query("SELECT * FROM `Record` WHERE id = :id")
    suspend fun getById(id: Long): Record?

    @Insert
    suspend fun insert(record: Record): Long

    @Update
    suspend fun update(record: Record)

    @Delete
    suspend fun delete(record: Record)
}

@Dao
interface RecordTypeDao {
    @Query("SELECT * FROM RecordType ORDER BY ranking ASC, id ASC")
    fun all(): Flow<List<RecordType>>

    @Query("SELECT * FROM RecordType WHERE type = :type AND state = 1 ORDER BY ranking ASC, id ASC")
    fun enabledByType(type: Int): Flow<List<RecordType>>

    @Query("SELECT * FROM RecordType WHERE id = :id")
    suspend fun getById(id: Long): RecordType?

    @Insert
    suspend fun insert(type: RecordType): Long

    @Update
    suspend fun update(type: RecordType)

    @Query("SELECT COUNT(*) FROM RecordType")
    suspend fun count(): Int
}

@Dao
interface AssetsDao {
    @Query("SELECT * FROM Assets WHERE state = 1 ORDER BY ranking ASC, id ASC")
    fun all(): Flow<List<Assets>>

    @Query("SELECT * FROM Assets WHERE id = :id")
    suspend fun getById(id: Long): Assets?

    @Insert
    suspend fun insert(assets: Assets): Long

    @Update
    suspend fun update(assets: Assets)

    @Query("UPDATE Assets SET money = :money WHERE id = :id")
    suspend fun updateMoney(id: Long, money: Long)
}

@Dao
interface AssetsModifyDao {
    @Query("SELECT * FROM AssetsModifyRecord WHERE assets_id = :assetsId ORDER BY create_time DESC, id DESC LIMIT 200")
    fun byAssets(assetsId: Long): Flow<List<AssetsModifyRecord>>

    @Insert
    suspend fun insert(record: AssetsModifyRecord)
}

@Dao
interface AssetsTransferDao {
    @Insert
    suspend fun insert(record: AssetsTransferRecord)

    @Delete
    suspend fun delete(record: AssetsTransferRecord)

    @Query("SELECT * FROM AssetsTransferRecord ORDER BY time DESC, id DESC LIMIT 200")
    fun all(): Flow<List<AssetsTransferRecord>>
}
