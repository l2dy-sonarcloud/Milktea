package net.pantasystem.milktea.data.infrastructure.drive

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DriveFileRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(record: DriveFileRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(records: List<DriveFileRecord>)

    @Update
    abstract suspend fun update(record: DriveFileRecord)

    @Query("select * from drive_file_v1 where relatedAccountId=:accountId and serverId=:id")
    abstract suspend fun findOne(accountId: Long, id: String): DriveFileRecord?

    @Query("delete from drive_file_v1 where relatedAccountId=:accountId and serverId=:id")
    abstract suspend fun delete(accountId: Long, id: String)

    @Query("select * from drive_file_v1 where relatedAccountId=:accountId and serverId in (:serverIds)")
    abstract fun observeIn(accountId: Long, serverIds: List<String>): Flow<List<DriveFileRecord>>

    @Query("select * from drive_file_v1 where relatedAccountId=:accountId and serverId=:id")
    abstract fun observe(accountId: Long, id: String): Flow<DriveFileRecord?>

    @Query("select * from drive_file_v1 where relatedAccountId=:accountId and serverId in (:serverIds)")
    abstract suspend fun findIn(accountId: Long, serverIds: List<String>): List<DriveFileRecord>
}