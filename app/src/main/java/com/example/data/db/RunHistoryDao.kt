package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RunHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunHistoryDao {
    @Query("SELECT * FROM run_history ORDER BY date DESC")
    fun getAllRuns(): Flow<List<RunHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunHistoryEntity)

    @Query("DELETE FROM run_history")
    suspend fun clearAllRuns()
}
