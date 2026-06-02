package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SyncLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentSyncLogs(): Flow<List<SyncLog>>

    @Query("SELECT * FROM sync_logs WHERE logType = :type ORDER BY timestamp DESC LIMIT 100")
    fun getLogsByType(type: String): Flow<List<SyncLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLog)

    @Query("DELETE FROM sync_logs")
    suspend fun clearAllLogs()
}
