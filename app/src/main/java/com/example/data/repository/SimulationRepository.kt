package com.example.data.repository

import com.example.data.db.RunHistoryDao
import com.example.data.db.StageProgressDao
import com.example.data.model.RunHistoryEntity
import com.example.data.model.StageProgressEntity
import kotlinx.coroutines.flow.Flow

class SimulationRepository(
    private val runHistoryDao: RunHistoryDao,
    private val stageProgressDao: StageProgressDao
) {
    val allRuns: Flow<List<RunHistoryEntity>> = runHistoryDao.getAllRuns()
    val allProgress: Flow<List<StageProgressEntity>> = stageProgressDao.getProgressList()

    suspend fun insertRun(run: RunHistoryEntity) {
        runHistoryDao.insertRun(run)
    }

    suspend fun clearAllRuns() {
        runHistoryDao.clearAllRuns()
    }

    suspend fun unlockStage(key: String) {
        stageProgressDao.unlockStage(key)
    }

    suspend fun completeStage(key: String) {
        stageProgressDao.completeStage(key)
    }

    suspend fun resetProgress() {
        stageProgressDao.resetProgress()
        stageProgressDao.insertProgressList(
            listOf(
                StageProgressEntity("normal", unlocked = true, completed = false),
                StageProgressEntity("bbm", unlocked = false, completed = false),
                StageProgressEntity("elnino_bbm", unlocked = false, completed = false),
                StageProgressEntity("overfished", unlocked = false, completed = false)
            )
        )
    }
}
