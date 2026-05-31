package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stage_progress")
data class StageProgressEntity(
    @PrimaryKey val scenarioKey: String,
    val unlocked: Boolean,
    val completed: Boolean
)
