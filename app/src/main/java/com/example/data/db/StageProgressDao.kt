package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StageProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StageProgressDao {
    @Query("SELECT * FROM stage_progress")
    fun getProgressList(): Flow<List<StageProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: StageProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<StageProgressEntity>)

    @Query("UPDATE stage_progress SET unlocked = 1 WHERE scenarioKey = :key")
    suspend fun unlockStage(key: String)

    @Query("UPDATE stage_progress SET completed = 1 WHERE scenarioKey = :key")
    suspend fun completeStage(key: String)

    @Query("DELETE FROM stage_progress")
    suspend fun resetProgress()
}
