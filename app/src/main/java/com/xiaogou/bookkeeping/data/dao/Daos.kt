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

    @Query("SELECT * FROM `Record` ORDER BY time DESC, id DESC")
    fun allRecords(): Flow<List<Record>>

    @Query("SELECT * FROM `Record` WHERE record_type_id = :typeId AND time BETWEEN :start AND :end ORDER BY time DESC, id DESC")
    fun recordsByType(typeId: Long, start: Long, end: Long): Flow<List<Record>>

    @Query("SELECT * FROM `Record` WHERE remark LIKE '%' || :query || '%' ORDER BY time DESC, id DESC LIMIT 200")
    fun searchByRemark(query: String): Flow<List<Record>>

    /** 搜索：备注或账户名命中都算（fragment_search 口径：结果带账户名）。 */
    @Query(
        "SELECT `Record`.* FROM `Record` LEFT JOIN `Assets` ON `Record`.assets_id = `Assets`.id " +
            "WHERE `Record`.remark LIKE '%' || :query || '%' OR `Assets`.name LIKE '%' || :query || '%' " +
            "ORDER BY `Record`.time DESC, `Record`.id DESC LIMIT 200",
    )
    fun searchByRemarkOrAsset(query: String): Flow<List<Record>>

    @Query("SELECT record_type_id AS recordTypeId, COALESCE(SUM(money), 0) AS total FROM `Record` WHERE time BETWEEN :start AND :end GROUP BY record_type_id")
    fun sumsByType(start: Long, end: Long): Flow<List<TypeSum>>

    @Query("SELECT * FROM `Record` WHERE assets_id = :assetsId ORDER BY time DESC, id DESC LIMIT 200")
    fun recordsByAsset(assetsId: Long): Flow<List<Record>>

    @Query("SELECT COUNT(*) FROM `Record` WHERE record_type_id = :typeId")
    suspend fun countByType(typeId: Long): Int

    @Query("SELECT COUNT(*) FROM `Record` WHERE assets_id = :assetsId")
    suspend fun countByAsset(assetsId: Long): Int

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

    @Query("SELECT COUNT(*) FROM RecordType WHERE parent_id = :parentId")
    suspend fun countByParent(parentId: Long): Int

    @Delete
    suspend fun delete(type: RecordType)
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

    @Delete
    suspend fun delete(assets: Assets)
}

@Dao
interface AssetsModifyDao {
    @Query("SELECT * FROM AssetsModifyRecord WHERE assets_id = :assetsId ORDER BY create_time DESC, id DESC LIMIT 200")
    fun byAssets(assetsId: Long): Flow<List<AssetsModifyRecord>>

    @Insert
    suspend fun insert(record: AssetsModifyRecord)

    @Delete
    suspend fun delete(record: AssetsModifyRecord)
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
